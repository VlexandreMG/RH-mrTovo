<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useContratStore } from '@/stores/contrat'

const router = useRouter()
const store = useContratStore()

const selectedEmployeId = ref('')
const selectedStatutId = ref('')

onMounted(async () => {
  await Promise.all([store.fetchContrats(), store.fetchEmployees(), store.fetchStatutsContrat(), store.fetchTypesContrat()])
})

watch([selectedEmployeId, selectedStatutId], async () => {
  store.setFilterEmployeId(selectedEmployeId.value || null)
  store.setFilterStatutContratId(selectedStatutId.value || null)
  await store.fetchContrats()
})

function goToCreate() {
  router.push({ name: 'contrat-create' })
}

function goToDetail(id) {
  router.push({ name: 'contrat-detail', params: { id } })
}

function goToEdit(id) {
  router.push({ name: 'contrat-edit', params: { id } })
}

async function handleDelete(id) {
  if (confirm('Êtes-vous sûr de vouloir supprimer ce contrat ?')) {
    await store.deleteContrat(id)
  }
}

async function handleDownloadPdf(id) {
  await store.downloadContratPdf(id)
}

function clearFilters() {
  selectedEmployeId.value = ''
  selectedStatutId.value = ''
  store.clearFilters()
  store.fetchContrats()
}

function formatDate(dateStr) {
  if (!dateStr) return 'N/A'
  const date = new Date(dateStr)
  return date.toLocaleDateString('fr-FR')
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
</script>

<template>
  <div class="contrat-list-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <h1>
            <span class="icon">📋</span>
            Gestion des Contrats
          </h1>
          <p class="subtitle">{{ store.contrats.length }} contrat(s) trouvé(s)</p>
        </div>
        <button class="btn btn-primary" @click="goToCreate">
          <span class="btn-icon">+</span>
          Nouveau Contrat
        </button>
      </div>
    </div>

    <!-- Filtres (B4-013) -->
    <div class="filters-card">
      <div class="filters-header">
        <span class="filter-icon">🔍</span>
        <h3>Filtres</h3>
      </div>
      <div class="filters-body">
        <div class="filter-group">
          <label for="filter-employe">Employé</label>
          <select id="filter-employe" v-model="selectedEmployeId" class="form-select">
            <option value="">Tous les employés</option>
            <option v-for="emp in store.employees" :key="emp.id" :value="emp.id">
              {{ emp.nom }} {{ emp.prenom }}
            </option>
          </select>
        </div>
        <div class="filter-group">
          <label for="filter-statut">Statut</label>
          <select id="filter-statut" v-model="selectedStatutId" class="form-select">
            <option value="">Tous les statuts</option>
            <option v-for="statut in store.statutsContrat" :key="statut.id" :value="statut.id">
              {{ statut.libelle }}
            </option>
          </select>
        </div>
        <div class="filter-group filter-actions">
          <button class="btn btn-outline" @click="clearFilters">
            Réinitialiser
          </button>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="loading-container">
      <div class="spinner"></div>
      <p>Chargement des contrats...</p>
    </div>

    <!-- Error -->
    <div v-else-if="store.error" class="error-container">
      <span class="error-icon">⚠️</span>
      <p>{{ store.error }}</p>
      <button class="btn btn-outline" @click="store.fetchContrats()">Réessayer</button>
    </div>

    <!-- Empty state -->
    <div v-else-if="store.contrats.length === 0" class="empty-state">
      <span class="empty-icon">📄</span>
      <h3>Aucun contrat trouvé</h3>
      <p>Commencez par créer un nouveau contrat.</p>
      <button class="btn btn-primary" @click="goToCreate">Créer un contrat</button>
    </div>

    <!-- Table des contrats (B4-012) -->
    <div v-else class="table-card">
      <div class="table-responsive">
        <table class="contrat-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Employé</th>
              <th>Type</th>
              <th>Date début</th>
              <th>Date fin</th>
              <th>Salaire</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="contrat in store.contrats" :key="contrat.id" @click="goToDetail(contrat.id)" class="table-row-clickable">
              <td class="td-id">#{{ contrat.id }}</td>
              <td class="td-employe">
                <div class="employe-info" v-if="contrat.employe">
                  <span class="employe-avatar">{{ contrat.employe.prenom?.charAt(0) }}{{ contrat.employe.nom?.charAt(0) }}</span>
                  <span>{{ contrat.employe.nom }} {{ contrat.employe.prenom }}</span>
                </div>
                <span v-else class="text-muted">N/A</span>
              </td>
              <td>
                <span class="badge badge-type">{{ contrat.typeContrat?.libelle || 'N/A' }}</span>
              </td>
              <td>{{ formatDate(contrat.dateDebut) }}</td>
              <td>{{ formatDate(contrat.dateFin) }}</td>
              <td class="td-salaire">{{ formatSalaire(contrat.salaire) }}</td>
              <td>
                <span class="badge" :class="getStatutClass(contrat.statutContrat)">
                  {{ contrat.statutContrat?.libelle || 'N/A' }}
                </span>
              </td>
              <td class="td-actions" @click.stop>
                <button class="btn-icon-action btn-view" @click="goToDetail(contrat.id)" title="Voir">
                  👁️
                </button>
                <button class="btn-icon-action btn-edit" @click="goToEdit(contrat.id)" title="Modifier">
                  ✏️
                </button>
                <button class="btn-icon-action btn-pdf" @click="handleDownloadPdf(contrat.id)" title="Télécharger PDF">
                  📥
                </button>
                <button class="btn-icon-action btn-delete" @click="handleDelete(contrat.id)" title="Supprimer">
                  🗑️
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contrat-list-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.page-header {
  margin-bottom: 1.5rem;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-left h1 {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.icon {
  font-size: 1.5rem;
}

.subtitle {
  color: #6c757d;
  margin: 0.25rem 0 0 0;
  font-size: 0.9rem;
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
}

.btn-outline {
  background: transparent;
  color: #6c757d;
  border: 1px solid #dee2e6;
}

.btn-outline:hover {
  background: #f8f9fa;
  color: #495057;
}

.btn-icon {
  font-size: 1.2rem;
  font-weight: 700;
}

/* Filters Card (B4-013) */
.filters-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 1.5rem;
  overflow: hidden;
}

.filters-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 1.5rem 0;
}

.filters-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1a1a2e;
}

.filter-icon {
  font-size: 1.1rem;
}

.filters-body {
  display: flex;
  gap: 1rem;
  padding: 1rem 1.5rem 1.25rem;
  flex-wrap: wrap;
  align-items: flex-end;
}

.filter-group {
  flex: 1;
  min-width: 200px;
}

.filter-group label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: #6c757d;
  margin-bottom: 0.4rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.form-select {
  width: 100%;
  padding: 0.6rem 0.75rem;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 0.9rem;
  color: #333;
  background: #f8f9fa;
  transition: border-color 0.2s, box-shadow 0.2s;
  appearance: auto;
}

.form-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

.filter-actions {
  flex: 0;
  min-width: auto;
}

/* Loading */
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

/* Error */
.error-container {
  text-align: center;
  padding: 3rem;
  background: #fff5f5;
  border-radius: 16px;
  border: 1px solid #fed7d7;
}

.error-icon {
  font-size: 2rem;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.empty-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #1a1a2e;
  margin: 0 0 0.5rem;
}

.empty-state p {
  color: #6c757d;
  margin: 0 0 1.5rem;
}

/* Table Card (B4-012) */
.table-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.table-responsive {
  overflow-x: auto;
}

.contrat-table {
  width: 100%;
  border-collapse: collapse;
}

.contrat-table thead {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.contrat-table th {
  color: white;
  padding: 0.9rem 1rem;
  text-align: left;
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.contrat-table td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid #f1f3f5;
  font-size: 0.9rem;
  color: #333;
}

.table-row-clickable {
  cursor: pointer;
  transition: background-color 0.15s;
}

.table-row-clickable:hover {
  background-color: #f8f9ff;
}

.td-id {
  font-weight: 600;
  color: #667eea;
}

.td-salaire {
  font-weight: 600;
  color: #1a1a2e;
}

.td-employe {
  white-space: nowrap;
}

.employe-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.employe-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}

.text-muted {
  color: #adb5bd;
}

/* Badges */
.badge {
  display: inline-block;
  padding: 0.3rem 0.7rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
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

/* Action Buttons */
.td-actions {
  display: flex;
  gap: 0.3rem;
  white-space: nowrap;
}

.btn-icon-action {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  transition: all 0.15s ease;
}

.btn-view:hover {
  background: #e8eeff;
}

.btn-edit:hover {
  background: #fff3cd;
}

.btn-pdf:hover {
  background: #d4edda;
}

.btn-delete:hover {
  background: #f8d7da;
}

@media (max-width: 768px) {
  .contrat-list-page {
    padding: 1rem;
  }

  .header-content {
    flex-direction: column;
    align-items: stretch;
  }

  .btn-primary {
    width: 100%;
    justify-content: center;
  }

  .filters-body {
    flex-direction: column;
  }

  .filter-group {
    min-width: 100%;
  }
}
</style>
