<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, apiPost } from '@/api/http'

const route = useRoute()
const router = useRouter()

const test = ref(null)
const reponses = reactive({})
const chargement = ref(true)
const envoi = ref(false)
const erreur = ref('')
const termine = ref(false)

const nbRepondues = computed(() => Object.keys(reponses).length)
const complet = computed(() => test.value && nbRepondues.value === test.value.questions.length)

async function charger() {
  try {
    test.value = await apiGet(`/candidat/tests/${route.params.id}`)
  } catch (e) {
    erreur.value = e.message || 'Impossible de charger le test.'
  } finally {
    chargement.value = false
  }
}

async function envoyer() {
  if (!complet.value) return
  if (!window.confirm('Envoyer vos réponses ? Une seule tentative est possible : vous ne pourrez plus les modifier.')) return
  envoi.value = true
  erreur.value = ''
  try {
    await apiPost(`/candidat/tests/${route.params.id}/reponses`, {
      reponses: Object.entries(reponses).map(([questionId, choixId]) => ({
        questionId: Number(questionId),
        choixId,
      })),
    })
    termine.value = true
  } catch (e) {
    erreur.value = e.message || "L'envoi a échoué."
  } finally {
    envoi.value = false
  }
}

onMounted(charger)
</script>

<template>
  <section class="test">
    <button class="back" type="button" @click="router.push('/candidats/espace')">← Retour à mon espace</button>

    <p v-if="chargement" class="state">Chargement du test…</p>

    <div v-else-if="termine" class="state fini" role="status">
      <h1>Réponses envoyées</h1>
      <p>Merci ! Le recruteur étudie votre test et reviendra vers vous pour la suite.</p>
      <RouterLink class="primary" to="/candidats/espace">Revenir à mon espace</RouterLink>
    </div>

    <p v-else-if="!test" class="state error" role="alert">{{ erreur }}</p>

    <form v-else class="formulaire" @submit.prevent="envoyer">
      <header>
        <p class="eyebrow">Test de recrutement</p>
        <h1>{{ test.offreTitre }}</h1>
        <p>Une seule réponse par question. Vous ne pourrez envoyer vos réponses qu'une fois.</p>
      </header>

      <fieldset v-for="(question, i) in test.questions" :key="question.id" class="question">
        <legend><span class="numero">{{ i + 1 }}</span>{{ question.enonce }}</legend>
        <label v-for="choix in question.choix" :key="choix.id" class="choix" :class="{ coche: reponses[question.id] === choix.id }">
          <input v-model="reponses[question.id]" type="radio" :name="`question-${question.id}`" :value="choix.id" />
          <span>{{ choix.libelle }}</span>
        </label>
      </fieldset>

      <footer class="pied">
        <span class="progression" aria-live="polite">{{ nbRepondues }} / {{ test.questions.length }} réponses</span>
        <button type="submit" class="primary" :disabled="!complet || envoi">Envoyer mes réponses</button>
      </footer>
      <p v-if="erreur" class="error" role="alert">{{ erreur }}</p>
    </form>
  </section>
</template>

<style scoped>
.test { display: flex; flex-direction: column; gap: 1rem; max-width: 760px; margin: 0 auto; width: 100%; }
.back { align-self: flex-start; border: 0; background: none; color: #34348f; font-weight: 600; cursor: pointer; padding: 0; }
.state { padding: 1.3rem; border-radius: 14px; background: #fff; color: rgba(20,20,58,.8); }
.fini h1 { margin: 0 0 .5rem; color: #19733a; }
.formulaire { display: flex; flex-direction: column; gap: 1rem; }
header h1 { margin: 0 0 .35rem; color: #14143a; }
header p:last-child { margin: 0; color: rgba(20,20,58,.7); }
.eyebrow { margin: 0 0 .35rem; color: #5b5bd6; text-transform: uppercase; letter-spacing: .08em; font-size: .75rem; font-weight: 700; }
.question { margin: 0; padding: 1.1rem 1.3rem; border: 0; border-radius: 16px; background: #fff; box-shadow: 0 30px 55px -45px rgba(50,50,130,.55); display: grid; gap: .5rem; }
legend { float: left; width: 100%; padding: 0; margin-bottom: .4rem; font-weight: 700; color: #14143a; display: flex; gap: .6rem; }
.numero { flex-shrink: 0; width: 1.6rem; height: 1.6rem; border-radius: 50%; background: #edf0ff; color: #4242a5; display: inline-grid; place-items: center; font-size: .85rem; }
.choix { display: flex; align-items: center; gap: .7rem; padding: .7rem .9rem; border: 1px solid #e3e3f0; border-radius: 10px; cursor: pointer; color: rgba(20,20,58,.88); }
.choix.coche { border-color: #5151c7; background: #f4f4ff; }
.choix input { width: 1.1rem; height: 1.1rem; accent-color: #5151c7; flex-shrink: 0; }
.choix:focus-within { outline: 2px solid #5151c7; outline-offset: 2px; }
.pied { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; }
.progression { color: rgba(20,20,58,.7); font-weight: 600; }
.primary { padding: .75rem 1.2rem; border: 0; border-radius: 10px; background: #5151c7; color: #fff; font: inherit; font-weight: 700; cursor: pointer; text-decoration: none; display: inline-block; }
.primary:disabled { opacity: .5; cursor: not-allowed; }
.error { margin: 0; color: #a92c21; }
</style>
