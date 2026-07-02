<template>
  <view class="page">
    <scroll-view class="list" scroll-y>
      <view v-if="products.length === 0 && !loading" class="empty">
        <text class="empty-icon">🛒</text>
        <text class="empty-text">暂无商品</text>
      </view>

      <view class="product-grid">
        <view v-for="p in products" :key="p.id" class="product-card">
          <view class="p-cover">
            <image v-if="p.image" :src="p.image" mode="aspectFill" class="p-img" />
            <view v-else class="p-placeholder">
              <text>{{ p.name.charAt(0) }}</text>
            </view>
            <text v-if="p.stock <= 5 && p.stock > 0" class="p-stock-low">仅剩{{ p.stock }}件</text>
            <text v-if="p.stock === 0" class="p-sold-out">已售罄</text>
          </view>
          <view class="p-body">
            <text class="p-name">{{ p.name }}</text>
            <text class="p-desc" v-if="p.description">{{ p.description }}</text>
            <view class="p-foot">
              <text class="p-price">¥{{ p.price }}</text>
              <button class="btn-buy" :disabled="p.stock === 0" @tap="handleBuy(p)">
                {{ p.stock === 0 ? '已售罄' : '购买' }}
              </button>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { listGymProducts, purchaseGymProduct, type GymProduct } from '@/api/gym'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId!

const products = ref<GymProduct[]>([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    products.value = await listGymProducts()
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
}

async function handleBuy(p: GymProduct) {
  const res = await new Promise<boolean>((resolve) => {
    uni.showModal({ title: '确认购买', content: `确定购买 "${p.name}" ($¥{p.price})？`, success: (r) => resolve(r.confirm) })
  })
  if (!res) return
  try {
    await purchaseGymProduct(userId!, p.id!, 1)
    uni.showToast({ title: '购买成功', icon: 'success' })
    fetchData()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '购买失败', icon: 'none' })
  }
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }
.list { padding: 16rpx 24rpx; }

.product-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }

.product-card {
  width: calc(50% - 8rpx); background: #fff; border-radius: 16rpx;
  overflow: hidden; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.p-cover {
  width: 100%; height: 260rpx; background: #f0f2f5; position: relative;
}
.p-img { width: 100%; height: 100%; }
.p-placeholder {
  width: 100%; height: 100%; background: linear-gradient(135deg, #e8f4fd, #dbeafe);
  display: flex; align-items: center; justify-content: center; font-size: 56rpx; color: #2563eb; font-weight: 700;
}
.p-stock-low {
  position: absolute; top: 8rpx; right: 8rpx; font-size: 20rpx;
  padding: 4rpx 12rpx; background: #fef0d0; color: #e6a23c; border-radius: 8rpx;
}
.p-sold-out {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  font-size: 28rpx; padding: 8rpx 24rpx; background: rgba(0,0,0,0.6); color: #fff; border-radius: 8rpx;
}

.p-body { padding: 16rpx 20rpx; }
.p-name { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 4rpx; display: block; }
.p-desc { font-size: 22rpx; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; margin-bottom: 8rpx; }

.p-foot { display: flex; justify-content: space-between; align-items: center; }
.p-price { font-size: 32rpx; font-weight: 700; color: #f56c6c; }
.btn-buy {
  height: 56rpx; padding: 0 24rpx; background: #2563eb; color: #fff; border-radius: 12rpx; font-size: 24rpx; border: none;
}

.empty { display: flex; flex-direction: column; align-items: center; padding: 160rpx 0; }
.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
