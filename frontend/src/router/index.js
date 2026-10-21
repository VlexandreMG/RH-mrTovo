import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/bloc3/dashboard',
    },
    {
      path: '/bloc3/dashboard',
      name: 'dashboard',
      component: () => import('../views/bloc3/DashboardView.vue'),
    },
    {
      path: '/bloc3/candidatures',
      name: 'candidatures',
      component: () => import('../views/bloc3/CandidaturesView.vue'),
    },
    {
      path: '/bloc3/propositions',
      name: 'propositions',
      component: () => import('../views/bloc3/PropositionsView.vue'),
    },
    {
      path: '/bloc3/propositions/:id',
      name: 'proposition-detail',
      component: () => import('../views/bloc3/PropositionDetailView.vue'),
    },
    {
      path: '/bloc3/negotiations',
      name: 'negotiations',
      component: () => import('../views/bloc3/NegotiationsView.vue'),
    },
    {
      path: '/bloc3/employes',
      name: 'employes',
      component: () => import('../views/bloc3/EmployesView.vue'),
    },
    {
      path: '/bloc3/contrats',
      name: 'contrats',
      component: () => import('../views/bloc3/ContratsView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/bloc3/dashboard',
    },
  ],
})

export default router
