<template>
  <div class="candidate-shell">
    <header class="candidate-header">
      <div class="brand">
        <div class="logo">RH</div>
        <div>
          <strong>Recrutement</strong>
          <small>Portail Candidats</small>
        </div>
      </div>
      <nav class="nav">
        <RouterLink v-if="auth.role === 'CANDIDAT'" to="/candidats/espace">Mon espace</RouterLink>
        <RouterLink to="/candidats/offres">Offres</RouterLink>
        <RouterLink v-if="!auth.user" to="/candidats/inscription">Créer un compte</RouterLink>
        <RouterLink v-if="!auth.user" to="/login">Connexion</RouterLink>
        <button v-else class="logout" type="button" @click="deconnecter">Déconnexion</button>
      </nav>
    </header>
    <main class="candidate-content">
      <RouterView />
    </main>
    <footer class="candidate-footer">
      <small>© {{ year }} RH-mrTovo</small>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const year = computed(() => new Date().getFullYear())
const auth = useAuthStore()
const router = useRouter()

function deconnecter() {
  auth.logout()
  router.push('/candidats/offres')
}
</script>

<style scoped>
.candidate-shell { min-height: 100vh; display: flex; flex-direction: column; background: radial-gradient(circle at top, #f8f9ff, #eef1ff); }
.candidate-header { display: flex; align-items: center; justify-content: space-between; padding: 1rem 2rem; background: rgba(20, 20, 58, 0.98); color: #fff; box-shadow: 0 10px 40px -20px #6c6cff; }
.brand { display: flex; align-items: center; gap: .8rem; }
.logo { width: 40px; height: 40px; border-radius: 12px; display: grid; place-items: center; background: linear-gradient(135deg, #6c6cff, #a05cf0); font-weight: 700; }
.brand strong { display: block; }
.brand small { display: block; opacity: .7; font-size: .8rem; }
.nav { display: flex; gap: 1rem; }
.nav a { color: rgba(255,255,255,.8); font-weight: 500; }
.nav a.router-link-active { color: #fff; }
.logout { padding: 0; border: 0; background: transparent; color: rgba(255,255,255,.8); font: inherit; cursor: pointer; }
.logout:hover { color: #fff; }
.candidate-content { flex: 1; width: min(1100px, 100%); margin: 0 auto; padding: 2rem; }
.candidate-footer { text-align: center; padding: 1rem; color: rgba(20,20,58,.6); }
</style>
