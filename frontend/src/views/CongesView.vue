<script setup>
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import DemandesConge from '@/components/conges/DemandesConge.vue'
import AbsencesAQualifier from '@/components/conges/AbsencesAQualifier.vue'
import SoldesConges from '@/components/conges/SoldesConges.vue'
import TypesConge from '@/components/conges/TypesConge.vue'

const auth = useAuthStore()
const estRh = computed(() => auth.role === 'RH')

const ONGLETS_RH = [
  { cle: 'demandes', libelle: 'Demandes' },
  { cle: 'absences', libelle: 'Absences' },
  { cle: 'soldes', libelle: 'Soldes et comptes' },
  { cle: 'types', libelle: 'Types de congé' }
]
const onglet = ref('demandes')
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Congés et absences</h1>
        <p v-if="estRh">Les demandes passent par le manager du département, puis par le RH qui décide.</p>
        <p v-else>Les demandes des salariés de votre département, à accepter ou refuser avant le passage au RH.</p>
      </div>
    </header>

    <nav v-if="estRh" class="onglets" role="tablist">
      <button
        v-for="o in ONGLETS_RH"
        :key="o.cle"
        role="tab"
        :aria-selected="onglet === o.cle"
        :class="{ actif: onglet === o.cle }"
        @click="onglet = o.cle"
      >
        {{ o.libelle }}
      </button>
    </nav>

    <DemandesConge v-if="onglet === 'demandes'" />
    <AbsencesAQualifier v-else-if="onglet === 'absences'" />
    <SoldesConges v-else-if="onglet === 'soldes'" />
    <TypesConge v-else />
  </section>
</template>

<style scoped>
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
</style>
