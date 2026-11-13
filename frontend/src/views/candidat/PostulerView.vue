<template>
  <div class="apply-page">
    <div class="apply-container">
      <button class="back-btn" @click="router.push('/candidats/offres')">
        ← Retour aux offres
      </button>

      <div v-if="loading" class="state-box">Chargement en cours...</div>
      <div v-else-if="erreur" class="state-box error">{{ erreur }}</div>

      <div v-else class="content-grid">
        <!-- Colonne Gauche : Détails de l'Offre -->
        <div class="info-section">
          <div class="card offer-details">
            <div class="badge-status">{{ offre.statut }}</div>
            <h1>{{ offre.titre }}</h1>
            <p class="publish-date">Publiée le {{ formatDate(offre.datePublication) }}</p>
            
            <div class="divider"></div>
            
            <h3>Description du poste</h3>
            <div class="job-description">{{ offre.description }}</div>
          </div>
        </div>

        <!-- Colonne Droite : Formulaire de Candidature -->
        <div class="form-section">
          <div class="card apply-card">
            <h2>Postuler à cette offre</h2>
            <p class="sub-text">Remplissez ce formulaire pour envoyer votre dossier.</p>

            <form @submit.prevent="soumettre">
              <div class="input-row">
                <div class="input-group">
                  <label>Nom</label>
                  <input v-model="form.nom" placeholder="Votre nom" required />
                </div>
                <div class="input-group">
                  <label>Prénom</label>
                  <input v-model="form.prenom" placeholder="Votre prénom" required />
                </div>
              </div>

              <div class="input-group">
                <label>Email professionnel ou personnel</label>
                <input v-model="form.email" type="email" placeholder="exemple@domaine.com" required />
              </div>

              <div class="input-group">
                <label>Téléphone</label>
                <input v-model="form.telephone" placeholder="+33 6 00 00 00 00" />
              </div>

              <div class="input-group">
                <label>CV (PDF ou DOCX, 10 Mo max)</label>
                <input 
                  type="file" 
                  accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" 
                  required 
                  @change="choisirCv" 
                />
              </div>

              <button class="submit-btn" :disabled="envoi">
                {{ envoi ? "Envoi en cours..." : "Envoyer ma candidature" }}
              </button>

              <p v-if="message" class="feedback success">{{ message }}</p>
              <p v-if="formErreur" class="feedback error">{{ formErreur }}</p>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
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
  return new Date(d).toLocaleDateString('fr-FR', { year: 'numeric', month: 'long', day: 'numeric' })
}

onMounted(async () => {
  try {
    offre.value = await apiGet(/public/offres/${route.params.id})
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
    formErreur.value = 'Veuillez sélectionner votre CV au format PDF ou DOCX.'
    envoi.value = false
    return
  }
  try {
    const donnees = new FormData()
    Object.entries(form).forEach(([cle, valeur]) => donnees.append(cle, valeur))
    donnees.append('cv', cv.value)
    await apiPostMultipart(/public/offres/${route.params.id}/candidatures, donnees)
    message.value = 'Candidature envoyée avec succès ! Redirection...'
    setTimeout(() => router.push('/candidats/offres'), 1800)
  } catch (e) {
    formErreur.value = e.message || "Erreur lors de l'envoi de la candidature."
  } finally {
    envoi.value = false
  }
}
</script>

<style scoped>
/* Page Globale */
.apply-page {
  width: 100%;
  min-height: 100vh;
  background-color: #f8fafc;
  padding: 2rem 1rem;
  box-sizing: border-box;
}

.apply-container {
  max-width: 1300px;
  margin: 0 auto;
}

/* Bouton Retour */
.back-btn {
  background: none;
  border: none;
  color: #4f46e5;
  font-weight: 600;
  font-size: 1rem;
  margin-bottom: 1.5rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  transition: color 0.2s;
}
.back-btn:hover { color: #3730a3; }

/* États */
.state-box {
  text-align: center;
  padding: 3rem;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.02);
  font-size: 1.1rem;
  color: #334155;
}
.state-box.error { color: #dc2626; }

/* Grille principale */
.content-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 2rem;
  align-items: start;
}

/* Cartes */
.card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.04), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
  padding: 2.5rem;
  box-sizing: border-box;
  width: 100%;
}

/* Détails Offre */
.badge-status {
  display: inline-block;
  background: #e0e7ff;
  color: #4f46e5;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.3rem 0.8rem;
  border-radius: 999px;
  text-transform: uppercase;
  margin-bottom: 1rem;
}

.offer-details h1 {
  font-size: 1.8rem;
  color: #0f172a;
  margin: 0 0 0.5rem 0;
  line-height: 1.3;
}

.publish-date {
  color: #64748b;
  font-size: 0.9rem;
  margin: 0;
}

.divider {
  height: 1px;
  background-color: #f1f5f9;
  margin: 1.5rem 0;
}

.offer-details h3 {
  font-size: 1.15rem;
  color: #0f172a;
  margin-bottom: 0.75rem;
}

.job-description {
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
  font-size: 1.02rem;
}

/* Formulaire */
.apply-card h2 {
  font-size: 1.5rem;
  color: #0f172a;
  margin: 0 0 0.3rem 0;
}

.sub-text {
  color: #64748b;
  font-size: 0.95rem;
  margin: 0 0 1.8rem 0;
}

form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
}

.input-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
}

.input-group label {
  font-weight: 600;
  font-size: 0.875rem;
  color: #1e293b;
}

.input-group input {
  width: 100%;
  padding: 0.8rem 1rem;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  font-size: 0.95rem;
  color: #0f172a;
  background-color: #f8fafc;
  box-sizing: border-box;
  transition: all 0.2s;
}

.input-group input:focus {
  outline: none;
  border-color: #4f46e5;
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.input-group input[type="file"] {
  background: #fff;
  padding: 0.5rem;
  cursor: pointer;
}

/* Bouton Soumettre */
.submit-btn {
  margin-top: 0.5rem;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  border: none;
  padding: 1rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  width: 100%;
  transition: opacity 0.2s, transform 0.1s;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.submit-btn:hover {
  opacity: 0.95;
  transform: translateY(-1px);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.feedback {
  font-size: 0.95rem;
  font-weight: 500;
  text-align: center;
  margin: 0;
}
.feedback.success { color: #16a34a; }
.feedback.error { color: #dc2626; }

/* Responsive */
@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>