<script setup>
import { onMounted, reactive, ref } from 'vue'
import { congesApi } from '@/api/conges'
import { useToastStore } from '@/stores/toast'
import { date as formatDate, dateIso } from '@/utils/format'
import { enregistrerBlob } from '@/utils/fichier'
import StatutBadge from '@/components/RecrutementStatutBadge.vue'

const toast = useToastStore()

function ilYA(jours) {
  const jour = new Date()
  jour.setDate(jour.getDate() - jours)
  return dateIso(jour)
}

const du = ref(ilYA(30))
const au = ref(dateIso())
const absences = ref([])
const saisies = reactive({})
const erreur = ref('')

const cle = (absence) => `${absence.employeId}-${absence.date}`

function saisie(absence) {
  saisies[cle(absence)] ??= { motif: '', fichier: null }
  return saisies[cle(absence)]
}

async function charger() {
  erreur.value = ''
  try {
    absences.value = await congesApi.absences(du.value, au.value)
  } catch (e) {
    erreur.value = e.message
  }
}

async function qualifier(absence, statut) {
  const { motif, fichier } = saisie(absence)
  try {
    await congesApi.qualifierAbsence({ employeId: absence.employeId, date: absence.date, statut, motif }, fichier)
    delete saisies[cle(absence)]
    toast.success('Absence qualifiée.')
    await charger()
  } catch (e) {
    toast.error(e.message)
  }
}

async function telecharger(absence) {
  try {
    enregistrerBlob(await congesApi.justificatifAbsence(absence.id), absence.justificatifNom ?? `justificatif-${absence.id}`)
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(charger)
</script>

<template>
  <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

  <div class="b3-card">
    <form class="filtres" @submit.prevent="charger">
      <div class="b3-field"><label for="du">Du</label><input id="du" v-model="du" type="date" required /></div>
      <div class="b3-field"><label for="au">Au</label><input id="au" v-model="au" type="date" required /></div>
      <button class="b3-btn is-primary">Rechercher</button>
    </form>
    <p class="b3-muted">
      Jours prévus au planning, sans pointage ni congé validé. Une absence non justifiée retire
      salaire brut / 30 de la paie. Une absence justifiée demande un motif et un justificatif.
    </p>

    <div class="b3-table-wrapper">
      <table class="b3-table">
        <thead><tr><th>Date</th><th>Salarié</th><th>État</th><th>Motif et justificatif</th><th></th></tr></thead>
        <tbody>
          <tr v-for="absence in absences" :key="cle(absence)">
            <td>{{ formatDate(absence.date) }}</td>
            <td>{{ absence.matricule }} — {{ absence.nomComplet }}</td>
            <td><StatutBadge :statut="absence.statut ?? 'A_QUALIFIER'" /></td>
            <td>
              <input v-model="saisie(absence).motif" :placeholder="absence.motif ?? 'Motif'" aria-label="Motif" class="motif" />
              <input type="file" accept=".pdf,.png,.jpg,.jpeg" aria-label="Justificatif" @change="saisie(absence).fichier = $event.target.files?.[0] ?? null" />
              <button v-if="absence.justificatifDisponible" class="b3-btn is-ghost" @click="telecharger(absence)">Voir le justificatif</button>
            </td>
            <td class="b3-actions">
              <button class="b3-btn is-primary" @click="qualifier(absence, 'JUSTIFIEE')">Justifiée</button>
              <button class="b3-btn is-danger" @click="qualifier(absence, 'NON_JUSTIFIEE')">Non justifiée</button>
            </td>
          </tr>
          <tr v-if="!absences.length"><td colspan="5" class="b3-empty">Aucune absence sur cette période.</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.filtres {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 0.6rem;
}

.motif {
  display: block;
  width: 100%;
  margin-bottom: 0.3rem;
}
</style>
