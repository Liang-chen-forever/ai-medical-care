<template>
  <div class="page-container">
    <div class="page-header">
      <h1>预约挂号</h1>
      <p>选择科室、日期和时间，查看可预约的号源</p>
    </div>

    <!-- 查询表单 -->
    <div class="card">
      <div class="form-row">
        <div class="form-group">
          <label>科室</label>
          <select v-model="form.department" class="form-select">
            <option value="">请选择科室</option>
            <option v-for="dept in departments" :key="dept" :value="dept">{{ dept }}</option>
          </select>
        </div>
        <div class="form-group">
          <label>日期</label>
          <input v-model="form.date" type="date" class="form-input" :min="today" />
        </div>
        <div class="form-group">
          <label>时间</label>
          <select v-model="form.period" class="form-select">
            <option value="">请选择</option>
            <option value="上午">上午</option>
            <option value="下午">下午</option>
          </select>
        </div>
        <div class="form-group" style="align-self: flex-end;">
          <button class="btn btn-primary btn-block" @click="searchSchedules" :disabled="!canSearch">
            查询号源
          </button>
        </div>
      </div>
    </div>

    <!-- 号源列表 -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      查询中...
    </div>

    <div v-else-if="schedules.length > 0" class="card">
      <div class="card-title">可预约号源 - {{ form.department }} {{ form.date }} {{ form.period }}</div>
      <table class="table">
        <thead>
          <tr>
            <th>医生</th>
            <th>科室</th>
            <th>日期</th>
            <th>时间</th>
            <th>剩余号源</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in schedules" :key="s.id">
            <td><strong>{{ s.doctorName }}</strong></td>
            <td>{{ s.department }}</td>
            <td>{{ s.date }}</td>
            <td>{{ s.time }}</td>
            <td>
              <span :class="['badge', (s.totalSlots - s.bookedSlots) > 5 ? 'badge-success' : 'badge-warning']">
                {{ s.totalSlots - s.bookedSlots }} / {{ s.totalSlots }}
              </span>
            </td>
            <td>
              <button class="btn btn-primary btn-sm" @click="showBookForm(s)">预约</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else-if="searched" class="empty-state">
      <div class="empty-icon">📅</div>
      <p>暂无可用号源，请尝试其他日期或科室</p>
    </div>

    <!-- 预约弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3>确认预约信息</h3>
          <button class="modal-close" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="booking-info">
            <div class="info-row"><span>医生</span><strong>{{ selectedSchedule?.doctorName }}</strong></div>
            <div class="info-row"><span>科室</span><strong>{{ selectedSchedule?.department }}</strong></div>
            <div class="info-row"><span>日期</span><strong>{{ selectedSchedule?.date }}</strong></div>
            <div class="info-row"><span>时间</span><strong>{{ selectedSchedule?.time }}</strong></div>
          </div>
          <p class="booking-note">将使用当前登录账号完成预约</p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showModal = false">取消</button>
          <button
            class="btn btn-primary"
            @click="submitBooking"
            :disabled="!selectedSchedule || submitting"
          >
            {{ submitting ? '提交中...' : '确认预约' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div v-if="toast.show" :class="['toast', `toast-${toast.type}`]">
      {{ toast.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getDepartments,
  getSchedules,
  bookAppointment,
  isAuthenticated
} from '../api/index.js'

const route = useRoute()
const router = useRouter()
const departments = ref([])

const today = new Date().toISOString().split('T')[0]

const form = ref({
  department: '',
  date: today,
  period: ''
})

const schedules = ref([])
const loading = ref(false)
const searched = ref(false)

const showModal = ref(false)
const selectedSchedule = ref(null)
const submitting = ref(false)

const toast = ref({ show: false, type: 'success', message: '' })

const canSearch = computed(() => form.value.department && form.value.date && form.value.period)

function showToast(type, message) {
  toast.value = { show: true, type, message }
  setTimeout(() => { toast.value.show = false }, 3000)
}

async function searchSchedules() {
  if (!canSearch.value) return
  loading.value = true
  searched.value = true
  try {
    const res = await getSchedules(form.value.department, form.value.date, form.value.period)
    schedules.value = res.data || []
  } catch (e) {
    console.error('查询号源失败:', e)
    schedules.value = []
    showToast('error', '查询失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function showBookForm(schedule) {
  if (!isAuthenticated()) {
    router.push({ name: 'Login', query: { redirect: '/appointment' } })
    return
  }
  selectedSchedule.value = schedule
  showModal.value = true
}

async function submitBooking() {
  if (!selectedSchedule.value || !isAuthenticated()) {
    router.push({ name: 'Login', query: { redirect: '/appointment' } })
    return
  }
  submitting.value = true
  try {
    const res = await bookAppointment(selectedSchedule.value.id)
    showToast('success', res._message || '预约成功！')
    showModal.value = false
    await searchSchedules()
  } catch (e) {
    showToast('error', e.message || '预约失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function loadDepartments() {
  try {
    const res = await getDepartments()
    departments.value = res.data || []
    if (!form.value.department) {
      form.value.department = departments.value[0] || ''
    }
  } catch (e) {
    console.error('获取科室列表失败:', e)
    departments.value = []
  }
}

onMounted(async () => {
  if (route.query.dept) form.value.department = route.query.dept
  if (route.query.date) form.value.date = route.query.date
  if (route.query.period || route.query.time) form.value.period = route.query.period || route.query.time
  await loadDepartments()
  if (canSearch.value) searchSchedules()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  backdrop-filter: blur(2px);
}

.modal {
  background: #fff;
  border-radius: 16px;
  width: 440px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h3 {
  font-size: 17px;
  font-weight: 600;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #94a3b8;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.modal-close:hover {
  color: #1e293b;
}

.modal-body {
  padding: 24px;
}

.booking-info {
  background: #f8fafc;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-row span {
  font-size: 12px;
  color: #94a3b8;
}

.info-row strong {
  font-size: 14px;
  color: #1e293b;
}

.required {
  color: #ef4444;
}

.booking-note {
  margin: 16px 0 0;
  font-size: 13px;
  color: #64748b;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .modal {
    width: calc(100vw - 24px);
    max-width: none;
    border-radius: 12px;
  }

  .modal-header {
    padding: 16px 20px;
  }

  .modal-body {
    padding: 20px;
  }

  .modal-footer {
    padding: 12px 20px;
    flex-direction: column-reverse;
    gap: 8px;
  }

  .modal-footer .btn {
    width: 100%;
  }

  .booking-info {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>
