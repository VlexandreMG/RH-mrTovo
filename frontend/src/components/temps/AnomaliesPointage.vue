<script setup>
import { onMounted, ref } from 'vue'
import { tempsApi } from '@/api/temps'
import { date as formatDate, dateIso } from '@/utils/format'

const emit = defineEmits(['ouvrir'])

function ilYA(jours) {
  const jour = new Date()
  jour.setDate(jour.getDate() - jours)
  return dateIso(jour)
}

const du = ref(ilYA(30))
const au = ref(dateIso())
const anomalies = ref([])
const chargement = ref(false)
const erreur = ref('')

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    anomalies.value = await tempsApi.anomalies(du.value, au.value)
  } catch (e) {
    erreur.value = e.message
  } finally {
    chargement.value = false
  }
}

onMounted(charger)
</script>

<template>
  <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

  <div class="b3-card">
    <form class="filtres" @submit.prevent="charger">
      <div class="b3-field"><label for="du">Du</label><input id="du" v-model="du" type="date" required /></div>
      <div class="b3-field"><label for="au">Au</label><input id="au" v-model="au" type="date" required /></div>
      <button class="b3-btn is-primary" :disabled="chargement">Rechercher</button>
    </form>

    <h2>{{ anomalies.length }} anomalie(s)</h2>
    <div class="b3-table-wrapper">
      <table class="b3-table">
        <thead>
          <tr><th>Date</th><th>Salarié</th><th>Anomalie</th><th>Détail</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="anomalie in anomalies" :key="`${anomalie.employeId}-${anomalie.date}-${anomalie.type}`">
            <td>{{ formatDate(anomalie.date) }}</td>
            <td>{{ anomalie.matricule }} — {{ anomalie.nomComplet }}</td>
            <td>{{ anomalie.libelle }}</td>
            <td class="b3-muted">{{ anomalie.detail }}</td>
            <td>
              <button class="b3-btn" @click="emit('ouvrir', { employeId: anomalie.employeId, date: anomalie.date })">
                Corriger
              </button>
            </td>
          </tr>
          <tr v-if="!anomalies.length"><td colspan="5" class="b3-empty">Aucune anomalie sur cette période.</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.filtres {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1rem;
}
</style>
