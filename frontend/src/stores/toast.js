import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const items = ref([])
  let seq = 0

  function push(message, type = 'success') {
    const id = ++seq
    items.value.push({ id, message, type })
    setTimeout(() => remove(id), 4200)
  }
  function remove(id) {
    items.value = items.value.filter((t) => t.id !== id)
  }

  return {
    items,
    remove,
    success: (m) => push(m, 'success'),
    error: (m) => push(m, 'error')
  }
})
