<script setup>
import { onMounted, ref } from 'vue'
import { useBloc3Store } from '@/stores/bloc3'
import api from '@/api/client'
import StatutBadge from '@/components/bloc3/StatutBadge.vue'

const store = useBloc3Store()
const erreur = ref(null)
const message = ref(null)

onMounted(() => store.chargerContrats())

const formatMoney = (valeur) =>
  new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(valeur ?? 0)

const formatDate = (valeur) => (valeur ? new Date(valeur).toLocaleDateString('fr-FR') : '—')

async function signer(contrat) {
  erreur.value = null
  message.value = null
  try {
    await api.signerContrat(contrat.id)
    message.value = `Contrat ${contrat.reference} signé.`
    await store.chargerContrats()
  } catch (exception) {
    erreur.value = exception.message
  }
}

async function activer(contrat) {
  erreur.value = null
  message.value = null
  try {
    await api.activerContrat(contrat.id)
    message.value = `Contrat ${contrat.reference} activé.`
    await store.chargerContrats()
  } catch (exception) {
    erreur.value = exception.message
  }
}
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Contrats</h1>
        <p>
          Cycle : à signer → signé → période d'essai → actif. Le contrat initial reprend les conditions de
          la proposition acceptée.
        </p>
      </div>
      <button class="b3-btn" :disabled="store.loading" @click="store.chargerContrats()">Actualiser</button>
    </header>

    <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>
    <div v-if="message" class="b3-alert is-success">{{ message }}</div>

    <div class="b3-card">
      <h2>Registre des contrats</h2>
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead>
            <tr>
              <th>Référence</th>
              <th>Employé</th>
              <th>Matricule</th>
              <th>Type</th>
              <th>Début</th>
              <th>Fin</th>
              <th>Salaire</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="contrat in store.contrats" :key="contrat.id">
              <td>{{ contrat.reference }}</td>
              <td>{{ contrat.employeNom ?? '—' }}</td>
              <td>{{ contrat.matricule ?? '—' }}</td>
              <td>{{ contrat.typeContrat }}</td>
              <td>{{ formatDate(contrat.dateDebut) }}</td>
              <td>{{ formatDate(contrat.dateFin) }}</td>
              <td class="b3-money">{{ formatMoney(contrat.salaire) }}</td>
              <td><StatutBadge :statut="contrat.statutContrat" /></td>
              <td>
                <div class="b3-actions">
                  <button v-if="contrat.statutContrat === 'A_SIGNER'" class="b3-btn is-primary" @click="signer(contrat)">
                    Signer
                  </button>
                  <button
                    v-if="['SIGNE', 'EN_PERIODE_DESSAI'].includes(contrat.statutContrat)"
                    class="b3-btn is-success"
                    @click="activer(contrat)"
                  >
                    Activer
                  </button>
                  <span
                    v-if="!['A_SIGNER', 'SIGNE', 'EN_PERIODE_DESSAI'].includes(contrat.statutContrat)"
                    class="b3-muted"
                  >
                    —
                  </span>
                </div>
              </td>
            </tr>
            <tr v-if="!store.contrats.length">
              <td colspan="9" class="b3-empty">Aucun contrat enregistré.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
