<script setup>
import { onMounted, ref, watch } from 'vue'
import { tempsApi } from '@/api/temps'
import { ar, dateIso, duree } from '@/utils/format'

function moisPrecedent() {
  const jour = new Date()
  jour.setDate(1)
  jour.setMonth(jour.getMonth() - 1)
  return dateIso(jour).slice(0, 7)
}

const mois = ref(moisPrecedent())
const variables = ref([])
const erreur = ref('')

async function charger() {
  erreur.value = ''
  try {
    variables.value = await tempsApi.variablesPaie(mois.value)
  } catch (e) {
    erreur.value = e.message
  }
}

function totalRetenues(ligne) {
  if (ligne.montantRetenue === null) return ligne.montantRetenueAbsences
  return Number(ligne.montantRetenue) + Number(ligne.montantRetenueAbsences ?? 0)
}

watch(mois, charger)
onMounted(charger)
</script>

<template>
  <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

  <div class="b3-card">
    <div class="b3-field mois">
      <label for="mois">Mois de paie</label>
      <input id="mois" v-model="mois" type="month" />
    </div>
    <p class="b3-muted">
      Une semaine appartient au mois de son lundi. Une ligne « Provisoire » peut encore changer :
      semaine en cours, heures sup à valider, sortie à pointer ou absence à qualifier. Les retenues
      regroupent retards, absences non justifiées et congés sans solde (salaire brut / 30 par jour).
    </p>

    <div class="b3-table-wrapper">
      <table class="b3-table">
        <thead>
          <tr>
            <th>Salarié</th><th>Travaillé</th><th>Retards</th><th>H. sup payées</th>
            <th>Heures sup</th><th>Retenues</th><th>Indemnité</th><th>État</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ligne in variables" :key="ligne.employeId">
            <td>
              <strong>{{ ligne.nomComplet }}</strong>
              <div class="b3-muted">{{ ligne.matricule }}</div>
              <div class="b3-muted">{{ ar(ligne.tauxHoraire) }} / h</div>
            </td>
            <td>
              <div>{{ duree(ligne.minutesTravaillees) }}</div>
              <div class="b3-muted">{{ ligne.joursConge }} j de congé</div>
              <div class="b3-muted">{{ ligne.joursAbsents }} j d'absence</div>
            </td>
            <td>
              <div>{{ duree(ligne.minutesDues) }} <span class="b3-muted">dus</span></div>
              <div>{{ duree(ligne.minutesCompensees) }} <span class="b3-muted">compensés</span></div>
              <div>{{ duree(ligne.minutesRetenues) }} <span class="b3-muted">retenus</span></div>
            </td>
            <td>
              <div>{{ duree(ligne.hsPalier1) }} <span class="b3-muted">à {{ ligne.tauxPalier1 }} %</span></div>
              <div>{{ duree(ligne.hsPalier2) }} <span class="b3-muted">à {{ ligne.tauxPalier2 }} %</span></div>
            </td>
            <td class="b3-money">{{ ar(ligne.montantHeuresSup) }}</td>
            <td>
              <div class="b3-money">{{ ar(totalRetenues(ligne)) }}</div>
              <div class="b3-muted">dont absences {{ ar(ligne.montantRetenueAbsences) }}</div>
            </td>
            <td class="b3-money">{{ ar(ligne.montantIndemniteConge) }}</td>
            <td>
              <span class="b3-badge" :class="ligne.provisoire ? 'is-warning' : 'is-success'">
                {{ ligne.provisoire ? 'Provisoire' : 'Prêt pour la paie' }}
              </span>
              <div v-if="ligne.semainesAValider" class="b3-muted">{{ ligne.semainesAValider }} semaine(s) à valider</div>
              <div v-if="ligne.joursAbsenceAQualifier" class="b3-muted">{{ ligne.joursAbsenceAQualifier }} absence(s) à qualifier</div>
              <div v-if="ligne.nbAnomalies" class="b3-muted">{{ ligne.nbAnomalies }} anomalie(s)</div>
            </td>
          </tr>
          <tr v-if="!variables.length"><td colspan="8" class="b3-empty">Aucun salarié sur ce mois.</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.b3-table td {
  white-space: nowrap;
}

/* L'état porte les textes les plus longs : c'est la seule colonne autorisée à passer à la ligne. */
.b3-table td:last-child {
  white-space: normal;
  min-width: 8.5rem;
}

.mois {
  max-width: 14rem;
  margin-bottom: 0.6rem;
}
</style>
