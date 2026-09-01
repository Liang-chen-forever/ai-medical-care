import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../api/index.js'

const routes = [
  { path: '/', redirect: '/chat' },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/ChatView.vue'),
    meta: { title: 'AI 智能问诊' }
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
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
})

export default router
