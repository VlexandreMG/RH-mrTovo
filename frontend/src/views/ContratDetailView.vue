<script setup>
import { onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useContratStore } from '@/stores/contrat'

const router = useRouter()
const route = useRoute()
const store = useContratStore()

const contrat = computed(() => store.currentContrat)

onMounted(async () => {
  await store.fetchContratById(route.params.id)
})

function goBack() {
  router.push({ name: 'contrat-list' })
}

function goToEdit() {
  router.push({ name: 'contrat-edit', params: { id: route.params.id } })
}

async function handleDownloadPdf() {
  await store.downloadContratPdf(route.params.id)
}

async function handleDelete() {
  if (confirm('Êtes-vous sûr de vouloir supprimer ce contrat ?')) {
    await store.deleteContrat(route.params.id)
    router.push({ name: 'contrat-list' })
  }
}

function formatDate(dateStr) {
  if (!dateStr) return 'N/A'
  const date = new Date(dateStr)
  return date.toLocaleDateString('fr-FR', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

function formatSalaire(salaire) {
  if (!salaire && salaire !== 0) return 'N/A'
  return new Intl.NumberFormat('fr-FR').format(salaire) + ' Ar'
}

function getStatutClass(statut) {
  if (!statut) return ''
  const libelle = statut.libelle?.toLowerCase()
  if (libelle?.includes('actif') || libelle?.includes('en cours')) return 'badge-success'
  if (libelle?.includes('termin') || libelle?.includes('expiré')) return 'badge-danger'
  if (libelle?.includes('suspendu') || libelle?.includes('attente')) return 'badge-warning'
  return 'badge-info'
}

function getDurationDays() {
  if (!contrat.value?.dateDebut || !contrat.value?.dateFin) return null
  const start = new Date(contrat.value.dateDebut)
  const end = new Date(contrat.value.dateFin)
  const diffTime = Math.abs(end - start)
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}
</script>

<template>
  <div class="contrat-detail-page">
    <!-- Loading -->
    <div v-if="store.loading" class="loading-container">
      <div class="spinner"></div>
      <p>Chargement du contrat...</p>
    </div>

    <!-- Not found -->
    <div v-else-if="!contrat" class="not-found">
      <span class="not-found-icon">🔍</span>
      <h2>Contrat non trouvé</h2>
      <p>Le contrat demandé n'existe pas ou a été supprimé.</p>
      <button class="btn btn-primary" @click="goBack">Retour à la liste</button>
    </div>

    <!-- Detail -->
    <template v-else>
      <!-- Header -->
      <div class="detail-header">
        <button class="btn-back" @click="goBack">← Retour à la liste</button>
        <div class="header-content">
          <div class="header-left">
            <h1>Contrat #{{ contrat.id }}</h1>
            <span class="badge" :class="getStatutClass(contrat.statutContrat)">
              {{ contrat.statutContrat?.libelle || 'N/A' }}
            </span>
          </div>
          <div class="header-actions">
            <button class="btn btn-outline" @click="goToEdit">
              ✏️ Modifier
            </button>
            <button class="btn btn-success" @click="handleDownloadPdf">
              📥 Télécharger PDF
            </button>
            <button class="btn btn-danger" @click="handleDelete">
              🗑️ Supprimer
            </button>
          </div>
        </div>
      </div>

      <div class="detail-grid">
        <!-- Carte Employé -->
        <div class="detail-card">
          <div class="card-header">
            <span class="card-icon">👤</span>
            <h2>Informations de l'employé</h2>
          </div>
          <div class="card-body" v-if="contrat.employe">
            <div class="info-row">
              <div class="avatar-large">
                {{ contrat.employe.prenom?.charAt(0) }}{{ contrat.employe.nom?.charAt(0) }}
              </div>
              <div class="employe-name">
                <h3>{{ contrat.employe.nom }} {{ contrat.employe.prenom }}</h3>
              </div>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">📧 Email</span>
                <span class="info-value">{{ contrat.employe.email }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">📞 Téléphone</span>
                <span class="info-value">{{ contrat.employe.telephone }}</span>
              </div>
              <div class="info-item info-full">
                <span class="info-label">📍 Adresse</span>
                <span class="info-value">{{ contrat.employe.adresse }}</span>
              </div>
            </div>
          </div>
          <div class="card-body" v-else>
            <p class="text-muted">Aucun employé associé</p>
          </div>
        </div>

        <!-- Carte Contrat -->
        <div class="detail-card">
          <div class="card-header">
            <span class="card-icon">📋</span>
            <h2>Détails du contrat</h2>
          </div>
          <div class="card-body">
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">📝 Type de contrat</span>
                <span class="info-value badge badge-type">
                  {{ contrat.typeContrat?.libelle || 'N/A' }}
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">💰 Salaire</span>
                <span class="info-value salaire-value">{{ formatSalaire(contrat.salaire) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">📅 Date de début</span>
                <span class="info-value">{{ formatDate(contrat.dateDebut) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">📅 Date de fin</span>
                <span class="info-value">{{ formatDate(contrat.dateFin) }}</span>
              </div>
              <div class="info-item" v-if="getDurationDays() !== null">
                <span class="info-label">⏱️ Durée</span>
                <span class="info-value">{{ getDurationDays() }} jours</span>
              </div>
              <div class="info-item">
                <span class="info-label">📊 Statut</span>
                <span class="info-value badge" :class="getStatutClass(contrat.statutContrat)">
                  {{ contrat.statutContrat?.libelle || 'N/A' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.contrat-detail-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4rem;
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

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.not-found {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.not-found-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 1rem;
}

.not-found h2 {
  color: #1a1a2e;
  margin: 0 0 0.5rem;
}

.not-found p {
  color: #6c757d;
  margin: 0 0 1.5rem;
}

/* Header */
.detail-header {
  margin-bottom: 2rem;
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

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.header-left h1 {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.6rem 1rem;
  border: none;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-outline {
  background: white;
  color: #495057;
  border: 1px solid #dee2e6;
}

.btn-outline:hover {
  background: #f8f9fa;
}

.btn-success {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.btn-success:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

/* Badge */
.badge {
  display: inline-block;
  padding: 0.3rem 0.75rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
}

.badge-type {
  background: #e8eeff;
  color: #667eea;
}

.badge-success {
  background: #d4edda;
  color: #155724;
}

.badge-danger {
  background: #f8d7da;
  color: #721c24;
}

.badge-warning {
  background: #fff3cd;
  color: #856404;
}

.badge-info {
  background: #d1ecf1;
  color: #0c5460;
}

/* Detail Grid */
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.detail-card {
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f1f3f5;
  background: #fafbfc;
}

.card-icon {
  font-size: 1.2rem;
}

.card-header h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1a1a2e;
}

.card-body {
  padding: 1.5rem;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid #f1f3f5;
}

.avatar-large {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 700;
  flex-shrink: 0;
}

.employe-name h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1a1a2e;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.info-full {
  grid-column: 1 / -1;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.info-label {
  font-size: 0.8rem;
  color: #6c757d;
  font-weight: 500;
}

.info-value {
  font-size: 0.95rem;
  color: #1a1a2e;
  font-weight: 500;
}

.salaire-value {
  font-size: 1.1rem;
  font-weight: 700;
  color: #28a745;
}

.text-muted {
  color: #adb5bd;
  font-style: italic;
}

@media (max-width: 768px) {
  .contrat-detail-page {
    padding: 1rem;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    flex: 1;
    justify-content: center;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
