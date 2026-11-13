<script setup>
import { computed, onMounted, ref } from 'vue'
import { questionnaireApi, messageErreur } from '@/services/api'

const props = defineProps({ id: { type: [String, Number], required: true } })

const CHOIX_MIN = 2
const CHOIX_MAX = 6

const titre = ref('')
const questions = ref([])
const verrouille = ref(false)
const chargement = ref(true)
const enregistrement = ref(false)
const erreur = ref('')
const message = ref('')

const nouveauChoix = (correct = false) => ({ libelle: '', correct })
const nouvelleQuestion = () => ({ enonce: '', choix: [nouveauChoix(true), nouveauChoix()] })

/** Ce que le serveur refuserait : on le signale avant l'envoi, question par question. */
const problemes = computed(() => questions.value.flatMap((q, i) => {
  const liste = []
  if (!q.enonce.trim()) liste.push(`Question ${i + 1} : l'énoncé est vide.`)
  if (q.choix.some((c) => !c.libelle.trim())) liste.push(`Question ${i + 1} : un choix est vide.`)
  if (q.choix.filter((c) => c.correct).length !== 1) liste.push(`Question ${i + 1} : cochez la bonne réponse.`)
  return liste
}))

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    const q = await questionnaireApi.get(props.id)
    titre.value = q.offreTitre
    verrouille.value = q.verrouille
    questions.value = q.questions.length
      ? q.questions.map((question) => ({
          enonce: question.enonce,
          choix: question.choix.map((c) => ({ libelle: c.libelle, correct: c.correct })),
        }))
      : [nouvelleQuestion()]
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

function marquerBonne(question, index) {
  question.choix.forEach((c, i) => { c.correct = i === index })
}

function supprimerChoix(question, index) {
  const etaitBonne = question.choix[index].correct
  question.choix.splice(index, 1)
  if (etaitBonne) question.choix[0].correct = true
}

async function enregistrer() {
  message.value = ''
  erreur.value = ''
  if (problemes.value.length) {
    erreur.value = problemes.value[0]
    return
  }
  enregistrement.value = true
  try {
    await questionnaireApi.enregistrer(props.id, questions.value.map((q) => ({
      enonce: q.enonce.trim(),
      choix: q.choix.map((c) => ({ libelle: c.libelle.trim(), correct: c.correct })),
    })))
    message.value = `Questionnaire enregistré : ${questions.value.length} question(s).`
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    enregistrement.value = false
  }
}

onMounted(charger)
</script>

<template>
  <div class="questionnaire">
    <div class="entete">
      <RouterLink :to="`/offres/${id}/kanban`" class="retour">← Retour au suivi des candidatures</RouterLink>
      <h1>Questionnaire du test</h1>
      <p class="muted">{{ titre }} · QCM : une seule bonne réponse par question. La note sur 20 se calcule seule.</p>
    </div>

    <p v-if="chargement" class="muted">Chargement…</p>
    <template v-else>
      <p v-if="verrouille" class="bandeau verrou" role="status">
        Des candidats ont déjà répondu : le questionnaire est en lecture seule, pour que leurs notes restent justes.
      </p>

      <fieldset :disabled="verrouille" class="liste">
        <section v-for="(question, i) in questions" :key="i" class="question card">
          <div class="question-tete">
            <label :for="`enonce-${i}`" class="label">Question {{ i + 1 }}</label>
            <button
              v-if="questions.length > 1"
              type="button"
              class="lien danger"
              @click="questions.splice(i, 1)"
            >Supprimer la question</button>
          </div>
          <textarea :id="`enonce-${i}`" v-model="question.enonce" rows="2" placeholder="Énoncé de la question" />

          <p class="consigne muted">Cochez la bonne réponse.</p>
          <div v-for="(choix, j) in question.choix" :key="j" class="choix">
            <input
              :id="`bonne-${i}-${j}`"
              type="radio"
              :name="`bonne-${i}`"
              :checked="choix.correct"
              :aria-label="`Choix ${j + 1} : bonne réponse`"
              @change="marquerBonne(question, j)"
            />
            <input
              v-model="choix.libelle"
              type="text"
              class="libelle"
              :aria-label="`Question ${i + 1}, choix ${j + 1}`"
              :placeholder="`Choix ${j + 1}`"
            />
            <button
              v-if="question.choix.length > CHOIX_MIN"
              type="button"
              class="lien"
              :aria-label="`Retirer le choix ${j + 1}`"
              @click="supprimerChoix(question, j)"
            >Retirer</button>
          </div>
          <button
            v-if="question.choix.length < CHOIX_MAX"
            type="button"
            class="lien"
            @click="question.choix.push(nouveauChoix())"
          >+ Ajouter un choix</button>
        </section>
      </fieldset>

      <div v-if="!verrouille" class="pied">
        <button type="button" class="btn btn-ghost" @click="questions.push(nouvelleQuestion())">+ Ajouter une question</button>
        <button type="button" class="btn" :disabled="enregistrement" @click="enregistrer">Enregistrer le questionnaire</button>
      </div>
      <p v-if="erreur" class="error" role="alert">{{ erreur }}</p>
      <p v-if="message" class="succes" role="status">{{ message }}</p>
    </template>
  </div>
</template>

<style scoped>
.questionnaire { max-width: 860px; }
.entete { margin-bottom: 16px; }
.retour { font-size: 13px; color: var(--primary); text-decoration: none; }
h1 { font-size: 22px; margin: 6px 0 2px; }
.entete p { margin: 0; }
.liste { border: 0; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px; }
.question { padding: 16px; display: flex; flex-direction: column; gap: 8px; }
.question-tete { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.label { font-weight: 600; }
textarea, .libelle {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
}
textarea { resize: vertical; }
.consigne { font-size: 12px; margin: 4px 0 0; }
.choix { display: flex; align-items: center; gap: 8px; }
.choix input[type='radio'] { width: 18px; height: 18px; accent-color: var(--success); flex-shrink: 0; }
.lien { border: 0; padding: 0; background: none; color: var(--primary); font-size: 13px; cursor: pointer; align-self: flex-start; white-space: nowrap; }
.lien.danger { color: var(--danger); }
.choix .lien { align-self: center; }
fieldset:disabled .lien { display: none; }
.pied { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
.bandeau { padding: 10px 12px; border-radius: 10px; margin: 0 0 12px; }
.verrou { background: var(--warning-50); color: #8a5a06; }
.error { color: var(--danger); margin-top: 10px; }
.succes { color: var(--success); font-weight: 600; margin-top: 10px; }
</style>
