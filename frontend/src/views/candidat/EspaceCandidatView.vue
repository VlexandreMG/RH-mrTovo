<template>
  <section class="space">
    <header class="heading">
      <div>
        <p class="eyebrow">Portail candidat</p>
        <h1>Bonjour {{ auth.user?.nom }}</h1>
        <p>Passez vos tests, consultez vos propositions et répondez au recruteur depuis cet espace.</p>
      </div>
      <RouterLink class="secondary" to="/candidats/offres">Parcourir les offres</RouterLink>
    </header>

    <section v-if="tests.length" class="tests" aria-labelledby="titre-tests">
      <h2 id="titre-tests">Mes tests</h2>
      <article v-for="test in tests" :key="test.candidatureId" class="test">
        <div>
          <h3>{{ test.offreTitre }}</h3>
          <p>{{ detailTest(test) }}</p>
        </div>
        <RouterLink v-if="test.etat === 'A_PASSER'" class="passer" :to="`/candidats/tests/${test.candidatureId}`">
          Passer le test
        </RouterLink>
        <span v-else class="status" :class="test.etat.toLowerCase()">{{ ETATS_TEST[test.etat] }}</span>
      </article>
    </section>

    <h2 v-if="tests.length" class="titre-propositions">Mes propositions</h2>
    <p v-if="erreur" class="state error">{{ erreur }}</p>
    <p v-else-if="chargement" class="state">Chargement de vos propositions…</p>
    <p v-else-if="!propositions.length" class="state">
      Aucune proposition ne vous est adressée pour le moment. Revenez consulter cet espace après votre candidature.
    </p>

    <div v-else class="list">
      <article v-for="proposition in propositions" :key="proposition.id" class="offer">
        <header class="offer-heading">
          <div>
            <p class="eyebrow">Proposition v{{ proposition.version }}</p>
            <h2>{{ proposition.posteTitre || proposition.offreTitre || 'Proposition d’embauche' }}</h2>
            <p>{{ proposition.departementNom || 'Équipe de recrutement' }} · {{ proposition.lieuTravail }}</p>
          </div>
          <span class="status" :class="proposition.statut.toLowerCase()">{{ statutLabel(proposition.statut) }}</span>
        </header>

        <div class="facts">
          <div><small>Rémunération brute</small><strong>{{ formatSalaire(proposition.salaireBase) }}</strong></div>
          <div><small>Contrat</small><strong>{{ proposition.typeContrat }}</strong></div>
          <div><small>Temps de travail</small><strong>{{ proposition.tempsTravail }}</strong></div>
          <div><small>Prise de poste</small><strong>{{ formatDate(proposition.datePrisePoste) }}</strong></div>
        </div>
        <p v-if="proposition.emploiDuTemps" class="detail"><strong>Horaires :</strong> {{ proposition.emploiDuTemps }}</p>
        <p v-if="proposition.dateFinContrat" class="detail"><strong>Fin du contrat :</strong> {{ formatDate(proposition.dateFinContrat) }}</p>

        <div v-if="proposition.avantages?.length" class="benefits">
          <h3>Avantages</h3>
          <ul>
            <li v-for="avantage in proposition.avantages" :key="avantage.id || avantage.ordre">
              <strong>{{ avantage.libelle }}</strong><span v-if="avantage.description"> — {{ avantage.description }}</span>
            </li>
          </ul>
        </div>

        <div class="deadline" :class="{ expired: joursRestants(proposition.dateLimiteValidite) < 0 }">
          <template v-if="proposition.statut === 'EMISE'">
            <strong>{{ libelleDelai(proposition.dateLimiteValidite) }}</strong>
            <span>Réponse attendue avant le {{ formatDate(proposition.dateLimiteValidite) }}</span>
          </template>
          <template v-else>
            <span>Date limite de validité : {{ formatDate(proposition.dateLimiteValidite) }}</span>
          </template>
        </div>

        <div v-if="proposition.statut === 'EMISE' && joursRestants(proposition.dateLimiteValidite) >= 0" class="actions">
          <button class="accept" :disabled="actionEnCours === proposition.id" @click="accepter(proposition)">
            Accepter la proposition
          </button>
          <button class="reject" :disabled="actionEnCours === proposition.id" @click="refuser(proposition)">
            Refuser définitivement
          </button>
          <button class="negotiate" :disabled="actionEnCours === proposition.id" @click="negociationOuverte = negociationOuverte === proposition.id ? null : proposition.id">
            Négocier / faire une revendication
          </button>
        </div>

        <form v-if="negociationOuverte === proposition.id" class="negotiate-form" @submit.prevent="revendiquer(proposition)">
          <h3>Votre demande au recruteur</h3>
          <label>
            Salaire brut souhaité (facultatif)
            <input v-model="negociation.salaireRevendique" type="number" min="1" step="0.01" />
          </label>
          <label>
            Avantage souhaité (facultatif)
            <input v-model.trim="negociation.avantageRevendique" maxlength="255" />
          </label>
          <label>
            Motif de la revendication
            <textarea v-model.trim="negociation.motif" required maxlength="255" rows="3" />
          </label>
          <p v-if="negociation.salaireRevendique === '' && !negociation.avantageRevendique" class="inline-error">
            Indiquez un salaire ou un avantage à discuter.
          </p>
          <button class="negotiate" type="submit" :disabled="actionEnCours === proposition.id">Envoyer ma revendication</button>
        </form>

        <div v-if="proposition.negociations?.length" class="negotiations">
          <h3>Suivi de la négociation</h3>
          <article v-for="negociation in proposition.negociations" :key="negociation.id" class="negotiation">
            <strong>{{ statutNegociation(negociation.statut) }}</strong>
            <p>{{ negociation.motif }}</p>
            <p v-if="negociation.salaireRevendique">
              Salaire demandé : {{ formatSalaire(negociation.salaireRevendique) }}
            </p>
            <p v-if="negociation.avantageRevendique">Avantage demandé : {{ negociation.avantageRevendique }}</p>
            <p v-if="negociation.commentaireDecision" class="decision">
              Réponse RH : {{ negociation.commentaireDecision }}
            </p>
            <small>Demande du {{ formatDate(negociation.dateDemande) }}
              <template v-if="negociation.dateDecision"> · Décision du {{ formatDate(negociation.dateDecision) }}</template>
            </small>
          </article>
        </div>
      </article>
    </div>
    <p v-if="message" class="feedback success">{{ message }}</p>
    <p v-if="actionErreur" class="feedback error">{{ actionErreur }}</p>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { apiGet, apiPost } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const propositions = ref([])
const tests = ref([])

const ETATS_TEST = {
  A_PASSER: 'À passer',
  EN_PREPARATION: 'Bientôt disponible',
  ENVOYE: 'Réponses envoyées',
  EVALUE: 'Évalué par le recruteur',
  TERMINE: 'Terminé',
}

function detailTest(test) {
  if (test.etat === 'A_PASSER') return `${test.nbQuestions} question(s) à choix multiples · une seule tentative`
  if (test.etat === 'EN_PREPARATION') return 'Le recruteur prépare encore les questions. Revenez plus tard.'
  if (test.etat === 'ENVOYE') {
    const le = new Date(test.dateSoumission).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' })
    return `Envoyé le ${le}. Le recruteur étudie votre test.`
  }
  return 'La suite de votre candidature se poursuit avec le recruteur.'
}
const chargement = ref(true)
const erreur = ref('')
const actionErreur = ref('')
const message = ref('')
const actionEnCours = ref(null)
const negociationOuverte = ref(null)
const negociation = reactive({ salaireRevendique: '', avantageRevendique: '', motif: '' })

async function charger() {
  erreur.value = ''
  try {
    const [listePropositions, listeTests] = await Promise.all([
      apiGet('/candidat/propositions'),
      apiGet('/candidat/tests'),
    ])
    propositions.value = listePropositions
    tests.value = listeTests
  } catch (e) {
    erreur.value = e.message || 'Impossible de charger vos propositions.'
  } finally {
    chargement.value = false
  }
}

function joursRestants(date) {
  if (!date) return Number.POSITIVE_INFINITY
  const [annee, mois, jour] = date.split('-').map(Number)
  const limite = Date.UTC(annee, mois - 1, jour)
  const aujourdhui = new Date()
  const debutJour = Date.UTC(aujourdhui.getFullYear(), aujourdhui.getMonth(), aujourdhui.getDate())
  return Math.ceil((limite - debutJour) / 86400000)
}

function libelleDelai(date) {
  const jours = joursRestants(date)
  if (jours < 0) return 'Le délai de réponse est dépassé.'
  if (jours === 0) return 'Dernier jour pour répondre aujourd’hui.'
  if (jours === 1) return 'Il vous reste 1 jour pour répondre.'
  return `Il vous reste ${jours} jours pour répondre.`
}

function formatDate(date) {
  if (!date) return '—'
  return new Date(`${date}T00:00:00`).toLocaleDateString('fr-FR')
}

function formatSalaire(salaire) {
  if (salaire == null) return '—'
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'MGA', maximumFractionDigits: 0 }).format(salaire)
}

function statutLabel(statut) {
  return {
    EMISE: 'En attente de réponse',
    EN_NEGOCIATION: 'Négociation en cours',
    ACCEPTEE: 'Acceptée',
    REFUSEE: 'Refusée',
    EXPIREE: 'Expirée',
    ANNULEE: 'Annulée',
    SUPERSEDEE: 'Remplacée'
  }[statut] || statut
}

function statutNegociation(statut) {
  return {
    EN_ATTENTE: 'En attente de réponse RH',
    EN_ANALYSE: 'En cours d’analyse par les RH',
    ACCEPTE: 'Revendication acceptée',
    REFUSE: 'Revendication refusée — proposition initiale maintenue',
    CONTRE_PROPOSITION: 'Contre-proposition RH'
  }[statut] || statut
}

async function executerAction(id, action, corps) {
  actionErreur.value = ''
  message.value = ''
  actionEnCours.value = id
  try {
    await apiPost(`/candidat/propositions/${id}/${action}`, corps)
    message.value = action === 'accepter'
      ? 'Proposition acceptée. Le recruteur a reçu votre réponse.'
      : action === 'refuser'
        ? 'Proposition refusée définitivement.'
        : 'Votre revendication a été transmise au recruteur. La proposition initiale reste en attente de sa réponse.'
    negociationOuverte.value = null
    await charger()
  } catch (e) {
    actionErreur.value = e.message || 'Impossible de transmettre votre réponse.'
  } finally {
    actionEnCours.value = null
  }
}

function accepter(proposition) {
  if (window.confirm('Confirmez-vous l’acceptation de cette proposition ?')) {
    return executerAction(proposition.id, 'accepter', {})
  }
}

function refuser(proposition) {
  if (window.confirm('Confirmez-vous le refus définitif de cette proposition ?')) {
    return executerAction(proposition.id, 'refuser', {})
  }
}

function revendiquer(proposition) {
  if (negociation.salaireRevendique === '' && !negociation.avantageRevendique) return
  const corps = {
    type: 'REVENDICATION',
    salaireRevendique: negociation.salaireRevendique === '' ? null : Number(negociation.salaireRevendique),
    avantageRevendique: negociation.avantageRevendique || null,
    motif: negociation.motif
  }
  return executerAction(proposition.id, 'revendiquer', corps)
}

onMounted(charger)
</script>

<style scoped>
.space { display: flex; flex-direction: column; gap: 1.2rem; }
.heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 1rem; }
h1, h2, h3, p { margin-top: 0; }
h1 { margin-bottom: .35rem; color: #14143a; }
.heading p:not(.eyebrow) { margin-bottom: 0; color: rgba(20,20,58,.7); }
.eyebrow { margin-bottom: .35rem; color: #5b5bd6; text-transform: uppercase; letter-spacing: .08em; font-size: .75rem; font-weight: 700; }
.secondary { padding: .65rem 1rem; border: 1px solid rgba(20,20,58,.16); border-radius: 999px; color: #34348f; font-weight: 600; white-space: nowrap; }
.state { padding: 1.2rem; border-radius: 14px; background: #fff; color: rgba(20,20,58,.75); }
.list { display: grid; gap: 1.2rem; }
.offer { padding: 1.5rem; border-radius: 18px; background: #fff; box-shadow: 0 30px 55px -45px rgba(50,50,130,.55); }
.offer-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
.offer-heading h2 { margin-bottom: .3rem; color: #14143a; }
.offer-heading p:last-child { color: rgba(20,20,58,.68); }
.status { padding: .4rem .75rem; border-radius: 999px; background: #edf0ff; color: #4242a5; font-size: .85rem; font-weight: 700; white-space: nowrap; }
.status.acceptee { background: #e7f7ed; color: #19733a; }
.status.refusee, .status.expiree, .status.annulee { background: #fff0ef; color: #a92c21; }
.facts { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: .8rem; padding: 1rem 0; border-top: 1px solid #eeeef6; border-bottom: 1px solid #eeeef6; }
.facts div { display: flex; flex-direction: column; gap: .25rem; }
.facts small { color: rgba(20,20,58,.62); }
.facts strong { color: #14143a; }
.detail { margin: .8rem 0; color: rgba(20,20,58,.85); }
.benefits { margin-top: 1rem; }
.benefits h3, .negotiate-form h3 { margin-bottom: .45rem; color: #14143a; font-size: 1rem; }
.benefits ul { margin: 0; padding-left: 1.2rem; line-height: 1.7; }
.deadline { display: flex; justify-content: space-between; gap: 1rem; margin-top: 1rem; padding: .85rem 1rem; border-radius: 12px; background: #f2f2ff; color: #35358b; }
.deadline.expired { background: #fff0ef; color: #a92c21; }
.actions { display: flex; flex-wrap: wrap; gap: .7rem; margin-top: 1rem; }
.actions button, .negotiate-form button { padding: .7rem 1rem; border: 0; border-radius: 10px; color: #fff; font: inherit; font-weight: 700; cursor: pointer; }
.accept { background: #218348; }
.reject { background: #ad3e36; }
.negotiate { background: #5151c7; }
button:disabled { opacity: .55; cursor: wait; }
.negotiate-form { display: grid; gap: .8rem; margin-top: 1rem; padding: 1rem; border-radius: 12px; background: #f8f8ff; }
.negotiate-form label { display: grid; gap: .4rem; color: #14143a; font-weight: 600; }
.negotiate-form input, .negotiate-form textarea { padding: .65rem .8rem; border: 1px solid #d8d8e8; border-radius: 8px; font: inherit; }
.inline-error { margin: 0; color: #a92c21; }
.negotiations { display: grid; gap: .7rem; margin-top: 1rem; }
.negotiations h3 { margin: 0; color: #14143a; font-size: 1rem; }
.negotiation { padding: .9rem; border-radius: 10px; background: #f8f8ff; color: rgba(20,20,58,.82); }
.negotiation p { margin: .35rem 0; }
.negotiation small { color: rgba(20,20,58,.62); }
.decision { font-weight: 600; }
.feedback { margin: 0; font-weight: 600; }
.success { color: #19733a; }
.error { color: #a92c21; }
.tests { display: grid; gap: .8rem; }
.tests h2, .titre-propositions { margin: 0; color: #14143a; font-size: 1.15rem; }
.test { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: 1.1rem 1.3rem; border-radius: 16px; background: #fff; box-shadow: 0 30px 55px -45px rgba(50,50,130,.55); }
.test h3 { margin: 0 0 .3rem; color: #14143a; font-size: 1rem; }
.test p { margin: 0; color: rgba(20,20,58,.7); }
.passer { padding: .7rem 1.1rem; border-radius: 10px; background: #5151c7; color: #fff; font-weight: 700; white-space: nowrap; text-decoration: none; }
.status.envoye { background: #e7f7ed; color: #19733a; }
.status.en_preparation { background: #fff6e5; color: #8a5a06; }
@media (max-width: 760px) { .test { flex-direction: column; align-items: flex-start; } }
@media (max-width: 760px) { .heading, .deadline { align-items: flex-start; flex-direction: column; } .facts { grid-template-columns: 1fr 1fr; } }
</style>
