<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

function deconnecter() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="coquille">
    <header class="entete">
      <div>
        <strong>Espace salarié</strong>
        <small>{{ auth.user?.nom }}</small>
      </div>
      <nav class="liens">
        <RouterLink to="/salarie" exact-active-class="actif">Mon espace</RouterLink>
        <RouterLink to="/salarie/demande-conge" active-class="actif">Demander un congé</RouterLink>
      </nav>
      <button class="b3-btn" type="button" @click="deconnecter">Déconnexion</button>
    </header>
    <main class="contenu">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.coquille {
  min-height: 100vh;
  background: var(--b3-bg, #f3f4fb);
}

.entete {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.9rem 2rem;
  background: var(--b3-surface, #fff);
  border-bottom: 1px solid var(--b3-border);
}

.entete strong,
.entete small {
  display: block;
}

.entete small {
  color: var(--b3-text-soft);
}

.liens {
  display: flex;
  gap: 0.35rem;
  margin-left: auto;
  margin-right: 1rem;
}

.liens a {
  padding: 0.45rem 0.75rem;
  border-radius: 8px;
  font-size: 0.85rem;
  color: var(--b3-text-soft);
}

.liens a:hover {
  background: var(--b3-bg, #f3f4fb);
}

.liens a.actif {
  color: var(--b3-primary);
  background: var(--b3-bg, #f3f4fb);
  font-weight: 600;
}

.contenu {
  max-width: 1040px;
  margin: 0 auto;
  padding: 1.5rem 1rem;
}
</style>
