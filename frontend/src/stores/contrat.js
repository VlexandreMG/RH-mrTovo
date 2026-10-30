import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { contratApi, typeContratApi, statutContratApi, employeeApi } from '@/services/api'

export const useContratStore = defineStore('contrat', () => {
  // State
  const contrats = ref([])
  const currentContrat = ref(null)
  const typesContrat = ref([])
  const statutsContrat = ref([])
  const employees = ref([])
  const loading = ref(false)
  const error = ref(null)

  // Filters
  const filterEmployeId = ref(null)
  const filterStatutContratId = ref(null)

  // Computed
  const filteredContrats = computed(() => contrats.value)

  // Actions
  async function fetchContrats() {
    loading.value = true
    error.value = null
    try {
      const params = {}
      if (filterEmployeId.value) params.employeId = filterEmployeId.value
      if (filterStatutContratId.value) params.statutContratId = filterStatutContratId.value
      const response = await contratApi.getAll(params)
      contrats.value = response.data
    } catch (err) {
      error.value = 'Erreur lors du chargement des contrats'
      console.error(err)
    } finally {
      loading.value = false
    }
  }

  async function fetchContratById(id) {
    loading.value = true
    error.value = null
    try {
      const response = await contratApi.getById(id)
      currentContrat.value = response.data
      return response.data
    } catch (err) {
      error.value = 'Erreur lors du chargement du contrat'
      console.error(err)
      return null
    } finally {
      loading.value = false
    }
  }

  async function createContrat(contrat) {
    loading.value = true
    error.value = null
    try {
      const response = await contratApi.create(contrat)
      contrats.value.push(response.data)
      return response.data
    } catch (err) {
      error.value = 'Erreur lors de la création du contrat'
      console.error(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateContrat(id, contrat) {
    loading.value = true
    error.value = null
    try {
      await contratApi.update(id, contrat)
      await fetchContrats()
      return true
    } catch (err) {
      error.value = 'Erreur lors de la modification du contrat'
      console.error(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteContrat(id) {
    loading.value = true
    error.value = null
    try {
      await contratApi.delete(id)
      contrats.value = contrats.value.filter((c) => c.id !== id)
      return true
    } catch (err) {
      error.value = 'Erreur lors de la suppression du contrat'
      console.error(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function downloadContratPdf(id) {
    try {
      const response = await contratApi.downloadPdf(id)
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `contrat_${id}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (err) {
      error.value = 'Erreur lors du téléchargement du PDF'
      console.error(err)
    }
  }

  async function fetchTypesContrat() {
    try {
      const response = await typeContratApi.getAll()
      typesContrat.value = response.data
    } catch (err) {
      console.error('Erreur chargement types contrat:', err)
    }
  }

  async function fetchStatutsContrat() {
    try {
      const response = await statutContratApi.getAll()
      statutsContrat.value = response.data
    } catch (err) {
      console.error('Erreur chargement statuts contrat:', err)
    }
  }

  async function fetchEmployees() {
    try {
      const response = await employeeApi.getAll()
      employees.value = response.data
    } catch (err) {
      console.error('Erreur chargement employés:', err)
    }
  }

  function setFilterEmployeId(id) {
    filterEmployeId.value = id
  }

  function setFilterStatutContratId(id) {
    filterStatutContratId.value = id
  }

  function clearFilters() {
    filterEmployeId.value = null
    filterStatutContratId.value = null
  }

  return {
    // State
    contrats,
    currentContrat,
    typesContrat,
    statutsContrat,
    employees,
    loading,
    error,
    filterEmployeId,
    filterStatutContratId,
    // Computed
    filteredContrats,
    // Actions
    fetchContrats,
    fetchContratById,
    createContrat,
    updateContrat,
    deleteContrat,
    downloadContratPdf,
    fetchTypesContrat,
    fetchStatutsContrat,
    fetchEmployees,
    setFilterEmployeId,
    setFilterStatutContratId,
    clearFilters,
  }
})
