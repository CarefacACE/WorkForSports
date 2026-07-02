<script lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

/** 无需登录即可访问的页面 */
const PUBLIC_PAGES = [
  'pages/auth/login',
  'pages/auth/register',
  'pages/auth/forgot-password',
]

export default {
  setup() {
    const userStore = useUserStore()

    onLaunch(() => {
      console.log('[App] Launch')
      userStore.checkAuth()
    })

    onShow(() => {
      console.log('[App] Show')
    })

    onHide(() => {
      console.log('[App] Hide')
    })

    // ===== 路由鉴权拦截 =====
    uni.addInterceptor('navigateTo', {
      invoke(args) {
        const path = typeof args.url === 'string' ? args.url.split('?')[0] : ''
        if (!userStore.isLoggedIn && !PUBLIC_PAGES.includes(path)) {
          uni.reLaunch({ url: '/pages/auth/login' })
          return false
        }
      },
    })
    uni.addInterceptor('switchTab', {
      invoke(args) {
        const path = typeof args.url === 'string' ? args.url.split('?')[0] : ''
        if (!userStore.isLoggedIn && !PUBLIC_PAGES.includes(path)) {
          uni.reLaunch({ url: '/pages/auth/login' })
          return false
        }
      },
    })
  },
}
</script>

<style lang="scss">
/* 全局样式 */
page {
  font-family:
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    'HarmonyOS Sans',
    sans-serif;
  font-size: 28rpx;
  color: #333;
  background-color: #f5f6fa;
  min-height: 100vh;
}

/* 通用工具类 */
.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.text-primary {
  color: #2563eb;
}

.text-secondary {
  color: #666;
}

.text-muted {
  color: #999;
}

.bg-primary {
  background-color: #2563eb;
}

.bg-white {
  background-color: #fff;
}

.rounded-8 {
  border-radius: 8rpx;
}

.rounded-16 {
  border-radius: 16rpx;
}

.rounded-24 {
  border-radius: 24rpx;
}

/* 卡片容器 */
.card {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin: 16rpx 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

/* 间距 */
.mt-16 { margin-top: 16rpx; }
.mt-24 { margin-top: 24rpx; }
.mt-32 { margin-top: 32rpx; }
.mb-16 { margin-bottom: 16rpx; }
.mb-24 { margin-bottom: 24rpx; }
.p-16 { padding: 16rpx; }
.p-24 { padding: 24rpx; }
.px-24 { padding-left: 24rpx; padding-right: 24rpx; }
.py-16 { padding-top: 16rpx; padding-bottom: 16rpx; }
</style>
