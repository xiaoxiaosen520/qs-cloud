<template>
  <view class="page" v-if="detail">
    <view class="status-card" :class="statusClass(detail.order.status)">
      <text class="status">{{ statusText(detail.order.status) }}</text>
      <text class="sub">订单号 {{ detail.order.orderNo }}</text>
      <text class="countdown" v-if="countdownText">{{ countdownText }}</text>
      <view class="status-row">
        <text class="income">实付 ¥{{ formatMoney(detail.order.payAmount) }}</text>
        <text class="sub" v-if="detail.order.createdAt">{{ formatTime(detail.order.createdAt) }}</text>
      </view>
    </view>

    <view class="card route-card" v-if="address">
      <view class="route-row">
        <view class="pin shop" />
        <view class="route-body">
          <view class="row">
            <text class="route-title">本店出餐</text>
            <text class="link" v-if="detail.order.deliveryType === 'PLATFORM'">蜂鸟众包</text>
            <text class="link" v-else>商家自配</text>
          </view>
          <text class="muted">{{ platformHint }}</text>
        </view>
      </view>
      <view class="route-line" />
      <view class="route-row">
        <view class="pin user" />
        <view class="route-body">
          <view class="row">
            <text class="route-title">送达 · {{ address.contactName || '顾客' }}</text>
            <text class="link" @click="callPhone(address.contactPhone)">打电话</text>
          </view>
          <text class="muted" @longpress="copyText(address.detail || address.address)">
            {{ address.contactPhone || '' }} {{ address.detail || address.address || '' }}
          </text>
        </view>
      </view>
    </view>

    <view class="card" v-if="detail.order.deliveryType === 'PLATFORM'">
      <view class="row">
        <text class="title">蜂鸟配送</text>
        <text class="link" v-if="dispatch?.riderPhone" @click="callPhone(dispatch.riderPhone)">打电话</text>
      </view>
      <text class="remark">{{ dispatch?.statusText || '接单后将自动呼叫蜂鸟' }}</text>
      <text class="muted" v-if="dispatch?.riderName">骑手 {{ dispatch.riderName }} {{ dispatch.riderPhone || '' }}</text>
      <text class="muted" v-if="dispatch?.trackingNo">运单 {{ dispatch.trackingNo }}</text>
      <text class="warn" v-if="dispatch?.status === 'FAILED'">{{ dispatch.failReason || '发单失败' }}</text>
      <view class="btn-accent mini" v-if="canRedispatch" @click="redispatch">重新呼叫蜂鸟</view>
    </view>

    <view class="card" v-if="detail.order.remark">
      <text class="title">用户备注</text>
      <text class="remark">{{ detail.order.remark }}</text>
    </view>

    <view class="card">
      <view class="row">
        <text class="title">商品明细</text>
        <text class="price">¥{{ formatMoney(detail.order.payAmount) }}</text>
      </view>
      <view v-for="it in detail.items" :key="it.id" class="line">
        <text class="goods">{{ it.goodsName }}（{{ it.skuName }}）x{{ it.quantity }}</text>
        <text class="muted">¥{{ formatMoney(Number(it.price) * it.quantity) }}</text>
      </view>
      <view class="fee muted">
        商品 ¥{{ formatMoney(detail.order.goodsAmount) }}
        · 配送 ¥{{ formatMoney(detail.order.deliveryFee) }}
        · 打包 ¥{{ formatMoney(detail.order.packingFee) }}
      </view>
    </view>

    <view class="card" v-if="detail.logs && detail.logs.length">
      <text class="title">订单进度</text>
      <view v-for="(log, idx) in detail.logs" :key="log.id" class="log">
        <view :class="['log-dot', idx === detail.logs.length - 1 && 'on']" />
        <view class="log-body">
          <text class="log-text">{{ log.remark || statusText(log.toStatus) }}</text>
          <text class="muted">{{ formatTime(log.createdAt) }}</text>
        </view>
      </view>
    </view>

    <view class="card soft">
      <view class="kv"><text class="muted">配送方式</text><text>{{ deliveryText }}</text></view>
      <view class="kv" v-if="detail.order.paidAt"><text class="muted">支付时间</text><text>{{ formatTime(detail.order.paidAt) }}</text></view>
      <view class="kv" v-if="detail.order.acceptedAt"><text class="muted">接单时间</text><text>{{ formatTime(detail.order.acceptedAt) }}</text></view>
      <view class="kv" v-if="detail.order.cancelReason"><text class="muted">取消原因</text><text>{{ detail.order.cancelReason }}</text></view>
    </view>

    <view class="safe-bottom" />

    <view class="action-bar" v-if="showBar">
      <template v-if="detail.order.status === 'PAID'">
        <view class="btn-ghost bar-half" @click="reject">拒单退款</view>
        <view class="btn-accent bar-half" @click="accept">接单出餐</view>
      </template>
      <template v-else-if="detail.order.status === 'ACCEPTED' && detail.order.deliveryType !== 'PLATFORM'">
        <view class="btn-ghost bar-third" @click="goChat">发消息</view>
        <view class="btn-ghost bar-third" @click="callPhone(address && address.contactPhone)">打电话</view>
        <view class="btn-accent bar-third" @click="complete">完成配送</view>
      </template>
      <template v-else-if="detail.order.status === 'ACCEPTED' || detail.order.status === 'DELIVERING'">
        <view class="btn-ghost bar-half" @click="goChat">发消息</view>
        <view class="btn-ghost bar-half" @click="callPhone(address && address.contactPhone)">打电话</view>
      </template>
    </view>
  </view>

  <view v-else-if="loading" class="page">
    <view class="skeleton" v-for="i in 4" :key="i" />
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
import { orderApi } from '../../api/http.js'
import { ORDER_STATUS_TEXT, callPhone, copyText, confirmModal } from '../../utils/auth.js'
import { formatMoney, formatTime, parseAddress } from '../../utils/format.js'

const id = ref(0)
const detail = ref(null)
const loading = ref(true)
const nowTick = ref(Date.now())
let countdownTimer = null
let pollTimer = null

const address = computed(() =>
  detail.value ? parseAddress(detail.value.order.addressSnapshot) : null
)

const deliveryText = computed(() => {
  if (!detail.value) return ''
  return detail.value.order.deliveryType === 'PLATFORM' ? '蜂鸟众包配送' : '商家自配送'
})
const dispatch = computed(() => detail.value?.dispatch || null)
const platformHint = computed(() => {
  if (!detail.value) return ''
  if (detail.value.order.deliveryType !== 'PLATFORM') return '备齐后自行配送'
  return dispatch.value?.statusText || '出餐后等待蜂鸟骑手取餐'
})
const canRedispatch = computed(() => {
  const s = detail.value?.order?.status
  return ['ACCEPTED', 'DELIVERING'].includes(s) && dispatch.value?.status === 'FAILED'
})

const countdownText = computed(() => {
  const order = detail.value?.order
  if (!order || order.status !== 'PAID' || !order.acceptDeadlineAt) return ''
  const end = Date.parse(String(order.acceptDeadlineAt).replace(' ', 'T'))
  if (!Number.isFinite(end)) return ''
  const left = Math.max(0, Math.floor((end - nowTick.value) / 1000))
  const mm = String(Math.floor(left / 60)).padStart(2, '0')
  const ss = String(left % 60).padStart(2, '0')
  if (left <= 0) return '即将超时自动退款，请尽快接单'
  return `接单剩余 ${mm}:${ss}，超时将自动退款`
})

function startCountdown() {
  stopCountdown()
  countdownTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
}
function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}
function startPoll() {
  stopPoll()
  const s = detail.value?.order?.status
  if (!['PAID', 'ACCEPTED', 'DELIVERING'].includes(s)) return
  pollTimer = setInterval(async () => {
    try {
      detail.value = await orderApi.detail(id.value)
      const ns = detail.value?.order?.status
      if (!['PAID', 'ACCEPTED', 'DELIVERING'].includes(ns)) stopPoll()
    } catch (e) { /* ignore */ }
  }, 5000)
}
function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const showBar = computed(() => {
  if (!detail.value) return false
  const s = detail.value.order.status
  return s === 'PAID' || s === 'ACCEPTED' || s === 'DELIVERING'
})

function goChat() {
  uni.navigateTo({ url: `/pages/im/chat?orderId=${id.value}&type=USER_MERCHANT&role=MERCHANT` })
}

function statusText(s) {
  return ORDER_STATUS_TEXT[s] || s
}
function statusClass(s) {
  if (s === 'COMPLETED') return 'done'
  if (s === 'CANCELLED' || s === 'REFUNDED') return 'bad'
  if (s === 'PAID') return 'hot'
  return 'live'
}

async function load() {
  loading.value = true
  try {
    detail.value = await orderApi.detail(id.value)
  } finally {
    loading.value = false
  }
}

onLoad(async (q) => {
  id.value = Number(q.id)
  await load()
  startCountdown()
  startPoll()
})
onShow(() => {
  startCountdown()
  startPoll()
})
onHide(() => {
  stopCountdown()
  stopPoll()
})
onUnload(() => {
  stopCountdown()
  stopPoll()
})
onUnmounted(() => {
  stopCountdown()
  stopPoll()
})

async function accept() {
  uni.showLoading({ title: '接单中', mask: true })
  try {
    detail.value = await orderApi.accept(id.value)
    uni.showToast({ title: '已接单', icon: 'success' })
    stopPoll()
  } finally {
    uni.hideLoading()
  }
}

async function reject() {
  const ok = await confirmModal('拒单后将退款给用户，确认拒单？')
  if (!ok) return
  detail.value = await orderApi.reject(id.value, '商家拒单')
  uni.showToast({ title: '已拒单', icon: 'none' })
  stopPoll()
}

async function complete() {
  const ok = await confirmModal('确认已送达顾客？')
  if (!ok) return
  detail.value = await orderApi.complete(id.value)
  uni.showToast({ title: '已完成', icon: 'success' })
}

async function redispatch() {
  uni.showLoading({ title: '呼叫蜂鸟', mask: true })
  try {
    detail.value = await orderApi.dispatchFengNiao(id.value)
    uni.showToast({ title: '已重新呼叫', icon: 'success' })
  } finally {
    uni.hideLoading()
  }
}
</script>

<style scoped>
.status-card {
  margin: 0 0 8rpx;
  padding: 36rpx 32rpx 28rpx;
  background: linear-gradient(160deg, #123d2f 0%, #1b5e45 100%);
  color: #fff;
}
.status-card.hot {
  background: linear-gradient(160deg, #f48c06, #e85d04);
}
.status-card.live {
  background: linear-gradient(160deg, #2a6f6f, #1b5e45);
}
.status-card.done {
  background: linear-gradient(160deg, #52796f, #354f52);
}
.status-card.bad {
  background: linear-gradient(160deg, #e76f51, #d62828);
}
.status {
  display: block;
  font-size: 40rpx;
  font-weight: 800;
  margin-bottom: 8rpx;
}
.countdown {
  display: block;
  margin: 8rpx 0 4rpx;
  font-size: 24rpx;
  font-weight: 600;
  opacity: 0.95;
}
.sub {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.75);
}
.status-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 20rpx;
}
.income {
  font-size: 34rpx;
  font-weight: 700;
  color: #ffd6a5;
}
.route-card { padding-bottom: 20rpx; }
.route-row {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
}
.pin {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  margin-top: 12rpx;
  flex-shrink: 0;
}
.pin.shop { background: var(--brand-soft); }
.pin.user { background: var(--accent); }
.route-line {
  width: 2rpx;
  height: 36rpx;
  background: var(--line);
  margin: 6rpx 0 6rpx 8rpx;
}
.route-body { flex: 1; }
.route-title { font-weight: 700; }
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6rpx;
}
.link {
  color: var(--accent);
  font-size: 24rpx;
  font-weight: 600;
}
.title {
  display: block;
  font-weight: 700;
  margin-bottom: 12rpx;
}
.remark {
  display: block;
  color: var(--warn);
  line-height: 1.5;
}
.warn {
  display: block;
  margin-top: 8rpx;
  color: #c2410c;
  font-size: 24rpx;
}
.mini {
  margin-top: 16rpx;
  text-align: center;
  padding: 16rpx 0;
}
.line {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  border-top: 1rpx solid var(--line);
}
.goods { flex: 1; padding-right: 16rpx; }
.fee { margin-top: 12rpx; }
.log {
  display: flex;
  gap: 16rpx;
  padding: 12rpx 0;
}
.log-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: var(--line);
  margin-top: 10rpx;
}
.log-dot.on { background: var(--accent); }
.log-text {
  display: block;
  font-weight: 600;
  margin-bottom: 4rpx;
}
.soft { background: #f8faf9; box-shadow: none; }
.kv {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
  gap: 24rpx;
}
.safe-bottom { height: 160rpx; }
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 16rpx;
  padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(20, 32, 27, 0.06);
}
.bar-btn { flex: 1; }
.bar-half { flex: 1; padding: 22rpx 0; }
.bar-third { flex: 1; padding: 22rpx 0; font-size: 24rpx; }
</style>
