<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDemandeStore } from '@/stores/demandes'
import { useToastStore } from '@/stores/toast'
import { ar, nb } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'

const store = useDemandeStore()
const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const aujourdhui = new Date().toISOString().slice(0, 10)
const envoi = ref(false)

const form = ref({
  idPoste: '',
  typeBesoin: 'CREATION',
  idEmployeARemplacer: '',
  motif: '',
  budgetDemandeTotal: null,
  dateSouhaitee: ''
})
const lignes = ref([{ idNiveau: '', effectif: 1 }])

onMounted(async () => {
  try {
    await store.chargerReferentiels()
  } catch (e) {
    toast.error(e.message)
  }
})

// L'employé à remplacer doit occuper le poste choisi
const employesDuPoste = computed(() => store.employes.filter((e) => e.idPoste === form.value.idPoste))
watch(() => form.value.idPoste, () => (form.value.idEmployeARemplacer = ''))
watch(() => form.value.typeBesoin, (t) => { if (t === 'CREATION') form.value.idEmployeARemplacer = '' })

const niveauxDispos = (ligne) =>
  store.niveaux.filter((n) => n.id === ligne.idNiveau || !lignes.value.some((l) => l.idNiveau === n.id))

const peutAjouter = computed(() => lignes.value.length < store.niveaux.length)
const ajouterLigne = () => peutAjouter.value && lignes.value.push({ idNiveau: '', effectif: 1 })
const retirerLigne = (i) => lignes.value.length > 1 && lignes.value.splice(i, 1)

const effectifTotal = computed(() => lignes.value.reduce((s, l) => s + (Number(l.effectif) || 0), 0))

// Fourchette indicative issue de la grille salariale (aide le manager à fixer son enveloppe)
const indicatif = computed(() => {
  let min = 0, max = 0, complet = true, any = false
  for (const l of lignes.value) {
    if (!l.idNiveau || !form.value.idPoste) { complet = false; continue }
    const g = store.grilleApplicable(form.value.idPoste, l.idNiveau)
    if (!g) { complet = false; continue }
    any = true
    min += g.salaireMin * (Number(l.effectif) || 0)
    max += g.salaireMax * (Number(l.effectif) || 0)
  }
  return any ? { min, max, complet } : null
})

const alerteBudget = computed(() => {
  const b = Number(form.value.budgetDemandeTotal)
  if (!indicatif.value || !b) return null
  if (b < indicatif.value.min) return { type: 'warn', texte: 'Votre budget est inférieur au minimum de la grille : la Finance et le RH risquent de le contester.' }
  if (b > indicatif.value.max) return { type: 'info', texte: 'Votre budget dépasse le maximum de la grille pour ces effectifs.' }
  return { type: 'ok', texte: 'Votre budget est cohérent avec la grille salariale.' }
})

async function soumettre() {
  const f = form.value
  const lignesValides = lignes.value.filter((l) => l.idNiveau)
  if (!lignesValides.length) return toast.error('Choisissez au moins un niveau de poste.')
  envoi.value = true
  try {
    const d = await store.creer({
      idPoste: f.idPoste,
      typeBesoin: f.typeBesoin,
      motif: f.motif,
      idEmployeARemplacer: f.idEmployeARemplacer || null,
      budgetDemandeTotal: f.budgetDemandeTotal,
      dateSouhaitee: f.dateSouhaitee,
      lignes: lignesValides.map((l) => ({ idNiveau: l.idNiveau, effectif: Number(l.effectif) }))
    })
    toast.success(`Demande n°${d.idDemande} envoyée au RH.`)
    router.push(`/demandes/${d.idDemande}`)
  } catch (e) {
    toast.error(e.message)
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <div class="page-head">
    <div>
      <button class="btn btn-ghost btn-sm retour" @click="router.push('/demandes')"><AppIcon name="arrowLeft" :size="15" /> Retour</button>
      <h1 class="page-title">Nouvelle demande de recrutement</h1>
      <p class="page-sub">Décrivez votre besoin : le RH le chiffrera ensuite à partir de la grille salariale.</p>
    </div>
  </div>

  <form class="layout" @submit.prevent="soumettre">
    <div class="main">
      <section class="card">
        <div class="card-head"><h2><AppIcon name="briefcase" /> Le besoin</h2></div>
        <div class="card-pad body">
          <div class="grid-2">
  <div class="field">
    <label for="poste">Poste</label>
    <select id="poste" v-model="form.idPoste" class="select" required>
      <option disabled value="">Sélectionner un poste</option>
      <option v-for="p in store.postes" :key="p.id" :value="p.id">{{ p.libelle }}</option>
    </select>
  </div>
  <div class="field">
    <label>Département demandeur</label>
    <div class="input" style="background: var(--surface-2); display:flex; align-items:center; gap:.5rem">
      <AppIcon name="building" :size="16" />
      <strong>{{ auth.user?.departement || 'Aucun département' }}</strong>
    </div>
    <span class="hint">Automatiquement celui de votre compte.</span>
  </div>
</div>

          <div class="field">
            <span class="label">Type de besoin</span>
            <div class="seg">
              <button type="button" :class="{ on: form.typeBesoin === 'CREATION' }" @click="form.typeBesoin = 'CREATION'">
                <AppIcon name="sparkles" :size="16" /> Création
                <small>Nouveau poste ou renfort</small>
              </button>
              <button type="button" :class="{ on: form.typeBesoin === 'REMPLACEMENT' }" @click="form.typeBesoin = 'REMPLACEMENT'">
                <AppIcon name="userMinus" :size="16" /> Remplacement
                <small>Le poste reste, la personne part</small>
              </button>
            </div>
          </div>

          <div v-if="form.typeBesoin === 'REMPLACEMENT'" class="field">
            <label for="emp">Employé à remplacer <span class="muted">(facultatif)</span></label>
            <select id="emp" v-model="form.idEmployeARemplacer" class="select" :disabled="!form.idPoste">
              <option value="">{{ form.idPoste ? 'Non précisé' : 'Choisissez d’abord un poste' }}</option>
              <option v-for="e in employesDuPoste" :key="e.idEmploye" :value="e.idEmploye">{{ e.nom }} · {{ e.matricule }}{{ e.niveau ? ' · ' + e.niveau : '' }}</option>
            </select>
            <span v-if="form.idPoste && !employesDuPoste.length" class="hint">Aucun employé actif n’occupe ce poste.</span>
            <span v-else class="hint">Le remplacement reprend la grille salariale déjà existante pour ce poste.</span>
          </div>

          <div class="field">
            <label for="motif">Motif</label>
            <textarea id="motif" v-model="form.motif" class="textarea" required
              placeholder="Pourquoi avez-vous besoin de ces personnes ? Ex. : développer une base de données sécurisée pour le projet X." />
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <h2><AppIcon name="users" /> Effectifs par niveau</h2>
          <button type="button" class="btn btn-ghost btn-sm" :disabled="!peutAjouter" @click="ajouterLigne"><AppIcon name="plus" :size="15" /> Ajouter un niveau</button>
        </div>
        <div class="card-pad body">
          <p class="hint">Précisez le profil recherché ; le RH associera chaque niveau à la grille salariale. Exemple : 2 juniors + 1 senior.</p>
          <div v-for="(l, i) in lignes" :key="i" class="ligne">
            <div class="field grow">
              <label>Niveau</label>
              <select v-model="l.idNiveau" class="select" required>
                <option disabled value="">Choisir un niveau</option>
                <option v-for="n in niveauxDispos(l)" :key="n.id" :value="n.id">{{ n.libelle }}</option>
              </select>
            </div>
            <div class="field qte">
              <label>Effectif</label>
              <input v-model.number="l.effectif" class="input" type="number" min="1" required />
            </div>
            <button type="button" class="btn btn-danger btn-sm del" :disabled="lignes.length === 1" title="Retirer" @click="retirerLigne(i)"><AppIcon name="trash" :size="16" /></button>
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head"><h2><AppIcon name="wallet" /> Budget et échéance</h2></div>
        <div class="card-pad body">
          <div class="grid-2">
            <div class="field">
              <label for="budget">Budget mensuel total demandé (Ar)</label>
              <input id="budget" v-model.number="form.budgetDemandeTotal" class="input" type="number" min="0" step="1000" required placeholder="Ex. 6 000 000" />
              <span class="hint">Enveloppe globale pour l’ensemble de l’effectif. Le RH chiffrera le coût réel.</span>
            </div>
            <div class="field">
              <label for="date">Date d’arrivée souhaitée</label>
              <input id="date" v-model="form.dateSouhaitee" class="input" type="date" :min="aujourdhui" required />
            </div>
          </div>
        </div>
      </section>
    </div>

    <aside class="side">
      <div class="card card-pad recap">
        <h3>Récapitulatif</h3>
        <dl>
          <div><dt>Effectif total</dt><dd>{{ nb(effectifTotal) }} personne{{ effectifTotal > 1 ? 's' : '' }}</dd></div>
          <div><dt>Budget demandé</dt><dd>{{ ar(form.budgetDemandeTotal) }}</dd></div>
          <div>
            <dt>Fourchette de la grille</dt>
            <dd v-if="indicatif">{{ ar(indicatif.min) }} – {{ ar(indicatif.max) }}<small v-if="!indicatif.complet" class="hint"> (partielle)</small></dd>
            <dd v-else class="muted">Choisissez poste et niveau</dd>
          </div>
        </dl>
        <p v-if="alerteBudget" class="alerte" :class="alerteBudget.type">{{ alerteBudget.texte }}</p>
        <button class="btn btn-block" :disabled="envoi"><AppIcon name="check" :size="16" /> {{ envoi ? 'Envoi…' : 'Soumettre la demande' }}</button>
        <p class="hint center">La demande sera transmise au RH pour chiffrage.</p>
      </div>
    </aside>
  </form>
</template>

<style scoped>
.retour { margin-bottom: .8rem; }
.layout { display: grid; grid-template-columns: 1fr 330px; gap: 1.4rem; align-items: start; }
.main { display: flex; flex-direction: column; gap: 1.2rem; min-width: 0; }
.body { display: flex; flex-direction: column; gap: 1.1rem; }
.seg { display: grid; grid-template-columns: 1fr 1fr; gap: .7rem; }
.seg button { display: flex; flex-direction: column; align-items: flex-start; gap: .15rem; padding: .85rem 1rem; border-radius: 12px; border: 1.5px solid var(--border); background: var(--surface); cursor: pointer; font-weight: 700; text-align: left; transition: all .15s; }
.seg button svg { color: var(--muted); margin-bottom: .2rem; }
.seg button small { font-weight: 400; color: var(--muted); }
.seg button:hover { border-color: #c5c8e6; }
.seg button.on { border-color: var(--primary); background: var(--primary-50); }
.seg button.on svg { color: var(--primary); }
.ligne { display: flex; align-items: flex-end; gap: .8rem; padding: .9rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface-2); }
.grow { flex: 1; }
.qte { width: 120px; }
.del { height: 42px; }
.side { position: sticky; top: 1.5rem; }
.recap { display: flex; flex-direction: column; gap: 1rem; }
.recap h3 { font-size: 1rem; }
dl { display: flex; flex-direction: column; gap: .8rem; }
dl > div { display: flex; flex-direction: column; gap: .1rem; padding-bottom: .8rem; border-bottom: 1px dashed var(--border); }
dt { color: var(--muted); font-size: .8rem; }
dd { font-weight: 700; }
.alerte { padding: .65rem .8rem; border-radius: 10px; font-size: .84rem; font-weight: 500; }
.alerte.ok { background: var(--success-50); color: #0b7f5f; }
.alerte.warn { background: var(--warning-50); color: #a96a05; }
.alerte.info { background: var(--info-50); color: #1d63b8; }
.center { text-align: center; }
@media (max-width: 1000px) { .layout { grid-template-columns: 1fr; } .side { position: static; } }
@media (max-width: 600px) { .seg { grid-template-columns: 1fr; } }
</style>
