<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDemandeStore } from '@/stores/demandes'
import { useToastStore } from '@/stores/toast'
import { A_TRAITER, STATUTS, TYPES_BESOIN, ar } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const auth = useAuthStore()
const store = useDemandeStore()
const toast = useToastStore()
const router = useRouter()

const filtre = ref('TOUTES')
const recherche = ref('')
const chargement = ref(true)

onMounted(async () => {
  try {
    await store.charger()
  } catch (e) {
    toast.error(e.message)
  } finally {
    chargement.value = false
  }
})

const filtres = computed(() => [
  { cle: 'TOUTES', label: 'Toutes' },
  ...(A_TRAITER[auth.role].length ? [{ cle: 'A_TRAITER', label: 'À traiter' }] : []),
  { cle: 'EN_COURS', label: 'En cours' },
  { cle: 'RECRUTEMENT_OUVERT', label: STATUTS.RECRUTEMENT_OUVERT.label },
  { cle: 'REFUSEE', label: STATUTS.REFUSEE.label }
])

const compte = (cle) => store.demandes.filter((d) => correspond(d, cle)).length

function correspond(d, cle) {
  if (cle === 'TOUTES') return true
  if (cle === 'A_TRAITER') return A_TRAITER[auth.role].includes(d.statut)
  if (cle === 'EN_COURS') return d.statut.startsWith('EN_ATTENTE') || d.statut === 'AUTORISEE'
  return d.statut === cle
}

const visibles = computed(() => {
  const q = recherche.value.trim().toLowerCase()
  return store.demandes.filter(
    (d) => correspond(d, filtre.value) &&
      (!q || `${d.idDemande} ${d.poste} ${d.departement} ${d.motif} ${d.manager}`.toLowerCase().includes(q))
  )
})
</script>

<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">Demandes de recrutement</h1>
      <p class="page-sub">{{ auth.role === 'MANAGER' ? 'Les demandes de votre département et leur avancement' : 'Toutes les demandes du circuit de validation.' }}</p>
    </div>
    <button v-if="auth.role === 'MANAGER'" class="btn" @click="router.push('/demandes/nouvelle')">
      <AppIcon name="plus" /> Nouvelle demande
    </button>
  </div>

  <div class="toolbar">
    <div class="chips">
      <button v-for="f in filtres" :key="f.cle" class="chip" :class="{ on: filtre === f.cle }" @click="filtre = f.cle">
        {{ f.label }} <span>{{ compte(f.cle) }}</span>
      </button>
    </div>
    <input v-model="recherche" class="input search" placeholder="Rechercher une demande…" />
  </div>

  <div class="card">
    <div v-if="chargement" class="empty">Chargement…</div>
    <div v-else-if="!visibles.length" class="empty">
      <AppIcon name="inbox" :size="40" />
      <p>Aucune demande ne correspond.</p>
    </div>
    <div v-else class="table-wrap">
      <table class="table list">
        <thead>
          <tr>
            <th>N°</th><th>Poste</th><th>Département</th><th>Type</th>
            <th class="num">Effectif</th><th class="num">Budget demandé</th><th>Statut</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in visibles" :key="d.idDemande" class="clickable" @click="router.push(`/demandes/${d.idDemande}`)">
            <td class="mono"><strong>#{{ d.idDemande }}</strong></td>
            <td>
              <strong>{{ d.poste }}</strong>
              <div class="motif">{{ d.motif }}</div>
            </td>
            <td>{{ d.departement }}</td>
            <td><span class="badge plain" :class="d.typeBesoin === 'CREATION' ? 'tone-violet' : 'tone-info'">{{ TYPES_BESOIN[d.typeBesoin] }}</span></td>
            <td class="num">{{ d.effectifTotal }}</td>
            <td class="num">{{ ar(d.budgetDemandeTotal) }}</td>
            <td><StatusBadge :statut="d.statut" /></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
.chips { display: flex; gap: .5rem; flex-wrap: wrap; }
.chip { display: inline-flex; align-items: center; gap: .5rem; padding: .45rem .9rem; border-radius: 999px; border: 1px solid var(--border); background: var(--surface); cursor: pointer; font-weight: 600; font-size: .85rem; color: #4c5273; transition: all .15s; }
.chip span { background: #eceef7; border-radius: 999px; padding: 0 .5rem; font-size: .75rem; }
.chip:hover { border-color: #c5c8e6; }
.chip.on { background: var(--primary); border-color: var(--primary); color: #fff; }
.chip.on span { background: rgba(255, 255, 255, .25); }
.search { max-width: 340px; }
.motif { color: var(--muted); font-size: .82rem; max-width: 210px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.list th, .list td { padding-left: .7rem; padding-right: .7rem; }
.list th:first-child, .list td:first-child { padding-left: 1.2rem; }
.list td { white-space: nowrap; }
</style>
