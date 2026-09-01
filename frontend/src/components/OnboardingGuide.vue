<template>
  <Teleport to="body">
    <div v-if="show" class="onboarding-overlay" @click.self="close">
      <div class="onboarding-card">
        <div class="step-icon">{{ steps[current].icon }}</div>
        <h3>{{ steps[current].title }}</h3>
        <p>{{ steps[current].desc }}</p>
        <div class="onboarding-dots">
          <span
            v-for="(_, i) in steps"
            :key="i"
            :class="['onboarding-dot', { active: i === current }]"
          />
        </div>
        <div style="display:flex;gap:12px;justify-content:center">
          <button v-if="current > 0" class="btn btn-outline" @click="prev">上一步</button>
          <button class="btn btn-primary" @click="next">
            {{ current === steps.length - 1 ? '开始使用' : '下一步' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'

const ONBOARDING_KEY = 'xiaozhi_onboarding_done'
const show = ref(!localStorage.getItem(ONBOARDING_KEY))
const current = ref(0)

const steps = [
  { icon: '&#x1F916;', title: 'AI 智能问诊', desc: '与硅谷小智对话，描述你的症状，AI 将为你推荐合适的科室和医生' },
  { icon: '&#x1F3E5;', title: '查看科室医生', desc: '浏览各科室的医生信息，了解医生专长和排班情况' },
  { icon: '&#x1F4C5;', title: '在线预约挂号', desc: '选择合适的医生和时间段，一键完成预约挂号' },
  { icon: '&#x1F4CB;', title: '管理我的预约', desc: '随时查看和取消预约记录，方便管理就诊安排' }
]

function next() {
  if (current.value < steps.length - 1) {
    current.value++
  } else {
    close()
  }
}

function prev() {
  if (current.value > 0) current.value--
}

function close() {
  show.value = false
  localStorage.setItem(ONBOARDING_KEY, '1')
}
</script>