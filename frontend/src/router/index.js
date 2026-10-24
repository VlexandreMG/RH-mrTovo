import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    {
      path: '/',
      component: () => import('@/components/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'demandes', name: 'demandes', component: () => import('@/views/DemandesView.vue') },
        {
          path: 'demandes/nouvelle',
          name: 'nouvelle',
          component: () => import('@/views/NouvelleDemandeView.vue'),
          meta: { roles: ['MANAGER'] }
        },
        { path: 'demandes/:id', name: 'detail', component: () => import('@/views/DemandeDetailView.vue'), props: true },
        { path: 'grille-salariale', name: 'grilles', component: () => import('@/views/GrillesView.vue') },
        {
          path: 'offres',
          name: 'offres',
          component: () => import('@/components/offre/OffreList.vue'),
          meta: { roles: ['RH'] }
        },
        {
          path: 'offres/:id/kanban',
          name: 'kanban',
          component: () => import('@/components/kanban/KanbanBoard.vue'),
          props: true,
          meta: { roles: ['RH'] }
        },
        {
          path: 'bloc3/dashboard',
          name: 'bloc3-dashboard',
          component: () => import('../views/bloc3/DashboardView.vue')
        },
        {
          path: 'bloc3/candidatures',
          name: 'bloc3-candidatures',
          component: () => import('../views/bloc3/CandidaturesView.vue')
        },
        {
          path: 'bloc3/propositions',
          name: 'bloc3-propositions',
          component: () => import('../views/bloc3/PropositionsView.vue')
        },
        {
          path: 'bloc3/propositions/:id',
          name: 'bloc3-proposition-detail',
          component: () => import('../views/bloc3/PropositionDetailView.vue')
        },
        {
          path: 'bloc3/negotiations',
          name: 'bloc3-negotiations',
          component: () => import('../views/bloc3/NegotiationsView.vue')
        },
        {
          path: 'bloc3/employes',
          name: 'bloc3-employes',
          component: () => import('../views/bloc3/EmployesView.vue')
        },
        {
          path: 'bloc3/contrats',
          name: 'bloc3-contrats',
          component: () => import('../views/bloc3/ContratsView.vue')
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.user) return '/login'
  if (to.path === '/login' && auth.user) return '/'
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) return '/'
  return true
})

export default router
