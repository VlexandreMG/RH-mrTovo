<script setup>
import { computed, ref, watch } from 'vue'
import { tempsApi } from '@/api/temps'
import { useToastStore } from '@/stores/toast'
import { duree } from '@/utils/format'

const props = defineProps({
  semaine: { type: Object, required: true }
})
const emit = defineEmits(['decidee'])

const toast = useToastStore()
// Le formulaire peut apparaître plusieurs fois sur une page : les identifiants doivent rester uniques.
const prefixe = computed(() => `decision-${props.semaine.employeId}-${props.semaine.semaineDebut}`)
const heures = ref(0)
const minutes = ref(0)
const commentaire = ref('')
const envoi = ref(false)

function initialiser() {
  const brutes = props.semaine.decision.minutesBrutes
  heures.value = Math.floor(brutes / 60)
  minutes.value = brutes % 60
  commentaire.value = ''
}

watch(() => props.semaine, initialiser, { immediate: true })

async function decider(decision) {
  envoi.value = true
  try {
    const semaine = await tempsApi.deciderHeuresSup({
      employeId: props.semaine.employeId,
      semaineDebut: props.semaine.semaineDebut,
      decision,
      minutesValidees: decision === 'VALIDEE' ? heures.value * 60 + minutes.value : 0,
      commentaire: commentaire.value
    })
    toast.success(decision === 'VALIDEE' ? 'Heures supplémentaires validées.' : 'Heures supplémentaires refusées.')
    emit('decidee', semaine)
  } catch (e) {
    toast.error(e.message)
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <form class="decision" @submit.prevent="decider('VALIDEE')">
    <p class="b3-muted">Constaté : {{ duree(semaine.decision.minutesBrutes) }}</p>
    <div class="b3-form-grid">
      <div class="b3-field">
        <label :for="`${prefixe}-heures`">Durée validée</label>
        <div class="duree">
          <input :id="`${prefixe}-heures`" v-model.number="heures" type="number" min="0" aria-label="Heures" /> h
          <input v-model.number="minutes" type="number" min="0" max="59" aria-label="Minutes" /> min
        </div>
      </div>
      <div class="b3-field">
        <label :for="`${prefixe}-commentaire`">Commentaire (obligatoire si refus ou validation partielle)</label>
        <input :id="`${prefixe}-commentaire`" v-model="commentaire" maxlength="255" />
      </div>
    </div>
    <div class="b3-actions actions">
      <button type="button" class="b3-btn is-danger" :disabled="envoi" @click="decider('REFUSEE')">Refuser</button>
      <button type="submit" class="b3-btn is-primary" :disabled="envoi">Valider</button>
    </div>
  </form>
</template>

<style scoped>
.decision {
  margin-top: 0.5rem;
}

.duree {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.85rem;
}

.duree input {
  width: 4.5rem;
}

.actions {
  justify-content: flex-end;
  margin-top: 0.8rem;
}
</style>
