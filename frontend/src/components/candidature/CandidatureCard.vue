<script setup>
import { ref, computed } from 'vue'
import {
  candidaturesApi, entretiensApi, messageErreur,
  STATUT, STATUT_ENTRETIEN, STATUT_TEST,
} from '@/services/api'

const props = defineProps({ candidature: { type: Object, required: true } })
const emit = defineEmits(['maj'])

const c = computed(() => props.candidature)
const enCours = ref(false)
const erreur = ref('')

// Champs de saisie locaux
const note = ref(c.value.test?.note ?? '')
const dateEntretien = ref('')
const lieu = ref('')
const commentaire = ref('')

const entretienPlanifie = computed(
  () => c.value.entretien?.statutId === STATUT_ENTRETIEN.PLANIFIE ? c.value.entretien : null,
)

const motifRefus = computed(() => {
  if (c.value.statutId !== STATUT.REFUSE) return ''
  if (c.value.entretien?.statutId === STATUT_ENTRETIEN.TERMINE) return "Refusé après l'entretien"
  if (c.value.test?.statutId === STATUT_TEST.ECHOUE) return 'Refusé au test'
  return 'Non retenu à la présélection'
})

const fmtDate = (d) => new Date(d).toLocaleDateString('fr-FR')
const fmtDateHeure = (d) =>
  new Date(d).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' })

/** Exécute un appel API, puis remonte la candidature mise à jour au tableau. */
async function agir(appel, confirmation) {
  if (confirmation && !window.confirm(confirmation)) return
  erreur.value = ''
  enCours.value = true
  try {
    emit('maj', await appel())
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    enCours.value = false
  }
}

const nom = computed(() => `${c.value.prenom} ${c.value.nom}`)

// 1. Présélection
const retenir = () => agir(() => candidaturesApi.preselection(c.value.id, true))
const rejeter = () =>
  agir(() => candidaturesApi.preselection(c.value.id, false), `Rejeter la candidature de ${nom.value} ?`)

// 2. Test
const enregistrerNote = () => {
  if (note.value === '' || note.value === null) {
    erreur.value = 'Saisissez une note'
    return
  }
  agir(() => candidaturesApi.saisirNote(c.value.id, Number(note.value)))
}
const noteModifiee = computed(() => String(note.value) !== String(c.value.test?.note ?? ''))
const validerTest = () => agir(() => candidaturesApi.deciderTest(c.value.id, true))
const refuserTest = () =>
  agir(() => candidaturesApi.deciderTest(c.value.id, false), `Refuser ${nom.value} au test ?`)

// 3. Entretien
const planifier = () => {
  if (!dateEntretien.value) {
    erreur.value = "Choisissez la date et l'heure de l'entretien"
    return
  }
  agir(() => candidaturesApi.planifierEntretien(c.value.id, dateEntretien.value, lieu.value || null))
}
const accepter = () =>
  agir(
    () => entretiensApi.resultat(entretienPlanifie.value.id, true, commentaire.value || null),
    `Accepter ${nom.value} ? Le dossier sera transmis à Sarobidy.`,
  )
const refuserEntretien = () =>
  agir(
    () => entretiensApi.resultat(entretienPlanifie.value.id, false, commentaire.value || null),
    `Refuser ${nom.value} après l'entretien ?`,
  )
const annulerEntretien = () =>
  agir(() => entretiensApi.annuler(entretienPlanifie.value.id), "Annuler l'entretien ?")
</script>

<template>
  <article class="card" :class="{ busy: enCours }">
    <header>
      <strong>{{ nom }}</strong>
      <a v-if="c.fichierCvUrl" :href="c.fichierCvUrl" target="_blank" rel="noopener" class="cv">CV</a>
    </header>
    <div class="meta muted">
      <div>{{ c.email }}</div>
      <div>Candidature du {{ fmtDate(c.dateCandidature) }}</div>
    </div>

    <!-- 1. À TRAITER : présélection -->
    <div v-if="c.statutId === STATUT.A_TRAITER" class="actions">
      <button class="success" :disabled="enCours" @click="retenir">Retenir</button>
      <button class="danger" :disabled="enCours" @click="rejeter">Rejeter</button>
    </div>

    <!-- 2. TEST : saisie de la note puis décision du recruteur -->
    <div v-else-if="c.statutId === STATUT.TEST" class="bloc">
      <label class="label">Note du test (/20)</label>
      <div class="row">
        <input v-model="note" type="number" min="0" max="20" step="0.25" @keyup.enter="enregistrerNote" />
        <button :disabled="enCours || !noteModifiee" @click="enregistrerNote">Enregistrer</button>
      </div>
      <template v-if="c.test?.note != null">
        <div class="note">Note : <strong>{{ c.test.note }}/20</strong></div>
        <div class="actions">
          <button class="success" :disabled="enCours || noteModifiee" @click="validerTest">Valider</button>
          <button class="danger" :disabled="enCours || noteModifiee" @click="refuserTest">Refuser</button>
        </div>
      </template>
      <p v-else class="muted small">Décision possible une fois la note enregistrée.</p>
    </div>

    <!-- 3. ATTENTE ENTRETIEN -->
    <div v-else-if="c.statutId === STATUT.ATTENTE_ENTRETIEN" class="bloc">
      <div class="note muted">Test : {{ c.test?.note }}/20</div>

      <template v-if="entretienPlanifie">
        <div class="rdv">
          📅 {{ fmtDateHeure(entretienPlanifie.date) }}
          <span v-if="entretienPlanifie.lieu"> · {{ entretienPlanifie.lieu }}</span>
        </div>
        <label class="label">Compte-rendu</label>
        <textarea v-model="commentaire" placeholder="Impressions, points forts, réserves…" />
        <div class="actions">
          <button class="success" :disabled="enCours" @click="accepter">Accepter</button>
          <button class="danger" :disabled="enCours" @click="refuserEntretien">Refuser</button>
          <button :disabled="enCours" @click="annulerEntretien">Annuler</button>
        </div>
      </template>

      <template v-else>
        <p v-if="c.entretien?.statutId === STATUT_ENTRETIEN.ANNULE" class="muted small">
          Entretien précédent annulé.
        </p>
        <label class="label">Planifier l'entretien</label>
        <input v-model="dateEntretien" type="datetime-local" />
        <input v-model="lieu" type="text" placeholder="Lieu (optionnel)" />
        <div class="actions">
          <button class="primary" :disabled="enCours" @click="planifier">Planifier</button>
        </div>
      </template>
    </div>

    <!-- 4. ACCEPTÉ -->
    <div v-else-if="c.statutId === STATUT.ACCEPTE" class="bloc">
      <div class="tag ok">Transmis à Sarobidy</div>
      <div class="note muted">Test : {{ c.test?.note }}/20</div>
      <p v-if="c.entretien?.commentaire" class="cr">« {{ c.entretien.commentaire }} »</p>
    </div>

    <!-- 5. REFUSÉ -->
    <div v-else-if="c.statutId === STATUT.REFUSE" class="bloc">
      <div class="tag ko">{{ motifRefus }}</div>
      <div v-if="c.test?.note != null" class="note muted">Test : {{ c.test.note }}/20</div>
      <p v-if="c.entretien?.commentaire" class="cr">« {{ c.entretien.commentaire }} »</p>
    </div>

    <p v-if="erreur" class="error">{{ erreur }}</p>
  </article>
</template>

<style scoped>
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.card.busy { opacity: 0.6; }
header { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.cv { font-size: 12px; color: var(--primary); }
.meta { font-size: 12px; word-break: break-all; }
.bloc { display: flex; flex-direction: column; gap: 6px; }
.row { display: flex; gap: 6px; }
.row input { flex: 1; min-width: 0; }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.label { font-size: 12px; font-weight: 600; }
.note { font-size: 13px; }
.small { font-size: 12px; margin: 0; }
.rdv { font-size: 13px; background: var(--warning-bg); padding: 6px 8px; border-radius: 6px; }
.tag { font-size: 12px; font-weight: 600; padding: 3px 8px; border-radius: 10px; align-self: flex-start; }
.tag.ok { background: var(--success-bg); color: var(--success); }
.tag.ko { background: var(--danger-bg); color: var(--danger); }
.cr { font-size: 12px; font-style: italic; margin: 0; color: var(--muted); }
</style>
