import { apiGet, apiPost, apiPut } from '@/api/http'

// Passe par le même client que le Bloc 1 : même URL (port 8012) et même authentification.

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
