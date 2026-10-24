<script setup>
import { onMounted, ref } from 'vue'
import { useBloc3Store } from '@/stores/bloc3'
import StatutBadge from '@/components/bloc3/StatutBadge.vue'

const store = useBloc3Store()
const employeOuvert = ref(null)

onMounted(() => store.chargerEmployes())

const formatDate = (valeur) => (valeur ? new Date(valeur).toLocaleDateString('fr-FR') : '—')

function basculer(employe) {
  employeOuvert.value = employeOuvert.value === employe.id ? null : employe.id
}
</script>

<template>
  <section>
    <header class="b3-page-header">
      <div>
        <h1>Dossiers salariés</h1>
        <p>
          Créés automatiquement à l'acceptation d'une proposition : matricule, contrat et affectation
          initiale.
        </p>
      </div>
      <button class="b3-btn" :disabled="store.loading" @click="store.chargerEmployes()">Actualiser</button>
    </header>

    <div v-if="store.error" class="b3-alert is-error">{{ store.error }}</div>

    <div class="b3-card">
      <h2>Effectif embauché (Bloc 3)</h2>
      <div class="b3-table-wrapper">
        <table class="b3-table">
          <thead>
            <tr>
              <th>Matricule</th>
              <th>Nom</th>
              <th>Poste</th>
              <th>Département</th>
              <th>Embauche</th>
              <th>Fin de période d’essai</th>
              <th>Statut</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="employe in store.employes" :key="employe.id">
              <td><strong>{{ employe.matricule }}</strong></td>
              <td>
                {{ employe.prenom }} {{ employe.nom }}
                <div class="b3-muted" style="font-size: 0.78rem">{{ employe.email }}</div>
              </td>
              <td>{{ employe.posteTitre ?? '—' }}</td>
              <td>{{ employe.departementNom ?? '—' }}</td>
              <td>{{ formatDate(employe.dateEmbauche) }}</td>
              <td>{{ formatDate(employe.dateFinPeriodeEssai) }}</td>
              <td><StatutBadge :statut="employe.statut" /></td>
              <td>
                <button class="b3-btn" @click="basculer(employe)">
                  {{ employeOuvert === employe.id ? 'Masquer' : 'Détail' }}
                </button>
              </td>
            </tr>
            <tr v-if="!store.employes.length">
              <td colspan="8" class="b3-empty">
                Aucun dossier salarié : acceptez une proposition pour le générer automatiquement.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-for="employe in store.employes.filter((item) => item.id === employeOuvert)" :key="employe.id" class="b3-card">
      <h2>{{ employe.matricule }} — {{ employe.prenom }} {{ employe.nom }}</h2>
      <div class="b3-grid b3-grid-2">
        <div>
          <h3>Contrats</h3>
          <div class="b3-table-wrapper">
            <table class="b3-table">
              <thead>
                <tr>
                  <th>Référence</th>
                  <th>Type</th>
                  <th>Début</th>
                  <th>Fin</th>
                  <th>Salaire</th>
                  <th>Statut</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="contrat in employe.contrats" :key="contrat.id">
                  <td>{{ contrat.reference }}</td>
                  <td>{{ contrat.typeContrat }}</td>
                  <td>{{ formatDate(contrat.dateDebut) }}</td>
                  <td>{{ formatDate(contrat.dateFin) }}</td>
                  <td class="b3-money">{{ contrat.salaire }}</td>
                  <td><StatutBadge :statut="contrat.statutContrat" /></td>
                </tr>
                <tr v-if="!employe.contrats.length">
                  <td colspan="6" class="b3-empty">Aucun contrat.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div>
          <h3>Affectations</h3>
          <div class="b3-table-wrapper">
            <table class="b3-table">
              <thead>
                <tr>
                  <th>Poste</th>
                  <th>Département</th>
                  <th>Type</th>
                  <th>Début</th>
                  <th>Fin</th>
                  <th>Statut</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="affectation in employe.affectations" :key="affectation.id">
                  <td>{{ affectation.posteTitre ?? '—' }}</td>
                  <td>{{ affectation.departementNom ?? '—' }}</td>
                  <td>{{ affectation.type }}</td>
                  <td>{{ formatDate(affectation.dateDebut) }}</td>
                  <td>{{ formatDate(affectation.dateFin) }}</td>
                  <td><StatutBadge :statut="affectation.statut" /></td>
                </tr>
                <tr v-if="!employe.affectations.length">
                  <td colspan="6" class="b3-empty">Aucune affectation.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
