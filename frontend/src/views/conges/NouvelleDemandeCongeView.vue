<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { salarieApi } from '@/api/salarie'
import { useToastStore } from '@/stores/toast'
import { date as formatDate, nb } from '@/utils/format'
import { enregistrerBlob } from '@/utils/fichier'
import StatutBadge from '@/components/RecrutementStatutBadge.vue'

const toast = useToastStore()
const profil = ref(null)
const demandes = ref([])
const erreur = ref('')
const envoi = ref(false)

const formulaire = reactive({ typeCongeId: null, dateDebut: '', dateFin: '', motif: '' })
const justificatif = ref(null)
const champFichier = ref(null)

const solde = computed(() => profil.value?.solde)
const typeChoisi = computed(() => profil.value?.types.find((t) => t.id === formulaire.typeCongeId))
const nbJours = computed(() => {
  if (!formulaire.dateDebut || !formulaire.dateFin || formulaire.dateFin < formulaire.dateDebut) return 0
  const debut = new Date(`${formulaire.dateDebut}T12:00:00`)
  const fin = new Date(`${formulaire.dateFin}T12:00:00`)
  return Math.round((fin - debut) / 86400000) + 1
})

async function charger() {
  erreur.value = ''
  try {
    const moi = await salarieApi.moi()
    profil.value = moi
    demandes.value = await salarieApi.demandes()
    if (!formulaire.typeCongeId && moi.types.length) {
      formulaire.typeCongeId = (moi.types.find((t) => t.code === 'CONGE_PAYE') ?? moi.types[0]).id
    }
  } catch (e) {
    erreur.value = e.message
  }
}

function peutAnnuler(demande) {
  return ['EN_ATTENTE_MANAGER', 'EN_ATTENTE_RH'].includes(demande.statut)
    || (demande.statut === 'VALIDEE' && new Date(`${demande.dateDebut}T12:00:00`) > new Date())
}

async function envoyer() {
  envoi.value = true
  try {
    await salarieApi.demander(formulaire, justificatif.value)
    Object.assign(formulaire, { dateDebut: '', dateFin: '', motif: '' })
    justificatif.value = null
    if (champFichier.value) champFichier.value.value = ''
    toast.success('Demande envoyée.')
    await charger()
  } catch (e) {
    toast.error(e.message)
  } finally {
    envoi.value = false
  }
}

async function annuler(demande) {
  if (!window.confirm('Annuler cette demande de congé ?')) return
  try {
    await salarieApi.annuler(demande.id)
    toast.success('Demande annulée.')
    await charger()
  } catch (e) {
    toast.error(e.message)
  }
}

async function telecharger(demande) {
  try {
    enregistrerBlob(await salarieApi.justificatif(demande.id), demande.justificatifNom ?? `justificatif-${demande.id}`)
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(charger)
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Demander un congé</h1>
        <p>Votre demande part au manager de votre département, puis au RH qui décide.</p>
      </div>
    </header>

    <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

    <template v-if="profil">
      <div v-if="profil.soldeSuivi && solde" class="b3-grid b3-grid-4 compteurs">
        <div class="b3-kpi"><span>Disponible</span><strong>{{ nb(solde.disponible) }} j</strong><small>à demander</small></div>
        <div class="b3-kpi"><span>Solde</span><strong>{{ nb(solde.solde) }} j</strong><small>{{ nb(solde.joursEnAttente) }} j en attente</small></div>
        <div class="b3-kpi"><span>Acquis</span><strong>{{ nb(solde.joursAcquis) }} j</strong><small>{{ solde.moisComplets }} mois de service</small></div>
        <div class="b3-kpi"><span>Pris</span><strong>{{ nb(solde.joursPris) }} j</strong><small>congés validés</small></div>
      </div>
      <div v-else class="b3-alert">
        Votre compte n'est pas rattaché à un dossier issu du recrutement : la demande est transmise
        directement au RH et aucun solde n'est décompté.
      </div>

      <div class="b3-card">
        <h2>Nouvelle demande</h2>
        <form @submit.prevent="envoyer">
          <div class="b3-form-grid">
            <div class="b3-field">
              <label for="type">Type de congé</label>
              <select id="type" v-model="formulaire.typeCongeId" required>
                <option v-for="type in profil.types" :key="type.id" :value="type.id">{{ type.libelle }}</option>
              </select>
            </div>
            <div class="b3-field">
              <label for="motif">Motif</label>
              <input id="motif" v-model="formulaire.motif" maxlength="255" />
            </div>
            <div class="b3-field"><label for="debut">Du</label><input id="debut" v-model="formulaire.dateDebut" type="date" required /></div>
            <div class="b3-field"><label for="fin">Au</label><input id="fin" v-model="formulaire.dateFin" type="date" required /></div>
            <div class="b3-field is-full">
              <label for="justificatif">
                Justificatif (PDF, PNG, JPG){{ typeChoisi?.justificatifRequis ? ' — obligatoire, peut être ajouté plus tard' : '' }}
              </label>
              <input
                id="justificatif"
                ref="champFichier"
                type="file"
                accept=".pdf,.png,.jpg,.jpeg"
                @change="justificatif = $event.target.files?.[0] ?? null"
              />
            </div>
          </div>
          <div class="b3-actions actions">
            <span class="b3-muted">
              {{ nbJours }} jour(s) calendaire(s){{ typeChoisi?.decompteSolde && profil.soldeSuivi ? ' décomptés du solde' : '' }}
            </span>
            <button class="b3-btn is-primary" :disabled="envoi">Envoyer la demande</button>
          </div>
        </form>
      </div>

      <div class="b3-card">
        <h2>Mes demandes</h2>
        <div class="b3-table-wrapper">
          <table class="b3-table">
            <thead><tr><th>Type</th><th>Période</th><th>Jours</th><th>État</th><th>Commentaire</th><th></th></tr></thead>
            <tbody>
              <tr v-for="demande in demandes" :key="demande.id">
                <td>{{ demande.typeLibelle }}</td>
                <td>{{ formatDate(demande.dateDebut) }} → {{ formatDate(demande.dateFin) }}</td>
                <td>{{ demande.nbJours }}</td>
                <td><StatutBadge :statut="demande.statut" /></td>
                <td class="b3-muted">{{ demande.commentaireRh ?? demande.commentaireManager ?? '—' }}</td>
                <td class="b3-actions">
                  <button v-if="demande.justificatifDisponible" class="b3-btn" @click="telecharger(demande)">Justificatif</button>
                  <button v-if="peutAnnuler(demande)" class="b3-btn is-ghost" @click="annuler(demande)">Annuler</button>
                </td>
              </tr>
              <tr v-if="!demandes.length"><td colspan="6" class="b3-empty">Aucune demande pour l'instant.</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.compteurs {
  margin-bottom: 1rem;
}

.actions {
  justify-content: space-between;
  align-items: center;
  margin-top: 0.8rem;
}
</style>
