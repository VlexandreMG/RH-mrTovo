<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useBloc3Store } from '@/stores/bloc3'
import api from '@/api/client'
import ModalDialog from '@/components/bloc3/ModalDialog.vue'
import StatutBadge from '@/components/bloc3/StatutBadge.vue'

const store = useBloc3Store()

const modalOuvert = ref(false)
const negociationCourante = ref(null)
const erreur = ref(null)
const message = ref(null)

const formulaire = reactive({
  decision: 'ACCEPTE',
  salaireAccorde: null,
  commentaire: '',
  derogation: false,
  motifDerogation: '',
  avantages: [],
})

const avantageCourant = reactive({ type: 'MUTUELLE', libelle: '', montant: null })

onMounted(() => store.chargerNegotiations())

const formatMoney = (valeur) =>
  new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(valeur ?? 0)

const formatDate = (valeur) => (valeur ? new Date(valeur).toLocaleDateString('fr-FR') : '—')

const enCours = computed(() =>
  store.negotiations.filter((negociation) =>
    ['EN_ATTENTE', 'EN_ANALYSE', 'CONTRE_PROPOSITION'].includes(negociation.statut),
  ),
)

const decisionnaires = computed(() =>
  store.negotiations.filter((negociation) => ['ACCEPTE', 'REFUSE'].includes(negociation.statut)),
)

async function analyser(negociation) {
  erreur.value = null
  message.value = null
  try {
    await api.analyserNegociation(negociation.id, { acteur: 'RH' })
    message.value = 'Négociation analysée par les RH.'
    await store.chargerNegotiations()
  } catch (exception) {
    erreur.value = exception.message
  }
}

function ouvrirDecision(negociation) {
  erreur.value = null
  message.value = null
  negociationCourante.value = negociation
  formulaire.decision = 'ACCEPTE'
  formulaire.salaireAccorde = negociation.salaireRevendique ?? null
  formulaire.commentaire = ''
  formulaire.derogation = false
  formulaire.motifDerogation = ''
  formulaire.avantages = []
  modalOuvert.value = true
}

function ajouterAvantage() {
  if (!avantageCourant.libelle) return
  formulaire.avantages.push({ ...avantageCourant })
  avantageCourant.libelle = ''
  avantageCourant.montant = null
}

async function soumettreDecision() {
  erreur.value = null
  try {
    await api.deciderNegociation(negociationCourante.value.id, {
      acteur: 'RH',
      decision: formulaire.decision,
      salaireAccorde: formulaire.salaireAccorde ? Number(formulaire.salaireAccorde) : null,
      commentaire: formulaire.commentaire,
      derogation: formulaire.derogation,
      motifDerogation: formulaire.motifDerogation,
      avantages: formulaire.avantages,
    })
    modalOuvert.value = false
    message.value = 'Décision enregistrée : une nouvelle version de la proposition a été créée.'
    await store.chargerNegotiations()
  } catch (exception) {
    erreur.value = exception.message
  }
}
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Négociations</h1>
        <p>
          Toute revendication du candidat est analysée par les RH ; l'acceptation produit une version révisée de
          la proposition (l'historique est conservé).
        </p>
      </div>
      <button class="b3-btn" :disabled="store.loading" @click="store.chargerNegotiations()">Actualiser</button>
    </header>

    <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>
    <div v-if="message" class="b3-alert is-success">{{ message }}</div>

    <div class="b3-card">
      <h2>Négociations en cours</h2>
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Candidat</th>
              <th>Type</th>
              <th>Demande</th>
              <th>Motif</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="negociation in enCours" :key="negociation.id">
              <td>#{{ negociation.id }}</td>
              <td>
                {{ negociation.candidatPrenom }} {{ negociation.candidatNom }}
                <div class="b3-muted" style="font-size: 0.78rem">
                  Proposition v{{ negociation.propositionVersion }} (#{{ negociation.propositionId }})
                </div>
              </td>
              <td>{{ negociation.type }}</td>
              <td>
                <span v-if="negociation.salaireRevendique" class="b3-money">
                  {{ formatMoney(negociation.salaireRevendique) }}
                </span>
                <span v-if="negociation.avantageRevendique" class="b3-muted">
                  {{ negociation.avantageRevendique }}
                </span>
                <span v-if="!negociation.salaireRevendique && !negociation.avantageRevendique" class="b3-muted">
                  —
                </span>
              </td>
              <td class="b3-cell-note">{{ negociation.motif }}</td>
              <td><StatutBadge :statut="negociation.statut" /></td>
              <td>
                <div class="b3-actions">
                  <button
                    v-if="negociation.statut === 'EN_ATTENTE'"
                    class="b3-btn"
                    @click="analyser(negociation)"
                  >
                    Analyser
                  </button>
                  <button class="b3-btn is-primary" @click="ouvrirDecision(negociation)">Décider</button>
                  <RouterLink class="b3-btn" :to="`/bloc3/propositions/${negociation.propositionId}`">
                    Proposition
                  </RouterLink>
                </div>
              </td>
            </tr>
            <tr v-if="!enCours.length">
              <td colspan="7" class="b3-empty">Aucune négociation en cours.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="b3-card">
      <h2>Négociations tranchées</h2>
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Candidat</th>
              <th>Décision</th>
              <th>Décideur</th>
              <th>Date</th>
              <th>Commentaire</th>
              <th>Version résultante</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="negociation in decisionnaires" :key="negociation.id">
              <td>#{{ negociation.id }}</td>
              <td>{{ negociation.candidatPrenom }} {{ negociation.candidatNom }}</td>
              <td><StatutBadge :statut="negociation.statut" /></td>
              <td>{{ negociation.decidePar ?? '—' }}</td>
              <td>{{ formatDate(negociation.dateDecision) }}</td>
              <td class="b3-cell-note">{{ negociation.commentaireDecision ?? '—' }}</td>
              <td>
                <RouterLink
                  v-if="negociation.propositionResultanteId"
                  class="b3-btn"
                  :to="`/bloc3/propositions/${negociation.propositionResultanteId}`"
                >
                  #{{ negociation.propositionResultanteId }}
                </RouterLink>
                <span v-else class="b3-muted">—</span>
              </td>
            </tr>
            <tr v-if="!decisionnaires.length">
              <td colspan="7" class="b3-empty">Aucune décision enregistrée.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <ModalDialog
      v-if="modalOuvert"
      title="Décision RH sur la négociation"
      :subtitle="`Négociation #${negociationCourante?.id} — ${negociationCourante?.candidatPrenom} ${negociationCourante?.candidatNom}`"
      @close="modalOuvert = false"
    >
      <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

      <div class="b3-form-grid">
        <div class="b3-field">
          <label for="decision">Décision</label>
          <select id="decision" v-model="formulaire.decision">
            <option value="ACCEPTE">Accepter la revendication</option>
            <option value="CONTRE_PROPOSITION">Contre-proposition</option>
            <option value="REFUSE">Refuser</option>
          </select>
        </div>

        <div class="b3-field">
          <label for="salaireAccorde">Salaire accordé</label>
          <input
            id="salaireAccorde"
            v-model.number="formulaire.salaireAccorde"
            type="number"
            min="0"
            step="1000"
            :disabled="formulaire.decision === 'REFUSE'"
          />
        </div>

        <div class="b3-field">
          <label for="derogationNeg">Dérogation hors grille</label>
          <select id="derogationNeg" v-model="formulaire.derogation">
            <option :value="false">Non</option>
            <option :value="true">Oui</option>
          </select>
        </div>

        <div class="b3-field">
          <label for="motifDerogationNeg">Motif de la dérogation</label>
          <input id="motifDerogationNeg" v-model="formulaire.motifDerogation" type="text" />
        </div>

        <div class="b3-field is-full">
          <label for="commentaireNeg">Commentaire de la décision</label>
          <textarea id="commentaireNeg" v-model="formulaire.commentaire" rows="3"></textarea>
        </div>
      </div>

      <div class="b3-card" style="margin-top: 1.25rem; box-shadow: none">
        <h2>Avantages accordés (optionnel)</h2>
        <div class="b3-form-grid">
          <div class="b3-field">
            <label for="typeAvNeg">Type</label>
            <select id="typeAvNeg" v-model="avantageCourant.type">
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
            <label for="libAvNeg">Libellé</label>
            <input id="libAvNeg" v-model="avantageCourant.libelle" type="text" />
          </div>
          <div class="b3-field">
            <label for="montAvNeg">Montant</label>
            <input id="montAvNeg" v-model.number="avantageCourant.montant" type="number" min="0" />
          </div>
          <div class="b3-field" style="justify-content: flex-end">
            <button type="button" class="b3-btn" @click="ajouterAvantage">Ajouter</button>
          </div>
        </div>
        <ul v-if="formulaire.avantages.length" style="margin: 0.9rem 0 0; padding-left: 1.1rem">
          <li v-for="(avantage, index) in formulaire.avantages" :key="index">
            {{ avantage.type }} — {{ avantage.libelle }}
            <button type="button" class="b3-btn is-ghost" @click="formulaire.avantages.splice(index, 1)">
              Retirer
            </button>
          </li>
        </ul>
      </div>

      <template #footer>
        <button class="b3-btn" @click="modalOuvert = false">Annuler</button>
        <button class="b3-btn is-primary" @click="soumettreDecision">Enregistrer la décision</button>
      </template>
    </ModalDialog>
  </section>
</template>
