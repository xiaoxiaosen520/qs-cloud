<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.key"
        :class="['tab', phase === t.key && 'on']"
        @click="setPhase(t.key)"
      >
        <text>{{ t.label }}</text>
        <text v-if="tabCount(t.key) > 0" class="tab-badge">{{ tabCount(t.key) }}</text>
      </view>
    </view>

    <view
      v-if="activeHint"
      class="active-hint"
      @click="setPhase(onWayCount > 0 ? 'ON_WAY' : 'WAIT_PICKUP')"
    >
      <view class="hint-dot" />
      <text class="hint-text">{{ activeHint }}</text>
      <text class="hint-go">查看</text>
    </view>

    <view v-if="loading">
      <view class="skeleton" v-for="i in 3" :key="i" />
    </view>

    <view v-else-if="!orders.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">暂无{{ currentLabel }}订单</text>
      <text class="muted">去抢单大厅看看有没有新单</text>
    </view>

    <view
      v-for="item in orders"
      :key="item.order.id"
      class="order-card"
      @click="goDetail(item.order.id)"
    >
      <view class="row">
        <text class="phase" :class="item.phase">{{ phaseText(item.phase) }}</text>
        <text class="price">收入 ¥{{ formatMoney(item.income) }}</text>
      </view>
      <text class="shop">{{ item.shopName || '商家' }}</text>
      <text class="muted block">取：{{ item.shopAddress || '-' }}</text>
      <text class="muted block">送：{{ addrDetail(item.address) || addrText(item.address) }}</text>
      <view class="row foot">
        <text class="muted">#{{ shortOrderNo(item.order.orderNo) }} · {{ item.itemCount || 0 }}件</text>
        <text class="muted" v-if="item.distanceMeters != null">{{ formatDistance(item.distanceMeters) }}</text>
      </view>

      <view class="ops" v-if="item.phase === 'WAIT_PICKUP'" @click.stop>
        <view class="btn-ghost mini" @click="goChat(item.order.id)">发消息</view>
        <view class="btn-ghost mini" @click="callPhone(item.shopPhone)">打电话</view>
        <view class="btn-accent mini" @click="pickup(item.order.id)">确认取餐</view>
      </view>
      <view class="ops" v-else-if="item.phase === 'ON_WAY'" @click.stop>
        <view class="btn-ghost mini" @click="goChat(item.order.id)">发消息</view>
        <view class="btn-ghost mini" @click="callUser(item.address)">打电话</view>
        <view class="btn-accent mini" @click="deliver(item.order.id)">确认送达</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onHide, onPullDownRefresh } from '@dcloudio/uni-app'
import { riderApi } from '../../api/http.js'
import { requireLogin, PHASE_TEXT, callPhone } from '../../utils/auth.js'
import {
  addrText,
  addrDetail,
  formatDistance,
  formatMoney,
  shortOrderNo
} from '../../utils/format.js'
import { alertOrderCancelled } from '../../utils/nav.js'
import { getSeenCancelledIds, markCancelledSeen } from '../../utils/settings.js'

const tabs = [
  { key: 'WAIT_PICKUP', label: '待取餐' },
  { key: 'ON_WAY', label: '配送中' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]

const phase = ref('WAIT_PICKUP')
const orders = ref([])
const loading = ref(false)
const counts = ref({ WAIT_PICKUP: 0, ON_WAY: 0, COMPLETED: 0, CANCELLED: 0 })

const currentLabel = computed(() => {
  const t = tabs.find((x) => x.key === phase.value)
  return t ? t.label : ''
})
const waitCount = computed(() => Number(counts.value.WAIT_PICKUP || 0))
const onWayCount = computed(() => Number(counts.value.ON_WAY || 0))
const activeHint = computed(() => {
  const w = waitCount.value
  const o = onWayCount.value
  if (!w && !o) return ''
  if (w && o) return `进行中 ${w + o} 单：待取餐 ${w} · 配送中 ${o}`
  if (w) return `有 ${w} 单待取餐，请尽快到店`
  return `有 ${o} 单配送中，请尽快送达`
})

function tabCount(key) {
  return Number(counts.value[key] || 0)
}

function phaseText(p) {
  return PHASE_TEXT[p] || p
}

function callUser(address) {
  callPhone(address && address.contactPhone)
}

function goChat(orderId) {
  uni.navigateTo({ url: `/pages/im/chat?orderId=${orderId}&type=USER_RIDER&role=RIDER` })
}

async function refreshCounts() {
  try {
    const st = await riderApi.stats()
    counts.value.WAIT_PICKUP = Number(st.waitPickupCount || 0)
    counts.value.ON_WAY = Number(st.onWayCount || 0)
    const n = counts.value.WAIT_PICKUP + counts.value.ON_WAY
    if (n > 0) {
      uni.setTabBarBadge({ index: 1, text: n > 99 ? '99+' : String(n) })
    } else {
      uni.removeTabBarBadge({ index: 1 })
    }
  } catch (e) { /* ignore */ }
}

async function load() {
  if (!requireLogin()) return
  loading.value = true
  try {
    orders.value = (await riderApi.mine(phase.value)) || []
    await refreshCounts()
    await checkCancelled()
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function checkCancelled() {
  try {
    const list = (await riderApi.cancelled(90)) || []
    const seen = new Set(getSeenCancelledIds())
    const fresh = list.filter((x) => x.order && !seen.has(Number(x.order.id)))
    if (fresh.length) {
      alertOrderCancelled(fresh.length)
      markCancelledSeen(fresh.map((x) => x.order.id))
      if (phase.value === 'WAIT_PICKUP' || phase.value === 'ON_WAY') {
        orders.value = (await riderApi.mine(phase.value)) || []
      }
    }
  } catch (e) { /* ignore */ }
}

function setPhase(v) {
  phase.value = v
  load()
}

function goDetail(id) {
  uni.navigateTo({ url: '/pages/order/detail?id=' + id })
}

async function pickup(id) {
  const ok = await confirm('确认已到店取餐？')
  if (!ok) return
  await riderApi.pickup(id)
  uni.showToast({ title: '已取餐，请尽快送达', icon: 'none' })
  await load()
}

async function deliver(id) {
  const ok = await confirm('确认已送达顾客？')
  if (!ok) return
  await riderApi.deliver(id)
  uni.showToast({ title: '配送完成', icon: 'success' })
  await load()
}

function confirm(content) {
  return new Promise((resolve) => {
    uni.showModal({
      title: '请确认',
      content,
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false)
    })
  })
}

let pollTimer = null
function startPoll() {
  stopPoll()
  pollTimer = setInterval(() => {
    if (phase.value === 'WAIT_PICKUP' || phase.value === 'ON_WAY') checkCancelled()
  }, 12000)
}
function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onShow(() => {
  const saved = uni.getStorageSync('rider_order_phase')
  if (saved && tabs.some((t) => t.key === saved)) {
    phase.value = saved
    uni.removeStorageSync('rider_order_phase')
  }
  load()
  startPoll()
})
onHide(stopPoll)
onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 8rpx;
  padding: 16rpx 24rpx;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 2;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  color: var(--muted);
  border-radius: 12rpx;
  font-size: 24rpx;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}
.tab.on {
  color: var(--accent);
  font-weight: 700;
  background: var(--accent-soft);
}
.tab-badge {
  min-width: 28rpx;
  height: 28rpx;
  padding: 0 8rpx;
  border-radius: 14rpx;
  background: #e85d04;
  color: #fff;
  font-size: 18rpx;
  line-height: 28rpx;
  font-weight: 700;
}
.active-hint {
  margin: 12rpx 28rpx 0;
  padding: 18rpx 22rpx;
  border-radius: 16rpx;
  background: #fff4ec;
  border: 2rpx solid rgba(232, 93, 4, 0.28);
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.hint-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #e85d04;
  animation: pulse-dot 1.2s ease infinite;
  flex-shrink: 0;
}
.hint-text {
  flex: 1;
  font-size: 24rpx;
  font-weight: 700;
  color: #9a3412;
}
.hint-go {
  font-size: 24rpx;
  font-weight: 800;
  color: #e85d04;
}
.order-card {
  background: #fff;
  border-radius: 20rpx;
  margin: 16rpx 28rpx;
  padding: 28rpx;
  box-shadow: 0 6rpx 18rpx rgba(20, 32, 27, 0.04);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.phase {
  font-size: 24rpx;
  font-weight: 700;
  padding: 6rpx 14rpx;
  border-radius: 8rpx;
  background: #eef6f2;
  color: var(--brand-soft);
}
.phase.WAIT_PICKUP {
  background: var(--accent-soft);
  color: var(--accent);
}
.phase.ON_WAY {
  background: #e8f4f2;
  color: var(--ok);
}
.phase.COMPLETED {
  background: #eef0ef;
  color: var(--muted);
}
.phase.CANCELLED {
  background: #fde8e8;
  color: var(--danger);
}
.shop {
  display: block;
  margin-top: 16rpx;
  font-size: 32rpx;
  font-weight: 700;
}
.block { display: block; margin-top: 8rpx; }
.foot { margin-top: 16rpx; }
.ops {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid var(--line);
}
.mini {
  flex: 1;
  padding: 16rpx 0;
  font-size: 26rpx;
}
</style>
