<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { date } from '@/utils/format'

const props = defineProps({ demande: { type: Object, required: true } })

const ORDRE = ['EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION', 'AUTORISEE', 'RECRUTEMENT_OUVERT']

const etapes = computed(() => {
  const d = props.demande
  const avis = (etape) => d.validations.find((v) => v.etape === etape)
  const refusee = d.statut === 'REFUSEE'
  const idx = ORDRE.indexOf(d.statut)

  const rh = avis('RH'), fin = avis('FINANCE'), dir = avis('DIRECTION')
  const etat = (v, enCours) => {
    if (v) return v.decision === 'FAVORABLE' ? 'ok' : 'ko'
    return enCours ? 'current' : 'todo'
  }

  return [
    { label: 'Demande soumise', sub: date(d.dateCreation), state: 'ok' },
    { label: 'Avis RH', sub: rh ? (rh.decision === 'FAVORABLE' ? 'Favorable' : 'Défavorable') : 'À venir', state: etat(rh, d.statut === 'EN_ATTENTE_RH') },
    { label: 'Avis Finance', sub: fin ? (fin.decision === 'FAVORABLE' ? 'Favorable' : 'Défavorable') : 'À venir', state: etat(fin, d.statut === 'EN_ATTENTE_FINANCE') },
    {
      label: 'Décision Direction',
      sub: dir ? (dir.decision === 'FAVORABLE' ? 'Autorisée' : 'Refusée') : 'À venir',
      state: dir ? (dir.decision === 'FAVORABLE' ? 'ok' : 'ko') : d.statut === 'EN_ATTENTE_DIRECTION' ? 'current' : 'todo'
    },
    {
      label: 'Recrutement ouvert',
      sub: d.statut === 'RECRUTEMENT_OUVERT' ? 'Ouvert' : refusee ? '—' : idx === 3 ? 'En attente du RH' : 'À venir',
      state: d.statut === 'RECRUTEMENT_OUVERT' ? 'ok' : idx === 3 ? 'current' : 'todo'
    }
  ]
})
</script>

<template>
  <ol class="stepper">
    <li v-for="(e, i) in etapes" :key="i" :class="e.state">
      <span class="dot">
        <AppIcon v-if="e.state === 'ok'" name="check" :size="14" />
        <AppIcon v-else-if="e.state === 'ko'" name="x" :size="14" />
        <span v-else>{{ i + 1 }}</span>
      </span>
      <span class="txt"><strong>{{ e.label }}</strong><small>{{ e.sub }}</small></span>
    </li>
  </ol>
</template>

<style scoped>
.stepper { list-style: none; display: grid; grid-template-columns: repeat(5, 1fr); gap: .5rem; counter-reset: s; }
li { display: flex; align-items: center; gap: .7rem; position: relative; padding-right: .5rem; }
li:not(:last-child)::after { content: ''; position: absolute; left: 44px; right: 6px; top: 16px; height: 2px; background: var(--border); z-index: 0; }
li.ok:not(:last-child)::after { background: var(--success); }
.dot { width: 32px; height: 32px; border-radius: 50%; display: grid; place-items: center; font-weight: 700; font-size: .82rem; background: #eceef7; color: var(--muted); flex-shrink: 0; position: relative; z-index: 1; border: 2px solid var(--surface); }
.ok .dot { background: var(--success); color: #fff; }
.ko .dot { background: var(--danger); color: #fff; }
.current .dot { background: var(--primary); color: #fff; box-shadow: 0 0 0 5px rgba(91, 91, 240, .18); }
.txt { display: flex; flex-direction: column; line-height: 1.25; min-width: 0; background: var(--surface); position: relative; z-index: 1; padding-right: .3rem; }
.txt strong { font-size: .82rem; white-space: nowrap; }
.txt small { color: var(--muted); font-size: .76rem; }
.ko .txt small { color: var(--danger); }
.ok .txt small { color: var(--success); }
@media (max-width: 1100px) {
  .stepper { grid-template-columns: 1fr; gap: .9rem; }
  li::after { display: none; }
}
</style>
