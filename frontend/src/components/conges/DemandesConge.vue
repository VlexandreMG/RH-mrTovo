<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { congesApi } from '@/api/conges'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { date as formatDate, nb } from '@/utils/format'
import { enregistrerBlob } from '@/utils/fichier'
import StatutBadge from '@/components/RecrutementStatutBadge.vue'

const auth = useAuthStore()
const toast = useToastStore()
const estRh = computed(() => auth.role === 'RH')

const demandes = ref([])
const filtre = ref('A_TRAITER')
const commentaires = reactive({})
const erreur = ref('')

const statutATraiter = computed(() => (estRh.value ? 'EN_ATTENTE_RH' : 'EN_ATTENTE_MANAGER'))
const visibles = computed(() => {
  if (filtre.value === 'A_TRAITER') return demandes.value.filter((d) => d.statut === statutATraiter.value)
  if (filtre.value === 'VALIDEE') return demandes.value.filter((d) => d.statut === 'VALIDEE')
  return demandes.value
})
const aTraiter = computed(() => demandes.value.filter((d) => d.statut === statutATraiter.value).length)

async function charger() {
  erreur.value = ''
  try {
    demandes.value = await congesApi.demandes()
  } catch (e) {
    erreur.value = e.message
  }
}

function peutDecider(demande) {
  return demande.statut === statutATraiter.value
}

function peutAnnuler(demande) {
  return estRh.value && demande.statut === 'VALIDEE' && new Date(`${demande.dateDebut}T12:00:00`) > new Date()
}

async function decider(demande, accepte) {
  const decision = { accepte, commentaire: commentaires[demande.id] ?? '' }
  try {
    if (estRh.value) await congesApi.decisionRh(demande.id, decision)
    else await congesApi.avisManager(demande.id, decision)
    toast.success(accepte ? 'Demande acceptée.' : 'Demande refusée.')
    await charger()
  } catch (e) {
    toast.error(e.message)
  }
}

async function annuler(demande) {
  const commentaire = window.prompt('Motif de l\'annulation (obligatoire) :')
  if (!commentaire?.trim()) return
  try {
    await congesApi.annuler(demande.id, commentaire)
    toast.success('Congé annulé : les jours reviennent au solde.')
    await charger()
  } catch (e) {
    toast.error(e.message)
  }
}

async function telecharger(demande) {
  try {
    enregistrerBlob(await congesApi.justificatif(demande.id), demande.justificatifNom ?? `justificatif-${demande.id}`)
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(charger)
</script>

<template>
  <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

  <div class="b3-card">
    <div class="b3-actions filtres">
      <button class="b3-btn" :class="{ 'is-primary': filtre === 'A_TRAITER' }" @click="filtre = 'A_TRAITER'">À traiter ({{ aTraiter }})</button>
      <button class="b3-btn" :class="{ 'is-primary': filtre === 'VALIDEE' }" @click="filtre = 'VALIDEE'">Congés validés</button>
      <button class="b3-btn" :class="{ 'is-primary': filtre === 'TOUTES' }" @click="filtre = 'TOUTES'">Toutes</button>
    </div>

    <div class="b3-table-wrapper">
      <table class="b3-table">
        <thead><tr><th>Salarié</th><th>Congé</th><th>Période</th><th>Solde dispo.</th><th>État</th><th>Décision</th></tr></thead>
        <tbody>
          <tr v-for="demande in visibles" :key="demande.id">
            <td>
              <strong>{{ demande.nomComplet }}</strong>
              <div class="b3-muted">{{ demande.matricule }} · {{ demande.departementNom ?? '—' }}</div>
            </td>
            <td>
              {{ demande.typeLibelle }}
              <div v-if="demande.motif" class="b3-muted">{{ demande.motif }}</div>
              <button v-if="demande.justificatifDisponible" class="b3-btn is-ghost" @click="telecharger(demande)">Justificatif</button>
              <div v-else-if="demande.justificatifRequis" class="alerte">Justificatif manquant</div>
            </td>
            <td>
              {{ formatDate(demande.dateDebut) }} → {{ formatDate(demande.dateFin) }}
              <div class="b3-muted">{{ demande.nbJours }} jour(s)</div>
            </td>
            <td>{{ demande.soldeDisponible !== null ? `${nb(demande.soldeDisponible)} j` : '—' }}</td>
            <td>
              <StatutBadge :statut="demande.statut" />
              <div v-if="demande.commentaireManager" class="b3-muted">Manager : {{ demande.commentaireManager }}</div>
              <div v-if="demande.commentaireRh" class="b3-muted">RH : {{ demande.commentaireRh }}</div>
            </td>
            <td class="decision">
              <template v-if="peutDecider(demande)">
                <input v-model="commentaires[demande.id]" placeholder="Commentaire (obligatoire si refus)" aria-label="Commentaire" />
                <div class="b3-actions">
                  <button class="b3-btn is-danger" @click="decider(demande, false)">Refuser</button>
                  <button class="b3-btn is-primary" @click="decider(demande, true)">Accepter</button>
                </div>
              </template>
              <button v-else-if="peutAnnuler(demande)" class="b3-btn is-ghost" @click="annuler(demande)">Annuler</button>
            </td>
          </tr>
          <tr v-if="!visibles.length"><td colspan="6" class="b3-empty">Aucune demande.</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.filtres {
  margin-bottom: 0.8rem;
}

.decision {
  min-width: 14rem;
}

.decision input {
  width: 100%;
  margin-bottom: 0.3rem;
}

.alerte {
  color: var(--b3-danger);
  font-size: 0.78rem;
}
</style>
