<script setup>
import { computed } from 'vue'
import { STATUTS } from '@/utils/format'

const props = defineProps({ statut: { type: String, required: true } })
const info = computed(() => STATUTS[props.statut] || { label: props.statut, tone: 'neutral' })
</script>

<template>
  <span class="badge" :class="'tone-' + info.tone">{{ info.label }}</span>
</template>
