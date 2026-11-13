import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiPost } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  localStorage.removeItem('user')
  localStorage.removeItem('token')

  const user = ref(null)
  const token = ref('')

  const role = computed(() => user.value?.role)
  const initiales = computed(() =>
    (user.value?.nom || '?').split(' ').map((m) => m[0]).slice(0, 2).join('').toUpperCase()
  )

  async function login(email, motDePasse) {
    const identifiant = email.trim().toLowerCase()
    const u = await apiPost('/auth/login', { email: identifiant, motDePasse })
    // Les appels suivants sont authentifiés en HTTP Basic (le serveur identifie l'utilisateur lui-même).
    token.value = 'Basic ' + btoa(unescape(encodeURIComponent(`${identifiant}:${motDePasse}`)))
    user.value = u
  }

  function logout() {
    user.value = null
    token.value = ''
  }

  return { user, token, role, initiales, login, logout }
})
