<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { dossiersApi } from '@/api/dossiers'
import { useToastStore } from '@/stores/toast'
import { ar, date, dateHeure } from '@/utils/format'
import StatutBadge from '@/components/RecrutementStatutBadge.vue'

const route = useRoute()
const toast = useToastStore()

const ONGLETS = [
  { cle: 'identite', libelle: 'Identité' },
  { cle: 'situation', libelle: 'Situation professionnelle' },
  { cle: 'contrats', libelle: 'Contrats & salaire' },
  { cle: 'carriere', libelle: 'Carrière' },
  { cle: 'documents', libelle: 'Documents' },
  { cle: 'journal', libelle: "Journal d'audit" }
]

const TYPES_DOCUMENT = {
  CIN: "Carte d'identité nationale",
  DIPLOME: 'Diplôme',
  CERTIFICAT_RESIDENCE: 'Certificat de résidence',
  CASIER_JUDICIAIRE: 'Bulletin de casier judiciaire',
  RIB: "Relevé d'identité bancaire",
  CONTRAT_SIGNE: 'Contrat signé',
  CERTIFICAT_MEDICAL: "Certificat médical d'aptitude",
  AUTRE: 'Autre document'
}

const SITUATIONS_FAMILIALES = {
  CELIBATAIRE: 'Célibataire',
  MARIE: 'Marié(e)',
  DIVORCE: 'Divorcé(e)',
  VEUF: 'Veuf / veuve',
  UNION_LIBRE: 'Union libre'
}

const dossier = ref(null)
const journal = ref([])
const chargement = ref(false)
const erreur = ref('')
const onglet = ref('identite')

const editionIdentite = ref(false)
const formIdentite = reactive({})
const enregistrement = ref(false)

const formDocument = reactive(documentVide())
const fichier = ref(null)
const champFichier = ref(null)
const depot = ref(false)

const employeId = computed(() => route.params.id)
const identite = computed(() => dossier.value?.identite ?? {})
const situation = computed(() => dossier.value?.situation ?? {})
const origine = computed(() => dossier.value?.origine ?? {})

function documentVide() {
  return { typeDocument: 'CIN', libelle: '', numero: '', dateEmission: '', dateExpiration: '' }
}

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    dossier.value = await dossiersApi.dossier(employeId.value)
  } catch (e) {
    erreur.value = e.message
  } finally {
    chargement.value = false
  }
}

async function chargerJournal() {
  try {
    journal.value = await dossiersApi.journal(employeId.value)
  } catch (e) {
    toast.error(e.message)
  }
}

function commencerEdition() {
  Object.assign(formIdentite, { ...identite.value })
  delete formIdentite.age
  editionIdentite.value = true
}

async function enregistrerIdentite() {
  enregistrement.value = true
  try {
    const corps = Object.fromEntries(
      Object.entries(formIdentite).map(([cle, valeur]) => [cle, valeur === '' ? null : valeur])
    )
    dossier.value = await dossiersApi.modifierIdentite(employeId.value, corps)
    editionIdentite.value = false
    toast.success('Identité mise à jour.')
  } catch (e) {
    toast.error(e.message)
  } finally {
    enregistrement.value = false
  }
}

function choisirFichier(evenement) {
  fichier.value = evenement.target.files?.[0] ?? null
}

async function deposerDocument() {
  depot.value = true
  try {
    await dossiersApi.ajouterDocument(employeId.value, formDocument, fichier.value)
    Object.assign(formDocument, documentVide())
    fichier.value = null
    if (champFichier.value) champFichier.value.value = ''
    toast.success('Document ajouté au dossier.')
    await charger()
  } catch (e) {
    toast.error(e.message)
  } finally {
    depot.value = false
  }
}

async function telecharger(document) {
  try {
    const blob = await dossiersApi.telechargerDocument(document.id)
    const url = URL.createObjectURL(blob)
    const lien = window.document.createElement('a')
    lien.href = url
    lien.download = document.nomFichier ?? `document-${document.id}`
    lien.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    toast.error(e.message)
  }
}

async function supprimer(document) {
  if (!window.confirm(`Supprimer « ${document.libelle} » du dossier ? Cette action est tracée.`)) return
  try {
    await dossiersApi.supprimerDocument(document.id)
    toast.success('Document supprimé.')
    await charger()
  } catch (e) {
    toast.error(e.message)
  }
}

function libelleSexe(sexe) {
  return sexe === 'M' ? 'Masculin' : sexe === 'F' ? 'Féminin' : '—'
}

function joursRestants(document) {
  const jours = document.joursAvantExpiration
  if (jours === null || jours === undefined) return ''
  if (jours < 0) return `depuis ${-jours} j`
  return `dans ${jours} j`
}

watch(onglet, (valeur) => {
  if (valeur === 'journal') chargerJournal()
})

watch(employeId, () => {
  editionIdentite.value = false
  onglet.value = 'identite'
  charger()
})

onMounted(charger)
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <RouterLink to="/bloc5/dossiers" class="b3-muted retour">← Tous les dossiers</RouterLink>
        <h1 v-if="dossier">
          {{ dossier.matricule }} — {{ identite.prenom }} {{ identite.nom }}
          <StatutBadge :statut="situation.statut" />
        </h1>
        <h1 v-else>Dossier salarié</h1>
        <p v-if="dossier">
          {{ situation.posteTitre ?? 'Poste non renseigné' }} · {{ situation.departementNom ?? '—' }} ·
          ancienneté : {{ situation.anciennete ?? '—' }}
        </p>
      </div>
      <button class="b3-btn" :disabled="chargement" @click="charger">Actualiser</button>
    </header>

    <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

    <template v-if="dossier">
      <div v-if="dossier.alertes.length" class="b3-card alertes">
        <h2>À traiter ({{ dossier.alertes.length }})</h2>
        <ul>
          <li v-for="alerte in dossier.alertes" :key="alerte">{{ alerte }}</li>
        </ul>
      </div>

      <nav class="onglets" role="tablist">
        <button
          v-for="o in ONGLETS"
          :key="o.cle"
          role="tab"
          :aria-selected="onglet === o.cle"
          :class="{ actif: onglet === o.cle }"
          @click="onglet = o.cle"
        >
          {{ o.libelle }}
          <span v-if="o.cle === 'documents'" class="compteur">{{ dossier.documents.length }}</span>
        </button>
      </nav>

      <!-- Identité -->
      <div v-if="onglet === 'identite'" class="b3-card">
        <div class="entete-carte">
          <h2>Identité et coordonnées</h2>
          <button v-if="!editionIdentite" class="b3-btn is-primary" @click="commencerEdition">Modifier</button>
        </div>

        <dl v-if="!editionIdentite" class="b3-detail-list">
          <div><dt>Nom</dt><dd>{{ identite.nom }}</dd></div>
          <div><dt>Prénom</dt><dd>{{ identite.prenom }}</dd></div>
          <div><dt>Date de naissance</dt><dd>{{ date(identite.dateNaissance) }}<template v-if="identite.age !== null"> ({{ identite.age }} ans)</template></dd></div>
          <div><dt>Lieu de naissance</dt><dd>{{ identite.lieuNaissance ?? '—' }}</dd></div>
          <div><dt>Sexe</dt><dd>{{ libelleSexe(identite.sexe) }}</dd></div>
          <div><dt>Nationalité</dt><dd>{{ identite.nationalite ?? '—' }}</dd></div>
          <div><dt>N° CIN</dt><dd>{{ identite.numeroCin ?? '—' }}</dd></div>
          <div><dt>N° CNaPS</dt><dd>{{ identite.numeroCnaps ?? '—' }}</dd></div>
          <div><dt>Situation familiale</dt><dd>{{ SITUATIONS_FAMILIALES[identite.situationFamiliale] ?? '—' }}</dd></div>
          <div><dt>Nombre d'enfants</dt><dd>{{ identite.nombreEnfants ?? '—' }}</dd></div>
          <div><dt>Email</dt><dd>{{ identite.email }}</dd></div>
          <div><dt>Téléphone</dt><dd>{{ identite.telephone ?? '—' }}</dd></div>
          <div><dt>Adresse</dt><dd>{{ identite.adresse ?? '—' }}</dd></div>
          <div><dt>Contact d'urgence</dt><dd>{{ identite.contactUrgenceNom ?? '—' }} {{ identite.contactUrgenceTelephone ?? '' }}</dd></div>
        </dl>

        <form v-else @submit.prevent="enregistrerIdentite">
          <div class="b3-form-grid">
            <div class="b3-field"><label for="nom">Nom *</label><input id="nom" v-model="formIdentite.nom" required /></div>
            <div class="b3-field"><label for="prenom">Prénom *</label><input id="prenom" v-model="formIdentite.prenom" required /></div>
            <div class="b3-field"><label for="naissance">Date de naissance</label><input id="naissance" v-model="formIdentite.dateNaissance" type="date" /></div>
            <div class="b3-field"><label for="lieu">Lieu de naissance</label><input id="lieu" v-model="formIdentite.lieuNaissance" /></div>
            <div class="b3-field">
              <label for="sexe">Sexe</label>
              <select id="sexe" v-model="formIdentite.sexe">
                <option :value="null">—</option>
                <option value="M">Masculin</option>
                <option value="F">Féminin</option>
              </select>
            </div>
            <div class="b3-field"><label for="nationalite">Nationalité</label><input id="nationalite" v-model="formIdentite.nationalite" /></div>
            <div class="b3-field"><label for="cin">N° CIN</label><input id="cin" v-model="formIdentite.numeroCin" /></div>
            <div class="b3-field"><label for="cnaps">N° CNaPS</label><input id="cnaps" v-model="formIdentite.numeroCnaps" /></div>
            <div class="b3-field">
              <label for="situationFamiliale">Situation familiale</label>
              <select id="situationFamiliale" v-model="formIdentite.situationFamiliale">
                <option :value="null">—</option>
                <option v-for="(libelle, cle) in SITUATIONS_FAMILIALES" :key="cle" :value="cle">{{ libelle }}</option>
              </select>
            </div>
            <div class="b3-field"><label for="enfants">Nombre d'enfants</label><input id="enfants" v-model.number="formIdentite.nombreEnfants" type="number" min="0" /></div>
            <div class="b3-field"><label for="email">Email *</label><input id="email" v-model="formIdentite.email" type="email" required /></div>
            <div class="b3-field"><label for="telephone">Téléphone</label><input id="telephone" v-model="formIdentite.telephone" /></div>
            <div class="b3-field is-full"><label for="adresse">Adresse</label><input id="adresse" v-model="formIdentite.adresse" /></div>
            <div class="b3-field"><label for="urgenceNom">Contact d'urgence</label><input id="urgenceNom" v-model="formIdentite.contactUrgenceNom" /></div>
            <div class="b3-field"><label for="urgenceTel">Tél. contact d'urgence</label><input id="urgenceTel" v-model="formIdentite.contactUrgenceTelephone" /></div>
          </div>
          <div class="b3-actions actions-formulaire">
            <button type="button" class="b3-btn" @click="editionIdentite = false">Annuler</button>
            <button type="submit" class="b3-btn is-primary" :disabled="enregistrement">Enregistrer</button>
          </div>
        </form>
      </div>

      <!-- Situation professionnelle -->
      <div v-else-if="onglet === 'situation'" class="b3-grid b3-grid-2">
        <div class="b3-card">
          <h2>Situation actuelle</h2>
          <dl class="b3-detail-list">
            <div><dt>Poste</dt><dd>{{ situation.posteTitre ?? '—' }}</dd></div>
            <div><dt>Département</dt><dd>{{ situation.departementNom ?? '—' }}</dd></div>
            <div><dt>Statut</dt><dd>{{ situation.statutLibelle }}</dd></div>
            <div><dt>Ancienneté</dt><dd>{{ situation.anciennete ?? '—' }}</dd></div>
            <div><dt>Date d'embauche</dt><dd>{{ date(situation.dateEmbauche) }}</dd></div>
            <div><dt>Prise de poste</dt><dd>{{ date(situation.dateEntree) }}</dd></div>
            <div><dt>Fin de période d'essai</dt><dd>{{ date(situation.dateFinPeriodeEssai) }}</dd></div>
            <div><dt>Fin de contrat</dt><dd>{{ date(situation.dateFinContrat) }}</dd></div>
            <div><dt>Contrat en cours</dt><dd>{{ situation.contratEnCoursReference ?? '—' }} {{ situation.contratEnCoursType ?? '' }}</dd></div>
            <div><dt>Salaire actuel</dt><dd class="b3-money">{{ ar(situation.salaireActuel) }}</dd></div>
          </dl>
        </div>
        <div class="b3-card">
          <h2>Origine du recrutement</h2>
          <dl class="b3-detail-list">
            <div><dt>Offre</dt><dd>{{ origine.offreTitre ?? '—' }}</dd></div>
            <div><dt>Date de candidature</dt><dd>{{ date(origine.dateCandidature) }}</dd></div>
            <div><dt>Proposition acceptée le</dt><dd>{{ date(origine.dateAcceptation) }}</dd></div>
            <div>
              <dt>Proposition</dt>
              <dd>
                <RouterLink v-if="origine.propositionId" :to="`/bloc3/propositions/${origine.propositionId}`">
                  Voir la proposition n° {{ origine.propositionId }}
                </RouterLink>
                <template v-else>—</template>
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <!-- Contrats & salaire -->
      <div v-else-if="onglet === 'contrats'" class="b3-grid">
        <div class="b3-card">
          <h2>Contrats successifs</h2>
          <div class="b3-table-wrapper">
            <table class="b3-table">
              <thead>
                <tr><th>Référence</th><th>Type</th><th>Début</th><th>Fin</th><th>Salaire</th><th>Statut</th></tr>
              </thead>
              <tbody>
                <tr v-for="contrat in dossier.contrats" :key="contrat.id">
                  <td>{{ contrat.reference }}</td>
                  <td>{{ contrat.typeContrat }}</td>
                  <td>{{ date(contrat.dateDebut) }}</td>
                  <td>{{ date(contrat.dateFin) }}</td>
                  <td class="b3-money">{{ ar(contrat.salaire) }}</td>
                  <td><StatutBadge :statut="contrat.statutContrat" /></td>
                </tr>
                <tr v-if="!dossier.contrats.length"><td colspan="6" class="b3-empty">Aucun contrat.</td></tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="b3-card">
          <h2>Affectations</h2>
          <div class="b3-table-wrapper">
            <table class="b3-table">
              <thead>
                <tr><th>Poste</th><th>Département</th><th>Type</th><th>Début</th><th>Fin</th><th>Motif</th><th>Statut</th></tr>
              </thead>
              <tbody>
                <tr v-for="affectation in dossier.affectations" :key="affectation.id">
                  <td>{{ affectation.posteTitre ?? '—' }}</td>
                  <td>{{ affectation.departementNom ?? '—' }}</td>
                  <td>{{ affectation.type }}</td>
                  <td>{{ date(affectation.dateDebut) }}</td>
                  <td>{{ date(affectation.dateFin) }}</td>
                  <td>{{ affectation.motif ?? '—' }}</td>
                  <td><StatutBadge :statut="affectation.statut" /></td>
                </tr>
                <tr v-if="!dossier.affectations.length"><td colspan="7" class="b3-empty">Aucune affectation.</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Carrière -->
      <div v-else-if="onglet === 'carriere'" class="b3-card">
        <h2>Historique de carrière</h2>
        <ul class="b3-timeline">
          <li v-for="(evenement, index) in dossier.carriere" :key="index" :class="`evt-${evenement.type}`">
            <strong>{{ date(evenement.date) }} — {{ evenement.titre }}</strong>
            <span v-if="evenement.description">{{ evenement.description }}</span>
          </li>
        </ul>
      </div>

      <!-- Documents -->
      <div v-else-if="onglet === 'documents'" class="b3-grid">
        <div class="b3-card">
          <h2>Pièces du dossier</h2>
          <div class="b3-table-wrapper">
            <table class="b3-table">
              <thead>
                <tr><th>Type</th><th>Libellé</th><th>N°</th><th>Émis le</th><th>Expire le</th><th>Validité</th><th>Déposé</th><th></th></tr>
              </thead>
              <tbody>
                <tr v-for="document in dossier.documents" :key="document.id">
                  <td>{{ document.typeLibelle }}</td>
                  <td>{{ document.libelle }}</td>
                  <td>{{ document.numero ?? '—' }}</td>
                  <td>{{ date(document.dateEmission) }}</td>
                  <td>{{ date(document.dateExpiration) }} <span class="b3-muted">{{ joursRestants(document) }}</span></td>
                  <td><StatutBadge :statut="document.etatValidite" /></td>
                  <td>{{ dateHeure(document.dateDepot) }}<div class="b3-muted">{{ document.deposePar }}</div></td>
                  <td class="b3-actions">
                    <button v-if="document.fichierDisponible" class="b3-btn" @click="telecharger(document)">Télécharger</button>
                    <button class="b3-btn is-ghost" @click="supprimer(document)">Supprimer</button>
                  </td>
                </tr>
                <tr v-if="!dossier.documents.length"><td colspan="8" class="b3-empty">Aucun document déposé.</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="b3-card">
          <h2>Ajouter un document</h2>
          <form @submit.prevent="deposerDocument">
            <div class="b3-form-grid">
              <div class="b3-field">
                <label for="typeDocument">Type *</label>
                <select id="typeDocument" v-model="formDocument.typeDocument" required>
                  <option v-for="(libelle, cle) in TYPES_DOCUMENT" :key="cle" :value="cle">{{ libelle }}</option>
                </select>
              </div>
              <div class="b3-field"><label for="libelle">Libellé</label><input id="libelle" v-model="formDocument.libelle" placeholder="Par défaut : le type" /></div>
              <div class="b3-field"><label for="numero">Numéro</label><input id="numero" v-model="formDocument.numero" /></div>
              <div class="b3-field"><label for="emission">Date d'émission</label><input id="emission" v-model="formDocument.dateEmission" type="date" /></div>
              <div class="b3-field"><label for="expiration">Date d'expiration</label><input id="expiration" v-model="formDocument.dateExpiration" type="date" /></div>
              <div class="b3-field">
                <label for="fichier">Fichier (PDF, PNG, JPG — 10 Mo max)</label>
                <input id="fichier" ref="champFichier" type="file" accept=".pdf,.png,.jpg,.jpeg" @change="choisirFichier" />
              </div>
            </div>
            <div class="b3-actions actions-formulaire">
              <button type="submit" class="b3-btn is-primary" :disabled="depot">Ajouter au dossier</button>
            </div>
          </form>
        </div>
      </div>

      <!-- Journal d'audit -->
      <div v-else-if="onglet === 'journal'" class="b3-card">
        <div class="entete-carte">
          <h2>Journal d'audit</h2>
          <button class="b3-btn" @click="chargerJournal">Actualiser</button>
        </div>
        <p class="b3-muted">Chaque consultation et modification du dossier est enregistrée ici et ne peut pas être effacée.</p>
        <div class="b3-table-wrapper">
          <table class="b3-table">
            <thead>
              <tr><th>Date</th><th>Action</th><th>Auteur</th><th>Détails</th></tr>
            </thead>
            <tbody>
              <tr v-for="ligne in journal" :key="ligne.id">
                <td>{{ dateHeure(ligne.dateAction) }}</td>
                <td>{{ ligne.actionLibelle }}</td>
                <td>{{ ligne.auteur }}</td>
                <td class="details">{{ ligne.details ?? '—' }}</td>
              </tr>
              <tr v-if="!journal.length"><td colspan="4" class="b3-empty">Aucune trace.</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.retour {
  font-size: 0.8rem;
  text-decoration: none;
}

.alertes {
  border-left: 4px solid var(--b3-warning);
}

.alertes ul {
  margin: 0;
  padding-left: 1.2rem;
  font-size: 0.86rem;
}

.onglets {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid var(--b3-border);
}

.onglets button {
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 0.55rem 0.9rem;
  font-size: 0.88rem;
  color: var(--b3-text-soft);
  cursor: pointer;
}

.onglets button.actif {
  color: var(--b3-primary);
  border-bottom-color: var(--b3-primary);
  font-weight: 600;
}

.compteur {
  margin-left: 0.3rem;
  font-size: 0.72rem;
  background: var(--b3-primary-soft);
  color: var(--b3-primary);
  border-radius: 999px;
  padding: 0 0.45rem;
}

.entete-carte {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.actions-formulaire {
  justify-content: flex-end;
  margin-top: 1rem;
}

.details {
  white-space: pre-line;
  font-size: 0.8rem;
}

.evt-EVOLUTION_SALAIRE strong,
.evt-CHANGEMENT_CONTRAT strong {
  color: var(--b3-success);
}

.evt-FIN_CONTRAT strong,
.evt-FIN_AFFECTATION strong {
  color: var(--b3-text-soft);
}
</style>
