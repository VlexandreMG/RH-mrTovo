import { createRouter, createWebHistory } from 'vue-router'
import OffreList from '@/components/offre/OffreList.vue'
import KanbanBoard from '@/components/kanban/KanbanBoard.vue'

const routes = [
  { path: '/', redirect: '/offres' },
  { path: '/offres', name: 'offres', component: OffreList },
  { path: '/offres/:id/kanban', name: 'kanban', component: KanbanBoard, props: true },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
