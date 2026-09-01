<template>
  <view class="page-container">
    <view class="page-header">
      <text class="title">科室医生</text>
      <text class="desc">浏览各科室的医生信息，选择适合的医生进行预约</text>
    </view>

    <view class="card">
      <scroll-view class="dept-tabs" scroll-x>
        <view
          v-for="dept in departments"
          :key="dept"
          :class="['tab-btn', { active: currentDept === dept }]"
          @tap="selectDepartment(dept)"
        >{{ dept }}</view>
      </scroll-view>
    </view>

    <view v-if="loading" class="loading">
      <text>加载中...</text>
    </view>

    <view v-else-if="doctors.length === 0" class="empty-state">
      <text class="icon">&#x1F468;</text>
      <text>暂无医生信息</text>
    </view>

    <view v-else>
      <view v-for="doctor in doctors" :key="doctor.id" class="card doctor-card">
        <view class="doctor-header">
          <view class="doctor-avatar">{{ doctor.name.charAt(0) }}</view>
          <view class="doctor-info">
            <text class="doctor-name">{{ doctor.name }}</text>
            <text class="badge badge-info">{{ doctor.title }}</text>
          </view>
        </view>
        <view class="doctor-body">
          <view class="info-section">
            <text class="label">擅长领域</text>
            <text class="content">{{ doctor.specialty }}</text>
          </view>
          <view class="info-section">
            <text class="label">医生简介</text>
            <text class="content">{{ doctor.description }}</text>
          </view>
        </view>
        <view class="doctor-footer">
          <navigator
            :url="'/pages/appointment/appointment?dept=' + encodeURIComponent(currentDept) + '&doctor=' + encodeURIComponent(doctor.name)"
            class="btn btn-primary btn-sm"
          >预约挂号</navigator>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getDoctors } from '@/api/index.js'

export default {
  data() {
    return {
      departments: ['神经内科', '口腔科'],
      currentDept: '神经内科',
      doctors: [],
      loading: false
    }
  },
  onLoad() {
    this.fetchDoctors()
  },
  methods: {
    async selectDepartment(dept) {
      this.currentDept = dept
      await this.fetchDoctors()
    },
    async fetchDoctors() {
      this.loading = true
      try {
        const res = await getDoctors(this.currentDept)
        this.doctors = res.data || []
      } catch (e) {
        console.error('获取医生列表失败:', e)
        this.doctors = []
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.dept-tabs {
  display: flex;
  gap: 16rpx;
  white-space: nowrap;
}

.tab-btn {
  display: inline-block;
  padding: 14rpx 32rpx;
  border: 2rpx solid #e2e8f0;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #64748b;
  background: #fff;
  flex-shrink: 0;
}

.tab-btn.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.doctor-card {
  padding: 30rpx;
}

.doctor-header {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.doctor-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 600;
}

.doctor-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.doctor-name {
  font-size: 32rpx;
  font-weight: 600;
}

.doctor-body {
  margin-bottom: 24rpx;
}

.info-section {
  margin-bottom: 16rpx;
}

.label {
  font-size: 22rpx;
  color: #94a3b8;
  display: block;
  margin-bottom: 6rpx;
}

.content {
  font-size: 26rpx;
  color: #475569;
  line-height: 1.6;
}

.doctor-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 20rpx;
  border-top: 2rpx solid #f1f5f9;
}
</style>