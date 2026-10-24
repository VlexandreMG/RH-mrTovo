<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useBloc3Store } from '@/stores/bloc3'
import StatutBadge from '@/components/bloc3/StatutBadge.vue'

const store = useBloc3Store()

onMounted(() => {
  store.chargerTableauDeBord()
})

const kpis = computed(() => {
  const dashboard = store.dashboard
  if (!dashboard) return []
  return [
    { label: 'Candidatures retenues', valeur: dashboard.candidaturesRetenues, hint: 'à traiter' },
    { label: 'Propositions en cours', valeur: dashboard.propositionsEnCours, hint: 'brouillon → émise' },
    { label: 'Propositions émises', valeur: dashboard.propositionsEmises, hint: 'en attente du candidat' },
    { label: 'Négociations en cours', valeur: dashboard.negotiationsEnCours, hint: 'à arbitrer' },
    { label: 'Propositions acceptées', valeur: dashboard.propositionsAcceptees, hint: 'embauches' },
    { label: 'Employés créés', valeur: dashboard.employesCrees, hint: 'dossiers salariés' },
    { label: 'Contrats à signer', valeur: dashboard.contratsASigner, hint: 'en attente signature' },
    { label: 'Statut candidature', valeur: 'Bloc 2', hint: 'entrée du module' },
  ]
})

const repartition = computed(() => {
  const dashboard = store.dashboard
  if (!dashboard) return []
  const entries = Object.entries(dashboard.repartitionPropositions)
  const max = Math.max(1, ...entries.map(([, valeur]) => valeur))
  return entries.map(([statut, valeur]) => ({
    statut,
    valeur,
    pourcentage: Math.round((valeur / max) * 100),
  }))
})

const formatMoney = (valeur) =>
  new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(valeur ?? 0)
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Tableau de bord — Bloc 3</h1>
        <p>Sélection et embauche : pipeline des propositions, négociations et dossiers salariés.</p>
      </div>
      <button class="b3-btn" :disabled="store.loading" @click="store.chargerTableauDeBord()">
        Actualiser
      </button>
    </header>

    <div v-if="store.error" class="b3-alert is-error">{{ store.error }}</div>

    <div class="b3-grid b3-grid-4" style="margin-bottom: 1.25rem">
      <article v-for="kpi in kpis" :key="kpi.label" class="b3-kpi">
        <span>{{ kpi.label }}</span>
        <strong>{{ kpi.valeur }}</strong>
        <small>{{ kpi.hint }}</small>
      </article>
    </div>

    <div class="b3-grid b3-grid-2">
      <div class="b3-card">
        <h2>Répartition des propositions par statut</h2>
        <div class="b3-bar-list">
          <div v-for="ligne in repartition" :key="ligne.statut" class="b3-bar-row">
            <StatutBadge :statut="ligne.statut" />
            <div class="b3-bar-track">
              <div class="b3-bar-fill" :style="{ width: `${ligne.pourcentage}%` }"></div>
            </div>
            <strong>{{ ligne.valeur }}</strong>
          </div>
        </div>
      </div>

      <div class="b3-card">
        <h2>Dernières propositions</h2>
        <div class="b3-table-wrapper">
          <table class="b3-table">
            <thead>
              <tr>
                <th>Candidat</th>
                <th>Poste</th>
                <th>Salaire</th>
                <th>Statut</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="proposition in store.dashboard?.dernieresPropositions ?? []" :key="proposition.id">
                <td>{{ proposition.candidatPrenom }} {{ proposition.candidatNom }}</td>
                <td>{{ proposition.posteTitre ?? '—' }}</td>
                <td class="b3-money">{{ formatMoney(proposition.salaireBase) }}</td>
                <td><StatutBadge :statut="proposition.statut" /></td>
                <td>
                  <RouterLink class="b3-btn" :to="`/bloc3/propositions/${proposition.id}`">Ouvrir</RouterLink>
                </td>
              </tr>
              <tr v-if="!(store.dashboard?.dernieresPropositions ?? []).length">
                <td colspan="5" class="b3-empty">Aucune proposition enregistrée.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </section>
</template>
