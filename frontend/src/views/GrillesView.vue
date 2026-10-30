<script setup>
import { computed, onMounted, ref } from 'vue'
import { useDemandeStore } from '@/stores/demandes'
import { useToastStore } from '@/stores/toast'
import { ar, date } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'

const store = useDemandeStore()
const toast = useToastStore()
const chargement = ref(true)
const poste = ref('TOUS')
const historique = ref(false)

onMounted(async () => {
  try {
    await store.chargerReferentiels(true)
  } catch (e) {
    toast.error(e.message)
  } finally {
    chargement.value = false
  }
})

const aujourdhui = new Date().toISOString().slice(0, 10)
const enVigueur = (g) => g.dateDebut <= aujourdhui && (!g.dateFin || g.dateFin >= aujourdhui)

const lignes = computed(() =>
  store.grilles.filter((g) => (poste.value === 'TOUS' || g.idPoste === poste.value) && (historique.value || enVigueur(g)))
)
</script>

<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">Grille salariale</h1>
      <p class="page-sub">Fourchettes mensuelles de référence par poste et par niveau. Elles servent de base au chiffrage du RH.</p>
    </div>
  </div>

  <div class="toolbar">
    <select v-model="poste" class="select sel">
      <option value="TOUS">Tous les postes</option>
      <option v-for="p in store.postes" :key="p.id" :value="p.id">{{ p.libelle }}</option>
    </select>
    <label class="check"><input v-model="historique" type="checkbox" /> Afficher l’historique des grilles</label>
  </div>

  <div class="card">
    <div v-if="chargement" class="empty">Chargement…</div>
    <div v-else-if="!lignes.length" class="empty"><AppIcon name="coins" :size="40" /><p>Aucune grille à afficher.</p></div>
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr><th>Poste</th><th>Niveau</th><th class="num">Salaire min.</th><th class="num">Salaire max.</th><th>Valable du</th><th>Au</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="g in lignes" :key="g.idGrille">
            <td><strong>{{ g.poste }}</strong></td>
            <td><span class="badge plain tone-primary">{{ g.niveau }}</span></td>
            <td class="num">{{ ar(g.salaireMin) }}</td>
            <td class="num">{{ ar(g.salaireMax) }}</td>
            <td>{{ date(g.dateDebut) }}</td>
            <td>{{ g.dateFin ? date(g.dateFin) : '—' }}</td>
            <td><span v-if="enVigueur(g)" class="badge tone-success">En vigueur</span><span v-else class="badge tone-neutral">Archivée</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
  <p class="hint note">Montants mensuels en ariary. Pour une création de poste, le RH définit la nouvelle fourchette au moment de son avis ; pour un remplacement, la grille existante est reprise.</p>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 1.2rem; margin-bottom: 1rem; flex-wrap: wrap; }
.sel { max-width: 260px; }
.check { display: flex; align-items: center; gap: .5rem; cursor: pointer; font-weight: 500; }
.note { margin-top: .9rem; }
</style>
