<script setup>
import { useToastStore } from '@/stores/toast'
import AppIcon from './AppIcon.vue'

const toast = useToastStore()
</script>

<template>
  <div class="toasts">
    <TransitionGroup name="toast">
      <div v-for="t in toast.items" :key="t.id" class="toast" :class="t.type" @click="toast.remove(t.id)">
        <AppIcon :name="t.type === 'success' ? 'check' : 'alert'" :size="18" />
        <span>{{ t.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toasts { position: fixed; top: 1.2rem; right: 1.2rem; z-index: 100; display: flex; flex-direction: column; gap: .6rem; max-width: 380px; }
.toast { display: flex; align-items: flex-start; gap: .65rem; padding: .85rem 1.05rem; border-radius: 12px; background: #fff; border: 1px solid var(--border); box-shadow: 0 12px 32px -10px rgba(21, 26, 51, .3); cursor: pointer; font-weight: 500; }
.toast svg { flex-shrink: 0; margin-top: 2px; }
.toast.success { border-left: 4px solid var(--success); }
.toast.success svg { color: var(--success); }
.toast.error { border-left: 4px solid var(--danger); }
.toast.error svg { color: var(--danger); }
.toast-enter-active, .toast-leave-active { transition: all .25s ease; }
.toast-enter-from { opacity: 0; transform: translateX(30px); }
.toast-leave-to { opacity: 0; transform: translateX(30px); }
</style>
