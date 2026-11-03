import { useAuthStore } from '@/stores/auth'

const API_BASE = '/api'

async function request(method, path, body, multipart = false) {
  const auth = useAuthStore()
  const headers = multipart ? {} : { 'Content-Type': 'application/json' }
  if (auth.token) headers.Authorization = auth.token

  let res
  try {
    res = await fetch(API_BASE + path, {
      method,
      headers,
      body: body === undefined ? undefined : multipart ? body : JSON.stringify(body)
    })
  } catch {
    throw new Error('Impossible de joindre le serveur. Vérifiez que le backend est démarré (port 8080).')
  }

  if (res.status === 401 && !path.startsWith('/auth/')) {
    auth.logout()
    window.location.assign('/login')
    throw new Error('Session expirée, veuillez vous reconnecter.')
  }
  if (!res.ok) {
    let message = 'Une erreur est survenue.'
    try {
      const data = await res.json()
      message = data.message || data.detail || message
    } catch { /* corps vide */ }
    if (res.status === 403) message = "Vous n'avez pas le droit d'effectuer cette action."
    throw new Error(message)
  }
  return res.status === 204 ? null : res.json()
}

export const apiGet = (path) => request('GET', path)
export const apiPost = (path, body = {}) => request('POST', path, body)
export const apiPut = (path, body = {}) => request('PUT', path, body)
export const apiPostMultipart = (path, body) => request('POST', path, body, true)

export async function apiGetBlob(path) {
  const auth = useAuthStore()
  const headers = auth.token ? { Authorization: auth.token } : {}
  let res
  try {
    res = await fetch(API_BASE + path, { headers })
  } catch {
    throw new Error('Impossible de joindre le serveur. Vérifiez que le backend est démarré (port 8080).')
  }

  if (res.status === 401) {
    auth.logout()
    window.location.assign('/login')
    throw new Error('Session expirée, veuillez vous reconnecter.')
  }
  if (!res.ok) {
    let message = 'Impossible de télécharger le fichier.'
    try {
      const data = await res.json()
      message = data.message || data.detail || message
    } catch { /* corps vide */ }
    if (res.status === 403) message = "Vous n'avez pas le droit d'effectuer cette action."
    throw new Error(message)
  }
  return res.blob()
}
