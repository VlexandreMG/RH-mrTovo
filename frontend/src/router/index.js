import { createRouter, createWebHistory } from 'vue-router'
<<<<<<< HEAD
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
        { path: 'grille-salariale', name: 'grilles', component: () => import('@/views/GrillesView.vue') }
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
=======
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
>>>>>>> bloc2-bis
})
