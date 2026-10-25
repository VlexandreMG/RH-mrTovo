<template>
  <section class="offers">
    <header class="page-header">
      <div>
        <h1>Offres d'emploi</h1>
        <p>Découvrez les postes ouverts et postulez en ligne</p>
      </div>
    </header>
    <div v-if="loading" class="state">Chargement...</div>
    <div v-else-if="erreur" class="state error">{{ erreur }}</div>
    <div v-else-if="!offres.length" class="state">Aucune offre ouverte pour le moment.</div>
    <div class="grid">
      <article v-for="offre in offres" :key="offre.id" class="card">
        <header>
          <div>
            <h2>{{ offre.titre }}</h2>
            <small>Publiée le {{ formatDate(offre.datePublication) }}</small>
          </div>
          <span class="badge">{{ offre.statut }}</span>
        </header>
        <p class="description">{{ offre.description }}</p>
        <footer>
          <div class="meta">
            <span>{{ offre.nombreCandidatures }} candidature(s)</span>
          </div>
          <RouterLink class="btn primary" :to="`/candidats/offres/${offre.id}`">Voir et postuler</RouterLink>
        </footer>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { apiGet } from '@/api/http'

const offres = ref([])
const loading = ref(true)
const erreur = ref('')

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('fr-FR')
}

onMounted(async () => {
  try {
    offres.value = await apiGet('/public/offres')
  } catch (e) {
    erreur.value = e.message || 'Impossible de charger les offres'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-header { margin-bottom: 1.8rem; }
.page-header h1 { font-size: 2rem; margin: 0; color: #14143a; }
.page-header p { margin: .4rem 0 0; color: rgba(20,20,58,.7); }
.state { text-align: center; padding: 2rem; background: #fff; border-radius: 16px; box-shadow: 0 40px 60px -40px rgba(108,108,255,.4); }
.state.error { color: #d64545; }
.grid { display: grid; gap: 1.2rem; }
.card { background: #fff; border-radius: 18px; box-shadow: 0 60px 80px -60px rgba(108,108,255,.5); overflow: hidden; display: flex; flex-direction: column; }
.card header { display: flex; align-items: flex-start; justify-content: space-between; padding: 1.8rem 2rem 0; gap: 1rem; }
.card h2 { margin: 0; font-size: 1.4rem; color: #14143a; }
.card small { color: rgba(20,20,58,.6); }
.badge { padding: .4rem .9rem; border-radius: 999px; background: rgba(108,108,255,.16); color: #4f4fff; font-weight: 600; font-size: .85rem; }
.description { padding: 1rem 2rem 0; margin: 0; color: rgba(20,20,58,.8); white-space: pre-wrap; line-height: 1.7; }
footer { display: flex; align-items: center; justify-content: space-between; padding: 1.6rem 2rem; margin-top: auto; }
.meta { color: rgba(20,20,58,.6); }
.btn { display: inline-flex; align-items: center; gap: .4rem; padding: .7rem 1.4rem; border-radius: 999px; font-weight: 600; }
.btn.primary { background: linear-gradient(135deg, #6c6cff, #a05cf0); color: #fff; box-shadow: 0 20px 40px -20px rgba(108,108,255,.9); }
</style>
