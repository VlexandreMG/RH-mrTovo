<script setup>
import CandidatureCard from '@/components/candidature/CandidatureCard.vue'

defineProps({
  titre: { type: String, required: true },
  candidatures: { type: Array, required: true },
  couleur: { type: String, default: 'var(--muted)' },
})
const emit = defineEmits(['maj'])
</script>

<template>
  <section class="colonne">
    <header :style="{ borderTopColor: couleur }">
      <h2>{{ titre }}</h2>
      <span class="compteur">{{ candidatures.length }}</span>
    </header>
    <div class="cartes">
      <CandidatureCard
        v-for="c in candidatures"
        :key="c.id"
        :candidature="c"
        @maj="(maj) => emit('maj', maj)"
      />
      <p v-if="!candidatures.length" class="vide muted">Aucune candidature</p>
    </div>
  </section>
</template>

<style scoped>
.colonne {
  background: #ebecf0;
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  min-width: 260px;
  flex: 1;
}
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-top: 3px solid;
  border-radius: var(--radius) var(--radius) 0 0;
}
h2 { font-size: 13px; text-transform: uppercase; letter-spacing: 0.03em; }
.compteur { font-size: 12px; background: var(--surface); border-radius: 10px; padding: 0 8px; }
.cartes { display: flex; flex-direction: column; gap: 8px; padding: 0 8px 8px; }
.vide { font-size: 12px; text-align: center; margin: 8px 0; }
</style>
