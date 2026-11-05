<script setup>
import { onMounted, reactive, ref } from 'vue'
import { congesApi } from '@/api/conges'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const types = ref([])
const erreur = ref('')

function typeVide() {
  return { code: '', libelle: '', decompteSolde: false, remunere: true, justificatifRequis: false, plafondAnnuelJours: null }
}

const nouveau = reactive(typeVide())

async function charger() {
  try {
    types.value = await congesApi.types()
  } catch (e) {
    erreur.value = e.message
  }
}

async function creer() {
  try {
    await congesApi.creerType({ ...nouveau, plafondAnnuelJours: nouveau.plafondAnnuelJours || null })
    Object.assign(nouveau, typeVide())
    toast.success('Type de congé créé.')
    await charger()
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(charger)
</script>

<template>
  <div v-if="erreur" class="b3-alert is-error">{{ erreur }}</div>

  <div class="b3-card">
    <div class="b3-table-wrapper">
      <table class="b3-table">
        <thead><tr><th>Type</th><th>Décompte le solde</th><th>Rémunéré</th><th>Justificatif</th><th>Plafond annuel</th></tr></thead>
        <tbody>
          <tr v-for="type in types" :key="type.id">
            <td><strong>{{ type.libelle }}</strong><div class="b3-muted">{{ type.code }}</div></td>
            <td>{{ type.decompteSolde ? 'Oui' : 'Non' }}</td>
            <td>{{ type.remunere ? 'Oui' : 'Non' }}</td>
            <td>{{ type.justificatifRequis ? 'Obligatoire' : 'Facultatif' }}</td>
            <td>{{ type.plafondAnnuelJours ? `${type.plafondAnnuelJours} j` : '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <div class="b3-card">
    <h2>Nouveau type</h2>
    <form @submit.prevent="creer">
      <div class="b3-form-grid">
        <div class="b3-field"><label for="code">Code *</label><input id="code" v-model="nouveau.code" maxlength="50" required /></div>
        <div class="b3-field"><label for="libelle">Libellé *</label><input id="libelle" v-model="nouveau.libelle" required /></div>
        <div class="b3-field"><label for="plafond">Plafond annuel (jours)</label><input id="plafond" v-model.number="nouveau.plafondAnnuelJours" type="number" min="1" /></div>
        <div class="options">
          <label><input v-model="nouveau.decompteSolde" type="checkbox" /> Décompte le solde</label>
          <label><input v-model="nouveau.remunere" type="checkbox" /> Rémunéré</label>
          <label><input v-model="nouveau.justificatifRequis" type="checkbox" /> Justificatif obligatoire</label>
        </div>
      </div>
      <div class="b3-actions actions"><button class="b3-btn is-primary">Créer le type</button></div>
    </form>
  </div>
</template>

<style scoped>
.options {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 0.25rem;
  font-size: 0.86rem;
}

.actions {
  justify-content: flex-end;
  margin-top: 0.8rem;
}
</style>
