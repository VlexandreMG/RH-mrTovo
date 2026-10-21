<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/client'
import ModalDialog from '@/components/bloc3/ModalDialog.vue'
import StatutBadge from '@/components/bloc3/StatutBadge.vue'

const route = useRoute()
const router = useRouter()

const proposition = ref(null)
const chargement = ref(true)
const erreur = ref(null)
const message = ref(null)
const modal = ref(null)
const acteur = ref('RH')

const formulaireValidation = reactive({ commentaire: '', derogation: false, motifDerogation: '' })
const formulaireRevendication = reactive({
  salaireRevendique: null,
  avantageRevendique: '',
  motif: '',
})

const formatMoney = (valeur) =>
  new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(valeur ?? 0)

const formatDate = (valeur) => (valeur ? new Date(valeur).toLocaleDateString('fr-FR') : '—')

const formatDateTime = (valeur) => (valeur ? new Date(valeur).toLocaleString('fr-FR') : '—')

onMounted(charger)

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    proposition.value = await api.proposition(route.params.id)
  } catch (exception) {
    erreur.value = exception.message
  } finally {
    chargement.value = false
  }
}

const statut = computed(() => proposition.value?.statut)

const actionsPossibles = computed(() => {
  switch (statut.value) {
    case 'BROUILLON':
      return [
        { code: 'valider', label: 'Valider (RH)', style: 'is-primary' },
        { code: 'annuler', label: 'Annuler', style: 'is-danger' },
      ]
    case 'VALIDEE':
      return [
        { code: 'emettre', label: 'Émettre au candidat', style: 'is-primary' },
        { code: 'annuler', label: 'Annuler', style: 'is-danger' },
      ]
    case 'EMISE':
      return [
        { code: 'accepter', label: 'Le candidat accepte', style: 'is-success' },
        { code: 'revoquer', label: 'Le candidat révocque', style: '' },
        { code: 'refuser', label: 'Le candidat refuse', style: 'is-danger' },
      ]
    case 'EN_NEGOCIATION':
      return [{ code: 'annuler', label: 'Annuler', style: 'is-danger' }]
    case 'REVISE':
      return [
        { code: 'valider', label: 'Valider la version révisée', style: 'is-primary' },
        { code: 'annuler', label: 'Annuler', style: 'is-danger' },
      ]
    default:
      return []
  }
})

function ouvrirAction(code) {
  erreur.value = null
  message.value = null
  if (code === 'valider') {
    formulaireValidation.commentaire = ''
    formulaireValidation.derogation = false
    formulaireValidation.motifDerogation = ''
  }
  if (code === 'revoquer') {
    formulaireRevendication.salaireRevendique = null
    formulaireRevendication.avantageRevendique = ''
    formulaireRevendication.motif = ''
  }
  modal.value = code
}

async function executer(action) {
  erreur.value = null
  message.value = null
  try {
    if (action === 'valider') {
      await api.validerProposition(proposition.value.id, {
        acteur: acteur.value,
        commentaire: formulaireValidation.commentaire,
        derogation: formulaireValidation.derogation,
        motifDerogation: formulaireValidation.motifDerogation,
      })
      message.value = 'Proposition validée par les RH.'
    } else if (action === 'emettre') {
      await api.emettreProposition(proposition.value.id, { acteur: acteur.value })
      message.value = 'Proposition émise au candidat.'
    } else if (action === 'accepter') {
      await api.accepterProposition(proposition.value.id, { acteur: 'CANDIDAT' })
      message.value = 'Offre acceptée : le dossier salarié, le contrat et l’affectation ont été créés.'
    } else if (action === 'refuser') {
      await api.refuserProposition(proposition.value.id, { acteur: 'CANDIDAT' })
      message.value = 'Le candidat a refusé l’offre.'
    } else if (action === 'annuler') {
      await api.annulerProposition(proposition.value.id, { acteur: acteur.value })
      message.value = 'Proposition annulée.'
    }
    modal.value = null
    await charger()
  } catch (exception) {
    erreur.value = exception.message
  }
}

async function soumettreRevendication() {
  erreur.value = null
  try {
    await api.revoquerProposition(proposition.value.id, {
      acteur: 'CANDIDAT',
      type: 'REVENDICATION',
      salaireRevendique: formulaireRevendication.salaireRevendique
        ? Number(formulaireRevendication.salaireRevendique)
        : null,
      avantageRevendique: formulaireRevendication.avantageRevendique,
      motif: formulaireRevendication.motif,
    })
    modal.value = null
    message.value = 'Revendication transmise aux RH.'
    await charger()
  } catch (exception) {
    erreur.value = exception.message
  }
}
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1 v-if="proposition">
          Proposition #{{ proposition.id }} — {{ proposition.candidatPrenom }} {{ proposition.candidatNom }}
          <StatutBadge :statut="proposition.statut" />
        </h1>
        <h1 v-else>Proposition</h1>
        <p v-if="proposition">
          Version {{ proposition.version }} · {{ proposition.offreTitre }} · poste {{ proposition.posteTitre }}
        </p>
      </div>
      <div class="b3-actions">
        <select v-model="acteur" class="b3-btn" style="padding: 0.4rem 0.6rem">
          <option value="RH">RH</option>
          <option value="RECRUTEUR">Recruteur</option>
          <option value="DIRECTION">Direction</option>
          <option value="CANDIDAT">Candidat</option>
        </select>
        <button class="b3-btn" @click="charger">Actualiser</button>
        <button class="b3-btn" @click="router.back()">Retour</button>
      </div>
    </header>

    <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>
    <div v-if="message" class="b3-alert is-success">{{ message }}</div>

    <div v-if="chargement" class="b3-card b3-empty">Chargement…</div>

    <template v-else-if="proposition">
      <div class="b3-card">
        <h2>
          Actions du workflow
          <span class="b3-muted" style="font-size: 0.8rem; font-weight: 400">
            validee par {{ proposition.valideePar ?? '—' }}
          </span>
        </h2>
        <div class="b3-actions">
          <button
            v-for="action in actionsPossibles"
            :key="action.code"
            class="b3-btn"
            :class="action.style"
            @click="ouvrirAction(action.code)"
          >
            {{ action.label }}
          </button>
          <span v-if="!actionsPossibles.length" class="b3-muted">
            Aucune action disponible pour le statut {{ proposition.statut }}.
          </span>
        </div>
      </div>

      <div class="b3-grid b3-grid-2">
        <div class="b3-card">
          <h2>Conditions de l'offre</h2>
          <dl class="b3-detail-list">
            <div><dt>Salaire de base</dt><dd class="b3-money">{{ formatMoney(proposition.salaireBase) }}</dd></div>
            <div>
              <dt>Conformité grille</dt>
              <dd>
                <span v-if="proposition.grille">
                  <StatutBadge statut="ACCEPTEE" /> {{ proposition.grille.niveau }}
                </span>
                <span v-else-if="proposition.dansLaGrille" class="b3-muted">Aucune grille pour ce poste</span>
                <span v-else><StatutBadge statut="REFUSEE" /> hors grille</span>
              </dd>
            </div>
            <div><dt>Type de contrat</dt><dd>{{ proposition.typeContrat }}</dd></div>
            <div><dt>Période d'essai</dt><dd>{{ proposition.periodeEssaiMois }} mois</dd></div>
            <div><dt>Temps de travail</dt><dd>{{ proposition.tempsTravail }}</dd></div>
            <div><dt>Emploi du temps</dt><dd>{{ proposition.emploiDuTemps ?? '—' }}</dd></div>
            <div><dt>Lieu de travail</dt><dd>{{ proposition.lieuTravail }}</dd></div>
            <div><dt>Prise de poste</dt><dd>{{ formatDate(proposition.datePrisePoste) }}</dd></div>
            <div><dt>Validité de l'offre</dt><dd>{{ formatDate(proposition.dateLimiteValidite) }}</dd></div>
            <div><dt>Émission</dt><dd>{{ formatDate(proposition.dateEmission) }}</dd></div>
            <div><dt>Acceptation</dt><dd>{{ formatDate(proposition.dateAcceptation) }}</dd></div>
            <div v-if="proposition.matricule">
              <dt>Matricule</dt><dd><strong>{{ proposition.matricule }}</strong></dd>
            </div>
            <div v-if="proposition.motifRevision">
              <dt>Motif de révision</dt><dd>{{ proposition.motifRevision }}</dd>
            </div>
          </dl>
        </div>

        <div class="b3-card">
          <h2>Avantages de l'offre</h2>
          <div class="b3-table-wrapper">
            <table class="b3-table">
              <thead>
                <tr><th>Type</th><th>Libellé</th><th>Montant</th></tr>
              </thead>
              <tbody>
                <tr v-for="avantage in proposition.avantages" :key="avantage.id">
                  <td>{{ avantage.type }}</td>
                  <td>{{ avantage.libelle }}</td>
                  <td class="b3-money">{{ avantage.montant ? formatMoney(avantage.montant) : '—' }}</td>
                </tr>
                <tr v-if="!proposition.avantages.length">
                  <td colspan="3" class="b3-empty">Aucun avantage déclaré.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <h2 style="margin-top: 1.25rem">Négociations</h2>
          <div v-if="proposition.negotiations.length">
            <p v-for="negociation in proposition.negotiations" :key="negociation.id" style="margin: 0 0 0.6rem">
              <StatutBadge :statut="negociation.statut" />
              {{ negociation.motif }}
              <span v-if="negociation.salaireRevendique" class="b3-muted">
                (salaire demandé : {{ formatMoney(negociation.salaireRevendique) }})
              </span>
            </p>
          </div>
          <p v-else class="b3-muted">Aucune négociation sur cette proposition.</p>
        </div>
      </div>

      <div class="b3-card">
        <h2>Historique de la proposition</h2>
        <ul class="b3-timeline">
          <li v-for="entree in proposition.historique" :key="entree.id">
            <strong>{{ entree.action }} — {{ entree.nouveauStatut }}</strong>
            <span>
              {{ formatDateTime(entree.dateChangement) }} · {{ entree.auteur ?? '—' }}
              <template v-if="entree.commentaire"> · {{ entree.commentaire }}</template>
            </span>
          </li>
          <li v-if="!proposition.historique.length" class="b3-empty">Aucun événement enregistré.</li>
        </ul>
      </div>
    </template>

    <ModalDialog
      v-if="modal === 'valider'"
      title="Validation RH de la proposition"
      subtitle="La grille salariale du poste est contrôlée automatiquement."
      @close="modal = null"
    >
      <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>
      <div v-if="proposition && !proposition.dansLaGrille" class="b3-alert is-info">
        Le salaire proposé est hors grille : une dérogation motivée est nécessaire.
      </div>
      <div class="b3-form-grid">
        <div class="b3-field is-full">
          <label for="commentaireValidation">Commentaire</label>
          <textarea id="commentaireValidation" v-model="formulaireValidation.commentaire" rows="3"></textarea>
        </div>
        <div class="b3-field">
          <label for="derogation">Dérogation hors grille</label>
          <select id="derogation" v-model="formulaireValidation.derogation">
            <option :value="false">Non</option>
            <option :value="true">Oui</option>
          </select>
        </div>
        <div class="b3-field">
          <label for="motifDerogation">Motif de la dérogation</label>
          <input id="motifDerogation" v-model="formulaireValidation.motifDerogation" type="text" />
        </div>
      </div>
      <template #footer>
        <button class="b3-btn" @click="modal = null">Annuler</button>
        <button class="b3-btn is-primary" @click="executer('valider')">Valider</button>
      </template>
    </ModalDialog>

    <ModalDialog
      v-if="modal === 'revoquer'"
      title="Revendication du candidat"
      subtitle="La proposition passe en négociation : les RH arbitrent et peuvent refuser."
      @close="modal = null"
    >
      <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>
      <div class="b3-form-grid">
        <div class="b3-field">
          <label for="salaireRevendique">Salaire revendiqué</label>
          <input id="salaireRevendique" v-model.number="formulaireRevendication.salaireRevendique" type="number" min="0" />
        </div>
        <div class="b3-field">
          <label for="avantageRevendique">Avantage revendiqué</label>
          <input id="avantageRevendique" v-model="formulaireRevendication.avantageRevendique" type="text" />
        </div>
        <div class="b3-field is-full">
          <label for="motifRevendication">Motif</label>
          <textarea id="motifRevendication" v-model="formulaireRevendication.motif" rows="3"></textarea>
        </div>
      </div>
      <template #footer>
        <button class="b3-btn" @click="modal = null">Annuler</button>
        <button class="b3-btn is-primary" @click="soumettreRevendication">Transmettre aux RH</button>
      </template>
    </ModalDialog>

    <ModalDialog
      v-if="modal === 'accepter' || modal === 'refuser' || modal === 'annuler' || modal === 'emettre'"
      :title="{
        accepter: 'Acceptation de l’offre par le candidat',
        refuser: 'Refus de l’offre par le candidat',
        annuler: 'Annulation de la proposition',
        emettre: 'Émission de la proposition au candidat',
      }[modal]"
      @close="modal = null"
    >
      <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>
      <p v-if="modal === 'accepter'" class="b3-muted">
        La création du dossier salarié (matricule), du contrat et de l’affectation sera immédiate.
      </p>
      <template #footer>
        <button class="b3-btn" @click="modal = null">Annuler</button>
        <button
          class="b3-btn"
          :class="modal === 'accepter' ? 'is-success' : modal === 'emettre' ? 'is-primary' : 'is-danger'"
          @click="executer(modal)"
        >
          Confirmer
        </button>
      </template>
    </ModalDialog>
  </section>
</template>
