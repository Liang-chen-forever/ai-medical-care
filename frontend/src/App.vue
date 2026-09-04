<template>
  <div id="app-root">
    <!-- 离线提示 -->
    <div class="offline-banner">当前处于离线状态，部分功能可能不可用</div>

    <!-- PC 顶部导航栏 -->
    <nav class="navbar">
      <router-link to="/chat" class="navbar-brand">
        <span class="logo-icon">&#x2B;</span>
        硅谷小智
      </router-link>
      <div class="navbar-links">
        <router-link to="/chat">AI 问诊</router-link>
        <router-link to="/department">科室医生</router-link>
        <router-link to="/appointment">预约挂号</router-link>
        <router-link to="/my-appointments">我的预约</router-link>
        <router-link v-if="user?.role === 'DOCTOR'" to="/doctor/appointments">医生工作台</router-link>
      </div>
      <div class="navbar-user">
        <template v-if="user">
          <span class="user-name">{{ user.username }}</span>
          <button class="btn btn-outline btn-sm" @click="handleLogout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login" class="btn btn-outline btn-sm">登录</router-link>
          <router-link to="/register" class="btn btn-primary btn-sm">注册</router-link>
        </template>
      </div>
    </nav>

    <!-- 页面主体（带过渡动画） -->
    <router-view v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>

    <!-- 移动端底部 TabBar -->
    <nav class="tab-bar">
      <router-link to="/chat" class="tab-bar-item" active-class="active">
        <span class="tab-icon">&#x1F916;</span>
        <span>AI问诊</span>
      </router-link>
      <router-link to="/department" class="tab-bar-item" active-class="active">
        <span class="tab-icon">&#x1F3E5;</span>
        <span>科室医生</span>
      </router-link>
      <router-link to="/appointment" class="tab-bar-item" active-class="active">
        <span class="tab-icon">&#x1F4C5;</span>
        <span>预约挂号</span>
      </router-link>
      <router-link to="/my-appointments" class="tab-bar-item" active-class="active">
        <span class="tab-icon">&#x1F4CB;</span>
        <span>我的预约</span>
      </router-link>
      <router-link v-if="user?.role === 'DOCTOR'" to="/doctor/appointments" class="tab-bar-item" active-class="active">
        <span class="tab-icon">&#x2695;</span>
        <span>工作台</span>
      </router-link>
    </nav>

    <!-- 全局 Toast -->
    <Toast ref="globalToast" />

    <!-- 首次引导 -->
    <OnboardingGuide />
  </div>
</template>

<script setup>
import { ref, onMounted, provide, watch } from 'vue'
import { useRouter } from 'vue-router'
import Toast from './components/Toast.vue'
import OnboardingGuide from './components/OnboardingGuide.vue'
import { clearAuth, getUser } from './api/index.js'

const router = useRouter()
const user = ref(null)
const globalToast = ref(null)

// 提供全局 Toast 方法
function showToast(msg, type = 'success') {
  globalToast.value?.show(msg, type)
}

provide('toast', showToast)

onMounted(() => {
  user.value = getUser()
})

watch(() => router.currentRoute.value.fullPath, () => {
  user.value = getUser()
})

function handleLogout() {
  clearAuth()
  user.value = null
  showToast('已退出登录')
  router.push('/login')
}
</script>
