import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiGet, apiPost } from '@/api/http'

export const useDemandeStore = defineStore('demandes', () => {
  const demandes = ref([])
  const departements = ref([])
  const postes = ref([])
  const niveaux = ref([])
  const employes = ref([])
  const grilles = ref([])
  const referentielsCharges = ref(false)

  async function chargerReferentiels(force = false) {
    if (referentielsCharges.value && !force) return
    ;[departements.value, postes.value, niveaux.value, employes.value, grilles.value] = await Promise.all([
      apiGet('/referentiels/departements'),
      apiGet('/referentiels/postes'),
      apiGet('/referentiels/niveaux'),
      apiGet('/referentiels/employes'),
      apiGet('/referentiels/grilles')
    ])
    referentielsCharges.value = true
  }

  async function charger() {
    demandes.value = await apiGet('/demandes')
  }

  const detail = (id) => apiGet(`/demandes/${id}`)
  const creer = (payload) => apiPost('/demandes', payload)
  const valider = (id, payload) => apiPost(`/demandes/${id}/validations`, payload)
  const ouvrir = (id) => apiPost(`/demandes/${id}/ouverture`)

  /** Grille en vigueur aujourd'hui pour un poste et un niveau (ou undefined). */
  function grilleApplicable(idPoste, idNiveau) {
    const aujourdhui = new Date().toISOString().slice(0, 10)
    return grilles.value
      .filter((g) => g.idPoste === idPoste && g.idNiveau === idNiveau)
      .filter((g) => g.dateDebut <= aujourdhui && (!g.dateFin || g.dateFin >= aujourdhui))
      .sort((a, b) => b.dateDebut.localeCompare(a.dateDebut))[0]
  }

  return {
    demandes, departements, postes, niveaux, employes, grilles,
    chargerReferentiels, charger, detail, creer, valider, ouvrir, grilleApplicable
  }
})
