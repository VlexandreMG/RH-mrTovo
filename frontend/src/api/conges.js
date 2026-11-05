// Bloc 7 : côté gestionnaires. /bloc7/rh/** est réservé au RH, le reste est ouvert au RH et aux managers.
import { apiGet, apiGetBlob, apiPost, apiPostMultipart } from './http'

const BASE = '/bloc7'

export const congesApi = {
  demandes: () => apiGet(`${BASE}/demandes`),
  avisManager: (id, decision) => apiPost(`${BASE}/demandes/${id}/avis`, decision),
  justificatif: (id) => apiGetBlob(`${BASE}/demandes/${id}/justificatif`),
  decisionRh: (id, decision) => apiPost(`${BASE}/rh/demandes/${id}/decision`, decision),
  annuler: (id, commentaire) => apiPost(`${BASE}/rh/demandes/${id}/annuler`, { commentaire }),

  types: () => apiGet(`${BASE}/rh/types`),
  creerType: (type) => apiPost(`${BASE}/rh/types`, type),

  soldes: () => apiGet(`${BASE}/rh/soldes`),
  mouvements: (employeId) => apiGet(`${BASE}/rh/employes/${employeId}/mouvements`),
  ajuster: (employeId, ajustement) => apiPost(`${BASE}/rh/employes/${employeId}/ajustement`, ajustement),
  indemniser: (employeId, indemnisation) => apiPost(`${BASE}/rh/employes/${employeId}/indemnisation`, indemnisation),
  creerCompte: (employeId, motDePasse) => apiPost(`${BASE}/rh/employes/${employeId}/compte`, { motDePasse }),

  absences: (du, au) => apiGet(`${BASE}/rh/absences?du=${du}&au=${au}`),
  qualifierAbsence(absence, fichier) {
    const form = new FormData()
    Object.entries(absence).forEach(([cle, valeur]) => {
      if (valeur !== undefined && valeur !== null && valeur !== '') form.append(cle, valeur)
    })
    if (fichier) form.append('justificatif', fichier)
    return apiPostMultipart(`${BASE}/rh/absences`, form)
  },
  justificatifAbsence: (id) => apiGetBlob(`${BASE}/rh/absences/${id}/justificatif`)
}

export default congesApi
