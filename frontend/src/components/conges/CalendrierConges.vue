<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { congesApi } from '@/api/conges'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { date as formatDate, dateIso, nb } from '@/utils/format'
import { enregistrerBlob } from '@/utils/fichier'
import StatutBadge from '@/components/RecrutementStatutBadge.vue'

const emit = defineEmits(['aller'])

const INITIALES = ['D', 'L', 'M', 'M', 'J', 'V', 'S']
const NB_COULEURS = 6

const auth = useAuthStore()
const toast = useToastStore()
const estRh = computed(() => auth.role === 'RH')

const mois = ref(dateIso().slice(0, 7))
const departement = ref('')
const recherche = ref('')
const calendrier = ref(null)
const erreur = ref('')
const selection = ref(null)
const demande = ref(null)
const commentaire = ref('')
let dernierChargement = 0

const titreMois = computed(() => new Date(`${mois.value}-15T12:00:00`)
  .toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' }))

const jours = computed(() => (calendrier.value?.jours ?? []).map((jour) => {
  const dateJour = new Date(`${jour.date}T12:00:00`)
  return {
    ...jour,
    numero: dateJour.getDate(),
    initiale: INITIALES[dateJour.getDay()],
    weekEnd: dateJour.getDay() === 0 || dateJour.getDay() === 6,
    aujourdhui: jour.date === dateIso()
  }
}))

const lignes = computed(() => {
  const texte = recherche.value.trim().toLowerCase()
  return (calendrier.value?.salaries ?? [])
    .filter((l) => !texte || `${l.nomComplet} ${l.matricule}`.toLowerCase().includes(texte))
    .map((l) => ({ ...l, parJour: new Map(l.cases.map((c) => [c.date, c])) }))
})

const typesDuMois = computed(() => {
  const types = new Map()
  for (const ligne of calendrier.value?.salaries ?? []) {
    for (const c of ligne.cases) {
      if (c.typeCongeId) types.set(c.typeCongeId, c.libelle)
    }
  }
  return [...types].map(([id, libelle]) => ({ id, libelle })).sort((a, b) => a.id - b.id)
})

const panneauAbsence = computed(() => selection.value?.case.nature.startsWith('ABSENCE'))

function couleur(c) {
  return c.typeCongeId ? `couleur-${c.typeCongeId % NB_COULEURS}` : ''
}

function memeBarre(a, b) {
  return a && b && a.nature === b.nature && a.demandeId === b.demandeId
}

function classesCase(ligne, jour, index) {
  const c = ligne.parJour.get(jour.date)
  return [
    couleur(c),
    c.nature.toLowerCase().replaceAll('_', '-'),
    {
      'suite-avant': memeBarre(c, ligne.parJour.get(jours.value[index - 1]?.date)),
      'suite-apres': memeBarre(c, ligne.parJour.get(jours.value[index + 1]?.date))
    }
  ]
}

async function charger() {
  erreur.value = ''
  const numero = ++dernierChargement
  try {
    const reponse = await congesApi.calendrier(mois.value, departement.value)
    // Après plusieurs clics rapprochés, seule la dernière demande compte.
    if (numero === dernierChargement) calendrier.value = reponse
  } catch (e) {
    if (numero === dernierChargement) erreur.value = e.message
  }
}

function changerMois(ecart) {
  const [annee, numero] = mois.value.split('-').map(Number)
  mois.value = dateIso(new Date(annee, numero - 1 + ecart, 1)).slice(0, 7)
}

async function ouvrir(ligne, c) {
  selection.value = { ligne, case: c }
  demande.value = null
  commentaire.value = ''
  if (!c.demandeId) return
  try {
    demande.value = (await congesApi.demandes()).find((d) => d.id === c.demandeId) ?? null
  } catch (e) {
    toast.error(e.message)
  }
}

async function decider(accepte) {
  try {
    await congesApi.decisionRh(demande.value.id, { accepte, commentaire: commentaire.value })
    toast.success(accepte ? 'Demande acceptée.' : 'Demande refusée.')
    selection.value = null
    await charger()
  } catch (e) {
    toast.error(e.message)
  }
}

async function telecharger() {
  try {
    enregistrerBlob(await congesApi.justificatif(demande.value.id), demande.value.justificatifNom ?? `justificatif-${demande.value.id}`)
  } catch (e) {
    toast.error(e.message)
  }
}

watch([mois, departement], () => {
  selection.value = null
  charger()
})

onMounted(charger)
</script>

<template>
  <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

  <div class="b3-card">
    <div class="barre">
      <div class="navigation">
        <button class="b3-btn" aria-label="Mois précédent" @click="changerMois(-1)">‹</button>
        <strong class="titre-mois">{{ titreMois }}</strong>
        <button class="b3-btn" aria-label="Mois suivant" @click="changerMois(1)">›</button>
        <button class="b3-btn" @click="mois = dateIso().slice(0, 7)">Ce mois-ci</button>
      </div>
      <div class="filtres">
        <select v-if="estRh" v-model="departement" class="select" aria-label="Département">
          <option value="">Tous les départements</option>
          <option v-for="d in calendrier?.departements ?? []" :key="d" :value="d">{{ d }}</option>
        </select>
        <input v-model="recherche" class="input" type="search" placeholder="Chercher un salarié" aria-label="Chercher un salarié" />
      </div>
    </div>

    <div class="legende">
      <template v-if="estRh">
        <span v-for="t in typesDuMois" :key="t.id"><i class="pastille" :class="`couleur-${t.id % NB_COULEURS}`"></i>{{ t.libelle }}</span>
        <span><i class="pastille couleur-attente en-attente"></i>En attente</span>
        <span><i class="pastille absence-a-qualifier"></i>À qualifier</span>
        <span><i class="pastille absence-justifiee"></i>Absence justifiée</span>
        <span><i class="pastille absence-non-justifiee"></i>Absence non justifiée</span>
      </template>
      <template v-else>
        <span><i class="pastille absent"></i>Absent</span>
        <span><i class="pastille couleur-attente en-attente"></i>Absence en attente</span>
      </template>
    </div>

    <div class="grille-wrapper">
      <table class="grille">
        <thead>
          <tr>
            <th scope="col" class="colonne-nom">Salarié</th>
            <th v-for="jour in jours" :key="jour.date" scope="col" :class="{ 'week-end': jour.weekEnd, aujourdhui: jour.aujourdhui }">
              <small>{{ jour.initiale }}</small>{{ jour.numero }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ligne in lignes" :key="ligne.employeId">
            <th scope="row" class="colonne-nom">
              {{ ligne.nomComplet }}
              <small>{{ ligne.departementNom ?? '—' }}</small>
            </th>
            <td v-for="(jour, index) in jours" :key="jour.date" :class="{ 'week-end': jour.weekEnd, aujourdhui: jour.aujourdhui }">
              <button
                v-if="ligne.parJour.has(jour.date)"
                class="case"
                :class="classesCase(ligne, jour, index)"
                :title="`${ligne.nomComplet} — ${ligne.parJour.get(jour.date).libelle}`"
                :aria-label="`${ligne.nomComplet}, ${formatDate(jour.date)} : ${ligne.parJour.get(jour.date).libelle}`"
                @click="ouvrir(ligne, ligne.parJour.get(jour.date))"
              ></button>
            </td>
          </tr>
          <tr v-if="!lignes.length"><td :colspan="jours.length + 1" class="b3-empty">Aucun salarié à afficher.</td></tr>
        </tbody>
        <tfoot>
          <tr>
            <th scope="row" class="colonne-nom">Présents</th>
            <td v-for="jour in jours" :key="jour.date" :class="{ 'week-end': jour.weekEnd, aujourdhui: jour.aujourdhui }" :title="`${jour.presents} présent(s) sur ${jour.attendus} attendu(s)`">
              {{ jour.attendus ? nb(jour.presents) : '–' }}
            </td>
          </tr>
          <tr>
            <th scope="row" class="colonne-nom">En attente</th>
            <td v-for="jour in jours" :key="jour.date" class="attente" :class="{ 'week-end': jour.weekEnd, aujourdhui: jour.aujourdhui }">
              {{ jour.enAttente ? nb(jour.enAttente) : '' }}
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
    <p class="b3-muted note">Les totaux portent sur le département affiché, pas sur la recherche. Un salarié sans régime n'y est pas compté.</p>
  </div>

  <div v-if="selection" class="b3-card panneau">
    <div class="entete-panneau">
      <h2>{{ selection.ligne.nomComplet }}</h2>
      <button class="b3-btn is-ghost" @click="selection = null">Fermer</button>
    </div>
    <p>
      <strong>{{ selection.case.libelle }}</strong>
      <span v-if="estRh && selection.case.dateDebut"> · {{ formatDate(selection.case.dateDebut) }} → {{ formatDate(selection.case.dateFin) }}</span>
      <span v-else> · {{ formatDate(selection.case.date) }}</span>
    </p>

    <template v-if="demande">
      <StatutBadge :statut="demande.statut" />
      <p v-if="demande.motif" class="b3-muted">Motif : {{ demande.motif }}</p>
      <p class="b3-muted">
        {{ demande.nbJours }} jour(s)<template v-if="demande.soldeDisponible !== null"> · solde disponible : {{ nb(demande.soldeDisponible) }} j</template>
      </p>
      <button v-if="demande.justificatifDisponible" class="b3-btn is-ghost" @click="telecharger">Justificatif</button>
      <p v-else-if="demande.justificatifRequis" class="alerte">Justificatif manquant</p>

      <div v-if="demande.statut === 'EN_ATTENTE_RH'" class="decision">
        <input v-model="commentaire" class="input" placeholder="Commentaire (obligatoire si refus)" aria-label="Commentaire" />
        <div class="b3-actions">
          <button class="b3-btn is-danger" @click="decider(false)">Refuser</button>
          <button class="b3-btn is-primary" @click="decider(true)">Accepter</button>
        </div>
      </div>
      <p v-else-if="demande.statut === 'EN_ATTENTE_MANAGER'" class="b3-muted">Cette demande attend l'avis du manager.</p>
    </template>

    <button v-if="estRh && panneauAbsence" class="b3-btn" @click="emit('aller', 'absences')">Voir l'onglet Absences</button>
  </div>
</template>

<style scoped>
.barre {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.6rem;
  margin-bottom: 0.8rem;
}

.navigation,
.filtres {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
}

.filtres .select,
.filtres .input {
  width: auto;
  padding: 0.45rem 0.7rem;
}

.titre-mois {
  min-width: 9.5rem;
  text-align: center;
  text-transform: capitalize;
}

.legende {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 1rem;
  margin-bottom: 0.8rem;
  font-size: 0.78rem;
  color: var(--b3-text-soft);
}

.legende span {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.pastille {
  display: inline-block;
  width: 1rem;
  height: 0.8rem;
  border-radius: 3px;
  background: var(--couleur);
}

.grille-wrapper {
  overflow-x: auto;
  border: 1px solid var(--b3-border);
  border-radius: 8px;
}

.grille {
  border-collapse: collapse;
  width: 100%;
  font-size: 0.78rem;
}

.grille th,
.grille td {
  min-width: 1.7rem;
  height: 2.1rem;
  padding: 0;
  text-align: center;
  font-weight: 500;
  border-bottom: 1px solid var(--b3-border);
}

.grille thead th small {
  display: block;
  font-size: 0.66rem;
  color: var(--b3-text-soft);
}

.grille .colonne-nom {
  position: sticky;
  left: 0;
  z-index: 1;
  min-width: 10rem;
  padding: 0.25rem 0.7rem;
  text-align: left;
  font-weight: 600;
  white-space: nowrap;
  background: var(--b3-surface);
  border-right: 1px solid var(--b3-border);
}

.colonne-nom small {
  display: block;
  font-weight: 400;
  color: var(--b3-text-soft);
}

.grille .week-end {
  background: var(--b3-neutral-soft);
}

.grille .aujourdhui {
  box-shadow: inset 1px 0 0 var(--b3-primary), inset -1px 0 0 var(--b3-primary);
}

.grille thead .aujourdhui {
  color: var(--b3-primary);
  font-weight: 700;
}

.grille tfoot td,
.grille tfoot th {
  font-weight: 700;
  background: var(--b3-surface-alt);
}

.grille tfoot .week-end {
  background: var(--b3-neutral-soft);
}

.grille tfoot .attente {
  color: var(--b3-warning);
}

.case {
  display: block;
  width: calc(100% - 4px);
  height: 1.3rem;
  margin: 0 2px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: var(--couleur);
  cursor: pointer;
}

.case.suite-avant {
  width: calc(100% - 2px);
  margin-left: 0;
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.case.suite-apres {
  width: calc(100% - 2px);
  margin-right: 0;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.case.suite-avant.suite-apres {
  width: 100%;
}

.case:focus-visible {
  outline: 2px solid var(--b3-text);
  outline-offset: 1px;
}

.couleur-0 { --couleur: #4d7c0f; }
.couleur-1 { --couleur: #2f5bea; }
.couleur-2 { --couleur: #0e7490; }
.couleur-3 { --couleur: #7c3aed; }
.couleur-4 { --couleur: #be185d; }
.couleur-5 { --couleur: #0f8a64; }
.couleur-attente { --couleur: #64748b; }

/* Une demande en attente garde la couleur de son type, en hachures : elle reste lisible sans la couleur. */
.en-attente {
  background: repeating-linear-gradient(135deg, var(--couleur) 0 3px, transparent 3px 7px);
  box-shadow: inset 0 0 0 1px var(--couleur);
}

.absent { --couleur: #2f5bea; }
.absence-justifiee { --couleur: #94a3b8; }
.absence-non-justifiee { --couleur: var(--b3-danger); }
.absence-a-qualifier {
  background: var(--b3-warning-soft);
  box-shadow: inset 0 0 0 2px var(--b3-warning);
}

.note {
  margin: 0.6rem 0 0;
  font-size: 0.76rem;
}

.panneau {
  margin-top: 1rem;
}

.entete-panneau {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.6rem;
}

.entete-panneau h2 {
  margin: 0;
  font-size: 1rem;
}

.decision {
  margin-top: 0.7rem;
  max-width: 26rem;
}

.decision input {
  margin-bottom: 0.4rem;
}

.alerte {
  color: var(--b3-danger);
  font-size: 0.78rem;
}
</style>
