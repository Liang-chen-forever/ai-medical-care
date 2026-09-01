<script>
import { getUserInfo } from '@/api/index.js'

export default {
  globalData: {
    userInfo: null
  },
  onLaunch() {
    console.log('[App] 硅谷小智 启动')
    this.globalData.userInfo = getUserInfo()
  }
}
</script>

<style lang="scss">
@import '@/uni.scss';
</style>
