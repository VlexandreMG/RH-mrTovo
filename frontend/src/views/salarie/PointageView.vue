<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { salarieApi } from '@/api/salarie'
import { useToastStore } from '@/stores/toast'
import { date as formatDate, dateIso, duree, heure } from '@/utils/format'

const JOURS = ['Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.', 'Dim.']

const toast = useToastStore()
const semaine = ref(null)
const jourReference = ref(dateIso())
const erreur = ref('')
const envoi = ref(false)
const maintenant = ref(new Date())
let minuteur = null

const aujourdhui = computed(() => semaine.value?.jours.find((j) => j.date === dateIso()) ?? null)
const etat = computed(() => {
  const jour = aujourdhui.value
  if (!jour?.pointageId) return 'A_ARRIVER'
  return jour.heureSortie ? 'TERMINE' : 'A_PARTIR'
})
const horloge = computed(() => maintenant.value.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }))
const titreJour = computed(() => new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }))
const estSemaineCourante = computed(() => semaine.value?.jours.some((j) => j.date === dateIso()))

async function charger() {
  erreur.value = ''
  try {
    semaine.value = await salarieApi.maSemaine(jourReference.value)
  } catch (e) {
    erreur.value = e.message
  }
}

async function pointer() {
  envoi.value = true
  try {
    if (etat.value === 'A_ARRIVER') {
      semaine.value = await salarieApi.pointerArrivee()
      toast.success(`Arrivée pointée à ${heure(aujourdhui.value.heureEntree)}.`)
    } else {
      semaine.value = await salarieApi.pointerDepart()
      toast.success(`Départ pointé à ${heure(aujourdhui.value.heureSortie)}.`)
    }
    jourReference.value = dateIso()
  } catch (e) {
    toast.error(e.message)
    await charger()
  } finally {
    envoi.value = false
  }
}

function changerSemaine(ecartJours) {
  const jour = new Date(`${jourReference.value}T12:00:00`)
  jour.setDate(jour.getDate() + ecartJours)
  jourReference.value = dateIso(jour)
  charger()
}

function semaineCourante() {
  jourReference.value = dateIso()
  charger()
}

onMounted(() => {
  charger()
  minuteur = setInterval(() => { maintenant.value = new Date() }, 15000)
})
onUnmounted(() => clearInterval(minuteur))
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Mon pointage</h1>
        <p>Pointez votre arrivée et votre départ. L'heure enregistrée est celle du serveur : vous ne la choisissez pas.</p>
      </div>
    </header>

    <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

    <div class="b3-card pointer">
      <div class="aujourdhui">
        <p class="jour">{{ titreJour }}</p>
        <p class="heure" aria-live="polite">{{ horloge }}</p>
        <p v-if="aujourdhui?.congeLibelle" class="b3-muted">{{ aujourdhui.congeLibelle }} : vous n'êtes pas attendu aujourd'hui.</p>
        <p v-else-if="aujourdhui?.heureDebutPrevue" class="b3-muted">
          Prévu : {{ heure(aujourdhui.heureDebutPrevue) }} – {{ heure(aujourdhui.heureFinPrevue) }}
        </p>
        <p v-else-if="aujourdhui" class="b3-muted">Jour de repos : un pointage compte en heures supplémentaires.</p>
      </div>

      <div class="action">
        <p class="etat">
          <template v-if="etat === 'A_ARRIVER'">Vous n'avez pas encore pointé aujourd'hui.</template>
          <template v-else-if="etat === 'A_PARTIR'">Arrivé à <strong>{{ heure(aujourdhui.heureEntree) }}</strong>.</template>
          <template v-else>
            Arrivé à <strong>{{ heure(aujourdhui.heureEntree) }}</strong>, parti à <strong>{{ heure(aujourdhui.heureSortie) }}</strong>.
          </template>
        </p>
        <button
          v-if="etat !== 'TERMINE'"
          class="b3-btn is-primary gros"
          :disabled="envoi || !semaine"
          @click="pointer"
        >
          {{ etat === 'A_ARRIVER' ? 'Pointer mon arrivée' : 'Pointer mon départ' }}
        </button>
        <p v-else class="fini">Journée terminée. Une erreur ? Signalez-la au RH, qui peut corriger.</p>
      </div>
    </div>

    <div v-if="semaine" class="b3-card">
      <div class="entete-semaine">
        <h2>Semaine du {{ formatDate(semaine.semaineDebut) }} au {{ formatDate(semaine.semaineFin) }}</h2>
        <div class="b3-actions">
          <button class="b3-btn" @click="changerSemaine(-7)">← Semaine précédente</button>
          <button class="b3-btn" :disabled="estSemaineCourante" @click="semaineCourante">Cette semaine</button>
        </div>
      </div>
      <p class="b3-muted">{{ semaine.regimeLibelle ?? 'Aucun horaire ne vous est encore affecté : contactez le RH.' }}</p>
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead>
            <tr><th>Jour</th><th>Prévu</th><th>Arrivée</th><th>Départ</th><th>Travaillé</th><th>Retard</th><th>H. sup</th></tr>
          </thead>
          <tbody>
            <tr v-for="(jour, index) in semaine.jours" :key="jour.date" :class="{ repos: !jour.heureDebutPrevue, courant: jour.date === dateIso() }">
              <td>
                <strong>{{ JOURS[index] }}</strong> {{ formatDate(jour.date) }}
                <div v-if="jour.pointageSource === 'CORRIGE_RH' || jour.pointageSource === 'RH'" class="b3-muted petit">
                  {{ jour.pointageSource === 'RH' ? 'Ajouté par le RH' : 'Corrigé par le RH' }}
                </div>
              </td>
              <td>
                <span v-if="jour.congeLibelle" class="b3-badge is-info">{{ jour.congeLibelle }}</span>
                <template v-else-if="jour.heureDebutPrevue">{{ heure(jour.heureDebutPrevue) }} – {{ heure(jour.heureFinPrevue) }}</template>
                <span v-else class="b3-muted">Repos</span>
              </td>
              <td>{{ heure(jour.heureEntree) || '—' }}</td>
              <td>{{ heure(jour.heureSortie) || '—' }}</td>
              <td>{{ duree(jour.minutesTravaillees) }}</td>
              <td :class="{ alerte: jour.minutesRetard }">{{ duree(jour.minutesRetard) }}</td>
              <td>{{ duree(jour.minutesHeuresSup) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<style scoped>
.pointer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1.2rem;
  margin-bottom: 1rem;
}
.jour { margin: 0; text-transform: capitalize; color: var(--b3-text-soft); font-weight: 600; }
.heure { margin: .1rem 0 .3rem; font-size: 2.6rem; font-weight: 800; line-height: 1; color: var(--b3-text); }
.aujourdhui p.b3-muted { margin: 0; }
.action { display: flex; flex-direction: column; align-items: flex-end; gap: .6rem; }
.etat { margin: 0; }
.gros { padding: .85rem 1.6rem; font-size: 1rem; }
.fini { margin: 0; color: var(--b3-success); font-weight: 600; max-width: 22rem; text-align: right; }
.entete-semaine { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: .6rem; }
.entete-semaine h2 { margin: 0; }
.repos { background: var(--b3-surface-alt); }
.courant td { font-weight: 600; }
.alerte { color: var(--b3-danger); font-weight: 600; }
.petit { font-size: .75rem; }
@media (max-width: 640px) { .action { align-items: stretch; } .fini { text-align: left; } }
</style>
