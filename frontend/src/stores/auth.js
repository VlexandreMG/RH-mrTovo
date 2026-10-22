import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiPost } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = ref(localStorage.getItem('token') || '')

  const role = computed(() => user.value?.role)
  const initiales = computed(() =>
    (user.value?.nom || '?').split(' ').map((m) => m[0]).slice(0, 2).join('').toUpperCase()
  )

  async function login(email, motDePasse) {
    const u = await apiPost('/auth/login', { email, motDePasse })
    // Les appels suivants sont authentifiés en HTTP Basic (le serveur identifie l'utilisateur lui-même).
    token.value = 'Basic ' + btoa(unescape(encodeURIComponent(`${email}:${motDePasse}`)))
    user.value = u
    localStorage.setItem('user', JSON.stringify(u))
    localStorage.setItem('token', token.value)
  }

  function logout() {
    user.value = null
    token.value = ''
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  return { user, token, role, initiales, login, logout }
})
