<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="brand">区惠骑手</text>
          <text class="hero-sub">{{ online ? '接单中 · 有新单会自动刷新' : '下线中 · 打开开关开始接单' }}</text>
        </view>
        <view class="online-wrap" @click.stop>
          <view :class="['dot', online && 'on']" />
          <switch :checked="online" @change="toggleOnline" color="#e85d04" />
        </view>
      </view>
      <view class="mini-stats" v-if="user && stats">
        <view class="mini" @click="goOrders('WAIT_PICKUP')">
          <text :class="['mini-num', waitCount > 0 && 'hot']">{{ waitCount }}</text>
          <text class="mini-label">待取餐 ›</text>
        </view>
        <view class="mini" @click="goOrders('ON_WAY')">
          <text :class="['mini-num', onWayCount > 0 && 'hot']">{{ onWayCount }}</text>
          <text class="mini-label">配送中 ›</text>
        </view>
        <view class="mini" @click="goWallet">
          <text class="mini-num accent">¥{{ formatMoney(stats.todayIncome) }}</text>
          <text class="mini-label">今日收入 ›</text>
        </view>
      </view>
    </view>

    <view v-if="!user" class="card guest">
      <text class="guest-title">登录后开始抢单</text>
      <text class="muted">平台配送订单在商家接单后进入大厅</text>
      <view class="btn-accent mt" @click="goLogin">骑手登录</view>
    </view>

    <template v-else>
      <view v-if="!selfEnabled" class="card closed-banner">
        <text class="guest-title">自有骑手暂未开放</text>
        <text class="muted">平台配送由蜂鸟众包承接，后期自招骑手后再开启抢单</text>
      </view>
      <view
        v-if="activeCount > 0"
        class="active-banner"
        @click="goOrders(onWayCount > 0 ? 'ON_WAY' : 'WAIT_PICKUP')"
      >
        <view class="active-pulse" />
        <view class="active-body">
          <text class="active-title">{{ activeTitle }}</text>
          <text class="active-sub">{{ activeSub }}</text>
        </view>
        <text class="active-go">去处理</text>
      </view>

      <view class="section-head" v-if="selfEnabled">
        <text class="section-title">可抢订单</text>
        <text class="muted" v-if="online">{{ pool.length }} 单 · {{ autoTip }}</text>
        <text class="muted" v-else>上线后可见</text>
      </view>

      <template v-if="selfEnabled">
      <view v-if="loading">
        <view class="skeleton" v-for="i in 3" :key="i" />
      </view>

      <view v-else-if="!online" class="empty">
        <view class="empty-icon" />
        <text class="empty-title">当前已下线</text>
        <text class="muted">打开右上角开关即可接收附近订单</text>
      </view>

      <view v-else-if="!pool.length" class="empty">
        <view class="empty-icon" />
        <text class="empty-title">暂无可抢订单</text>
        <text class="muted">下拉刷新，或等商家接单后入池</text>
      </view>

      <view
        v-for="item in pool"
        :key="item.order.id"
        class="order-card"
        @click="goDetail(item.order.id)"
      >
        <view class="card-top">
          <view class="income">
            <text class="income-label">预计收入</text>
            <text class="income-val">¥{{ formatMoney(item.income) }}</text>
          </view>
          <view class="meta-tags">
            <text class="tag" v-if="item.distanceMeters != null">{{ formatDistance(item.distanceMeters) }}</text>
            <text class="tag warn" v-if="item.waitMinutes != null">{{ formatWait(item.waitMinutes) }}</text>
          </view>
        </view>

        <view class="route">
          <view class="route-row">
            <view class="pin shop" />
            <view class="route-body">
              <text class="route-title">{{ item.shopName || '商家' }}</text>
              <text class="muted">{{ item.shopAddress || '地址待完善' }}</text>
            </view>
          </view>
          <view class="route-line" />
          <view class="route-row">
            <view class="pin user" />
            <view class="route-body">
              <text class="route-title">送至顾客</text>
              <text class="muted">{{ addrDetail(item.address) || addrText(item.address) }}</text>
            </view>
          </view>
        </view>

        <view class="card-foot">
          <text class="muted">{{ item.itemCount || 0 }} 件商品 · ¥{{ formatMoney(item.order.payAmount) }}</text>
          <view class="btn-accent grab" @click.stop="grab(item.order.id)">立即抢单</view>
        </view>
      </view>
      </template>
    </template>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onHide, onPullDownRefresh } from '@dcloudio/uni-app'
import { riderApi } from '../../api/http.js'
import { getUser, requireLogin } from '../../utils/auth.js'
import { addrText, addrDetail, formatDistance, formatMoney, formatWait } from '../../utils/format.js'
import { alertNewOrder, alertOrderCancelled, reportLocationOnce, confirmRest } from '../../utils/nav.js'
import { getSeenCancelledIds, markCancelledSeen } from '../../utils/settings.js'

const user = ref(null)
const online = ref(false)
const selfEnabled = ref(false)
const pool = ref([])
const stats = ref(null)
const loading = ref(false)
const ticking = ref(false)
let timer = null
let lastPoolCount = -1
let locTimer = null

const autoTip = computed(() => (ticking.value ? '自动刷新中' : '下拉刷新'))
const waitCount = computed(() => Number(stats.value?.waitPickupCount || 0))
const onWayCount = computed(() => Number(stats.value?.onWayCount || 0))
const activeCount = computed(() => waitCount.value + onWayCount.value)
const activeTitle = computed(() => {
  if (waitCount.value && onWayCount.value) {
    return `进行中 ${activeCount.value} 单：待取餐 ${waitCount.value} · 配送中 ${onWayCount.value}`
  }
  if (waitCount.value) return `有 ${waitCount.value} 单待取餐，请尽快到店`
  return `有 ${onWayCount.value} 单配送中，请尽快送达`
})
const activeSub = computed(() => '点击查看并处理，避免超时')

function goOrders(phase) {
  uni.setStorageSync('rider_order_phase', phase)
  uni.switchTab({ url: '/pages/order/list' })
}

function goWallet() {
  uni.navigateTo({ url: '/pages/finance/index' })
}

function updateTabBadge() {
  const n = activeCount.value
  if (n > 0) {
    uni.setTabBarBadge({ index: 1, text: n > 99 ? '99+' : String(n) })
  } else {
    uni.removeTabBarBadge({ index: 1 })
  }
}

async function refresh({ silent = false } = {}) {
  user.value = getUser()
  if (!user.value) {
    pool.value = []
    stats.value = null
    online.value = false
    loading.value = false
    lastPoolCount = -1
    uni.removeTabBarBadge({ index: 1 })
    return
  }
  if (!silent) loading.value = true
  try {
    const [profile, st, cap] = await Promise.all([
      riderApi.profile(),
      riderApi.stats().catch(() => null),
      riderApi.capability().catch(() => ({ selfEnabled: false }))
    ])
    selfEnabled.value = !!cap?.selfEnabled
    online.value = profile.online === 1
    stats.value = st
    const nextPool = selfEnabled.value && online.value ? ((await riderApi.pool()) || []) : []
    if (online.value && lastPoolCount >= 0 && nextPool.length > lastPoolCount) {
      alertNewOrder(nextPool.length - lastPoolCount)
    }
    lastPoolCount = nextPool.length
    pool.value = nextPool
    updateTabBadge()
    await checkCancelled()
  } catch (e) {
    pool.value = []
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
    }
  } catch (e) { /* ignore */ }
}

async function toggleOnline(e) {
  if (!requireLogin()) return
  const on = e.detail.value
  if (!on) {
    const ok = await confirmRest()
    if (!ok) {
      online.value = true
      await refresh({ silent: true })
      return
    }
  }
  try {
    const profile = await riderApi.setOnline(on)
    online.value = profile.online === 1
    if (online.value) {
      await reportLocationOnce(riderApi.reportLocation)
      startLoc()
    } else {
      stopLoc()
    }
    await refresh()
  } catch (err) {
    online.value = !on
  }
}

async function grab(id) {
  if (!requireLogin()) return
  uni.showLoading({ title: '抢单中', mask: true })
  try {
    await riderApi.grab(id)
    uni.showToast({ title: '抢单成功', icon: 'success' })
    uni.navigateTo({ url: '/pages/order/detail?id=' + id })
  } finally {
    uni.hideLoading()
    await refresh()
  }
}

function goDetail(id) {
  uni.navigateTo({ url: '/pages/order/detail?id=' + id })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}

function startPoll() {
  stopPoll()
  ticking.value = true
  timer = setInterval(() => {
    if (user.value && online.value) refresh({ silent: true })
  }, 8000)
}

function stopPoll() {
  ticking.value = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function startLoc() {
  stopLoc()
  locTimer = setInterval(() => {
    if (user.value && online.value) reportLocationOnce(riderApi.reportLocation)
  }, 30000)
}

function stopLoc() {
  if (locTimer) {
    clearInterval(locTimer)
    locTimer = null
  }
}

onShow(() => {
  refresh().then(() => {
    if (online.value) {
      reportLocationOnce(riderApi.reportLocation)
      startLoc()
    }
  })
  startPoll()
})
onHide(() => {
  stopPoll()
  stopLoc()
})
onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.hero {
  margin: 0 0 8rpx;
  padding: 36rpx 32rpx 28rpx;
  background:
    radial-gradient(ellipse 90% 80% at 100% 0%, rgba(232, 93, 4, 0.18), transparent 55%),
    linear-gradient(160deg, #123d2f 0%, #1b5e45 55%, #0f3d2e 100%);
  color: #fff;
}
.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.brand {
  display: block;
  font-size: 44rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}
.hero-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.72);
}
.online-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 999rpx;
  padding: 8rpx 12rpx 8rpx 18rpx;
}
.dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #9aa;
}
.dot.on {
  background: #7dffb3;
  animation: pulse-dot 1.4s ease infinite;
}
.mini-stats {
  display: flex;
  margin-top: 28rpx;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16rpx;
  padding: 18rpx 0;
}
.mini {
  flex: 1;
  text-align: center;
}
.mini-num {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
}
.mini-num.accent { color: #ffd6a5; }
.mini-num.hot {
  color: #ffb703;
  animation: pulse-dot 1.4s ease infinite;
}
.mini-label {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.65);
}
.active-banner {
  margin: 8rpx 28rpx 4rpx;
  padding: 24rpx 28rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #fff4ec, #ffe0c2);
  border: 2rpx solid rgba(232, 93, 4, 0.35);
  display: flex;
  align-items: center;
  gap: 16rpx;
  box-shadow: 0 8rpx 20rpx rgba(232, 93, 4, 0.12);
}
.active-pulse {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: #e85d04;
  flex-shrink: 0;
  animation: pulse-dot 1.2s ease infinite;
}
.active-body { flex: 1; min-width: 0; }
.active-title {
  display: block;
  font-size: 28rpx;
  font-weight: 800;
  color: #9a3412;
}
.active-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #c2410c;
}
.active-go {
  flex-shrink: 0;
  font-size: 26rpx;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #f48c06, #e85d04);
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
}
.guest-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}
.closed-banner {
  margin: 20rpx 24rpx 0;
}
.mt { margin-top: 24rpx; }
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 12rpx 32rpx 0;
}
.section-title {
  font-size: 30rpx;
  font-weight: 700;
}
.order-card {
  background: #fff;
  border-radius: 20rpx;
  margin: 20rpx 28rpx;
  padding: 28rpx;
  box-shadow: 0 6rpx 18rpx rgba(20, 32, 27, 0.04);
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20rpx;
}
.income-label {
  display: block;
  font-size: 22rpx;
  color: var(--muted);
}
.income-val {
  display: block;
  font-size: 40rpx;
  font-weight: 800;
  color: var(--accent);
  line-height: 1.2;
}
.meta-tags {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
}
.tag {
  font-size: 22rpx;
  padding: 6rpx 14rpx;
  border-radius: 8rpx;
  background: #eef6f2;
  color: var(--brand-soft);
}
.tag.warn {
  background: var(--accent-soft);
  color: var(--accent);
}
.route-row {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
}
.pin {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  margin-top: 10rpx;
  flex-shrink: 0;
}
.pin.shop { background: var(--brand-soft); }
.pin.user { background: var(--accent); }
.route-line {
  width: 2rpx;
  height: 28rpx;
  background: var(--line);
  margin: 4rpx 0 4rpx 8rpx;
}
.route-title {
  display: block;
  font-weight: 700;
  margin-bottom: 4rpx;
}
.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid var(--line);
}
.grab {
  min-width: 180rpx;
  padding: 16rpx 0;
  font-size: 26rpx;
}
</style>
