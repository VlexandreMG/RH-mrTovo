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
  // Bloc 5 : validite des documents
  VALIDE: 'is-success',
  EXPIRE_BIENTOT: 'is-warning',
  EXPIRE: 'is-danger',
  PERMANENT: 'is-info',
  // Bloc 6 : heures supplémentaires
  A_VALIDER: 'is-warning',
  SANS_HS: '',
  // Bloc 7 : demandes de congé et absences
  EN_ATTENTE_MANAGER: 'is-warning',
  EN_ATTENTE_RH: 'is-warning',
  ANNULEE: '',
  JUSTIFIEE: 'is-success',
  NON_JUSTIFIEE: 'is-danger',
  A_QUALIFIER: 'is-warning',
}

const libelles = {
  EN_NEGOCIATION: 'En négociation',
  PROPOSITION_ENVOYEE: 'Proposition envoyée',
  A_SIGNER: 'À signer',
  EN_PERIODE_DESSAI: 'Période d’essai',
  CONTRE_PROPOSITION: 'Contre-proposition',
  SUPERSEDEE: 'Remplacée',
  EXPIREE: 'Expirée',
  VALIDE: 'Valide',
  EXPIRE_BIENTOT: 'Expire bientôt',
  EXPIRE: 'Expiré',
  PERMANENT: 'Permanent',
  A_VALIDER: 'À valider',
  SANS_HS: 'Aucune',
  VALIDEE: 'Validée',
  REFUSEE: 'Refusée',
  EN_ATTENTE_MANAGER: 'Attente manager',
  EN_ATTENTE_RH: 'Attente RH',
  ANNULEE: 'Annulée',
  JUSTIFIEE: 'Justifiée',
  NON_JUSTIFIEE: 'Non justifiée',
  A_QUALIFIER: 'À qualifier',
}

const classe = computed(() => styles[props.statut] ?? '')
const libelle = computed(() => libelles[props.statut] ?? props.statut)
</script>

<template>
  <span class="b3-badge" :class="classe">{{ libelle }}</span>
</template>
