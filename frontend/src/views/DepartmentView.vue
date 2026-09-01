<template>
  <div class="page-container">
    <div class="page-header">
      <h1>科室医生</h1>
      <p>浏览各科室的医生信息，选择适合的医生进行预约</p>
    </div>

    <div class="card">
      <div class="department-tabs">
        <button
          v-for="dept in departments"
          :key="dept"
          :class="['tab-btn', { active: currentDept === dept }]"
          @click="selectDepartment(dept)"
        >{{ dept }}</button>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      加载中...
    </div>

    <div v-else-if="doctors.length === 0" class="empty-state">
      <div class="empty-icon">👨‍⚕️</div>
      <p>暂无医生信息</p>
    </div>

    <div v-else class="doctor-list">
      <div v-for="doctor in doctors" :key="doctor.id" class="card doctor-card">
        <div class="doctor-header">
          <div class="doctor-avatar">{{ doctor.name.charAt(0) }}</div>
          <div class="doctor-info">
            <h3>{{ doctor.name }}</h3>
            <span class="badge badge-info">{{ doctor.title }}</span>
          </div>
        </div>
        <div class="doctor-body">
          <div class="doctor-specialty">
            <span class="label">擅长领域</span>
            <p>{{ doctor.specialty }}</p>
          </div>
          <div class="doctor-desc">
            <span class="label">医生简介</span>
            <p>{{ doctor.description }}</p>
          </div>
        </div>
        <div class="doctor-footer">
          <router-link
            :to="`/appointment?dept=${encodeURIComponent(currentDept)}&doctor=${encodeURIComponent(doctor.name)}`"
            class="btn btn-primary btn-sm"
          >
            预约挂号
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDepartments, getDoctors } from '../api/index.js'

const departments = ref([])
const currentDept = ref('')
const doctors = ref([])
const loading = ref(false)

async function selectDepartment(dept) {
  currentDept.value = dept
  await fetchDoctors()
}

async function fetchDoctors() {
  loading.value = true
  try {
    const res = await getDoctors(currentDept.value)
    doctors.value = res.data
  } catch (e) {
    console.error('获取医生列表失败:', e)
    doctors.value = []
  } finally {
    loading.value = false
  }
}

async function fetchDepartments() {
  try {
    const res = await getDepartments()
    departments.value = res.data || []
    currentDept.value = departments.value[0] || ''
    if (currentDept.value) await fetchDoctors()
  } catch (e) {
    console.error('获取科室列表失败:', e)
    departments.value = []
    doctors.value = []
  }
}

onMounted(fetchDepartments)
</script>

<style scoped>
.department-tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 8px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.tab-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.tab-btn.active {
  background: #2563eb;
  color: white;
  border-color: #2563eb;
}

.doctor-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.doctor-card {
  padding: 20px;
}

.doctor-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.doctor-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}

.doctor-info h3 {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 4px;
}

.doctor-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.label {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: block;
  margin-bottom: 4px;
}

.doctor-body p {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
}

.doctor-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .department-tabs {
    gap: 6px;
    overflow-x: auto;
  }

  .tab-btn {
    padding: 6px 14px;
    font-size: 13px;
    white-space: nowrap;
  }

  .doctor-card {
    padding: 16px;
  }

  .doctor-header {
    gap: 10px;
    margin-bottom: 12px;
  }

  .doctor-avatar {
    width: 40px;
    height: 40px;
    font-size: 17px;
  }

  .doctor-info h3 {
    font-size: 15px;
  }

  .doctor-body p {
    font-size: 13px;
  }
}
</style>
