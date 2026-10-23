<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDemandeStore } from '@/stores/demandes'
import { useToastStore } from '@/stores/toast'
import { A_TRAITER, ROLES, ar, date } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const auth = useAuthStore()
const store = useDemandeStore()
const toast = useToastStore()
const router = useRouter()
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

const demandes = computed(() => store.demandes)
const aTraiter = computed(() => demandes.value.filter((d) => A_TRAITER[auth.role].includes(d.statut)))
const enCours = computed(() => demandes.value.filter((d) => d.statut.startsWith('EN_ATTENTE')))
const ouvertes = computed(() => demandes.value.filter((d) => d.statut === 'RECRUTEMENT_OUVERT'))
const refusees = computed(() => demandes.value.filter((d) => d.statut === 'REFUSEE'))
const postesOuverts = computed(() => ouvertes.value.reduce((s, d) => s + d.effectifTotal, 0))
const recentes = computed(() => demandes.value.slice(0, 5))

const cartes = computed(() => [
  { label: auth.role === 'MANAGER' ? 'Demandes de mon département' : 'Demandes totales', valeur: demandes.value.length, icon: 'file', tone: 'primary' },
  { label: 'En cours de validation', valeur: enCours.value.length, icon: 'clock', tone: 'warning' },
  { label: 'Postes ouverts', valeur: postesOuverts.value, icon: 'rocket', tone: 'success' },
  { label: 'Demandes refusées', valeur: refusees.value.length, icon: 'x', tone: 'danger' }
])

const consigne = {
  MANAGER: 'Exprimez un nouveau besoin ou suivez l’avancement de vos demandes.',
  RH: 'Chiffrez les nouvelles demandes et ouvrez les recrutements autorisés.',
  FINANCE: 'Donnez votre avis budgétaire sur les demandes chiffrées par le RH.',
  DIRECTION: 'Tranchez les demandes qui ont reçu l’avis du RH et de la Finance.'
}
const prenom = computed(() => auth.user?.nom || '')
</script>

<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">Bonjour, {{ prenom }} 👋</h1>
      <p class="page-sub">{{ ROLES[auth.role] }} · {{ consigne[auth.role] }}</p>
    </div>
    <button v-if="auth.role === 'MANAGER'" class="btn" @click="router.push('/demandes/nouvelle')">
      <AppIcon name="plus" /> Nouvelle demande
    </button>
  </div>

  <div class="stats">
    <div v-for="c in cartes" :key="c.label" class="card stat">
      <span class="ico" :class="'tone-' + c.tone"><AppIcon :name="c.icon" :size="20" /></span>
      <div><strong>{{ c.valeur }}</strong><small>{{ c.label }}</small></div>
    </div>
  </div>

  <div class="cols">
    <section class="card">
      <div class="card-head">
        <h2><AppIcon name="alert" :size="18" /> À traiter par vous</h2>
        <span class="badge plain tone-primary">{{ aTraiter.length }}</span>
      </div>
      <div v-if="chargement" class="empty">Chargement…</div>
      <div v-else-if="!aTraiter.length" class="empty">
        <AppIcon name="check" :size="34" />
        <p>{{ auth.role === 'MANAGER' ? 'Vos demandes sont suivies par les autres services.' : 'Rien en attente, tout est à jour.' }}</p>
      </div>
      <ul v-else class="liste">
        <li v-for="d in aTraiter" :key="d.idDemande" @click="router.push(`/demandes/${d.idDemande}`)">
          <div class="lib">
            <strong>#{{ d.idDemande }} · {{ d.poste }}</strong>
            <small>{{ d.departement }} · {{ d.effectifTotal }} pers. · souhaité le {{ date(d.dateSouhaitee) }}</small>
          </div>
          <StatusBadge :statut="d.statut" />
          <AppIcon name="arrowRight" :size="16" />
        </li>
      </ul>
    </section>

    <section class="card">
      <div class="card-head">
        <h2><AppIcon name="history" :size="18" /> Activité récente</h2>
        <RouterLink to="/demandes" class="btn btn-ghost btn-sm">Tout voir</RouterLink>
      </div>
      <div v-if="chargement" class="empty">Chargement…</div>
      <div v-else-if="!recentes.length" class="empty">
        <AppIcon name="inbox" :size="34" /><p>Aucune demande pour le moment.</p>
      </div>
      <ul v-else class="liste">
        <li v-for="d in recentes" :key="d.idDemande" @click="router.push(`/demandes/${d.idDemande}`)">
          <div class="lib">
            <strong>#{{ d.idDemande }} · {{ d.poste }}</strong>
            <small>{{ d.effectifTotal }} pers. · budget {{ ar(d.budgetDemandeTotal) }}</small>
          </div>
          <StatusBadge :statut="d.statut" />
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1.4rem; }
.stat { display: flex; align-items: center; gap: 1rem; padding: 1.2rem; }
.ico { width: 46px; height: 46px; border-radius: 13px; display: grid; place-items: center; }
.stat strong { display: block; font-family: var(--font-display); font-size: 1.7rem; line-height: 1.1; font-weight: 800; }
.stat small { color: var(--muted); font-weight: 500; }
.cols { display: grid; grid-template-columns: 1fr 1fr; gap: 1.2rem; align-items: start; }
.liste { list-style: none; }
.liste li { display: flex; align-items: center; gap: 1rem; padding: .95rem 1.4rem; border-bottom: 1px solid var(--border); cursor: pointer; transition: background .12s; }
.liste li:last-child { border-bottom: none; }
.liste li:hover { background: var(--surface-2); }
.lib { flex: 1; min-width: 0; }
.lib strong { display: block; }
.lib small { color: var(--muted); }
.liste li > svg { color: #b3b7d6; }
@media (max-width: 1100px) { .stats { grid-template-columns: 1fr 1fr; } .cols { grid-template-columns: 1fr; } }
</style>
