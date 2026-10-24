<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useBloc3Store } from '@/stores/bloc3'
import api from '@/api/client'
import ModalDialog from '@/components/bloc3/ModalDialog.vue'
import StatutBadge from '@/components/bloc3/StatutBadge.vue'

const store = useBloc3Store()
const router = useRouter()

const modalOuvert = ref(false)
const candidatureSelectionnee = ref(null)
const grilles = ref([])
const avantageCourant = reactive({ type: 'MUTUELLE', libelle: '', montant: null })
const message = ref(null)
const erreur = ref(null)

const formulaire = reactive({
  posteId: '',
  salaireBase: null,
  tempsTravail: '35h',
  emploiDuTemps: 'Lundi au vendredi, 9h-18h',
  lieuTravail: 'Antananarivo',
  datePrisePoste: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10),
  dateLimiteValidite: new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10),
  typeContrat: 'CDI',
  periodeEssaiMois: 6,
  dateFinContrat: '',
  avantages: [],
})

onMounted(async () => {
  await Promise.all([store.chargerCandidatures(), store.chargerPostes()])
})

const pipeline = computed(() => store.candidatures)

const retenues = computed(() =>
  pipeline.value.filter((candidature) => candidature.statut === 'RETENU'),
)

async function ouvrirCreation(candidature) {
  candidatureSelectionnee.value = candidature
  formulaire.posteId = store.postes[0]?.id ?? ''
  formulaire.salaireBase = null
  formulaire.avantages = []
  modalOuvert.value = true
  message.value = null
  erreur.value = null
  await chargerGrilles()
}

async function chargerGrilles() {
  if (!formulaire.posteId) {
    grilles.value = []
    return
  }
  grilles.value = await api.grilles(formulaire.posteId)
}

const horsGrille = computed(() => {
  if (!grilles.value.length || !formulaire.salaireBase) return false
  return !grilles.value.some(
    (grille) =>
      Number(formulaire.salaireBase) >= Number(grille.salaireMin) &&
      Number(formulaire.salaireBase) <= Number(grille.salaireMax),
  )
})

function ajouterAvantage() {
  if (!avantageCourant.libelle) return
  formulaire.avantages.push({ ...avantageCourant })
  avantageCourant.libelle = ''
  avantageCourant.montant = null
}

function retirerAvantage(index) {
  formulaire.avantages.splice(index, 1)
}

async function soumettre() {
  erreur.value = null
  message.value = null
  const corps = {
    candidatureId: candidatureSelectionnee.value.id,
    posteId: Number(formulaire.posteId),
    salaireBase: Number(formulaire.salaireBase),
    tempsTravail: formulaire.tempsTravail,
    emploiDuTemps: formulaire.emploiDuTemps,
    lieuTravail: formulaire.lieuTravail,
    datePrisePoste: formulaire.datePrisePoste,
    dateLimiteValidite: formulaire.dateLimiteValidite,
    typeContrat: formulaire.typeContrat,
    periodeEssaiMois: Number(formulaire.periodeEssaiMois),
    dateFinContrat: formulaire.typeContrat === 'CDD' ? formulaire.dateFinContrat : null,
    avantages: formulaire.avantages,
  }
  try {
    const proposition = await api.creerProposition(corps)
    modalOuvert.value = false
    await store.rafraichirTout()
    router.push(`/bloc3/propositions/${proposition.id}`)
  } catch (exception) {
    erreur.value = exception.message
  }
}
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Candidatures</h1>
        <p>
          Sortie du Bloc 2 : la candidature <strong>RETENU</strong> devient une proposition d'embauche (Bloc
          3). <strong>{{ retenues.length }}</strong> candidature(s) retenue(s) en attente de proposition.
        </p>
      </div>
      <button class="b3-btn" :disabled="store.loading" @click="store.chargerCandidatures()">Actualiser</button>
    </header>

    <div v-if="store.error" class="b3-alert is-error">{{ store.error }}</div>

    <div class="b3-card">
      <h2>Pipeline d'embauche</h2>
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead>
            <tr>
              <th>Candidat</th>
              <th>Offre</th>
              <th>Candidature</th>
              <th>Statut candidature</th>
              <th>Proposition</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="candidature in pipeline" :key="candidature.id">
              <td>
                <strong>{{ candidature.prenom }} {{ candidature.nom }}</strong>
                <div class="b3-muted" style="font-size: 0.78rem">{{ candidature.email }}</div>
              </td>
              <td>{{ candidature.offreTitre }}</td>
              <td>#{{ candidature.id }}</td>
              <td><StatutBadge :statut="candidature.statut" /></td>
              <td>
                <span v-if="candidature.propositionStatut">
                  v{{ candidature.propositionId }} · <StatutBadge :statut="candidature.propositionStatut" />
                </span>
                <span v-else class="b3-muted">—</span>
              </td>
              <td>
                <button
                  v-if="candidature.statut === 'RETENU'"
                  class="b3-btn is-primary"
                  @click="ouvrirCreation(candidature)"
                >
                  Émettre une proposition
                </button>
                <span v-else class="b3-muted">—</span>
              </td>
            </tr>
            <tr v-if="!pipeline.length">
              <td colspan="6" class="b3-empty">Aucune candidature dans le pipeline.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <ModalDialog
      v-if="modalOuvert"
      title="Nouvelle proposition d'embauche"
      :subtitle="`Candidature #${candidatureSelectionnee?.id} — ${candidatureSelectionnee?.prenom} ${candidatureSelectionnee?.nom}`"
      @close="modalOuvert = false"
    >
      <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

      <div class="b3-form-grid">
        <div class="b3-field">
          <label for="poste">Poste</label>
          <select id="poste" v-model="formulaire.posteId" @change="chargerGrilles">
            <option v-for="poste in store.postes" :key="poste.id" :value="poste.id">
              {{ poste.titre }} — {{ poste.departementNom }}
            </option>
          </select>
        </div>

        <div class="b3-field">
          <label for="salaire">Salaire de base</label>
          <input id="salaire" v-model.number="formulaire.salaireBase" type="number" min="0" step="1000" />
          <span v-if="grilles.length" class="b3-field-hint">
            Grille :
            <span v-for="grille in grilles" :key="grille.id" style="margin-right: 0.5rem">
              {{ grille.niveau }} {{ Number(grille.salaireMin).toLocaleString('fr-FR') }} –
              {{ Number(grille.salaireMax).toLocaleString('fr-FR') }}
            </span>
          </span>
          <span v-if="horsGrille" class="b3-field-hint" style="color: var(--b3-warning)">
            Salaire hors grille : une dérogation RH sera exigée lors de la validation.
          </span>
        </div>

        <div class="b3-field">
          <label for="tempsTravail">Temps de travail</label>
          <select id="tempsTravail" v-model="formulaire.tempsTravail">
            <option>35h</option>
            <option>39h</option>
            <option>40h</option>
            <option>Temps partiel</option>
          </select>
        </div>

        <div class="b3-field">
          <label for="emploiDuTemps">Emploi du temps</label>
          <input id="emploiDuTemps" v-model="formulaire.emploiDuTemps" type="text" />
        </div>

        <div class="b3-field">
          <label for="lieu">Lieu de travail</label>
          <input id="lieu" v-model="formulaire.lieuTravail" type="text" />
        </div>

        <div class="b3-field">
          <label for="prisePoste">Date de prise de poste</label>
          <input id="prisePoste" v-model="formulaire.datePrisePoste" type="date" />
        </div>

        <div class="b3-field">
          <label for="validite">Date limite de validité</label>
          <input id="validite" v-model="formulaire.dateLimiteValidite" type="date" />
        </div>

        <div class="b3-field">
          <label for="typeContrat">Type de contrat</label>
          <select id="typeContrat" v-model="formulaire.typeContrat">
            <option value="CDI">CDI</option>
            <option value="CDD">CDD (max 24 mois)</option>
            <option value="STAGE">Stage</option>
            <option value="ALTERNANCE">Alternance</option>
          </select>
        </div>

        <div class="b3-field">
          <label for="periodeEssai">Période d'essai (mois)</label>
          <input id="periodeEssai" v-model.number="formulaire.periodeEssaiMois" type="number" min="0" max="12" />
        </div>

        <div v-if="formulaire.typeContrat === 'CDD'" class="b3-field is-full">
          <label for="finCdd">Date de fin du CDD</label>
          <input id="finCdd" v-model="formulaire.dateFinContrat" type="date" />
          <span class="b3-field-hint">La durée maximale d'un CDD est de 24 mois (règle métier).</span>
        </div>
      </div>

      <div class="b3-card" style="margin-top: 1.25rem; box-shadow: none">
        <h2>Avantages de l'offre</h2>
        <div class="b3-form-grid">
          <div class="b3-field">
            <label for="typeAvantage">Type</label>
            <select id="typeAvantage" v-model="avantageCourant.type">
              <option>MUTUELLE</option>
              <option>EQUIPEMENT</option>
              <option>TELEPHONE</option>
              <option>TRANSPORT</option>
              <option>PRIMES</option>
              <option>FORMATION</option>
              <option>CONGE_SUPPLEMENTAIRE</option>
              <option>AUTRE</option>
            </select>
          </div>
          <div class="b3-field">
            <label for="libelleAvantage">Libellé</label>
            <input id="libelleAvantage" v-model="avantageCourant.libelle" type="text" />
          </div>
          <div class="b3-field">
            <label for="montantAvantage">Montant (optionnel)</label>
            <input id="montantAvantage" v-model.number="avantageCourant.montant" type="number" min="0" />
          </div>
          <div class="b3-field" style="justify-content: flex-end">
            <button type="button" class="b3-btn" @click="ajouterAvantage">Ajouter l'avantage</button>
          </div>
        </div>
        <ul v-if="formulaire.avantages.length" style="margin: 0.9rem 0 0; padding-left: 1.1rem">
          <li v-for="(avantage, index) in formulaire.avantages" :key="index">
            {{ avantage.type }} — {{ avantage.libelle }}
            <span v-if="avantage.montant">({{ avantage.montant.toLocaleString('fr-FR') }})</span>
            <button type="button" class="b3-btn is-ghost" @click="retirerAvantage(index)">Retirer</button>
          </li>
        </ul>
      </div>

      <template #footer>
        <button class="b3-btn" @click="modalOuvert = false">Annuler</button>
        <button class="b3-btn is-primary" @click="soumettre">Créer la proposition</button>
      </template>
    </ModalDialog>
  </section>
</template>
