<template>
  <view class="page">
    <!-- 余额卡片 -->
    <view class="balance-card">
      <text class="balance-label">账户余额</text>
      <text class="balance-value">¥{{ balance }}</text>
      <view class="balance-actions">
        <button class="btn-action recharge" @tap="showRecharge = true">充值</button>
        <button class="btn-action withdraw" @tap="showWithdraw = true">提现</button>
      </view>
    </view>

    <!-- 交易记录 -->
    <view class="section">
      <text class="section-title">交易记录</text>
    </view>

    <scroll-view class="list" scroll-y>
      <view v-if="transactions.length === 0 && !loading" class="empty">
        <text class="empty-text">暂无交易记录</text>
      </view>

      <view v-for="t in transactions" :key="t.id" class="tx-card">
        <view class="tx-left">
          <text class="tx-type">{{ t.type === 'RECHARGE' ? '充值' : t.type === 'WITHDRAW' ? '提现' : t.type }}</text>
          <text class="tx-remark" v-if="t.remark">{{ t.remark }}</text>
          <text class="tx-time">{{ t.createTime?.split(' ')[0] }}</text>
        </view>
        <view class="tx-right">
          <text class="tx-amount" :class="{ plus: t.type === 'RECHARGE', minus: t.type !== 'RECHARGE' }">
            {{ t.type === 'RECHARGE' ? '+' : '-' }}{{ (t.amount ?? 0).toFixed(2) }}
          </text>
        </view>
      </view>
    </scroll-view>

    <!-- 充值弹窗 -->
    <uni-popup ref="popupRecharge" type="bottom">
      <view class="popup">
        <text class="popup-title">充值</text>
        <input v-model.number="rechargeAmount" class="popup-input" type="digit" placeholder="输入金额" placeholder-style="color:#bbb" />
        <button class="popup-btn" :disabled="!rechargeAmount || recharging" @tap="handleRecharge">
          {{ recharging ? '充值中...' : '确认充值' }}
        </button>
        <button class="popup-cancel" @tap="showRecharge = false">取消</button>
      </view>
    </uni-popup>

    <!-- 提现弹窗 -->
    <uni-popup ref="popupWithdraw" type="bottom">
      <view class="popup">
        <text class="popup-title">提现</text>
        <input v-model.number="withdrawAmount" class="popup-input" type="digit" placeholder="输入金额" placeholder-style="color:#bbb" />
        <button class="popup-btn" :disabled="!withdrawAmount || withdrawing" @tap="handleWithdraw">
          {{ withdrawing ? '提现中...' : '确认提现' }}
        </button>
        <button class="popup-cancel" @tap="showWithdraw = false">取消</button>
      </view>
    </uni-popup>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { getTransactions, recharge, withdraw, type WalletTransaction } from '@/api/finance'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId

const balance = ref('0.00')
const transactions = ref<WalletTransaction[]>([])
const loading = ref(false)

const showRecharge = ref(false)
const showWithdraw = ref(false)
const rechargeAmount = ref(0)
const withdrawAmount = ref(0)
const recharging = ref(false)
const withdrawing = ref(false)

async function fetchData() {
  if (!userId) return
  loading.value = true
  try {
    const [bal, txs] = await Promise.all([
      import('@/api/finance').then(m => m.getBalance(userId!)),
      getTransactions(userId),
    ])
    balance.value = String(bal || 0)
    transactions.value = txs || []
  } catch {
    // 静默失败
  } finally {
    loading.value = false
  }
}

async function handleRecharge() {
  if (!userId || !rechargeAmount.value || recharging.value) return
  recharging.value = true
  try {
    await recharge(userId, rechargeAmount.value)
    uni.showToast({ title: '充值成功', icon: 'success' })
    showRecharge.value = false
    rechargeAmount.value = 0
    fetchData()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '充值失败', icon: 'none' })
  } finally {
    recharging.value = false
  }
}

async function handleWithdraw() {
  if (!userId || !withdrawAmount.value || withdrawing.value) return
  withdrawing.value = true
  try {
    await withdraw(userId, withdrawAmount.value)
    uni.showToast({ title: '提现申请已提交', icon: 'success' })
    showWithdraw.value = false
    withdrawAmount.value = 0
    fetchData()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '提现失败', icon: 'none' })
  } finally {
    withdrawing.value = false
  }
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding-bottom: 32rpx; }

.balance-card {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  margin: 24rpx;
  border-radius: 16rpx;
  padding: 40rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.balance-label { font-size: 24rpx; color: rgba(255,255,255,0.7); margin-bottom: 8rpx; }
.balance-value { font-size: 64rpx; font-weight: 700; color: #fff; margin-bottom: 24rpx; }

.balance-actions { display: flex; gap: 16rpx; width: 100%; }

.btn-action {
  flex: 1;
  height: 72rpx;
  border-radius: 12rpx;
  font-size: 26rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;

  &.recharge { background: #fff; color: #2563eb; }
  &.withdraw { background: rgba(255,255,255,0.2); color: #fff; }
}

.section { margin: 16rpx 24rpx 8rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #333; }

.list { padding: 0 24rpx; }

.tx-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 8rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}

.tx-type { font-size: 28rpx; color: #333; font-weight: 500; display: block; }
.tx-remark { font-size: 22rpx; color: #999; }
.tx-time { font-size: 20rpx; color: #bbb; }

.tx-amount {
  font-size: 30rpx;
  font-weight: 600;

  &.plus { color: #67c23a; }
  &.minus { color: #f56c6c; }
}

.empty { padding: 80rpx 0; text-align: center; }
.empty-text { font-size: 28rpx; color: #999; }

/* 弹窗 */
.popup {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 40rpx 32rpx 48rpx;
}

.popup-title { font-size: 34rpx; font-weight: 700; color: #333; text-align: center; margin-bottom: 24rpx; display: block; }

.popup-input {
  width: 100%;
  height: 88rpx;
  border: 2rpx solid #e5e7eb;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 32rpx;
  color: #333;
  text-align: center;
  margin-bottom: 24rpx;
  box-sizing: border-box;
}

.popup-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  margin-bottom: 16rpx;
}

.popup-cancel {
  width: 100%;
  height: 88rpx;
  background: #f5f6fa;
  color: #666;
  border-radius: 16rpx;
  font-size: 28rpx;
  border: none;
}
</style>
