<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ROLES } from '@/utils/format'
import AppIcon from './AppIcon.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const menu = computed(() => [
  { to: '/', label: 'Tableau de bord', icon: 'dashboard', exact: true },
  { to: '/demandes', label: 'Demandes', icon: 'list' },
  ...(auth.role === 'MANAGER' ? [{ to: '/demandes/nouvelle', label: 'Nouvelle demande', icon: 'plus' }] : []),
  { to: '/grille-salariale', label: 'Grille salariale', icon: 'coins' }
])

function isActive(m) {
  if (m.to === '/') return route.path === '/'
  if (m.to === '/demandes') return route.path === '/demandes' || /^\/demandes\/\d+/.test(route.path)
  return route.path.startsWith(m.to)
}

function deconnexion() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo"><AppIcon name="users" :size="20" /></div>
        <div>
          <strong>Besoin en personnel</strong>
          <small>Gestion des recrutements</small>
        </div>
      </div>

      <nav>
        <RouterLink v-for="m in menu" :key="m.to" :to="m.to" class="nav-item"
          :class="{ active: isActive(m) }" active-class="" exact-active-class="">
          <AppIcon :name="m.icon" />
          <span>{{ m.label }}</span>
        </RouterLink>
      </nav>

      <div class="user">
        <div class="avatar">{{ auth.initiales }}</div>
        <div class="who">
          <strong>{{ auth.user?.nom }}</strong>
          <small>{{ ROLES[auth.role] }}</small>
        </div>
        <button class="logout" title="Se déconnecter" @click="deconnexion"><AppIcon name="logout" /></button>
      </div>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.shell { display: grid; grid-template-columns: 264px 1fr; min-height: 100%; }
.sidebar { background: linear-gradient(180deg, #14143a 0%, #0d0d24 100%); color: #c9cbe6; padding: 1.4rem 1rem; display: flex; flex-direction: column; gap: 1.8rem; position: sticky; top: 0; height: 100vh; }
.brand { display: flex; align-items: center; gap: .8rem; padding: 0 .5rem; }
.logo { width: 40px; height: 40px; border-radius: 12px; display: grid; place-items: center; color: #fff; background: linear-gradient(135deg, #6c6cff, #a05cf0); box-shadow: 0 8px 20px -6px rgba(124, 92, 255, .7); }
.brand strong { display: block; color: #fff; font-family: var(--font-display); font-size: .98rem; line-height: 1.2; }
.brand small { color: #8a8db5; font-size: .76rem; }
nav { display: flex; flex-direction: column; gap: .3rem; flex: 1; }
.nav-item { display: flex; align-items: center; gap: .8rem; padding: .7rem .9rem; border-radius: 10px; color: #aeb1d4; font-weight: 500; transition: all .15s; }
.nav-item:hover { background: rgba(255, 255, 255, .06); color: #fff; }
.nav-item.active { background: rgba(108, 108, 255, .18); color: #fff; box-shadow: inset 3px 0 0 #8c8cff; }
.user { display: flex; align-items: center; gap: .7rem; padding: .8rem; border-radius: 12px; background: rgba(255, 255, 255, .06); }
.avatar { width: 38px; height: 38px; border-radius: 50%; display: grid; place-items: center; background: linear-gradient(135deg, #6c6cff, #a05cf0); color: #fff; font-weight: 700; font-size: .85rem; flex-shrink: 0; }
.who { min-width: 0; flex: 1; }
.who strong { display: block; color: #fff; font-size: .85rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.who small { color: #8a8db5; font-size: .75rem; }
.logout { background: none; border: none; color: #9a9dc4; cursor: pointer; padding: .35rem; border-radius: 8px; display: grid; }
.logout:hover { color: #fff; background: rgba(255, 255, 255, .1); }
.content { padding: 2rem 2.4rem 3rem; min-width: 0; max-width: 1280px; width: 100%; margin: 0 auto; }

@media (max-width: 900px) {
  .shell { grid-template-columns: 1fr; }
  .sidebar { position: static; height: auto; flex-direction: row; align-items: center; flex-wrap: wrap; gap: .8rem; padding: .8rem 1rem; }
  nav { flex-direction: row; flex-wrap: wrap; order: 3; width: 100%; }
  .nav-item span { font-size: .85rem; }
  .brand small, .who { display: none; }
  .user { margin-left: auto; padding: .4rem; }
  .content { padding: 1.2rem; }
}
</style>
