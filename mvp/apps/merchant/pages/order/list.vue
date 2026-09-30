<template>
  <view class="page">
    <view class="tabs">
      <text
        v-for="t in tabs"
        :key="t.value"
        :class="['tab', filter === t.value && 'on']"
        @click="setFilter(t.value)"
      >{{ t.label }}{{ t.value === 'PAID' && pendingCount ? ' ' + pendingCount : '' }}</text>
    </view>

    <view v-if="loading">
      <view class="skeleton" v-for="i in 3" :key="i" />
    </view>

    <view v-else-if="!orders.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">暂无{{ currentLabel }}订单</text>
      <text class="muted">用户下单支付后会出现在这里</text>
    </view>

    <view
      v-for="card in orders"
      :key="card.order.id"
      class="order-card"
      @click="goDetail(card.order.id)"
    >
      <view class="row">
        <text :class="['phase', card.order.status]">{{ statusText(card.order.status) }}</text>
        <text class="price">¥{{ formatMoney(card.order.payAmount) }}</text>
      </view>
      <text class="deadline" v-if="acceptLeft(card.order)">{{ acceptLeft(card.order) }}</text>

      <text class="summary">{{ card.itemSummary || '查看商品明细' }}</text>
      <text class="muted block" v-if="addrOf(card.order)">
        {{ addrOf(card.order).contactName }} {{ addrOf(card.order).contactPhone }}
      </text>
      <text class="muted block">{{ addrDetail(addrOf(card.order)) || '地址待完善' }}</text>
      <view class="remark" v-if="card.order.remark">备注：{{ card.order.remark }}</view>

      <view class="row foot">
        <text class="muted">#{{ shortOrderNo(card.order.orderNo) }} · {{ card.itemCount || 0 }}件</text>
        <text class="muted">{{ formatTime(card.order.createdAt) }}</text>
      </view>

      <view class="ops" v-if="card.order.status === 'PAID'" @click.stop>
        <view class="btn-ghost mini" @click="reject(card.order.id)">拒单</view>
        <view class="btn-accent mini" @click="accept(card.order.id)">接单</view>
      </view>
      <view class="ops" v-else-if="card.order.status === 'REFUNDING'" @click.stop>
        <view class="btn-ghost mini" @click="rejectRefund(card.order.id)">拒绝退款</view>
        <view class="btn-accent mini" @click="approveRefund(card.order.id)">同意退款</view>
      </view>
      <view class="ops" v-else-if="card.order.status === 'ACCEPTED' && card.order.deliveryType !== 'PLATFORM'" @click.stop>
        <view class="btn-ghost mini" @click="goChat(card.order.id)">发消息</view>
        <view class="btn-ghost mini" @click="callUser(card.order)">打电话</view>
        <view class="btn-accent mini" @click="complete(card.order.id)">完成配送</view>
      </view>
      <view class="ops" v-else-if="card.order.status === 'ACCEPTED' || card.order.status === 'DELIVERING'" @click.stop>
        <view class="btn-ghost mini" @click="goChat(card.order.id)">发消息</view>
        <view class="btn-ghost mini" @click="callUser(card.order)">打电话</view>
      </view>
      <view class="remark" v-if="card.order.status === 'REFUNDING' && card.order.refundReason">
        退款原因：{{ card.order.refundReason }}
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onHide, onPullDownRefresh } from '@dcloudio/uni-app'
import { orderApi } from '../../api/http.js'
import { requireLogin, ORDER_STATUS_TEXT, callPhone, confirmModal } from '../../utils/auth.js'
import {
  formatMoney,
  formatTime,
  shortOrderNo,
  parseAddress,
  addrDetail
} from '../../utils/format.js'

const tabs = [
  { label: '进行中', value: '' },
  { label: '待接单', value: 'PAID' },
  { label: '退款', value: 'REFUNDING' },
  { label: '已接单', value: 'ACCEPTED' },
  { label: '配送中', value: 'DELIVERING' },
  { label: '已完成', value: 'COMPLETED' }
]

const filter = ref('')
const orders = ref([])
const pendingCount = ref(0)
const loading = ref(false)
const nowTick = ref(Date.now())
let timer = null
let tickTimer = null

const currentLabel = computed(() => {
  const t = tabs.find((x) => x.value === filter.value)
  return t ? t.label : ''
})

function statusText(s) {
  return ORDER_STATUS_TEXT[s] || s
}

function acceptLeft(order) {
  if (!order || order.status !== 'PAID' || !order.acceptDeadlineAt) return ''
  const end = Date.parse(String(order.acceptDeadlineAt).replace(' ', 'T'))
  if (!Number.isFinite(end)) return ''
  const left = Math.max(0, Math.floor((end - nowTick.value) / 1000))
  const mm = String(Math.floor(left / 60)).padStart(2, '0')
  const ss = String(left % 60).padStart(2, '0')
  if (left <= 0) return '即将超时自动退款'
  return `接单剩余 ${mm}:${ss}`
}

function unwrap(list) {
  return (list || []).map((row) => {
    if (row && row.order) return row
    return { order: row, itemCount: 0, itemSummary: '' }
  })
}

function addrOf(order) {
  return parseAddress(order.addressSnapshot)
}

function callUser(order) {
  const a = addrOf(order)
  callPhone(a && a.contactPhone)
}

function goChat(orderId) {
  uni.navigateTo({ url: `/pages/im/chat?orderId=${orderId}&type=USER_MERCHANT&role=MERCHANT` })
}

async function load() {
  if (!requireLogin()) return
  loading.value = true
  try {
    const saved = uni.getStorageSync('merchant_order_filter')
    if (saved !== undefined && saved !== null && String(saved) !== '') {
      filter.value = saved
      uni.removeStorageSync('merchant_order_filter')
    }
    const [list, pending] = await Promise.all([
      orderApi.list(filter.value || undefined),
      orderApi.list('PAID')
    ])
    orders.value = unwrap(list)
    pendingCount.value = unwrap(pending).length
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

function setFilter(v) {
  filter.value = v
  load()
}

function goDetail(id) {
  uni.navigateTo({ url: '/pages/order/detail?id=' + id })
}

async function accept(id) {
  uni.showLoading({ title: '接单中', mask: true })
  try {
    await orderApi.accept(id)
    uni.showToast({ title: '已接单', icon: 'success' })
    await load()
  } finally {
    uni.hideLoading()
  }
}

async function reject(id) {
  const ok = await confirmModal('拒单后将退款给用户，确认拒单？')
  if (!ok) return
  await orderApi.reject(id, '商家拒单')
  uni.showToast({ title: '已拒单', icon: 'none' })
  await load()
}

async function complete(id) {
  const ok = await confirmModal('确认已送达顾客？')
  if (!ok) return
  await orderApi.complete(id)
  uni.showToast({ title: '已完成', icon: 'success' })
  await load()
}

async function approveRefund(id) {
  const ok = await confirmModal('同意退款后将退库存并关闭订单，确认？')
  if (!ok) return
  await orderApi.approveRefund(id)
  uni.showToast({ title: '已退款', icon: 'success' })
  await load()
}

async function rejectRefund(id) {
  const ok = await confirmModal('拒绝后订单将回到已接单，确认？')
  if (!ok) return
  await orderApi.rejectRefund(id, '商家拒绝退款')
  uni.showToast({ title: '已拒绝', icon: 'none' })
  await load()
}

function startPoll() {
  stopPoll()
  timer = setInterval(load, 8000)
}
function stopPoll() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}
function startTick() {
  stopTick()
  tickTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
}
function stopTick() {
  if (tickTimer) {
    clearInterval(tickTimer)
    tickTimer = null
  }
}

onShow(() => {
  load()
  startPoll()
  startTick()
})
onHide(() => {
  stopPoll()
  stopTick()
})
onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 8rpx;
  padding: 16rpx 20rpx;
  background: #fff;
  border-bottom: 1rpx solid var(--line);
  position: sticky;
  top: 0;
  z-index: 2;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 14rpx 4rpx;
  border-radius: 12rpx;
  color: var(--muted);
  font-size: 22rpx;
  background: #f3f6f4;
}
.tab.on {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 700;
}

.order-card {
  background: #fff;
  border-radius: 20rpx;
  margin: 20rpx 28rpx;
  padding: 28rpx;
  box-shadow: 0 6rpx 18rpx rgba(20, 32, 27, 0.04);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.phase {
  font-size: 22rpx;
  font-weight: 700;
  padding: 6rpx 14rpx;
  border-radius: 8rpx;
  background: #eef6f2;
  color: var(--brand-soft);
}
.phase.PAID {
  background: var(--accent-soft);
  color: var(--accent);
}
.phase.ACCEPTED,
.phase.DELIVERING {
  background: rgba(42, 157, 143, 0.12);
  color: var(--ok);
}
.phase.COMPLETED {
  background: #eef3f0;
  color: var(--muted);
}
.phase.CANCELLED,
.phase.REFUNDED {
  background: #fde8e8;
  color: var(--danger);
}
.phase.REFUNDING {
  background: #fff7ed;
  color: #c2410c;
}
.summary {
  display: block;
  margin-top: 16rpx;
  font-weight: 700;
  font-size: 30rpx;
}
.deadline {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #e85d04;
  font-weight: 600;
}
.block { display: block; margin-top: 8rpx; }
.remark {
  margin-top: 12rpx;
  padding: 10rpx 14rpx;
  border-radius: 10rpx;
  background: var(--accent-soft);
  color: var(--warn);
  font-size: 24rpx;
}
.foot { margin-top: 16rpx; }
.ops {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid var(--line);
}
.mini { flex: 1; padding: 16rpx 0; font-size: 26rpx; }
.wait {
  margin-top: 16rpx;
  color: var(--muted);
  font-size: 24rpx;
}
</style>
