<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { dossiersApi } from '@/api/dossiers'
import { tempsApi } from '@/api/temps'
import { useToastStore } from '@/stores/toast'
import { date as formatDate, duree, heure } from '@/utils/format'

const JOURS = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche']

const toast = useToastStore()
const regimes = ref([])
const salaries = ref([])
const plannings = ref([])
const erreur = ref('')

const affectation = reactive({ employeId: null, regimeId: null, dateDebut: '' })
const nouveau = reactive(regimeVide())

function regimeVide() {
  return {
    code: '',
    libelle: '',
    toleranceRetardMinutes: 10,
    jours: JOURS.map((_, index) => ({
      jourSemaine: index + 1,
      travaille: index < 5,
      heureDebut: '08:00',
      heureFin: '17:00',
      pauseMinutes: 60
    }))
  }
}

function horaireDu(regime, jourSemaine) {
  return regime.horaires.find((horaire) => horaire.jourSemaine === jourSemaine)
}

async function chargerRegimes() {
  regimes.value = await tempsApi.regimes()
}

async function chargerPlannings() {
  if (!affectation.employeId) return
  const salarie = salaries.value.find((s) => s.id === affectation.employeId)
  affectation.dateDebut = salarie?.dateEntree ?? ''
  try {
    plannings.value = await tempsApi.plannings(affectation.employeId)
  } catch (e) {
    toast.error(e.message)
  }
}

async function affecter() {
  try {
    await tempsApi.affecterRegime(affectation.employeId, {
      regimeId: affectation.regimeId,
      dateDebut: affectation.dateDebut
    })
    toast.success('Régime affecté.')
    await chargerPlannings()
  } catch (e) {
    toast.error(e.message)
  }
}

async function creer() {
  try {
    await tempsApi.creerRegime({
      code: nouveau.code,
      libelle: nouveau.libelle,
      toleranceRetardMinutes: nouveau.toleranceRetardMinutes,
      horaires: nouveau.jours
        .filter((jour) => jour.travaille)
        .map(({ jourSemaine, heureDebut, heureFin, pauseMinutes }) => ({
          jourSemaine, heureDebut, heureFin, pauseMinutes
        }))
    })
    Object.assign(nouveau, regimeVide())
    toast.success('Régime créé.')
    await chargerRegimes()
  } catch (e) {
    toast.error(e.message)
  }
}

watch(() => affectation.employeId, chargerPlannings)

onMounted(async () => {
  try {
    await chargerRegimes()
    salaries.value = (await dossiersApi.lister()).filter((salarie) => salarie.statut !== 'SORTI')
    if (salaries.value.length) affectation.employeId = salaries.value[0].id
    if (regimes.value.length) affectation.regimeId = regimes.value[0].id
  } catch (e) {
    erreur.value = e.message
  }
})
</script>

<template>
  <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

  <div class="b3-card">
    <h2>Planning d'un salarié</h2>
    <form @submit.prevent="affecter">
      <div class="b3-form-grid">
        <div class="b3-field">
          <label for="salarie">Salarié</label>
          <select id="salarie" v-model="affectation.employeId" required>
            <option v-for="salarie in salaries" :key="salarie.id" :value="salarie.id">
              {{ salarie.matricule }} — {{ salarie.prenom }} {{ salarie.nom }}
            </option>
          </select>
        </div>
        <div class="b3-field">
          <label for="regime">Régime</label>
          <select id="regime" v-model="affectation.regimeId" required>
            <option v-for="regime in regimes" :key="regime.id" :value="regime.id">{{ regime.libelle }}</option>
          </select>
        </div>
        <div class="b3-field">
          <label for="debut">À partir du</label>
          <input id="debut" v-model="affectation.dateDebut" type="date" required />
        </div>
      </div>
      <div class="b3-actions actions"><button class="b3-btn is-primary">Affecter</button></div>
    </form>

    <div class="b3-table-wrapper">
      <table class="b3-table">
        <thead><tr><th>Régime</th><th>Début</th><th>Fin</th><th>Affecté par</th></tr></thead>
        <tbody>
          <tr v-for="planning in plannings" :key="planning.id">
            <td>{{ planning.regimeLibelle }}</td>
            <td>{{ formatDate(planning.dateDebut) }}</td>
            <td>{{ planning.dateFin ? formatDate(planning.dateFin) : 'En cours' }}</td>
            <td>{{ planning.affectePar }}</td>
          </tr>
          <tr v-if="!plannings.length"><td colspan="4" class="b3-empty">Aucun régime affecté.</td></tr>
        </tbody>
      </table>
    </div>
  </div>

  <div class="b3-card">
    <h2>Régimes de travail</h2>
    <div class="b3-table-wrapper">
      <table class="b3-table">
        <thead>
          <tr><th>Régime</th><th v-for="jour in JOURS" :key="jour">{{ jour.slice(0, 3) }}.</th><th>Semaine</th><th>Tolérance</th></tr>
        </thead>
        <tbody>
          <tr v-for="regime in regimes" :key="regime.id">
            <td><strong>{{ regime.libelle }}</strong><div class="b3-muted">{{ regime.code }}</div></td>
            <td v-for="(jour, index) in JOURS" :key="jour">
              <template v-if="horaireDu(regime, index + 1)">
                {{ heure(horaireDu(regime, index + 1).heureDebut) }}–{{ heure(horaireDu(regime, index + 1).heureFin) }}
              </template>
              <span v-else class="b3-muted">Repos</span>
            </td>
            <td>{{ duree(regime.minutesHebdomadaires) }}</td>
            <td>{{ regime.toleranceRetardMinutes }} min</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <div class="b3-card">
    <h2>Nouveau régime</h2>
    <p class="b3-muted">Un régime n'est plus modifiable après sa création : cela protège le calcul des semaines passées.</p>
    <form @submit.prevent="creer">
      <div class="b3-form-grid">
        <div class="b3-field"><label for="code">Code *</label><input id="code" v-model="nouveau.code" maxlength="50" required /></div>
        <div class="b3-field"><label for="libelle">Libellé *</label><input id="libelle" v-model="nouveau.libelle" required /></div>
        <div class="b3-field">
          <label for="tolerance">Tolérance de retard (min)</label>
          <input id="tolerance" v-model.number="nouveau.toleranceRetardMinutes" type="number" min="0" max="60" />
        </div>
      </div>
      <div class="b3-table-wrapper grille">
        <table class="b3-table">
          <thead><tr><th>Jour</th><th>Travaillé</th><th>Début</th><th>Fin</th><th>Pause (min)</th></tr></thead>
          <tbody>
            <tr v-for="(jour, index) in nouveau.jours" :key="jour.jourSemaine">
              <td>{{ JOURS[index] }}</td>
              <td><input v-model="jour.travaille" type="checkbox" :aria-label="`${JOURS[index]} travaillé`" /></td>
              <td><input v-model="jour.heureDebut" type="time" :disabled="!jour.travaille" aria-label="Début" /></td>
              <td><input v-model="jour.heureFin" type="time" :disabled="!jour.travaille" aria-label="Fin" /></td>
              <td><input v-model.number="jour.pauseMinutes" type="number" min="0" :disabled="!jour.travaille" aria-label="Pause" /></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="b3-actions actions"><button class="b3-btn is-primary">Créer le régime</button></div>
    </form>
  </div>
</template>

<style scoped>
.actions {
  justify-content: flex-end;
  margin: 0.8rem 0;
}

.grille {
  margin-top: 0.8rem;
}
</style>
