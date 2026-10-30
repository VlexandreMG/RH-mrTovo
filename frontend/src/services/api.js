import { apiDelete, apiGet, apiGetBlob, apiPost, apiPut } from '@/api/http'

function withParams(path, params = {}) {
  const query = new URLSearchParams(
    Object.entries(params).filter(([, value]) => value !== null && value !== undefined && value !== ''),
  ).toString()
  return query ? `${path}?${query}` : path
}

const asResponse = (data) => ({ data })

// ==================== Contrat ====================
export const contratApi = {
  async getAll(params = {}) {
    return asResponse(await apiGet(withParams('/contrat', params)))
  },
  async getById(id) {
    return asResponse(await apiGet(`/contrat/${id}`))
  },
  async create(contrat) {
    return asResponse(await apiPost('/contrat', contrat))
  },
  async update(id, contrat) {
    return asResponse(await apiPut(`/contrat/${id}`, contrat))
  },
  async delete(id) {
    return asResponse(await apiDelete(`/contrat/${id}`))
  },
  async downloadPdf(id) {
    return asResponse(await apiGetBlob(`/contrat/${id}/pdf`))
  },
}

// ==================== TypeContrat ====================
export const typeContratApi = {
  async getAll() {
    return asResponse(await apiGet('/typeContrat'))
  },
}

// ==================== StatutContrat ====================
export const statutContratApi = {
  async getAll() {
    return asResponse(await apiGet('/statutContrat'))
  },
}

// ==================== Employee ====================
export const employeeApi = {
  async getAll() {
    return asResponse(await apiGet('/employee'))
  },
}

/** Message d'erreur lisible renvoyé par le backend (champ "message"). */
export function messageErreur(error) {
  return error?.message ?? 'Erreur inconnue'
}

export const offresApi = {
  lister: () => apiGet('/offres'),
  get: (id) => apiGet(`/offres/${id}`),
}

export const candidaturesApi = {
  parOffre: (offreId) => apiGet(`/candidatures/offre/${offreId}`),
  acceptees: () => apiGet('/candidatures/acceptees'),

  // 1. Présélection
  preselection: (id, valide) => apiPost(`/candidatures/${id}/preselection`, { valide }),

  // 2. Test
  saisirNote: (id, note) => apiPut(`/candidatures/${id}/test/note`, { note }),
  deciderTest: (id, valide) => apiPost(`/candidatures/${id}/test/decision`, { valide }),

  // 3. Entretien
  planifierEntretien: (id, date, lieu) => apiPost(`/candidatures/${id}/entretiens`, { date, lieu }),
}

export const entretiensApi = {
  resultat: (id, valide, commentaire) => apiPost(`/entretiens/${id}/resultat`, { valide, commentaire }),
  annuler: (id) => apiPost(`/entretiens/${id}/annulation`),
}

/** Identifiants des statuts (identiques à data.sql / Statuts.java). */
export const STATUT = { A_TRAITER: 1, TEST: 2, ATTENTE_ENTRETIEN: 3, ACCEPTE: 4, REFUSE: 5 }
export const STATUT_ENTRETIEN = { PLANIFIE: 1, TERMINE: 2, ANNULE: 3 }
export const STATUT_TEST = { EN_ATTENTE: 1, VALIDE: 2, ECHOUE: 3 }
