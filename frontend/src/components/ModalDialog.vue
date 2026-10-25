<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})

const emit = defineEmits(['close'])
</script>

<template>
  <div class="b3-modal-backdrop" @click.self="emit('close')">
    <div class="b3-modal">
      <header class="b3-modal-header">
        <div>
          <h3>{{ title }}</h3>
          <p v-if="subtitle" class="b3-muted" style="margin: 0.25rem 0 0; font-size: 0.85rem">
            {{ subtitle }}
          </p>
        </div>
        <button type="button" class="b3-btn is-ghost" @click="emit('close')">Fermer</button>
      </header>
      <slot />
      <footer v-if="$slots.footer" class="b3-modal-footer">
        <slot name="footer" />
      </footer>
    </div>
  </div>
</template>
