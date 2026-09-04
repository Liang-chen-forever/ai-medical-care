import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, getUser } from '../api/index.js'
import { routeAccess } from '../utils/routeAccess.js'

const routes = [
  { path: '/', redirect: '/chat' },
  {
    path: '/triage', name: 'Triage', component: () => import('../views/TriageView.vue'),
    meta: { title: '智能分诊', requiresAuth: true, role: 'PATIENT' }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/ChatView.vue'),
    meta: { title: 'AI 智能问诊' }
  },
  {
    path: '/doctor/appointments',
    name: 'DoctorAppointments',
    component: () => import('../views/DoctorAppointments.vue'),
    meta: { title: '医生工作台', requiresAuth: true, role: 'DOCTOR' }
  },
  {
    path: '/department',
    name: 'Department',
    component: () => import('../views/DepartmentView.vue'),
    meta: { title: '科室医生' }
  },
  {
    path: '/appointment',
    name: 'Appointment',
    component: () => import('../views/AppointmentView.vue'),
    meta: { title: '预约挂号', requiresAuth: true }
  },
  {
    path: '/my-appointments',
    name: 'MyAppointments',
    component: () => import('../views/MyAppointments.vue'),
    meta: { title: '我的预约', requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { title: '注册' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 硅谷小智` : '硅谷小智'
  const decision = routeAccess(to.meta, isAuthenticated(), getUser()?.role)
  if (decision) return decision.name === 'Login' ? { ...decision, query: { redirect: to.fullPath } } : decision
})

export default router
