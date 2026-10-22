import { createRouter, createWebHistory } from 'vue-router';

// 1. Importation de tes composants
import KanbanBoard from '@/components/kanban/KanbanBoard.vue';

// 2. Définition des routes
const routes = [
  {
    path: '/',
    redirect: '/offres' // Redirige automatiquement l'accueil vers la liste des offres
  },
  {
    path: '/offres/:id/kanban', // Route dynamique avec l'ID de l'offre (:id)
    name: 'kanban',
    component: KanbanBoard
  }
];

// 3. Création et configuration du routeur
const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;