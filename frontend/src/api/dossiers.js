// Bloc 5 : appels de l'API du dossier administratif du salarié (réservée au rôle RH).
import { apiDelete, apiGet, apiGetBlob, apiPostMultipart, apiPut } from './http'

const BASE = '/bloc5/dossiers'

export const dossiersApi = {
  lister: () => apiGet(BASE),
  dossier: (id) => apiGet(`${BASE}/${id}`),
  modifierIdentite: (id, identite) => apiPut(`${BASE}/${id}/identite`, identite),
  journal: (id) => apiGet(`${BASE}/${id}/journal`),

  ajouterDocument(id, document, fichier) {
    const form = new FormData()
    Object.entries(document).forEach(([cle, valeur]) => {
      if (valeur !== undefined && valeur !== null && valeur !== '') form.append(cle, valeur)
    })
    if (fichier) form.append('fichier', fichier)
    return apiPostMultipart(`${BASE}/${id}/documents`, form)
  },

  telechargerDocument: (documentId) => apiGetBlob(`${BASE}/documents/${documentId}/fichier`),
  supprimerDocument: (documentId) => apiDelete(`${BASE}/documents/${documentId}`)
}

export default dossiersApi
