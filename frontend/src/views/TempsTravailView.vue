<script setup>
import { ref } from 'vue'
import PointagesSemaine from '@/components/temps/PointagesSemaine.vue'
import HeuresSupAValider from '@/components/temps/HeuresSupAValider.vue'
import AnomaliesPointage from '@/components/temps/AnomaliesPointage.vue'
import VariablesPaie from '@/components/temps/VariablesPaie.vue'
import RegimesTravail from '@/components/temps/RegimesTravail.vue'

const ONGLETS = [
  { cle: 'pointages', libelle: 'Pointages' },
  { cle: 'heures-sup', libelle: 'Heures sup à valider' },
  { cle: 'anomalies', libelle: 'Anomalies' },
  { cle: 'paie', libelle: 'Variables de paie' },
  { cle: 'regimes', libelle: 'Régimes et planning' }
]

const onglet = ref('pointages')
// Salarié et date à afficher quand on arrive depuis une anomalie ou une semaine à valider.
const cible = ref(null)

function ouvrirSemaine(destination) {
  cible.value = destination
  onglet.value = 'pointages'
}
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Temps de travail</h1>
        <p>
          Les salariés pointent eux-mêmes depuis leur espace ; ici, le RH contrôle, ajoute un oubli ou corrige,
          toujours avec un motif. Les retards de la semaine sont compensés par les heures sup validées ;
          le solde alimente la paie.
        </p>
      </div>
    </header>

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
      </button>
    </nav>

    <PointagesSemaine v-if="onglet === 'pointages'" :cible="cible" @voir-regimes="onglet = 'regimes'" />
    <HeuresSupAValider v-else-if="onglet === 'heures-sup'" @ouvrir="ouvrirSemaine" />
    <AnomaliesPointage v-else-if="onglet === 'anomalies'" @ouvrir="ouvrirSemaine" />
    <VariablesPaie v-else-if="onglet === 'paie'" />
    <RegimesTravail v-else />
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
