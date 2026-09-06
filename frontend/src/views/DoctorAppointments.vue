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
    <p v-if="errorMessage" class="error-msg" role="alert">{{ errorMessage }}</p>
    <div v-if="loading" class="loading">查询中...</div>
    <div v-else class="table-wrapper">
      <table class="table">
        <thead><tr><th>日期</th><th>时间</th><th>科室</th><th>医生</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="a in appointments" :key="a.id">
            <td>{{ a.date }}</td><td>{{ a.time }}</td><td>{{ a.department }}</td><td>{{ a.doctorName }}</td>
            <td>{{ statusLabel(a.status) }}</td>
            <td class="actions">
              <button v-if="a.status === 'PENDING'" class="btn btn-primary icon-action" :disabled="busyId === a.id" aria-label="确认预约" title="确认预约" @click="act(a, 'confirm')">&#10003;</button>
              <button v-if="a.status === 'PENDING' || a.status === 'CONFIRMED'" class="btn btn-danger icon-action" :disabled="busyId === a.id" aria-label="拒绝预约" title="拒绝预约" @click="reject(a)">&#10005;</button>
              <button v-if="a.status === 'CONFIRMED'" class="btn btn-outline icon-action" :disabled="busyId === a.id" aria-label="填写就诊摘要" title="填写就诊摘要" @click="openEncounter(a)">&#10004;</button>
            </td>
          </tr>
          <tr v-if="!appointments.length"><td colspan="6" class="empty-state">暂无预约</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="encounterModal.show" class="modal-overlay" @click.self="closeEncounter">
      <form class="modal encounter-modal" @submit.prevent="submitEncounter">
        <div class="modal-header">
          <h3>完成就诊并填写摘要</h3>
          <button type="button" class="modal-close" aria-label="关闭" @click="closeEncounter">&times;</button>
        </div>
        <div class="modal-body">
          <p class="encounter-context">{{ encounterModal.appointment?.date }} {{ encounterModal.appointment?.time }} · {{ encounterModal.appointment?.department }}</p>
          <label class="field-label" for="encounter-summary">就诊摘要</label>
          <textarea id="encounter-summary" v-model="encounterModal.summary" class="textarea" maxlength="2000" rows="6" required placeholder="记录本次问诊的客观情况"></textarea>
          <label class="field-label" for="encounter-follow-up">随访建议（可选）</label>
          <textarea id="encounter-follow-up" v-model="encounterModal.followUpAdvice" class="textarea" maxlength="2000" rows="4" placeholder="填写复诊或观察建议"></textarea>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline" @click="closeEncounter">取消</button>
          <button type="submit" class="btn btn-primary" :disabled="!encounterModal.summary.trim() || busyId === encounterModal.appointment?.id">
            {{ busyId === encounterModal.appointment?.id ? '提交中...' : '完成并保存' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getDoctorAppointments, confirmDoctorAppointment, rejectDoctorAppointment, completeDoctorEncounter } from '../api/index.js'

const appointments = ref([])
const filter = ref('')
const loading = ref(false)
const busyId = ref(null)
const errorMessage = ref('')
const encounterModal = ref({ show: false, appointment: null, summary: '', followUpAdvice: '' })
const statusLabel = (status) => ({ PENDING: '待确认', CONFIRMED: '已确认', COMPLETED: '已完成', CANCELLED: '已取消', REJECTED: '已拒绝', EXPIRED: '已过期', LEGACY: '历史记录' })[status] || status || '未知'

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getDoctorAppointments(filter.value || undefined)
    appointments.value = res.data || []
  } catch (error) {
    errorMessage.value = error.message || '查询预约失败，请稍后重试'
  } finally { loading.value = false }
}
async function act(a, type) {
  busyId.value = a.id
  errorMessage.value = ''
  try {
    await confirmDoctorAppointment(a.id)
    await load()
  } catch (error) {
    errorMessage.value = error.message || '操作失败，请稍后重试'
  } finally { busyId.value = null }
}

function openEncounter(appointment) {
  encounterModal.value = { show: true, appointment, summary: '', followUpAdvice: '' }
  errorMessage.value = ''
}

function closeEncounter() {
  if (busyId.value === encounterModal.value.appointment?.id) return
  encounterModal.value.show = false
}

async function submitEncounter() {
  const appointment = encounterModal.value.appointment
  const summary = encounterModal.value.summary.trim()
  if (!appointment || !summary) return
  busyId.value = appointment.id
  errorMessage.value = ''
  try {
    await completeDoctorEncounter(appointment.id, summary, encounterModal.value.followUpAdvice.trim())
    encounterModal.value.show = false
    await load()
  } catch (error) {
    errorMessage.value = error.message || '保存就诊摘要失败，请稍后重试'
  } finally { busyId.value = null }
}
async function reject(a) {
  const reason = window.prompt('请输入拒绝原因（1-200字）', '')
  if (reason === null || !reason.trim() || reason.trim().length > 200) return
  busyId.value = a.id
  errorMessage.value = ''
  try {
    await rejectDoctorAppointment(a.id, reason.trim())
    await load()
  } catch (error) {
    errorMessage.value = error.message || '操作失败，请稍后重试'
  } finally { busyId.value = null }
}
onMounted(load)
</script>

<style scoped>
.toolbar { display:flex; gap:10px; align-items:center; margin-bottom:16px; }
.toolbar select { padding:8px 10px; border:1px solid #cbd5e1; border-radius:6px; }
.actions { display:flex; gap:6px; }
.icon-action { width:32px; height:32px; padding:0; display:inline-flex; align-items:center; justify-content:center; }
.error-msg { margin:0 0 12px; color:#b91c1c; font-size:14px; }
.encounter-modal { width: 560px; max-width: 92vw; }
.encounter-context { margin: 0 0 16px; color: #64748b; font-size: 14px; }
.field-label { display: block; margin: 12px 0 6px; color: #334155; font-size: 14px; font-weight: 600; }
.textarea { width: 100%; box-sizing: border-box; resize: vertical; border: 1px solid #cbd5e1; border-radius: 6px; padding: 10px 12px; font: inherit; line-height: 1.5; }
.textarea:focus { outline: 2px solid #93c5fd; border-color: #2563eb; }
</style>
