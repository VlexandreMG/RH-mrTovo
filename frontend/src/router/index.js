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
        // Bloc 1 : expression du besoin en personnel
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

        // Bloc 2 : suivi des candidatures (présélection → test → entretien)
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
})

export default router
