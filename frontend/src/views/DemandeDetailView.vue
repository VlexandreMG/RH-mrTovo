<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDemandeStore } from '@/stores/demandes'
import { useToastStore } from '@/stores/toast'
import { TYPES_BESOIN, ROLES, ar, date, dateHeure } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import WorkflowStepper from '@/components/WorkflowStepper.vue'

const props = defineProps({ id: { type: String, required: true } })

const auth = useAuthStore()
const store = useDemandeStore()
const toast = useToastStore()
const router = useRouter()

const d = ref(null)
const erreur = ref('')
const commentaire = ref('')
const envoi = ref(false)

async function charger() {
  try {
    d.value = await store.detail(props.id)
  } catch (e) {
    erreur.value = e.message
  }
}
onMounted(charger)

// ----- Actions disponibles selon rôle et statut
const enAttente = computed(() =>
  ['EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION'].includes(d.value?.statut)
)
const peutDonnerAvis = computed(() => enAttente.value && auth.role === 'DIRECTION')
const consultationSeule = computed(() =>
  enAttente.value && ['RH', 'FINANCE'].includes(auth.role)
)
const peutOuvrir = computed(() => d.value?.statut === 'AUTORISEE' && auth.role === 'RH')

// ----- Synthèse budget
const ecart = computed(() =>
  d.value?.coutEstimeTotal == null ? null : d.value.budgetDemandeTotal - d.value.coutEstimeTotal
)
const ratio = computed(() => {
  if (!d.value || d.value.coutEstimeTotal == null || !d.value.budgetDemandeTotal) return 0
  return Math.min(100, Math.round((d.value.coutEstimeTotal / d.value.budgetDemandeTotal) * 100))
})

const libellesAction = { oui: 'Accepter la demande', non: 'Refuser la demande' }

async function donnerAvis(decision) {
  if (decision === 'DEFAVORABLE' && !commentaire.value.trim()) {
    return toast.error('Un commentaire est obligatoire pour une décision défavorable.')
  }
  envoi.value = true
  try {
    d.value = await store.valider(d.value.idDemande, { decision, commentaire: commentaire.value })
    commentaire.value = ''
    toast.success('Votre décision a été enregistrée.')
  } catch (e) {
    toast.error(e.message)
  } finally {
    envoi.value = false
  }
}

async function ouvrir() {
  envoi.value = true
  try {
    d.value = await store.ouvrir(d.value.idDemande)
    toast.success('Le recrutement est ouvert.')
  } catch (e) {
    toast.error(e.message)
  } finally {
    envoi.value = false
  }
}

const etapeLabel = { RH: 'Avis RH', FINANCE: 'Avis Finance', DIRECTION: 'Décision Direction' }
</script>

<template>
  <div v-if="erreur" class="card empty">
    <AppIcon name="alert" :size="40" />
    <p>{{ erreur }}</p>
    <button class="btn btn-ghost" style="margin-top:1rem" @click="router.push('/demandes')">Retour aux demandes</button>
  </div>
  <div v-else-if="!d" class="empty">Chargement…</div>

  <template v-else>
    <button class="btn btn-ghost btn-sm retour" @click="router.push('/demandes')"><AppIcon name="arrowLeft" :size="15" /> Toutes les demandes</button>
    <div class="page-head">
      <div>
        <h1 class="page-title">Demande n°{{ d.idDemande }} · {{ d.poste }}</h1>
        <p class="page-sub">{{ d.departement }} · par {{ d.manager }} · créée le {{ dateHeure(d.dateCreation) }}</p>
      </div>
      <StatusBadge :statut="d.statut" />
    </div>

    <div class="card card-pad stepper-card"><WorkflowStepper :demande="d" /></div>

    <div class="layout">
      <div class="col">
        <section class="card">
          <div class="card-head"><h2><AppIcon name="file" /> Détail du besoin</h2></div>
          <div class="card-pad infos">
            <div><span>Type de besoin</span><b><span class="badge plain" :class="d.typeBesoin === 'CREATION' ? 'tone-violet' : 'tone-info'">{{ TYPES_BESOIN[d.typeBesoin] }}</span></b></div>
            <div><span>Souhaité pour le</span><b>{{ date(d.dateSouhaitee) }}</b></div>
            <div v-if="d.employeARemplacer"><span>Employé à remplacer</span><b>{{ d.employeARemplacer }}</b></div>
            <div v-if="d.dateOuverture"><span>Recrutement ouvert le</span><b>{{ dateHeure(d.dateOuverture) }}</b></div>
            <div v-if="d.idOffre"><span>Offre d'emploi</span><b>
              <RouterLink v-if="auth.role === 'RH'" :to="{ name: 'kanban', params: { id: d.idOffre } }">Offre #{{ d.idOffre }}</RouterLink>
              <template v-else>Offre #{{ d.idOffre }}</template>
              · {{ d.statutOffre }}
            </b></div>
            <div class="full"><span>Motif</span><p class="motif">{{ d.motif }}</p></div>
          </div>
        </section>

        <section class="card">
          <div class="card-head">
            <h2><AppIcon name="coins" /> Effectifs et coûts</h2>
            <span class="badge plain tone-neutral">{{ d.effectifTotal }} personne{{ d.effectifTotal > 1 ? 's' : '' }}</span>
          </div>
          <div class="table-wrap">
            <table class="table compact">
              <thead><tr><th>Niveau</th><th class="num">Effectif</th><th class="num">Grille / mois</th><th class="num">Par pers.</th><th class="num">Total</th></tr></thead>
              <tbody>
                <tr v-for="l in d.lignes" :key="l.idLigne">
                  <td><strong>{{ l.niveau }}</strong></td>
                  <td class="num">{{ l.effectif }}</td>
                  <td class="num"><template v-if="l.salaireMin != null">{{ ar(l.salaireMin) }}<br /><span class="muted">à {{ ar(l.salaireMax) }}</span></template><template v-else>—</template></td>
                  <td class="num">{{ ar(l.coutEstimeParPersonne) }}</td>
                  <td class="num"><strong>{{ ar(l.coutEstimeTotal) }}</strong></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="budget card-pad">
            <div class="bl"><span>Budget demandé par le manager</span><b>{{ ar(d.budgetDemandeTotal) }}</b></div>
            <div class="bl"><span>Coût estimé</span><b>{{ d.coutEstimeTotal == null ? '—' : ar(d.coutEstimeTotal) }}</b></div>
            <template v-if="ecart !== null">
              <div class="bar"><i :style="{ width: ratio + '%' }" :class="{ over: ecart < 0 }" /></div>
              <p class="bilan" :class="ecart < 0 ? 'neg' : 'pos'">
                {{ ecart < 0 ? `Dépassement de ${ar(-ecart)} par rapport au budget demandé` : `Marge de ${ar(ecart)} sous le budget demandé` }}
              </p>
            </template>
            <p v-else class="hint">La Direction décide sur la base du budget demandé.</p>
          </div>
        </section>
      </div>

      <div class="col">
        <!-- Panneau d'action : seule la Direction décide -->
        <section v-if="peutDonnerAvis" class="card action">
          <div class="card-head"><h2><AppIcon name="shield" /> Décision Direction</h2></div>
          <div class="card-pad stack">
            <p class="note">Vous avez le dernier mot : votre décision porte sur la demande entière. Le RH et la Finance sont en lecture seule.</p>
            <div class="field">
              <label for="com">Commentaire <span class="muted">(obligatoire si refus)</span></label>
              <textarea id="com" v-model="commentaire" class="textarea" placeholder="Justifiez votre décision…" />
            </div>
            <div class="btns">
              <button class="btn btn-success" :disabled="envoi" @click="donnerAvis('FAVORABLE')"><AppIcon name="check" :size="16" /> {{ libellesAction.oui }}</button>
              <button class="btn btn-danger" :disabled="envoi" @click="donnerAvis('DEFAVORABLE')"><AppIcon name="x" :size="16" /> {{ libellesAction.non }}</button>
            </div>
          </div>
        </section>

        <section v-else-if="consultationSeule" class="card">
          <div class="card-head"><h2><AppIcon name="shield" /> Consultation</h2></div>
          <div class="card-pad">
            <p class="note">Lecture seule : la demande est en attente de la décision de la Direction.</p>
          </div>
        </section>

        <section v-else-if="peutOuvrir" class="card action">
          <div class="card-head"><h2><AppIcon name="rocket" /> Ouverture du recrutement</h2></div>
          <div class="card-pad stack">
            <p class="note">La Direction a autorisé cette demande. L’ouverture est enregistrée avec sa date et reste traçable.</p>
            <button class="btn btn-success btn-block" :disabled="envoi" @click="ouvrir"><AppIcon name="rocket" :size="16" /> Ouvrir le recrutement</button>
          </div>
        </section>

        <section class="card">
          <div class="card-head"><h2><AppIcon name="history" /> Historique des décisions</h2></div>
          <div v-if="!d.validations.length" class="empty small">Aucune décision pour l’instant.</div>
          <ol v-else class="timeline">
            <li v-for="v in d.validations" :key="v.idValidation" :class="v.decision === 'FAVORABLE' ? 'ok' : 'ko'">
              <span class="pt"><AppIcon :name="v.decision === 'FAVORABLE' ? 'check' : 'x'" :size="13" /></span>
              <div>
                <div class="t-top">
                  <strong>{{ etapeLabel[v.etape] }}</strong>
                  <span class="badge plain" :class="v.decision === 'FAVORABLE' ? 'tone-success' : 'tone-danger'">{{ v.decision === 'FAVORABLE' ? 'Favorable' : 'Défavorable' }}</span>
                </div>
                <small class="muted">{{ v.validateur }} · {{ ROLES[v.roleValidateur] }} · {{ dateHeure(v.dateValidation) }}</small>
                <p v-if="v.commentaire" class="quote">« {{ v.commentaire }} »</p>
              </div>
            </li>
          </ol>
        </section>
      </div>
    </div>
  </template>
</template>

<style scoped>
.retour { margin-bottom: .8rem; }
.stepper-card { margin-bottom: 1.2rem; }
.compact th { padding: .7rem .6rem; letter-spacing: .03em; }
.compact td { padding: .75rem .6rem; font-size: .88rem; }
.compact th:first-child, .compact td:first-child { padding-left: 1.2rem; }
.compact th:last-child, .compact td:last-child { padding-right: 1.2rem; }
.layout { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 1.2rem; align-items: start; }
.col { display: flex; flex-direction: column; gap: 1.2rem; min-width: 0; }
.infos { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem 1.4rem; }
.infos > div { display: flex; flex-direction: column; gap: .2rem; }
.infos span { color: var(--muted); font-size: .8rem; }
.infos .full { grid-column: 1 / -1; }
.motif { background: var(--surface-2); border-radius: 10px; padding: .8rem 1rem; white-space: pre-wrap; }
.budget { border-top: 1px solid var(--border); display: flex; flex-direction: column; gap: .6rem; }
.bl { display: flex; justify-content: space-between; align-items: baseline; gap: 1rem; }
.bl span { color: var(--muted); }
.bar { height: 9px; border-radius: 999px; background: #eceef7; overflow: hidden; margin-top: .3rem; }
.bar i { display: block; height: 100%; background: var(--success); border-radius: 999px; }
.bar i.over { background: var(--danger); }
.bilan { font-weight: 600; font-size: .86rem; }
.bilan.pos { color: var(--success); }
.bilan.neg { color: var(--danger); }
.action { border: 1.5px solid #cfd0fb; box-shadow: 0 10px 30px -12px rgba(91, 91, 240, .35); }
.stack { display: flex; flex-direction: column; gap: 1rem; }
.note { background: var(--primary-50); color: #3d3db8; padding: .75rem .9rem; border-radius: 10px; font-size: .86rem; }
.est { border: 1px solid var(--border); border-radius: 12px; padding: .9rem; background: var(--surface-2); display: flex; flex-direction: column; gap: .7rem; }
.est-top { display: flex; justify-content: space-between; }
.bad { background: var(--danger-50); color: #bb2d42; padding: .6rem .8rem; border-radius: 9px; font-size: .84rem; font-weight: 500; }
.check { display: flex; align-items: center; gap: .5rem; cursor: pointer; font-weight: 500; font-size: .86rem; }
.total { padding: .6rem .2rem; border-top: 1px dashed var(--border); }
.total b { font-size: 1.05rem; }
.btns { display: flex; gap: .6rem; flex-wrap: wrap; }
.btns .btn { flex: 1; }
.empty.small { padding: 1.6rem 1rem; }
.timeline { list-style: none; padding: 1.2rem 1.4rem; display: flex; flex-direction: column; gap: 1.2rem; }
.timeline li { display: flex; gap: .9rem; position: relative; }
.timeline li:not(:last-child)::before { content: ''; position: absolute; left: 11px; top: 26px; bottom: -1.2rem; width: 2px; background: var(--border); }
.pt { width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center; flex-shrink: 0; color: #fff; }
.ok .pt { background: var(--success); }
.ko .pt { background: var(--danger); }
.t-top { display: flex; align-items: center; gap: .6rem; flex-wrap: wrap; }
.quote { margin-top: .4rem; background: var(--surface-2); border-left: 3px solid #cfd2ea; padding: .5rem .8rem; border-radius: 0 8px 8px 0; font-style: italic; color: #4c5273; }
@media (max-width: 1100px) { .layout { grid-template-columns: 1fr; } }
</style>
