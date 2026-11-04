// Bloc 6 : appels de l'API du temps de travail (réservée au rôle RH).
import { apiDelete, apiGet, apiPost, apiPut } from './http'

const BASE = '/bloc6'

export const tempsApi = {
  regimes: () => apiGet(`${BASE}/regimes`),
  creerRegime: (regime) => apiPost(`${BASE}/regimes`, regime),
  plannings: (employeId) => apiGet(`${BASE}/employes/${employeId}/plannings`),
  affecterRegime: (employeId, affectation) => apiPost(`${BASE}/employes/${employeId}/plannings`, affectation),

  semaine: (employeId, date) => apiGet(`${BASE}/employes/${employeId}/semaine?date=${date}`),
  saisirPointage: (pointage) => apiPost(`${BASE}/pointages`, pointage),
  corrigerPointage: (id, correction) => apiPut(`${BASE}/pointages/${id}`, correction),
  supprimerPointage: (id, motif) => apiDelete(`${BASE}/pointages/${id}?motif=${encodeURIComponent(motif)}`),

  heuresSupAValider: () => apiGet(`${BASE}/heures-sup/a-valider`),
  deciderHeuresSup: (decision) => apiPost(`${BASE}/heures-sup/decision`, decision),

  anomalies: (du, au) => apiGet(`${BASE}/anomalies?du=${du}&au=${au}`),
  variablesPaie: (mois) => apiGet(`${BASE}/variables-paie?mois=${mois}`)
}

export default tempsApi
