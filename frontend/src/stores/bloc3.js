import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/client'

export const useBloc3Store = defineStore('bloc3', () => {
  const dashboard = ref(null)
  const candidatures = ref([])
  const propositions = ref([])
  const negotiations = ref([])
  const employes = ref([])
  const contrats = ref([])
  const postes = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function run(action) {
    loading.value = true
    error.value = null
    try {
      await action()
    } catch (exception) {
      error.value = exception.message
      throw exception
    } finally {
      loading.value = false
    }
  }

  async function chargerTableauDeBord() {
    dashboard.value = await api.dashboard()
  }

  async function chargerCandidatures() {
    candidatures.value = await api.candidatures()
  }

  async function chargerPropositions(filtres = {}) {
    propositions.value = await api.propositions(filtres)
  }

  async function chargerNegotiations() {
    negotiations.value = await api.negotiations()
  }

  async function chargerEmployes() {
    employes.value = await api.employes()
  }

  async function chargerContrats() {
    contrats.value = await api.contrats()
  }

  async function chargerPostes() {
    postes.value = await api.postes()
  }

  async function rafraichirTout() {
    await Promise.all([
      chargerTableauDeBord(),
      chargerCandidatures(),
      chargerPropositions(),
      chargerNegotiations(),
      chargerEmployes(),
      chargerContrats(),
    ])
  }

  return {
    dashboard,
    candidatures,
    propositions,
    negotiations,
    employes,
    contrats,
    postes,
    loading,
    error,
    run,
    chargerTableauDeBord,
    chargerCandidatures,
    chargerPropositions,
    chargerNegotiations,
    chargerEmployes,
    chargerContrats,
    chargerPostes,
    rafraichirTout,
  }
})
