// Bloc 7 : espace salarié. Aucun appel ne transmet d'identifiant de salarié : le serveur le déduit du compte.
import { apiGet, apiGetBlob, apiPost, apiPostMultipart, apiPut } from './http'

const BASE = '/salarie'

export const salarieApi = {
  moi: () => apiGet(`${BASE}/moi`),
  mouvements: () => apiGet(`${BASE}/mouvements`),
  demandes: () => apiGet(`${BASE}/conges`),
  demander(demande, justificatif) {
    const form = new FormData()
    Object.entries(demande).forEach(([cle, valeur]) => {
      if (valeur !== undefined && valeur !== null && valeur !== '') form.append(cle, valeur)
    })
    if (justificatif) form.append('justificatif', justificatif)
    return apiPostMultipart(`${BASE}/conges`, form)
  },
  ajouterJustificatif(id, justificatif) {
    const form = new FormData()
    form.append('justificatif', justificatif)
    return apiPostMultipart(`${BASE}/conges/${id}/justificatif`, form)
  },
  justificatif: (id) => apiGetBlob(`${BASE}/conges/${id}/justificatif`),
  annuler: (id) => apiPost(`${BASE}/conges/${id}/annuler`),
  changerMotDePasse: (ancienMotDePasse, nouveauMotDePasse) =>
    apiPut(`${BASE}/mot-de-passe`, { ancienMotDePasse, nouveauMotDePasse })
}

export default salarieApi
