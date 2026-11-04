<script setup>
import { onMounted, ref } from 'vue'
import { tempsApi } from '@/api/temps'
import { date as formatDate, duree } from '@/utils/format'
import DecisionHeuresSup from './DecisionHeuresSup.vue'

const emit = defineEmits(['ouvrir'])

const semaines = ref([])
const chargement = ref(false)
const erreur = ref('')

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    semaines.value = await tempsApi.heuresSupAValider()
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
  <div v-if="!chargement && !semaines.length && !erreur" class="b3-card b3-empty">
    Aucune heure supplémentaire en attente sur les 12 dernières semaines.
  </div>

  <div v-for="semaine in semaines" :key="`${semaine.employeId}-${semaine.semaineDebut}`" class="b3-card">
    <div class="entete">
      <h2>{{ semaine.matricule }} — {{ semaine.nomComplet }}</h2>
      <button class="b3-btn" @click="emit('ouvrir', { employeId: semaine.employeId, date: semaine.semaineDebut })">
        Voir la semaine
      </button>
    </div>
    <p class="b3-muted">
      Semaine du {{ formatDate(semaine.semaineDebut) }} · retards et départs anticipés :
      {{ duree(semaine.bilan.minutesDues) }}
    </p>
    <DecisionHeuresSup v-if="semaine.decision.validable" :semaine="semaine" @decidee="charger" />
    <p v-else class="b3-alert is-error">{{ semaine.decision.raisonBlocage }}</p>
  </div>
</template>

<style scoped>
.entete {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
</style>
