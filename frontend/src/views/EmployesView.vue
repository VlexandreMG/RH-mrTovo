<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { dossiersApi } from '@/api/dossiers'
import { date } from '@/utils/format'
import StatutBadge from '@/components/RecrutementStatutBadge.vue'

const dossiers = ref([])
const chargement = ref(false)
const erreur = ref('')
const recherche = ref('')

const dossiersFiltres = computed(() => {
  const terme = recherche.value.trim().toLowerCase()
  if (!terme) return dossiers.value
  return dossiers.value.filter((d) =>
    [d.matricule, d.nom, d.prenom, d.email, d.posteTitre, d.departementNom]
      .filter(Boolean)
      .some((valeur) => valeur.toLowerCase().includes(terme))
  )
})

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    dossiers.value = await dossiersApi.lister()
  } catch (e) {
    erreur.value = e.message
  } finally {
    chargement.value = false
  }
}

onMounted(charger)
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Dossiers salariés</h1>
        <p>
          Référentiel central : identité, situation professionnelle, contrats, carrière et pièces
          administratives de chaque salarié. Accès réservé au RH et tracé.
        </p>
      </div>
      <button class="b3-btn" :disabled="chargement" @click="charger">Actualiser</button>
    </header>

    <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

    <div class="b3-card">
      <div class="entete-liste">
        <h2>Effectif ({{ dossiersFiltres.length }})</h2>
        <input v-model="recherche" class="recherche" type="search" placeholder="Matricule, nom, poste…" />
      </div>
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead>
            <tr>
              <th>Matricule</th>
              <th>Nom</th>
              <th>Poste</th>
              <th>Département</th>
              <th>Entrée</th>
              <th>Statut</th>
              <th>Documents</th>
              <th>Alertes</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in dossiersFiltres" :key="d.id">
              <td><strong>{{ d.matricule }}</strong></td>
              <td>
                {{ d.prenom }} {{ d.nom }}
                <div class="b3-muted" style="font-size: 0.78rem">{{ d.email }}</div>
              </td>
              <td>{{ d.posteTitre ?? '—' }}</td>
              <td>{{ d.departementNom ?? '—' }}</td>
              <td>{{ date(d.dateEntree) }}</td>
              <td><StatutBadge :statut="d.statut" /></td>
              <td>
                {{ d.nbDocuments }}
                <StatutBadge v-if="d.nbDocumentsExpires" statut="EXPIRE" />
                <StatutBadge v-else-if="d.nbDocumentsBientotExpires" statut="EXPIRE_BIENTOT" />
              </td>
              <td>
                <span v-if="d.nbAlertes" class="b3-badge is-warning">{{ d.nbAlertes }}</span>
                <span v-else class="b3-muted">—</span>
              </td>
              <td>
                <RouterLink :to="`/bloc5/dossiers/${d.id}`" class="b3-btn">Ouvrir le dossier</RouterLink>
              </td>
            </tr>
            <tr v-if="!dossiersFiltres.length">
              <td colspan="9" class="b3-empty">
                {{ dossiers.length ? 'Aucun dossier ne correspond à la recherche.'
                  : 'Aucun dossier salarié : acceptez une proposition pour le générer automatiquement.' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<style scoped>
.entete-liste {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.recherche {
  border: 1px solid var(--b3-border);
  border-radius: 8px;
  padding: 0.45rem 0.65rem;
  font-size: 0.86rem;
  min-width: 220px;
}
</style>
