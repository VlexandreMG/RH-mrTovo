const BASE_URL = '/api/bloc3'

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }
  if (!response.ok) {
    const error = new Error(data?.message || `Erreur ${response.status}`)
    error.code = data?.code || 'ERREUR'
    error.details = data?.details || []
    error.status = response.status
    throw error
  }
  return data
}

function query(params) {
  if (!params) return ''
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.append(key, value)
  })
  const rendered = search.toString()
  return rendered ? `?${rendered}` : ''
}

function post(path, body) {
  return request(path, { method: 'POST', body: JSON.stringify(body ?? {}) })
}

export const api = {
  dashboard: () => request('/dashboard'),
  candidatures: () => request('/candidatures'),
  candidaturesRetenues: () => request('/candidatures/retenues'),
  propositions: (params) => request(`/propositions${query(params)}`),
  proposition: (id) => request(`/propositions/${id}`),
  creerProposition: (body) => post('/propositions', body),
  validerProposition: (id, body) => post(`/propositions/${id}/valider`, body),
  emettreProposition: (id, body) => post(`/propositions/${id}/emettre`, body),
  accepterProposition: (id, body) => post(`/propositions/${id}/accepter`, body),
  refuserProposition: (id, body) => post(`/propositions/${id}/refuser`, body),
  annulerProposition: (id, body) => post(`/propositions/${id}/annuler`, body),
  revoquerProposition: (id, body) => post(`/propositions/${id}/revoquer`, body),
  negotiations: () => request('/negotiations'),
  negotiation: (id) => request(`/negotiations/${id}`),
  analyserNegociation: (id, body) => post(`/negotiations/${id}/analyser`, body),
  deciderNegociation: (id, body) => post(`/negotiations/${id}/decider`, body),
  employes: () => request('/employes'),
  employe: (id) => request(`/employes/${id}`),
  contrats: () => request('/contrats'),
  signerContrat: (id) => post(`/contrats/${id}/signer`, {}),
  activerContrat: (id) => post(`/contrats/${id}/activer`, {}),
  postes: () => request('/reference/postes'),
  departements: () => request('/reference/departements'),
  grilles: (posteId) => request(`/reference/grilles/${posteId}`),
}

export default api
