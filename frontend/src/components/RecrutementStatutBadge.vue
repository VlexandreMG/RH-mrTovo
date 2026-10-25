<script setup>
import { computed } from 'vue'

const props = defineProps({
  statut: { type: String, required: true },
})

const styles = {
  BROUILLON: '',
  VALIDEE: 'is-info',
  EMISE: 'is-primary',
  EN_NEGOCIATION: 'is-warning',
  REVISE: 'is-warning',
  ACCEPTEE: 'is-success',
  REFUSEE: 'is-danger',
  EXPIREE: 'is-danger',
  ANNULEE: '',
  SUPERSEDEE: '',
  EN_ATTENTE: 'is-warning',
  EN_ANALYSE: 'is-info',
  CONTRE_PROPOSITION: 'is-warning',
  RETENU: 'is-primary',
  PROPOSITION_ENVOYEE: 'is-info',
  EMBAUCHE: 'is-success',
  ABANDONNE: '',
  A_SIGNER: 'is-warning',
  SIGNE: 'is-info',
  ACTIF: 'is-success',
  EN_PERIODE_DESSAI: 'is-warning',
  TERMINE: '',
  RUPTURE: 'is-danger',
  SORTI: '',
  SUSPENDU: 'is-warning',
}

const libelles = {
  EN_NEGOCIATION: 'En négociation',
  PROPOSITION_ENVOYEE: 'Proposition envoyée',
  A_SIGNER: 'À signer',
  EN_PERIODE_DESSAI: 'Période d’essai',
  CONTRE_PROPOSITION: 'Contre-proposition',
  SUPERSEDEE: 'Remplacée',
  EXPIREE: 'Expirée',
}

const classe = computed(() => styles[props.statut] ?? '')
const libelle = computed(() => libelles[props.statut] ?? props.statut)
</script>

<template>
  <span class="b3-badge" :class="classe">{{ libelle }}</span>
</template>
