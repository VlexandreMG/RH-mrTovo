<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { congesApi } from '@/api/conges'
import { useToastStore } from '@/stores/toast'
import { ar, dateHeure, dateIso, nb } from '@/utils/format'

const toast = useToastStore()
const soldes = ref([])
const erreur = ref('')
const selection = ref(null)
const mouvements = ref([])

const ajustement = reactive({ jours: null, motif: '' })
const indemnisation = reactive({ jours: null, moisPaie: dateIso().slice(0, 7), motif: '' })
const motDePasse = ref('')

const salarie = computed(() => soldes.value.find((s) => s.employeId === selection.value))
const estimation = computed(() => {
  if (!salarie.value?.salaireBrut || !indemnisation.jours) return null
  return (Number(salarie.value.salaireBrut) * Number(indemnisation.jours)) / 30
})

async function charger() {
  erreur.value = ''
  try {
    soldes.value = await congesApi.soldes()
  } catch (e) {
    erreur.value = e.message
  }
}

async function ouvrir(employeId) {
  selection.value = employeId
  try {
    mouvements.value = await congesApi.mouvements(employeId)
  } catch (e) {
    toast.error(e.message)
  }
}

async function apres(action, message) {
  try {
    await action()
    toast.success(message)
    await charger()
    await ouvrir(selection.value)
    return true
  } catch (e) {
    toast.error(e.message)
    return false
  }
}

async function ajuster() {
  const ok = await apres(() => congesApi.ajuster(selection.value, ajustement), 'Solde ajusté.')
  if (ok) Object.assign(ajustement, { jours: null, motif: '' })
}

async function indemniser() {
  const ok = await apres(() => congesApi.indemniser(selection.value, indemnisation), 'Indemnité ajoutée aux variables de paie.')
  if (ok) Object.assign(indemnisation, { jours: null, motif: '' })
}

async function creerCompte() {
  const ok = await apres(() => congesApi.creerCompte(selection.value, motDePasse.value), 'Compte créé : transmettez le mot de passe au salarié.')
  if (ok) motDePasse.value = ''
}

onMounted(charger)
</script>

<template>
  <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

  <div class="b3-card">
    <p class="b3-muted">Droit : 2,5 jours par mois de service complet. Indemnité = salaire brut × jours non pris / 30.</p>
    <div class="b3-table-wrapper">
      <table class="b3-table">
        <thead><tr><th>Salarié</th><th>Acquis</th><th>Pris</th><th>Solde</th><th>En attente</th><th>Disponible</th><th>Indemnité</th><th>Compte</th><th></th></tr></thead>
        <tbody>
          <tr v-for="ligne in soldes" :key="ligne.employeId">
            <td><strong>{{ ligne.nomComplet }}</strong><div class="b3-muted">{{ ligne.matricule }}</div></td>
            <td>{{ nb(ligne.joursAcquis) }} j</td>
            <td>{{ nb(ligne.joursPris) }} j</td>
            <td>{{ nb(ligne.solde) }} j</td>
            <td>{{ nb(ligne.joursEnAttente) }} j</td>
            <td><strong>{{ nb(ligne.disponible) }} j</strong></td>
            <td class="b3-money">{{ ar(ligne.indemniteCompensatrice) }}</td>
            <td><span class="b3-badge" :class="ligne.compteActif ? 'is-success' : 'is-warning'">{{ ligne.compteActif ? 'Actif' : 'Aucun' }}</span></td>
            <td><button class="b3-btn" @click="ouvrir(ligne.employeId)">Gérer</button></td>
          </tr>
          <tr v-if="!soldes.length"><td colspan="9" class="b3-empty">Aucun salarié.</td></tr>
        </tbody>
      </table>
    </div>
  </div>

  <template v-if="salarie">
    <div class="b3-grid b3-grid-3">
      <div v-if="!salarie.compteActif" class="b3-card">
        <h2>Compte salarié</h2>
        <p class="b3-muted">Le salarié se connecte avec l'email de son dossier.</p>
        <form @submit.prevent="creerCompte">
          <div class="b3-field">
            <label for="mdp">Mot de passe initial (8 caractères minimum)</label>
            <input id="mdp" v-model="motDePasse" type="text" autocomplete="off" minlength="8" maxlength="72" required />
          </div>
          <div class="b3-actions actions"><button class="b3-btn is-primary">Créer le compte</button></div>
        </form>
      </div>

      <div class="b3-card">
        <h2>Ajuster le solde</h2>
        <form @submit.prevent="ajuster">
          <div class="b3-field"><label for="ajust">Jours (négatif pour retirer)</label><input id="ajust" v-model.number="ajustement.jours" type="number" step="0.5" required /></div>
          <div class="b3-field"><label for="ajustMotif">Motif</label><input id="ajustMotif" v-model="ajustement.motif" maxlength="255" required /></div>
          <div class="b3-actions actions"><button class="b3-btn is-primary">Ajuster</button></div>
        </form>
      </div>

      <div class="b3-card">
        <h2>Indemniser des jours</h2>
        <form @submit.prevent="indemniser">
          <div class="b3-field"><label for="indJours">Jours non pris</label><input id="indJours" v-model.number="indemnisation.jours" type="number" min="0.5" step="0.5" required /></div>
          <div class="b3-field"><label for="indMois">Mois de paie</label><input id="indMois" v-model="indemnisation.moisPaie" type="month" required /></div>
          <div class="b3-field"><label for="indMotif">Motif</label><input id="indMotif" v-model="indemnisation.motif" maxlength="255" required /></div>
          <p v-if="estimation" class="b3-muted">Montant estimé : {{ ar(estimation) }}</p>
          <div class="b3-actions actions"><button class="b3-btn is-primary">Indemniser</button></div>
        </form>
      </div>
    </div>

    <div class="b3-card">
      <h2>Mouvements de {{ salarie.nomComplet }}</h2>
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead><tr><th>Date</th><th>Type</th><th>Jours</th><th>Motif</th><th>Montant</th><th>Par</th></tr></thead>
          <tbody>
            <tr v-for="mouvement in mouvements" :key="mouvement.id">
              <td>{{ dateHeure(mouvement.dateCreation) }}</td>
              <td>{{ mouvement.typeDroit === 'INDEMNISATION' ? 'Indemnisation' : 'Ajustement' }}</td>
              <td>{{ mouvement.jours }}</td>
              <td>{{ mouvement.motif }}</td>
              <td class="b3-money">{{ ar(mouvement.montant) }}</td>
              <td>{{ mouvement.creePar }}</td>
            </tr>
            <tr v-if="!mouvements.length"><td colspan="6" class="b3-empty">Aucun mouvement.</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </template>
</template>

<style scoped>
.actions {
  justify-content: flex-end;
  margin-top: 0.8rem;
}

.b3-field + .b3-field {
  margin-top: 0.6rem;
}
</style>
