<script setup>
import { ref, onMounted } from 'vue'
import OffreCard from './OffreCard.vue'
import { offresApi, messageErreur } from '@/services/api'

const offres = ref([])
const chargement = ref(true)
const erreur = ref('')

onMounted(async () => {
  try {
    offres.value = await offresApi.lister()
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
})
</script>

<template>
  <div class="recrutement">
    <h1>Offres d'emploi</h1>
    <p v-if="chargement" class="muted">Chargement…</p>
    <p v-else-if="erreur" class="error">Impossible de charger les offres : {{ erreur }}</p>
    <p v-else-if="!offres.length" class="muted">Aucune offre pour le moment.</p>
    <div v-else class="grid">
      <OffreCard v-for="o in offres" :key="o.id" :offre="o" />
    </div>
  </div>
</template>

<style scoped>
h1 { font-size: 22px; margin-bottom: 16px; }
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
</style>
