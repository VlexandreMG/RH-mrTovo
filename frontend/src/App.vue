<script setup>
<<<<<<< HEAD
import { RouterView } from 'vue-router'
import ToastHost from '@/components/ToastHost.vue'
</script>

<template>
  <RouterView />
  <ToastHost />
</template>
=======
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header class="topbar">
    <RouterLink to="/offres" class="brand">Recrutement</RouterLink>
  </header>
  <main class="page">
    <RouterView />
  </main>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 24px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.brand {
  font-weight: 700;
  font-size: 1.05rem;
  color: var(--text);
  text-decoration: none;
}
.page {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
}
</style>
>>>>>>> bloc2-bis
