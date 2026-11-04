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
      semaine en cours, heures sup à valider ou sortie à pointer. Les jours d'absence ne sont pas
      inclus dans le montant retenu : leur traitement relève du bloc Congés et absences.
    </p>

    <div class="b3-table-wrapper">
      <table class="b3-table">
        <thead>
          <tr>
            <th>Salarié</th><th>Travaillé</th><th>Dû</th><th>Compensé</th>
            <th>Retenu</th><th>H. sup payées</th><th>Montant h. sup</th><th>Montant retenu</th><th>État</th>
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
              <div class="b3-muted">{{ ligne.joursAbsents }} j d'absence</div>
            </td>
            <td>{{ duree(ligne.minutesDues) }}</td>
            <td>{{ duree(ligne.minutesCompensees) }}</td>
            <td>{{ duree(ligne.minutesRetenues) }}</td>
            <td>
              <div>{{ duree(ligne.hsPalier1) }} <span class="b3-muted">à {{ ligne.tauxPalier1 }} %</span></div>
              <div>{{ duree(ligne.hsPalier2) }} <span class="b3-muted">à {{ ligne.tauxPalier2 }} %</span></div>
            </td>
            <td class="b3-money">{{ ar(ligne.montantHeuresSup) }}</td>
            <td class="b3-money">{{ ar(ligne.montantRetenue) }}</td>
            <td>
              <span class="b3-badge" :class="ligne.provisoire ? 'is-warning' : 'is-success'">
                {{ ligne.provisoire ? 'Provisoire' : 'Prêt pour la paie' }}
              </span>
              <div v-if="ligne.semainesAValider" class="b3-muted">{{ ligne.semainesAValider }} semaine(s) à valider</div>
              <div v-if="ligne.nbAnomalies" class="b3-muted">{{ ligne.nbAnomalies }} anomalie(s)</div>
            </td>
          </tr>
          <tr v-if="!variables.length"><td colspan="9" class="b3-empty">Aucun salarié sur ce mois.</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.b3-table td {
  white-space: nowrap;
}

.mois {
  max-width: 14rem;
  margin-bottom: 0.6rem;
}
</style>
