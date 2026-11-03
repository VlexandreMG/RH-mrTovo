import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    {
      path: '/candidats',
      component: () => import('@/layouts/CandidateLayout.vue'),
      children: [
        {
          path: 'offres',
          name: 'offres-publiques',
          component: () => import('@/views/candidat/OffresPubliquesView.vue')
        },
        {
          path: 'offres/:id',
          name: 'postuler-offre',
          component: () => import('@/views/candidat/PostulerView.vue')
        },
        {
          path: 'inscription',
          name: 'inscription-candidat',
          component: () => import('@/views/candidat/InscriptionCandidatView.vue')
        },
        {
          path: 'espace',
          name: 'espace-candidat',
          component: () => import('@/views/candidat/EspaceCandidatView.vue'),
          meta: { requiresAuth: true, roles: ['CANDIDAT'] }
        }
      ]
    },
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
          component: () => import('@/views/RecrutementDashboardView.vue'),
          meta: { roles: ['RH'] }
        },
        {
          path: 'bloc3/candidatures',
          name: 'bloc3-candidatures',
          component: () => import('@/views/CandidaturesRecrutementView.vue'),
          meta: { roles: ['RH'] }
        },
        {
          path: 'bloc3/propositions',
          name: 'bloc3-propositions',
          component: () => import('@/views/PropositionsView.vue'),
          meta: { roles: ['RH'] }
        },
        {
          path: 'bloc3/propositions/:id',
          name: 'bloc3-proposition-detail',
          component: () => import('@/views/PropositionDetailView.vue'),
          meta: { roles: ['RH'] }
        },
        {
          path: 'bloc3/negotiations',
          name: 'bloc3-negotiations',
          component: () => import('@/views/NegotiationsView.vue'),
          meta: { roles: ['RH'] }
        },
        {
          path: 'bloc3/employes',
          name: 'bloc3-employes',
          component: () => import('@/views/EmployesView.vue'),
          meta: { roles: ['RH'] }
        },
        {
          path: 'bloc5/dossiers',
          name: 'bloc5-dossiers',
          component: () => import('@/views/EmployesView.vue'),
          meta: { roles: ['RH'] }
        },
        {
          path: 'bloc5/dossiers/:id',
          name: 'bloc5-dossier',
          component: () => import('@/views/DossierSalarieView.vue'),
          meta: { roles: ['RH'] }
        },
        {
          path: 'bloc3/contrats',
          name: 'bloc3-contrats',
          component: () => import('@/views/ContratsView.vue'),
          meta: { roles: ['RH'] }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (auth.role === 'CANDIDAT' && to.path !== '/candidats/espace'
      && !to.path.startsWith('/candidats/offres')) return '/candidats/espace'
  if (auth.role === 'CANDIDAT' && to.path === '/login') return '/candidats/espace'
  if (to.meta.requiresAuth && !auth.user) return '/login'
  if (to.path === '/login' && auth.user) return '/'
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) return '/'
  return true
})

export default router
