import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ==================== Contrat ====================
export const contratApi = {
  getAll(params = {}) {
    return api.get('/contrat', { params })
  },
  getById(id) {
    return api.get(`/contrat/${id}`)
  },
  create(contrat) {
    return api.post('/contrat', contrat)
  },
  update(id, contrat) {
    return api.put(`/contrat/${id}`, contrat)
  },
  delete(id) {
    return api.delete(`/contrat/${id}`)
  },
  downloadPdf(id) {
    return api.get(`/contrat/${id}/pdf`, {
      responseType: 'blob',
    })
  },
}

// ==================== TypeContrat ====================
export const typeContratApi = {
  getAll() {
    return api.get('/typeContrat')
  },
}

// ==================== StatutContrat ====================
export const statutContratApi = {
  getAll() {
    return api.get('/statutContrat')
  },
}

// ==================== Employee ====================
export const employeeApi = {
  getAll() {
    return api.get('/employee')
  },
}

export default api
