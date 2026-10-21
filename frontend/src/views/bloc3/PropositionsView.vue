<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useBloc3Store } from '@/stores/bloc3'
import StatutBadge from '@/components/bloc3/StatutBadge.vue'

const store = useBloc3Store()
const filtreStatut = ref('')

const statuts = [
  'BROUILLON',
  'VALIDEE',
  'EMISE',
  'EN_NEGOCIATION',
  'REVISE',
  'ACCEPTEE',
  'REFUSEE',
  'EXPIREE',
  'ANNULEE',
  'SUPERSEDEE',
]

onMounted(() => charger())

watch(filtreStatut, () => charger())

async function charger() {
  await store.chargerPropositions(filtreStatut.value ? { statut: filtreStatut.value } : {})
}

const formatMoney = (valeur) =>
  new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(valeur ?? 0)

const formatDate = (valeur) => (valeur ? new Date(valeur).toLocaleDateString('fr-FR') : '—')
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Propositions d'embauche</h1>
        <p>Chaque négociation acceptée génère une nouvelle version : l'historique n'est jamais écrasé.</p>
      </div>
      <div class="b3-actions">
        <select v-model="filtreStatut" class="b3-btn" style="padding: 0.4rem 0.6rem">
          <option value="">Tous les statuts</option>
          <option v-for="statut in statuts" :key="statut" :value="statut">{{ statut }}</option>
        </select>
        <button class="b3-btn" :disabled="store.loading" @click="charger">Actualiser</button>
      </div>
    </header>

    <div v-if="store.error" class="b3-alert is-error">{{ store.error }}</div>

    <div class="b3-card">
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Version</th>
              <th>Candidat</th>
              <th>Poste</th>
              <th>Salaire</th>
              <th>Contrat</th>
              <th>Émise le</th>
              <th>Validité</th>
              <th>Statut</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="proposition in store.propositions" :key="proposition.id">
              <td>#{{ proposition.id }}</td>
              <td>v{{ proposition.version }}</td>
              <td>
                {{ proposition.candidatPrenom }} {{ proposition.candidatNom }}
                <div class="b3-muted" style="font-size: 0.78rem">{{ proposition.offreTitre }}</div>
              </td>
              <td>{{ proposition.posteTitre ?? '—' }}</td>
              <td class="b3-money">{{ formatMoney(proposition.salaireBase) }}</td>
              <td>{{ proposition.typeContrat }}</td>
              <td>{{ formatDate(proposition.dateEmission) }}</td>
              <td>{{ formatDate(proposition.dateLimiteValidite) }}</td>
              <td><StatutBadge :statut="proposition.statut" /></td>
              <td>
                <RouterLink class="b3-btn" :to="`/bloc3/propositions/${proposition.id}`">Ouvrir</RouterLink>
              </td>
            </tr>
            <tr v-if="!store.propositions.length">
              <td colspan="10" class="b3-empty">Aucune proposition pour ce filtre.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
