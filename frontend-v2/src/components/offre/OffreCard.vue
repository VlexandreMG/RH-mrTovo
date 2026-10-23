<script setup>
defineProps({ offre: { type: Object, required: true } })
</script>

<template>
  <RouterLink :to="{ name: 'kanban', params: { id: offre.id } }" class="card">
    <div class="head">
      <h3>{{ offre.titre }}</h3>
      <span class="badge" :class="{ fermee: offre.statut === 'Fermée' }">{{ offre.statut }}</span>
    </div>
    <p class="desc">{{ offre.description }}</p>
    <div class="foot muted">
      <span>Publiée le {{ new Date(offre.datePublication).toLocaleDateString('fr-FR') }}</span>
      <span>{{ offre.nbCandidatures }} candidature(s)</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.card {
  display: block;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
  color: inherit;
  text-decoration: none;
  transition: box-shadow 0.15s;
}
.card:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); }
.head { display: flex; justify-content: space-between; gap: 8px; align-items: start; }
h3 { font-size: 15px; }
.badge {
  font-size: 12px; padding: 1px 8px; border-radius: 10px;
  background: var(--success-bg); color: var(--success); white-space: nowrap;
}
.badge.fermee { background: var(--surface-2); color: var(--muted); }
.desc { margin: 8px 0 12px; }
.foot { display: flex; justify-content: space-between; font-size: 12px; }
</style>
