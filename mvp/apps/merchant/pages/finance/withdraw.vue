<template>
  <view class="page">
    <view class="tabs">
      <text :class="['tab', tab === 'apply' && 'on']" @click="tab = 'apply'">申请提现</text>
      <text :class="['tab', tab === 'list' && 'on']" @click="tab = 'list'">提现记录</text>
    </view>

    <view v-if="tab === 'apply'" class="card">
      <text class="muted">可提现 ¥{{ formatMoney(wallet.withdrawableBalance) }} · 手续费 ¥{{ formatMoney(wallet.withdrawFee) }}</text>
      <text class="label">提现金额</text>
      <input class="input" type="digit" v-model="amount" placeholder="最低 1 元" />
      <text class="label">收款方式</text>
      <view class="modes">
        <text :class="['mode', mode === 'WECHAT' && 'on']" @click="mode = 'WECHAT'">微信</text>
        <text :class="['mode', mode === 'ALIPAY' && 'on']" @click="mode = 'ALIPAY'">支付宝</text>
        <text :class="['mode', mode === 'BANK' && 'on']" @click="mode = 'BANK'">银行卡</text>
      </view>
      <text class="hint muted">实际到账约 ¥{{ actualText }}（审核通过后线下打款）</text>
      <view class="btn-accent" @click="submit">提交申请</view>
    </view>

    <view v-else>
      <view v-if="!records.length" class="empty">
        <view class="empty-icon" />
        <text class="empty-title">暂无提现记录</text>
      </view>
      <view v-for="r in records" :key="r.id" class="card">
        <view class="row">
          <text class="title">¥{{ formatMoney(r.amount) }}</text>
          <text :class="['pill', r.auditStatus]">{{ statusText(r.auditStatus) }}</text>
        </view>
        <text class="muted">到账 ¥{{ formatMoney(r.actualAmount) }} · {{ r.paymentMode }}</text>
        <text class="muted block">{{ r.withdrawNo }} · {{ formatTime(r.createdAt) }}</text>
        <text class="muted block" v-if="r.auditReason">{{ r.auditReason }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { financeApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { formatMoney, formatTime } from '../../utils/format.js'

const tab = ref('apply')
const wallet = ref({ withdrawableBalance: 0, withdrawFee: 1 })
const amount = ref('')
const mode = ref('WECHAT')
const records = ref([])

const actualText = computed(() => {
  const a = Number(amount.value || 0)
  const fee = Number(wallet.value.withdrawFee || 0)
  return formatMoney(Math.max(0, a - fee))
})

function statusText(s) {
  return { PENDING: '审核中', APPROVED: '已通过', REJECTED: '已拒绝' }[s] || s
}

onLoad((q) => {
  if (q.tab === 'list') tab.value = 'list'
})

async function load() {
  if (!requireLogin()) return
  wallet.value = await financeApi.wallet()
  records.value = (await financeApi.withdraws()) || []
}

async function submit() {
  const a = Number(amount.value || 0)
  if (a < 1) {
    uni.showToast({ title: '金额至少 1 元', icon: 'none' })
    return
  }
  uni.showLoading({ title: '提交中', mask: true })
  try {
    await financeApi.applyWithdraw({ amount: a, paymentMode: mode.value })
    uni.showToast({ title: '已提交', icon: 'success' })
    amount.value = ''
    tab.value = 'list'
    await load()
  } finally {
    uni.hideLoading()
  }
}

onShow(load)
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 8rpx;
  padding: 16rpx 20rpx;
  background: #fff;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  border-radius: 12rpx;
  background: #f3f6f4;
  color: var(--muted);
  font-size: 26rpx;
}
.tab.on {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 700;
}
.label {
  display: block;
  font-size: 24rpx;
  color: var(--muted);
  margin: 20rpx 0 10rpx;
}
.input {
  background: #f3f6f4;
  border-radius: 14rpx;
  padding: 22rpx 20rpx;
}
.modes { display: flex; gap: 12rpx; }
.mode {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 12rpx;
  background: #f0f3f1;
  color: var(--muted);
  font-weight: 600;
}
.mode.on { background: var(--brand); color: #fff; }
.hint { display: block; margin: 20rpx 0; }
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}
.title { font-size: 34rpx; font-weight: 800; }
.pill {
  font-size: 22rpx;
  font-weight: 700;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #eef3f0;
}
.pill.PENDING { background: var(--accent-soft); color: var(--accent); }
.pill.APPROVED { background: rgba(42, 157, 143, 0.12); color: var(--ok); }
.pill.REJECTED { background: #fde8e8; color: var(--danger); }
.block { display: block; margin-top: 6rpx; }
</style>
