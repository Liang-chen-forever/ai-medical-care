<template>
  <view class="page-container">
    <view class="page-header">
      <text class="title">预约挂号</text>
      <text class="desc">选择科室、日期和时间，查看可预约的号源</text>
    </view>

    <!-- 查询表单 -->
    <view class="card">
      <view class="form-group">
        <text class="form-label">科室</text>
        <picker :range="departments" @change="onDeptChange">
          <view class="picker-value">{{ form.department || '请选择科室' }}</view>
        </picker>
      </view>
      <view class="form-group">
        <text class="form-label">日期</text>
        <picker mode="date" :value="form.date" :start="today" @change="onDateChange">
          <view class="picker-value">{{ form.date || '请选择日期' }}</view>
        </picker>
      </view>
      <view class="form-group">
        <text class="form-label">时间</text>
        <picker :range="timeSlots" @change="onTimeChange">
          <view class="picker-value">{{ form.time || '请选择时间' }}</view>
        </picker>
      </view>
      <button class="btn btn-primary btn-block" @tap="searchSchedules" :disabled="!canSearch">查询号源</button>
    </view>

    <!-- 号源列表 -->
    <view v-if="loading" class="loading">
      <text>查询中...</text>
    </view>

    <view v-else-if="schedules.length > 0">
      <view class="card" v-for="s in schedules" :key="s.id">
        <view class="schedule-row">
          <view class="schedule-info">
            <text class="doctor-name">{{ s.doctorName }}</text>
            <text class="schedule-meta">{{ s.department }} | {{ s.date }} {{ s.time }}</text>
            <text :class="['badge', (s.totalSlots - s.bookedSlots) > 5 ? 'badge-success' : 'badge-warning']">
              剩余 {{ s.totalSlots - s.bookedSlots }} / {{ s.totalSlots }}
            </text>
          </view>
          <button class="btn btn-primary btn-sm" @tap="showBookForm(s)">预约</button>
        </view>
      </view>
    </view>

    <view v-else-if="searched" class="empty-state">
      <text class="icon">&#x1F4C5;</text>
      <text>暂无可用号源，请尝试其他日期或科室</text>
    </view>

    <!-- 预约弹窗 -->
    <view v-if="showModal" class="modal-mask" @tap="showModal = false">
      <view class="modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">确认预约信息</text>
          <text class="modal-close" @tap="showModal = false">&#x2715;</text>
        </view>
        <view class="modal-body">
          <view class="booking-info">
            <view class="info-row"><text class="info-label">医生</text><text class="info-value">{{ selectedSchedule?.doctorName }}</text></view>
            <view class="info-row"><text class="info-label">科室</text><text class="info-value">{{ selectedSchedule?.department }}</text></view>
            <view class="info-row"><text class="info-label">日期</text><text class="info-value">{{ selectedSchedule?.date }}</text></view>
            <view class="info-row"><text class="info-label">时间</text><text class="info-value">{{ selectedSchedule?.time }}</text></view>
          </view>
          <view class="form-group">
            <text class="form-label">姓名 <text style="color:#ef4444">*</text></text>
            <input v-model="bookForm.username" class="form-input" placeholder="请输入您的姓名" />
          </view>
          <view class="form-group">
            <text class="form-label">身份证号 <text style="color:#ef4444">*</text></text>
            <input v-model="bookForm.idCard" class="form-input" placeholder="请输入18位身份证号" maxlength="18" />
          </view>
        </view>
        <view class="modal-footer">
          <button class="btn btn-outline btn-sm" @tap="showModal = false">取消</button>
          <button
            class="btn btn-primary btn-sm"
            @tap="submitBooking"
            :disabled="!bookForm.username || bookForm.idCard.length !== 18 || submitting"
          >{{ submitting ? '提交中...' : '确认预约' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getSchedules, bookAppointment } from '@/api/index.js'

export default {
  data() {
    const today = new Date()
    const todayStr = `${today.getFullYear()}-${(today.getMonth()+1).toString().padStart(2,'0')}-${today.getDate().toString().padStart(2,'0')}`
    return {
      departments: ['神经内科', '口腔科'],
      timeSlots: ['上午', '下午'],
      today: todayStr,
      form: { department: '', date: todayStr, time: '' },
      schedules: [],
      loading: false,
      searched: false,
      showModal: false,
      selectedSchedule: null,
      submitting: false,
      bookForm: { username: '', idCard: '' }
    }
  },
  computed: {
    canSearch() {
      return this.form.department && this.form.date && this.form.time
    }
  },
  onLoad(options) {
    if (options.dept) this.form.department = options.dept
    if (options.date) this.form.date = options.date
    if (options.time) this.form.time = options.time
    if (this.canSearch) this.searchSchedules()
  },
  methods: {
    onDeptChange(e) {
      this.form.department = this.departments[e.detail.value]
    },
    onDateChange(e) {
      this.form.date = e.detail.value
    },
    onTimeChange(e) {
      this.form.time = this.timeSlots[e.detail.value]
    },
    async searchSchedules() {
      if (!this.canSearch) return
      this.loading = true
      this.searched = true
      try {
        const res = await getSchedules(this.form.department, this.form.date, this.form.time)
        this.schedules = res.data || []
      } catch (e) {
        this.schedules = []
        uni.showToast({ title: '查询失败', icon: 'error' })
      } finally {
        this.loading = false
      }
    },
    showBookForm(schedule) {
      this.selectedSchedule = schedule
      this.bookForm = { username: '', idCard: '' }
      this.showModal = true
    },
    async submitBooking() {
      if (!this.bookForm.username || this.bookForm.idCard.length !== 18) return
      this.submitting = true
      try {
        await bookAppointment({
          username: this.bookForm.username,
          idCard: this.bookForm.idCard,
          department: this.selectedSchedule.department,
          date: this.selectedSchedule.date,
          time: this.selectedSchedule.time,
          doctorName: this.selectedSchedule.doctorName
        })
        uni.showToast({ title: '预约成功', icon: 'success' })
        this.showModal = false
        this.searchSchedules()
      } catch (e) {
        uni.showToast({ title: e.message || '预约失败', icon: 'error' })
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.picker-value {
  padding: 20rpx 28rpx;
  border: 2rpx solid #e2e8f0;
  border-radius: 8rpx;
  font-size: 28rpx;
  background: #f0f4f8;
  color: #1e293b;
}

.schedule-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.schedule-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.doctor-name {
  font-size: 30rpx;
  font-weight: 600;
}

.schedule-meta {
  font-size: 24rpx;
  color: #64748b;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.modal {
  background: #fff;
  border-radius: 24rpx;
  width: 650rpx;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-bottom: 2rpx solid #e2e8f0;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
}

.modal-close {
  font-size: 36rpx;
  color: #94a3b8;
  padding: 10rpx;
}

.modal-body {
  padding: 30rpx;
}

.booking-info {
  background: #f8fafc;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 30rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.info-label {
  font-size: 22rpx;
  color: #94a3b8;
}

.info-value {
  font-size: 26rpx;
  color: #1e293b;
  font-weight: 500;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  padding: 24rpx 30rpx;
  border-top: 2rpx solid #e2e8f0;
  background: #f8fafc;
}
</style>