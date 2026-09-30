<template>
  <view class="page">
    <view class="hero">
      <image v-if="avatarSrc" class="avatar-img" :src="avatarSrc" mode="aspectFill" />
      <view v-else class="avatar">{{ avatarText }}</view>
      <view class="hero-meta">
        <text class="name">{{ displayName }}</text>
        <text class="hero-sub">{{ user ? user.phone : '登录后查看收入与接单状态' }}</text>
      </view>
      <view v-if="profile" :class="['badge', profile.online === 1 ? 'on' : 'off']">
        {{ profile.online === 1 ? '接单中' : '休息中' }}
      </view>
    </view>

    <view v-if="user && stats" class="stats">
      <view class="stat" @click="goOrders('COMPLETED')">
        <text class="stat-num">{{ stats.todayCompleted || 0 }}</text>
        <text class="stat-label">今日完成 ›</text>
      </view>
      <view class="stat" @click="goOrders(activePhase)">
        <text :class="['stat-num', activeDoing > 0 && 'hot']">{{ activeDoing }}</text>
        <text class="stat-label">进行中 ›</text>
      </view>
      <view class="stat" @click="goWallet">
        <text class="stat-num accent">¥{{ formatMoney(stats.withdrawableBalance) }}</text>
        <text class="stat-label">可提现 ›</text>
      </view>
    </view>

    <view class="card wallet-card" v-if="user" @click="goWallet">
      <view>
        <text class="row-title">我的钱包</text>
        <text class="muted">今日 ¥{{ formatMoney(stats?.todayIncome) }} · 本周 ¥{{ formatMoney(stats?.weekIncome) }}</text>
      </view>
      <text class="go">提现 ›</text>
    </view>

    <view class="card" v-if="user">
      <view class="row">
        <view>
          <text class="row-title">接单开关</text>
          <text class="muted">{{ profile && profile.online === 1 ? '上线中，可进入抢单大厅' : '休息中，不会收到新单' }}</text>
        </view>
        <switch :checked="profile && profile.online === 1" @change="toggle" color="#e85d04" />
      </view>
    </view>

    <view class="menu card" v-if="user">
      <view class="menu-item" @click="goIm">
        <text>顾客消息</text>
        <view class="menu-right">
          <view v-if="imUnread" class="menu-badge">{{ imUnread > 99 ? '99+' : imUnread }}</view>
          <text class="muted">订单临时会话 ›</text>
        </view>
      </view>
      <view class="menu-item" @click="goOrders('WAIT_PICKUP')">
        <text>待取餐订单</text>
        <text class="muted">{{ stats ? stats.waitPickupCount : 0 }} ›</text>
      </view>
      <view class="menu-item" @click="goOrders('ON_WAY')">
        <text>配送中订单</text>
        <text class="muted">{{ stats ? stats.onWayCount : 0 }} ›</text>
      </view>
      <view class="menu-item" @click="goOrders('COMPLETED')">
        <text>历史完成</text>
        <text class="muted">累计 {{ stats ? stats.totalCompleted : 0 }} ›</text>
      </view>
      <view class="menu-item" @click="goStats">
        <text>订单统计</text>
        <text class="muted">按日收入 ›</text>
      </view>
      <view class="menu-item" @click="goReviews">
        <text>我的评价</text>
        <text class="muted">顾客评分 ›</text>
      </view>
      <view class="menu-item" @click="goWallet">
        <text>资金流水 / 提现</text>
        <text class="muted">›</text>
      </view>
      <view class="menu-item" @click="goSet">
        <text>设置</text>
        <text class="muted">提醒 / 资料 ›</text>
      </view>
    </view>

    <view class="card btn-accent" v-if="!user" @click="goLogin">骑手登录</view>
    <view class="card btn-ghost" v-if="user" @click="logout">退出登录</view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { riderApi, imApi, absUrl } from '../../api/http.js'
import { clearLogin, getUser, requireLogin } from '../../utils/auth.js'
import { formatMoney } from '../../utils/format.js'
import { refreshImBadge } from '../../utils/imBadge.js'
import { confirmRest } from '../../utils/nav.js'

const user = ref(null)
const profile = ref(null)
const stats = ref(null)
const imUnread = ref(0)

const displayName = computed(() => {
  if (!user.value) return '未登录'
  return profile.value?.name || user.value.nickname || user.value.phone || '骑手'
})

const avatarText = computed(() => {
  const n = displayName.value
  return n ? String(n).slice(0, 1) : '骑'
})

const avatarSrc = computed(() => {
  const u = profile.value?.avatarUrl
  return u ? absUrl(u) : ''
})
const activeDoing = computed(() =>
  Number(stats.value?.waitPickupCount || 0) + Number(stats.value?.onWayCount || 0)
)
const activePhase = computed(() =>
  Number(stats.value?.onWayCount || 0) > 0 ? 'ON_WAY' : 'WAIT_PICKUP'
)

async function refresh() {
  user.value = getUser()
  if (!user.value) {
    profile.value = null
    stats.value = null
    imUnread.value = 0
    return
  }
  try {
    const [p, s, u] = await Promise.all([
      riderApi.profile(),
      riderApi.stats(),
      imApi.unread({ silent: true }).catch(() => null)
    ])
    profile.value = p
    stats.value = s
    imUnread.value = Number((u && u.unreadMessages) || 0)
    refreshImBadge(2)
  } catch (e) {
    profile.value = null
    stats.value = null
  }
}

async function toggle(e) {
  if (!requireLogin()) return
  const on = e.detail.value
  if (!on) {
    const ok = await confirmRest()
    if (!ok) {
      if (profile.value) profile.value = { ...profile.value, online: 1 }
      return
    }
  }
  profile.value = await riderApi.setOnline(on)
  stats.value = await riderApi.stats().catch(() => stats.value)
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}

function goOrders(phase) {
  uni.setStorageSync('rider_order_phase', phase)
  uni.switchTab({ url: '/pages/order/list' })
}

function goIm() {
  uni.navigateTo({ url: '/pages/im/list' })
}

function goWallet() {
  uni.navigateTo({ url: '/pages/finance/index' })
}

function goStats() {
  uni.navigateTo({ url: '/pages/stats/index' })
}

function goReviews() {
  uni.navigateTo({ url: '/pages/reviews/index' })
}

function goSet() {
  uni.navigateTo({ url: '/pages/set/index' })
}

function logout() {
  clearLogin()
  user.value = null
  profile.value = null
  stats.value = null
  uni.showToast({ title: '已退出', icon: 'none' })
}

onShow(refresh)
onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 40rpx 32rpx 32rpx;
  background:
    radial-gradient(ellipse 80% 70% at 0% 0%, rgba(232, 93, 4, 0.2), transparent 50%),
    linear-gradient(160deg, #123d2f, #1b5e45);
  color: #fff;
}
.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  font-weight: 800;
}
.avatar-img {
  width: 96rpx;
  height: 96rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.18);
}
.hero-meta { flex: 1; }
.name {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
}
.hero-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}
.badge {
  font-size: 22rpx;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.15);
}
.badge.on {
  background: rgba(125, 255, 179, 0.25);
  color: #b8ffd6;
}
.stats {
  display: flex;
  margin: -20rpx 28rpx 0;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx 0;
  box-shadow: 0 8rpx 24rpx rgba(20, 32, 27, 0.06);
  position: relative;
  z-index: 1;
}
.stat {
  flex: 1;
  text-align: center;
}
.stat-num {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
}
.stat-num.accent { color: var(--accent); }
.stat-num.hot { color: var(--accent); }
.stat-label {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--muted);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.row-title {
  display: block;
  font-weight: 700;
  margin-bottom: 6rpx;
}
.menu { padding: 0 28rpx; }
.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid var(--line);
}
.menu-item:last-child { border-bottom: none; }
.menu-right { display: flex; align-items: center; gap: 12rpx; }
.menu-badge {
  min-width: 32rpx; height: 32rpx; padding: 0 8rpx; border-radius: 16rpx;
  background: #e11d48; color: #fff; font-size: 20rpx; text-align: center; line-height: 32rpx;
}
.wallet-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20rpx;
}
.go { color: var(--accent); font-weight: 700; }
</style>
