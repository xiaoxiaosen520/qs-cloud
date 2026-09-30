<template>
  <view class="page">
    <view v-if="loading">
      <view class="skeleton" v-for="i in 4" :key="i" />
    </view>
    <view v-else-if="!list.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">暂无流水</text>
      <text class="muted">订单完成后会入账到这里</text>
    </view>
    <view v-else>
      <view v-for="b in list" :key="b.id" class="card row">
        <view class="left">
          <text class="title">{{ typeText(b.type) }}</text>
          <text class="muted">{{ b.message || b.orderNo || '' }}</text>
          <text class="muted">{{ formatTime(b.createdAt) }}</text>
        </view>
        <text :class="['amt', b.operateType === 1 ? 'plus' : 'minus']">
          {{ b.operateType === 1 ? '+' : '-' }}{{ formatMoney(b.amount) }}
        </text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { financeApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { formatMoney, formatTime } from '../../utils/format.js'

const list = ref([])
const loading = ref(true)

const TYPE_MAP = {
  ORDER_INCOME: '订单入账',
  REFUND: '退款冲正',
  WITHDRAW: '提现冻结',
  WITHDRAW_BACK: '提现退回'
}

function typeText(t) {
  return TYPE_MAP[t] || t
}

async function load() {
  if (!requireLogin()) return
  loading.value = true
  try {
    list.value = (await financeApi.billings()) || []
  } finally {
    loading.value = false
  }
}

onShow(load)
</script>

<style scoped>
.row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
}
.left { flex: 1; min-width: 0; }
.title { display: block; font-weight: 700; margin-bottom: 6rpx; }
.amt { font-size: 34rpx; font-weight: 800; }
.amt.plus { color: var(--ok); }
.amt.minus { color: var(--danger); }
</style>
