<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useContratStore } from '@/stores/contrat'

const router = useRouter()
const route = useRoute()
const store = useContratStore()

const form = ref({
  dateDebut: '',
  dateFin: '',
  typeContrat: null,
  salaire: '',
  statutContrat: null,
  employe: null,
})

const errors = ref({})
const submitting = ref(false)
const successMessage = ref('')
const loadingContrat = ref(true)

onMounted(async () => {
  await Promise.all([store.fetchTypesContrat(), store.fetchStatutsContrat(), store.fetchEmployees()])

  const contrat = await store.fetchContratById(route.params.id)
  if (contrat) {
    form.value = {
      dateDebut: contrat.dateDebut || '',
      dateFin: contrat.dateFin || '',
      typeContrat: contrat.typeContrat?.id || null,
      salaire: contrat.salaire || '',
      statutContrat: contrat.statutContrat?.id || null,
      employe: contrat.employe?.id || null,
    }
  }
  loadingContrat.value = false
})

function validate() {
  const e = {}
  if (!form.value.dateDebut) e.dateDebut = 'La date de début est requise'
  if (!form.value.dateFin) e.dateFin = 'La date de fin est requise'
  if (!form.value.typeContrat) e.typeContrat = 'Le type de contrat est requis'
  if (!form.value.salaire || form.value.salaire <= 0) e.salaire = 'Le salaire doit être supérieur à 0'
  if (!form.value.statutContrat) e.statutContrat = 'Le statut est requis'
  if (!form.value.employe) e.employe = "L'employé est requis"
  if (form.value.dateDebut && form.value.dateFin && form.value.dateDebut > form.value.dateFin) {
    e.dateFin = 'La date de fin doit être après la date de début'
  }
  errors.value = e
  return Object.keys(e).length === 0
}

async function handleSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    const contratData = {
      dateDebut: form.value.dateDebut,
      dateFin: form.value.dateFin,
      typeContrat: { id: Number(form.value.typeContrat) },
      salaire: Number(form.value.salaire),
      statutContrat: { id: Number(form.value.statutContrat) },
      employe: { id: Number(form.value.employe) },
    }
    await store.updateContrat(route.params.id, contratData)
    successMessage.value = 'Contrat modifié avec succès !'
    setTimeout(() => {
      router.push({ name: 'contrat-list' })
    }, 1000)
  } catch {
    errors.value.general = 'Erreur lors de la modification du contrat'
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.push({ name: 'contrat-list' })
}
</script>

<template>
  <div class="contrat-form-page">
    <div class="form-card">
      <div class="form-header">
        <button class="btn-back" @click="goBack">← Retour</button>
        <h1>
          <span class="icon">✏️</span>
          Modifier le contrat #{{ route.params.id }}
        </h1>
        <p class="subtitle">Modifiez les informations du contrat</p>
      </div>

      <!-- Loading -->
      <div v-if="loadingContrat" class="loading-container">
        <div class="spinner"></div>
        <p>Chargement du contrat...</p>
      </div>

      <template v-else>
        <!-- Success message -->
        <div v-if="successMessage" class="alert alert-success">
          <span>✅</span> {{ successMessage }}
        </div>

        <!-- Error message -->
        <div v-if="errors.general" class="alert alert-error">
          <span>❌</span> {{ errors.general }}
        </div>

        <form @submit.prevent="handleSubmit" class="form-body">
          <!-- Employé -->
          <div class="form-group" :class="{ 'has-error': errors.employe }">
            <label for="employe">Employé <span class="required">*</span></label>
            <select id="employe" v-model="form.employe" class="form-control">
              <option :value="null" disabled>Sélectionner un employé</option>
              <option v-for="emp in store.employees" :key="emp.id" :value="emp.id">
                {{ emp.nom }} {{ emp.prenom }}
              </option>
            </select>
            <span v-if="errors.employe" class="error-text">{{ errors.employe }}</span>
          </div>

          <!-- Type de contrat -->
          <div class="form-group" :class="{ 'has-error': errors.typeContrat }">
            <label for="typeContrat">Type de contrat <span class="required">*</span></label>
            <select id="typeContrat" v-model="form.typeContrat" class="form-control">
              <option :value="null" disabled>Sélectionner un type</option>
              <option v-for="type in store.typesContrat" :key="type.id" :value="type.id">
                {{ type.libelle }}
              </option>
            </select>
            <span v-if="errors.typeContrat" class="error-text">{{ errors.typeContrat }}</span>
          </div>

          <!-- Dates -->
          <div class="form-row">
            <div class="form-group" :class="{ 'has-error': errors.dateDebut }">
              <label for="dateDebut">Date de début <span class="required">*</span></label>
              <input type="date" id="dateDebut" v-model="form.dateDebut" class="form-control" />
              <span v-if="errors.dateDebut" class="error-text">{{ errors.dateDebut }}</span>
            </div>
            <div class="form-group" :class="{ 'has-error': errors.dateFin }">
              <label for="dateFin">Date de fin <span class="required">*</span></label>
              <input type="date" id="dateFin" v-model="form.dateFin" class="form-control" />
              <span v-if="errors.dateFin" class="error-text">{{ errors.dateFin }}</span>
            </div>
          </div>

          <!-- Salaire -->
          <div class="form-group" :class="{ 'has-error': errors.salaire }">
            <label for="salaire">Salaire (Ar) <span class="required">*</span></label>
            <input type="number" id="salaire" v-model.number="form.salaire" class="form-control" placeholder="Ex: 500000" min="0" step="1000" />
            <span v-if="errors.salaire" class="error-text">{{ errors.salaire }}</span>
          </div>

          <!-- Statut -->
          <div class="form-group" :class="{ 'has-error': errors.statutContrat }">
            <label for="statutContrat">Statut <span class="required">*</span></label>
            <select id="statutContrat" v-model="form.statutContrat" class="form-control">
              <option :value="null" disabled>Sélectionner un statut</option>
              <option v-for="statut in store.statutsContrat" :key="statut.id" :value="statut.id">
                {{ statut.libelle }}
              </option>
            </select>
            <span v-if="errors.statutContrat" class="error-text">{{ errors.statutContrat }}</span>
          </div>

          <!-- Submit -->
          <div class="form-actions">
            <button type="button" class="btn btn-cancel" @click="goBack">Annuler</button>
            <button type="submit" class="btn btn-submit" :disabled="submitting">
              <span v-if="submitting" class="spinner-small"></span>
              {{ submitting ? 'Modification...' : 'Enregistrer les modifications' }}
            </button>
          </div>
        </form>
      </template>
    </div>
  </div>
</template>

<style scoped>
.contrat-form-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 2rem;
}

.form-card {
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.form-header {
  padding: 2rem 2rem 1.5rem;
  border-bottom: 1px solid #f1f3f5;
}

.btn-back {
  background: none;
  border: none;
  color: #667eea;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  margin-bottom: 1rem;
  display: inline-block;
}

.btn-back:hover {
  color: #764ba2;
}

.form-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.icon {
  font-size: 1.3rem;
}

.subtitle {
  color: #6c757d;
  margin: 0.25rem 0 0;
  font-size: 0.9rem;
}

/* Loading */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3rem;
  color: #6c757d;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e2e8f0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 1rem;
}

/* Alerts */
.alert {
  margin: 1rem 2rem 0;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.alert-success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.alert-error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

/* Form */
.form-body {
  padding: 1.5rem 2rem 2rem;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  color: #495057;
  margin-bottom: 0.4rem;
}

.required {
  color: #e74c3c;
}

.form-control {
  width: 100%;
  padding: 0.7rem 0.85rem;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 0.9rem;
  color: #333;
  background: #f8f9fa;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.form-control:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
  background: white;
}

.has-error .form-control {
  border-color: #e74c3c;
}

.error-text {
  display: block;
  color: #e74c3c;
  font-size: 0.8rem;
  margin-top: 0.3rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

/* Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #f1f3f5;
}

.btn {
  padding: 0.7rem 1.5rem;
  border: none;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  transition: all 0.2s ease;
}

.btn-cancel {
  background: #f8f9fa;
  color: #6c757d;
  border: 1px solid #dee2e6;
}

.btn-cancel:hover {
  background: #e9ecef;
}

.btn-submit {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .contrat-form-page {
    padding: 1rem;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
