import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8080/api' })

/** Message d'erreur lisible renvoyé par le backend (champ "message"). */
export function messageErreur(error) {
  return error?.response?.data?.message ?? error?.message ?? 'Erreur inconnue'
}

export const offresApi = {
  lister: () => api.get('/offres').then((r) => r.data),
  get: (id) => api.get(`/offres/${id}`).then((r) => r.data),
}

export const candidaturesApi = {
  parOffre: (offreId) => api.get(`/candidatures/offre/${offreId}`).then((r) => r.data),
  acceptees: () => api.get('/candidatures/acceptees').then((r) => r.data),

  // 1. Présélection
  preselection: (id, valide) => api.post(`/candidatures/${id}/preselection`, { valide }).then((r) => r.data),

  // 2. Test
  saisirNote: (id, note) => api.put(`/candidatures/${id}/test/note`, { note }).then((r) => r.data),
  deciderTest: (id, valide) => api.post(`/candidatures/${id}/test/decision`, { valide }).then((r) => r.data),

  // 3. Entretien
  planifierEntretien: (id, date, lieu) =>
    api.post(`/candidatures/${id}/entretiens`, { date, lieu }).then((r) => r.data),
}

export const entretiensApi = {
  resultat: (id, valide, commentaire) =>
    api.post(`/entretiens/${id}/resultat`, { valide, commentaire }).then((r) => r.data),
  annuler: (id) => api.post(`/entretiens/${id}/annulation`).then((r) => r.data),
}

/** Identifiants des statuts (identiques à data.sql / Statuts.java). */
export const STATUT = { A_TRAITER: 1, TEST: 2, ATTENTE_ENTRETIEN: 3, ACCEPTE: 4, REFUSE: 5 }
export const STATUT_ENTRETIEN = { PLANIFIE: 1, TERMINE: 2, ANNULE: 3 }
export const STATUT_TEST = { EN_ATTENTE: 1, VALIDE: 2, ECHOUE: 3 }
