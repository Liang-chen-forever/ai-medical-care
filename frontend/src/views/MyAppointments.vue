<template>
  <div class="page-container">
    <div class="page-header">
      <h1>我的预约</h1>
      <p>查看和管理您的预约记录</p>
    </div>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      查询中...
    </div>

    <div v-else-if="appointments.length > 0" class="card">
      <table class="table">
        <thead>
          <tr>
            <th>编号</th>
            <th>姓名</th>
            <th>科室</th>
            <th>医生</th>
            <th>日期</th>
            <th>时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in appointments" :key="a.id">
            <td>{{ a.id }}</td>
            <td><strong>{{ a.username }}</strong></td>
            <td><span class="badge badge-info">{{ a.department }}</span></td>
            <td>{{ a.doctorName || '--' }}</td>
            <td>{{ a.date }}</td>
            <td>{{ a.time }}</td>
            <td><span class="badge" :class="`status-${String(a.status || '').toLowerCase()}`">{{ statusLabel(a.status) }}</span></td>
            <td>
              <button
                v-if="isCancellable(a.status)"
                class="btn btn-danger btn-sm"
                @click="handleCancel(a)"
                :disabled="cancellingId === a.id"
              >
                {{ cancellingId === a.id ? '取消中...' : '取消预约' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else-if="searched" class="empty-state">
      <div class="empty-icon">📋</div>
      <p>暂无预约记录</p>
    </div>

    <!-- 取消确认弹窗 -->
    <div v-if="cancelModal.show" class="modal-overlay" @click.self="cancelModal.show = false">
      <div class="modal">
        <div class="modal-header">
          <h3>确认取消预约</h3>
          <button class="modal-close" @click="cancelModal.show = false">&times;</button>
        </div>
        <div class="modal-body">
          <p style="font-size: 14px; color: #475569; margin-bottom: 16px;">
            确定要取消以下预约吗？此操作不可撤销。
          </p>
          <div class="booking-info">
            <div class="info-row"><span>姓名</span><strong>{{ cancelModal.appointment?.username }}</strong></div>
            <div class="info-row"><span>科室</span><strong>{{ cancelModal.appointment?.department }}</strong></div>
            <div class="info-row"><span>日期</span><strong>{{ cancelModal.appointment?.date }}</strong></div>
            <div class="info-row"><span>时间</span><strong>{{ cancelModal.appointment?.time }}</strong></div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="cancelModal.show = false">返回</button>
          <button class="btn btn-danger" @click="confirmCancel">
            确认取消
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
import { onMounted, ref } from 'vue'
import { getAppointments, cancelAppointment, isAuthenticated } from '../api/index.js'

const appointments = ref([])
const loading = ref(false)
const searched = ref(false)
const cancellingId = ref(null)

const cancelModal = ref({ show: false, appointment: null })
const toast = ref({ show: false, type: 'success', message: '' })

function showToast(type, message) {
  toast.value = { show: true, type, message }
  setTimeout(() => { toast.value.show = false }, 3000)
}

async function fetchAppointments() {
  if (!isAuthenticated()) return
  loading.value = true
  searched.value = true
  try {
    const res = await getAppointments()
    appointments.value = res.data || []
  } catch (e) {
    console.error('查询预约失败:', e)
    appointments.value = []
    showToast('error', '查询失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(fetchAppointments)

function handleCancel(appointment) {
  if (!isCancellable(appointment.status)) return
  cancelModal.value = { show: true, appointment }
}

function isCancellable(status) {
  return status === 'PENDING' || status === 'CONFIRMED'
}

function statusLabel(status) {
  return ({ PENDING: '待确认', CONFIRMED: '已确认', COMPLETED: '已完成', CANCELLED: '已取消', REJECTED: '已拒绝', EXPIRED: '已过期', LEGACY: '历史记录' })[status] || status || '未知'
}

async function confirmCancel() {
  const a = cancelModal.value.appointment
  if (!a) return
  cancellingId.value = a.id
  try {
    const res = await cancelAppointment(a.id)
    showToast('success', res._message || '取消成功')
    cancelModal.value.show = false
    await fetchAppointments()
  } catch (e) {
    showToast('error', e.message || '取消失败，请稍后重试')
  } finally {
    cancellingId.value = null
  }
}
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
