import { useAuthStore } from '@/stores/auth'

const API_BASE = 'http://localhost:8012/api'

async function request(method, path, body) {
  const auth = useAuthStore()
  const headers = { 'Content-Type': 'application/json' }
  if (auth.token) headers.Authorization = auth.token

  let res
  try {
    res = await fetch(API_BASE + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  } catch {
    throw new Error('Impossible de joindre le serveur. Vérifiez que le backend est démarré (port 8012).')
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
      message = data.message || message
    } catch { /* corps vide */ }
    if (res.status === 403) message = "Vous n'avez pas le droit d'effectuer cette action."
    throw new Error(message)
  }
  return res.status === 204 ? null : res.json()
}

export const apiGet = (path) => request('GET', path)
export const apiPost = (path, body = {}) => request('POST', path, body)
