<script setup>
import { ref, computed, onMounted } from 'vue'
import KanbanColumn from './KanbanColumn.vue'
import { offresApi, candidaturesApi, messageErreur, STATUT } from '@/services/api'

const props = defineProps({ id: { type: [String, Number], required: true } })

const offre = ref(null)
const candidatures = ref([])
const chargement = ref(true)
const erreur = ref('')

const COLONNES = [
  { statut: STATUT.A_TRAITER, titre: 'À traiter', couleur: '#8590a2' },
  { statut: STATUT.TEST, titre: 'Test', couleur: '#0c66e4' },
  { statut: STATUT.ATTENTE_ENTRETIEN, titre: 'Attente entretien', couleur: '#e2b203' },
  { statut: STATUT.ACCEPTE, titre: 'Accepté', couleur: '#1f845a' },
  { statut: STATUT.REFUSE, titre: 'Refusé', couleur: '#c9372c' },
]

const parStatut = computed(() =>
  Object.fromEntries(
    COLONNES.map((col) => [col.statut, candidatures.value.filter((c) => c.statutId === col.statut)]),
  ),
)

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    const [o, liste] = await Promise.all([offresApi.get(props.id), candidaturesApi.parOffre(props.id)])
    offre.value = o
    candidatures.value = liste
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

/** Remplace la candidature modifiée : elle change de colonne automatiquement. */
function majCandidature(maj) {
  const i = candidatures.value.findIndex((c) => c.id === maj.id)
  if (i !== -1) candidatures.value[i] = maj
}

onMounted(charger)
</script>

<template>
  <div class="recrutement">
    <div class="entete">
      <RouterLink to="/offres" class="retour">← Retour aux offres</RouterLink>
      <h1>{{ offre?.titre ?? `Offre #${id}` }}</h1>
      <p class="muted">Suivi des candidatures : présélection → test → entretien</p>
    </div>

    <p v-if="chargement" class="muted">Chargement…</p>
    <p v-else-if="erreur" class="error">Impossible de charger les candidatures : {{ erreur }}</p>

    <div v-else class="board">
      <KanbanColumn
        v-for="col in COLONNES"
        :key="col.statut"
        :titre="col.titre"
        :couleur="col.couleur"
        :candidatures="parStatut[col.statut]"
        @maj="majCandidature"
      />
    </div>
  </div>
</template>

<style scoped>
.entete { margin-bottom: 16px; }
.retour { font-size: 13px; color: var(--primary); text-decoration: none; }
h1 { font-size: 22px; margin: 6px 0 2px; }
.entete p { margin: 0; }
.board {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  overflow-x: auto;
  padding-bottom: 12px;
}
</style>
