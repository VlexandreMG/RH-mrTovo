<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { dossiersApi } from '@/api/dossiers'
import { tempsApi } from '@/api/temps'
import { useToastStore } from '@/stores/toast'
import { date as formatDate, dateIso, duree, heure } from '@/utils/format'
import StatutBadge from '@/components/RecrutementStatutBadge.vue'
import DecisionHeuresSup from './DecisionHeuresSup.vue'

const props = defineProps({
  cible: { type: Object, default: null }
})
defineEmits(['voir-regimes'])

const JOURS = ['Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.', 'Dim.']

/** Le salarie pointe lui-meme ; le RH n'intervient que pour ajouter un oubli ou corriger. */
const SOURCES = {
  SALARIE: { libelle: 'Salarié', ton: 'is-success' },
  RH: { libelle: 'Ajouté par le RH', ton: 'is-warning' },
  CORRIGE_RH: { libelle: 'Corrigé par le RH', ton: 'is-info' }
}

const toast = useToastStore()
const salaries = ref([])
const employeId = ref(null)
const jourReference = ref(dateIso())
const semaine = ref(null)
const lignes = reactive({})
const chargement = ref(false)
const erreur = ref('')
const aujourdhui = dateIso()

const modifierDecision = ref(false)
const peutDecider = computed(() => semaine.value?.decision.validable)
const dejaDecidee = computed(() => ['VALIDEE', 'REFUSEE'].includes(semaine.value?.decision.statut))

function remplirLignes(donnees) {
  Object.keys(lignes).forEach((cle) => delete lignes[cle])
  donnees.jours.forEach((jour) => {
    lignes[jour.date] = {
      entree: heure(jour.heureEntree),
      sortie: heure(jour.heureSortie),
      commentaire: jour.commentaire ?? '',
      motif: ''
    }
  })
}

function afficher(donnees) {
  semaine.value = donnees
  modifierDecision.value = false
  remplirLignes(donnees)
}

async function charger() {
  if (!employeId.value) return
  chargement.value = true
  erreur.value = ''
  try {
    afficher(await tempsApi.semaine(employeId.value, jourReference.value))
  } catch (e) {
    erreur.value = e.message
  } finally {
    chargement.value = false
  }
}

function changerSemaine(ecartJours) {
  const jour = new Date(`${jourReference.value}T12:00:00`)
  jour.setDate(jour.getDate() + ecartJours)
  jourReference.value = dateIso(jour)
}

function modifiee(jour) {
  const ligne = lignes[jour.date]
  return ligne.entree !== heure(jour.heureEntree)
    || ligne.sortie !== heure(jour.heureSortie)
    || ligne.commentaire !== (jour.commentaire ?? '')
}

async function enregistrer(jour) {
  const ligne = lignes[jour.date]
  try {
    if (jour.pointageId) {
      afficher(await tempsApi.corrigerPointage(jour.pointageId, {
        heureEntree: ligne.entree,
        heureSortie: ligne.sortie || null,
        commentaire: ligne.commentaire,
        motif: ligne.motif
      }))
      toast.success('Pointage corrigé.')
    } else {
      afficher(await tempsApi.saisirPointage({
        employeId: employeId.value,
        date: jour.date,
        heureEntree: ligne.entree,
        heureSortie: ligne.sortie || null,
        commentaire: ligne.commentaire,
        motif: ligne.motif
      }))
      toast.success('Pointage ajouté.')
    }
  } catch (e) {
    toast.error(e.message)
  }
}

async function supprimer(jour) {
  const motif = window.prompt('Motif de la suppression (obligatoire) :')
  if (!motif?.trim()) return
  try {
    afficher(await tempsApi.supprimerPointage(jour.pointageId, motif))
    toast.success('Pointage supprimé.')
  } catch (e) {
    toast.error(e.message)
  }
}

watch([employeId, jourReference], charger)

watch(() => props.cible, (cible) => {
  if (!cible) return
  employeId.value = cible.employeId
  jourReference.value = cible.date
}, { immediate: true })

onMounted(async () => {
  try {
    salaries.value = (await dossiersApi.lister()).filter((salarie) => salarie.statut !== 'SORTI')
    if (!employeId.value && salaries.value.length) employeId.value = salaries.value[0].id
  } catch (e) {
    erreur.value = e.message
  }
})
</script>

<template>
  <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

  <div class="b3-card barre">
    <div class="b3-field">
      <label for="salarie">Salarié</label>
      <select id="salarie" v-model="employeId">
        <option v-for="salarie in salaries" :key="salarie.id" :value="salarie.id">
          {{ salarie.matricule }} — {{ salarie.prenom }} {{ salarie.nom }}
        </option>
      </select>
    </div>
    <div class="b3-actions">
      <button class="b3-btn" @click="changerSemaine(-7)">← Semaine précédente</button>
      <button class="b3-btn" @click="jourReference = aujourdhui">Cette semaine</button>
      <button class="b3-btn" @click="changerSemaine(7)">Semaine suivante →</button>
    </div>
  </div>

  <div v-if="!salaries.length" class="b3-card b3-empty">Aucun salarié dans l'effectif.</div>

  <template v-if="semaine">
    <div v-if="!semaine.regimeLibelle" class="b3-alert is-error">
      Aucun régime de travail pour cette semaine : retards et heures sup ne peuvent pas être calculés.
      <button class="b3-btn is-ghost" @click="$emit('voir-regimes')">Affecter un régime</button>
    </div>

    <div class="b3-card">
      <h2>
        Semaine du {{ formatDate(semaine.semaineDebut) }} au {{ formatDate(semaine.semaineFin) }}
      </h2>
      <p class="b3-muted">{{ semaine.regimeLibelle ?? 'Sans régime' }}</p>
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead>
            <tr>
              <th>Jour</th><th>Prévu</th><th>Entrée</th><th>Sortie</th>
              <th>Source</th><th>Travaillé</th><th>Retard</th><th>Départ</th><th>H. sup</th><th>Correction</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(jour, index) in semaine.jours" :key="jour.date" :class="{ repos: !jour.heureDebutPrevue }">
              <td>
                <strong>{{ JOURS[index] }}</strong> {{ formatDate(jour.date) }}
                <div v-for="anomalie in jour.anomalies" :key="anomalie.type" class="anomalie">
                  {{ anomalie.libelle }}
                </div>
              </td>
              <td>
                <span v-if="jour.congeLibelle" class="b3-badge is-info">{{ jour.congeLibelle }}</span>
                <template v-else-if="jour.heureDebutPrevue">{{ heure(jour.heureDebutPrevue) }} – {{ heure(jour.heureFinPrevue) }}</template>
                <span v-else class="b3-muted">Repos</span>
                <div v-if="jour.absenceStatut"><StatutBadge :statut="jour.absenceStatut" /></div>
              </td>
              <td><input v-model="lignes[jour.date].entree" type="time" :disabled="jour.date > aujourdhui" aria-label="Entrée" /></td>
              <td><input v-model="lignes[jour.date].sortie" type="time" :disabled="jour.date > aujourdhui" aria-label="Sortie" /></td>
              <td>
                <span v-if="jour.pointageSource" class="b3-badge" :class="SOURCES[jour.pointageSource].ton">
                  {{ SOURCES[jour.pointageSource].libelle }}
                </span>
                <span v-else class="b3-muted">—</span>
              </td>
              <td>{{ duree(jour.minutesTravaillees) }}</td>
              <td :class="{ alerte: jour.minutesRetard }">{{ duree(jour.minutesRetard) }}</td>
              <td :class="{ alerte: jour.minutesDepartAnticipe }">{{ duree(jour.minutesDepartAnticipe) }}</td>
              <td>{{ duree(jour.minutesHeuresSup) }}</td>
              <td class="saisie">
                <template v-if="modifiee(jour)">
                  <input
                    v-model="lignes[jour.date].motif"
                    :placeholder="jour.pointageId ? 'Motif de la correction' : 'Motif de l\'ajout'"
                    :aria-label="jour.pointageId ? 'Motif de la correction' : 'Motif de l\'ajout'"
                  />
                  <button
                    class="b3-btn is-primary"
                    :disabled="!lignes[jour.date].entree || !lignes[jour.date].motif.trim()"
                    @click="enregistrer(jour)"
                  >
                    Enregistrer
                  </button>
                </template>
                <button v-else-if="jour.pointageId" class="b3-btn is-ghost" @click="supprimer(jour)">Supprimer</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="b3-grid b3-grid-2">
      <div class="b3-card">
        <h2>Bilan de la semaine</h2>
        <dl class="b3-detail-list">
          <div><dt>Prévu</dt><dd>{{ duree(semaine.minutesPrevues) }}</dd></div>
          <div><dt>Travaillé</dt><dd>{{ duree(semaine.minutesTravaillees) }}</dd></div>
          <div><dt>Retards</dt><dd>{{ duree(semaine.minutesRetard) }}</dd></div>
          <div><dt>Départs anticipés</dt><dd>{{ duree(semaine.minutesDepartAnticipe) }}</dd></div>
          <div><dt>Jours absents</dt><dd>{{ semaine.joursAbsents }}</dd></div>
          <div><dt>Heures sup constatées</dt><dd>{{ duree(semaine.decision.minutesBrutes) }}</dd></div>
        </dl>
      </div>

      <div class="b3-card">
        <h2>Compensation retards / heures sup</h2>
        <dl class="b3-detail-list">
          <div><dt>Heures sup</dt><dd><StatutBadge :statut="semaine.decision.statut" /></dd></div>
          <div><dt>Heures sup validées</dt><dd>{{ duree(semaine.decision.minutesValidees) }}</dd></div>
          <div><dt>Minutes dues</dt><dd>{{ duree(semaine.bilan.minutesDues) }}</dd></div>
          <div><dt>Compensé</dt><dd>{{ duree(semaine.bilan.minutesCompensees) }}</dd></div>
          <div><dt>Retenu sur la paie</dt><dd class="alerte">{{ duree(semaine.bilan.minutesRetenues) }}</dd></div>
          <div><dt>Heures sup payées</dt><dd>{{ duree(semaine.bilan.hsPayees) }}</dd></div>
        </dl>
        <p v-if="semaine.decision.commentaire" class="b3-muted">
          {{ semaine.decision.commentaire }} — {{ semaine.decision.decidePar }}
        </p>
        <template v-if="peutDecider">
          <DecisionHeuresSup v-if="!dejaDecidee || modifierDecision" :semaine="semaine" @decidee="afficher" />
          <button v-else class="b3-btn" @click="modifierDecision = true">Modifier la décision</button>
        </template>
        <p v-else-if="semaine.decision.minutesBrutes" class="b3-muted">{{ semaine.decision.raisonBlocage }}</p>
      </div>
    </div>
  </template>
</template>

<style scoped>
.barre {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 1rem;
}

.repos {
  background: var(--b3-border);
  background: color-mix(in srgb, var(--b3-border) 30%, transparent);
}

.anomalie {
  font-size: 0.72rem;
  color: var(--b3-danger);
}

.alerte {
  color: var(--b3-danger);
  font-weight: 600;
}

.saisie {
  min-width: 11rem;
}

.saisie input {
  width: 100%;
  margin-bottom: 0.25rem;
}
</style>
