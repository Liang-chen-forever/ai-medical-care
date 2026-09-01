<template>
  <view class="page-container">
    <view class="card">
      <!-- 用户信息头部 -->
      <view class="user-header">
        <view class="user-avatar">{{ userInfo ? userInfo.username.charAt(0) : '?' }}</view>
        <view class="user-name">{{ userInfo ? userInfo.username : '未登录' }}</view>
      </view>
      <view class="user-stats">
        <view class="stat-item" @tap="goToLogin" v-if="!userInfo">
          <text class="stat-num">登录</text>
          <text class="stat-label">点击登录</text>
        </view>
        <view class="stat-item" v-else>
          <text class="stat-num">{{ appointmentCount }}</text>
          <text class="stat-label">预约记录</text>
        </view>
      </view>
    </view>

    <!-- 操作菜单 -->
    <view class="card menu-card">
      <view class="menu-item" @tap="goToLogin" v-if="!userInfo">
        <text>登录 / 注册</text>
        <text class="menu-arrow">&#x203A;</text>
      </view>
      <view class="menu-item" @tap="openAppointments" v-else>
        <text>查看我的预约</text>
        <text class="menu-arrow">&#x203A;</text>
      </view>
      <view class="menu-item" @tap="goToChat">
        <text>AI 智能问诊</text>
        <text class="menu-arrow">&#x203A;</text>
      </view>
      <view class="menu-item" @tap="handleLogout" v-if="userInfo">
        <text style="color:#ef4444">退出登录</text>
        <text class="menu-arrow" style="color:#ef4444">&#x203A;</text>
      </view>
    </view>

    <!-- 预约列表弹窗 -->
    <view v-if="showAppointments" class="modal-mask" @tap="showAppointments = false">
      <view class="modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">我的预约</text>
          <text class="modal-close" @tap="showAppointments = false">&#x2715;</text>
        </view>
        <view class="modal-body">
          <view v-if="appointments.length === 0" class="empty-state">
            <text class="icon">&#x1F4CB;</text>
            <text>暂无预约记录</text>
          </view>
          <view v-for="a in appointments" :key="a.id" class="apt-item">
            <view class="apt-info">
              <text class="apt-dept">{{ a.department }}</text>
              <text class="apt-doctor">{{ a.doctorName || '--' }}</text>
              <text class="apt-time">{{ a.date }} {{ a.time }}</text>
            </view>
            <button class="btn btn-danger btn-sm" @tap="handleCancel(a)" :disabled="cancellingId === a.id">
              {{ cancellingId === a.id ? '...' : '取消' }}
            </button>
          </view>
        </view>
      </view>
    </view>

  </view>
</template>

<script>
import {
  clearAuth,
  getAppointments,
  cancelAppointment,
  getAuth,
  getUserInfo
} from '@/api/index.js'

export default {
  data() {
    return {
      userInfo: null,
      showAppointments: false,
      appointments: [],
      cancellingId: null
    }
  },
  computed: {
    appointmentCount() {
      return this.appointments.length || 0
    }
  },
  onShow() {
    this.userInfo = getUserInfo()
    if (getAuth()) this.fetchAppointments()
    else this.appointments = []
  },
  methods: {
    openAppointments() {
      if (!getAuth()) {
        this.goToLogin()
        return
      }
      this.showAppointments = true
      this.fetchAppointments()
    },
    async fetchAppointments() {
      if (!getAuth()) return
      try {
        const res = await getAppointments()
        this.appointments = res.data || []
      } catch (e) {
        uni.showToast({ title: e.message || '查询失败', icon: 'error' })
      }
    },
    async handleCancel(appointment) {
      const res = await new Promise((resolve) => {
        uni.showModal({
          title: '确认取消',
          content: `确定取消 ${appointment.department} ${appointment.date} ${appointment.time} 的预约吗？`,
          success: (r) => resolve(r.confirm)
        })
      })
      if (!res) return
      this.cancellingId = appointment.id
      try {
        await cancelAppointment(appointment.id)
        uni.showToast({ title: '取消成功', icon: 'success' })
        // 刷新列表
        await this.fetchAppointments()
      } catch (e) {
        uni.showToast({ title: '取消失败', icon: 'error' })
      } finally {
        this.cancellingId = null
      }
    },
    goToLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    },
    goToChat() {
      uni.switchTab({ url: '/pages/chat/chat' })
    },
    handleLogout() {
      clearAuth()
      getApp().globalData.userInfo = null
      this.userInfo = null
      this.appointments = []
      uni.showToast({ title: '已退出登录' })
    }
  }
}
</script>

<style scoped>
.user-header {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 30rpx;
}

.user-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  font-weight: 600;
}

.user-name {
  font-size: 34rpx;
  font-weight: 600;
}

.user-stats {
  display: flex;
  gap: 40rpx;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 32rpx;
  font-weight: 700;
  color: #2563eb;
}

.stat-label {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 4rpx;
}

.menu-card {
  padding: 0;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 2rpx solid #f1f5f9;
  font-size: 28rpx;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-arrow {
  font-size: 36rpx;
  color: #94a3b8;
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
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  padding: 24rpx 30rpx;
  border-top: 2rpx solid #e2e8f0;
  background: #f8fafc;
}

.apt-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f1f5f9;
}

.apt-item:last-child {
  border-bottom: none;
}

.apt-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.apt-dept {
  font-size: 28rpx;
  font-weight: 600;
}

.apt-doctor {
  font-size: 24rpx;
  color: #64748b;
}

.apt-time {
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
