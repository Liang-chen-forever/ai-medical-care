<template>
  <div class="page-container">
    <div class="page-header"><h1>医生工作台</h1><p>处理当前账号名下的预约</p></div>
    <div class="toolbar">
      <label for="status-filter">状态</label>
      <select id="status-filter" v-model="filter" @change="load">
        <option value="">全部</option><option value="PENDING">待确认</option><option value="CONFIRMED">已确认</option>
        <option value="COMPLETED">已完成</option><option value="REJECTED">已拒绝</option><option value="CANCELLED">已取消</option>
      </select>
    </div>
    <div v-if="loading" class="loading">查询中...</div>
    <table v-else class="table">
      <thead><tr><th>日期</th><th>时间</th><th>科室</th><th>医生</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="a in appointments" :key="a.id">
          <td>{{ a.date }}</td><td>{{ a.time }}</td><td>{{ a.department }}</td><td>{{ a.doctorName }}</td>
          <td>{{ statusLabel(a.status) }}</td>
          <td class="actions">
            <button v-if="a.status === 'PENDING'" class="btn btn-primary btn-sm" :disabled="busyId === a.id" aria-label="确认预约" title="确认预约" @click="act(a, 'confirm')">确认</button>
            <button v-if="a.status === 'PENDING' || a.status === 'CONFIRMED'" class="btn btn-danger btn-sm" :disabled="busyId === a.id" aria-label="拒绝预约" title="拒绝预约" @click="reject(a)">拒绝</button>
            <button v-if="a.status === 'CONFIRMED'" class="btn btn-outline btn-sm" :disabled="busyId === a.id" aria-label="完成预约" title="完成预约" @click="act(a, 'complete')">完成</button>
          </td>
        </tr>
        <tr v-if="!appointments.length"><td colspan="6" class="empty-state">暂无预约</td></tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getDoctorAppointments, confirmDoctorAppointment, rejectDoctorAppointment, completeDoctorAppointment } from '../api/index.js'

const appointments = ref([])
const filter = ref('')
const loading = ref(false)
const busyId = ref(null)
const statusLabel = (status) => ({ PENDING: '待确认', CONFIRMED: '已确认', COMPLETED: '已完成', CANCELLED: '已取消', REJECTED: '已拒绝', EXPIRED: '已过期', LEGACY: '历史记录' })[status] || status || '未知'

async function load() {
  loading.value = true
  try { const res = await getDoctorAppointments(filter.value || undefined); appointments.value = res.data || [] } finally { loading.value = false }
}
async function act(a, type) {
  busyId.value = a.id
  try { await (type === 'confirm' ? confirmDoctorAppointment(a.id) : completeDoctorAppointment(a.id)); await load() } finally { busyId.value = null }
}
async function reject(a) {
  const reason = window.prompt('请输入拒绝原因（1-200字）', '')
  if (reason === null || !reason.trim() || reason.trim().length > 200) return
  busyId.value = a.id
  try { await rejectDoctorAppointment(a.id, reason.trim()); await load() } finally { busyId.value = null }
}
onMounted(load)
</script>

<style scoped>
.toolbar { display:flex; gap:10px; align-items:center; margin-bottom:16px; }
.toolbar select { padding:8px 10px; border:1px solid #cbd5e1; border-radius:6px; }
.actions { display:flex; gap:6px; }
</style>
