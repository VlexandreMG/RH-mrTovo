<template>
  <section class="apply">
    <button class="back" @click="router.push('/candidats/offres')">← Retour aux offres</button>
    <div v-if="loading" class="state">Chargement...</div>
    <div v-else-if="erreur" class="state error">{{ erreur }}</div>
    <div v-else class="layout">
      <article class="offer-card">
        <header>
          <div>
            <h1>{{ offre.titre }}</h1>
            <small>Publiée le {{ formatDate(offre.datePublication) }} · {{ offre.statut }}</small>
          </div>
        </header>
        <div class="content">{{ offre.description }}</div>
      </article>
      <aside class="form-card">
        <header>
          <h2>Postuler</h2>
          <p>Remplissez ce formulaire pour envoyer votre candidature</p>
        </header>
        <form @submit.prevent="soumettre">
          <div class="grid">
            <label>
              Nom
              <input v-model="form.nom" required />
            </label>
            <label>
              Prénom
              <input v-model="form.prenom" required />
            </label>
          </div>
          <label>
            Email
            <input v-model="form.email" type="email" required />
          </label>
          <label>
            Téléphone
            <input v-model="form.telephone" />
          </label>
          <label>
            CV (PDF ou DOCX, 10 Mo maximum)
            <input type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" required @change="choisirCv" />
          </label>
          <button class="btn primary" :disabled="envoi">Envoyer ma candidature</button>
          <p v-if="message" class="success">{{ message }}</p>
          <p v-if="formErreur" class="error">{{ formErreur }}</p>
        </form>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, apiPostMultipart } from '@/api/http'

const route = useRoute()
const router = useRouter()
const offre = ref({})
const loading = ref(true)
const erreur = ref('')
const envoi = ref(false)
const message = ref('')
const formErreur = ref('')

const form = reactive({
  nom: '',
  prenom: '',
  email: '',
  telephone: '',
})
const cv = ref(null)

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('fr-FR')
}

onMounted(async () => {
  try {
    offre.value = await apiGet(`/public/offres/${route.params.id}`)
  } catch (e) {
    erreur.value = e.message || "Impossible de charger l'offre"
  } finally {
    loading.value = false
  }
})

function choisirCv(event) {
  cv.value = event.target.files?.[0] ?? null
}

async function soumettre() {
  envoi.value = true
  message.value = ''
  formErreur.value = ''
  if (!cv.value) {
    formErreur.value = 'Sélectionnez votre CV au format PDF ou DOCX.'
    envoi.value = false
    return
  }
  try {
    const donnees = new FormData()
    Object.entries(form).forEach(([cle, valeur]) => donnees.append(cle, valeur))
    donnees.append('cv', cv.value)
    await apiPostMultipart(`/public/offres/${route.params.id}/candidatures`, donnees)
    message.value = 'Candidature envoyée avec succès !'
    setTimeout(() => router.push('/candidats/offres'), 1500)
  } catch (e) {
    formErreur.value = e.message || "Erreur lors de l'envoi"
  } finally {
    envoi.value = false
  }
}
</script>

<style scoped>
.back { background: none; border: none; color: rgba(20,20,58,.7); font-weight: 500; margin-bottom: 1.6rem; cursor: pointer; }
.state { text-align: center; padding: 2rem; background: #fff; border-radius: 16px; }
.state.error { color: #d64545; }
.layout { display: grid; grid-template-columns: 2fr 1fr; gap: 1.6rem; align-items: start; }
.offer-card, .form-card { background: #fff; border-radius: 18px; box-shadow: 0 60px 80px -60px rgba(108,108,255,.5); overflow: hidden; }
.offer-card header { padding: 2rem 2rem 0; }
.offer-card h1 { margin: 0; color: #14143a; }
.offer-card small { color: rgba(20,20,58,.6); }
.content { padding: 1.4rem 2rem 2rem; line-height: 1.8; color: rgba(20,20,58,.85); white-space: pre-wrap; }
.form-card header { padding: 2rem 2rem 0; }
.form-card h2 { margin: 0; color: #14143a; }
.form-card p { margin: .4rem 0 0; color: rgba(20,20,58,.7); }
form { display: flex; flex-direction: column; gap: 1rem; padding: 1.6rem 2rem 2rem; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
label { display: flex; flex-direction: column; gap: .5rem; font-weight: 500; color: #14143a; }
input { padding: .7rem .9rem; border-radius: 12px; border: 1px solid rgba(20,20,58,.12); font-size: .98rem; }
.btn { border: none; padding: .8rem 1.6rem; border-radius: 999px; font-weight: 600; cursor: pointer; align-self: flex-start; }
.btn.primary { background: linear-gradient(135deg, #6c6cff, #a05cf0); color: #fff; }
.btn:disabled { opacity: .7; cursor: not-allowed; }
.success { color: #2f9e44; font-weight: 500; }
.error { color: #d64545; }
@media (max-width: 980px) { .layout { grid-template-columns: 1fr; } }
</style>
