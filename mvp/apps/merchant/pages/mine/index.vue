<template>
  <view class="page">
    <view class="hero">
      <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
      <view v-else class="avatar">{{ avatarText }}</view>
      <view class="hero-meta">
        <text class="name">{{ displayName }}</text>
        <text class="hero-sub">{{ user ? user.phone : '登录后查看营业数据' }}</text>
      </view>
      <view v-if="shop" :class="['badge', shop.openStatus === 1 ? 'on' : 'off']">
        {{ shop.openStatus === 1 ? '营业中' : '休息中' }}
      </view>
    </view>

    <view v-if="user && shop" class="stats">
      <view class="stat">
        <text class="stat-num">{{ stats.pending }}</text>
        <text class="stat-label">待接单</text>
      </view>
      <view class="stat">
        <text class="stat-num">{{ stats.todayOrders }}</text>
        <text class="stat-label">今日完成</text>
      </view>
      <view class="stat">
        <text class="stat-num accent">¥{{ stats.withdrawable }}</text>
        <text class="stat-label">可提现</text>
      </view>
    </view>

    <view class="card" v-if="shop">
      <view class="row">
        <view>
          <text class="row-title">营业开关</text>
          <text class="muted">{{ shop.openStatus === 1 ? '营业中，用户可下单' : '休息中，店铺对外不可下单' }}</text>
        </view>
        <switch :checked="shop.openStatus === 1" @change="toggle" color="#e85d04" />
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
      <view class="menu-item" @click="goFinance">
        <text>资金账户</text>
        <text class="muted">账单 / 提现 ›</text>
      </view>
      <view class="menu-item" @click="goOrders('PAID')">
        <text>待接订单</text>
        <text class="muted">{{ stats.pending }} ›</text>
      </view>
      <view class="menu-item" @click="goGoods">
        <text>商品管理</text>
        <text class="muted">在售 {{ stats.onSale }} ›</text>
      </view>
      <view class="menu-item" @click="goShop">
        <text>店铺设置</text>
        <text class="muted">起送 / 公告 ›</text>
      </view>
      <view class="menu-item" @click="goApply">
        <text>入驻申请</text>
        <text class="muted">审核状态 ›</text>
      </view>
    </view>

    <view class="card btn-accent" v-if="!user" @click="goLogin">商家登录</view>
    <view class="card btn-ghost" v-if="user" @click="logout">退出登录</view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { shopApi, financeApi, imApi, absUrl } from '../../api/http.js'
import { clearLogin, getUser, requireLogin } from '../../utils/auth.js'
import { formatMoney } from '../../utils/format.js'
import { refreshImBadge } from '../../utils/imBadge.js'

const user = ref(null)
const shop = ref(null)
const imUnread = ref(0)
const stats = reactive({
  pending: 0,
  todayOrders: 0,
  withdrawable: '0.00',
  onSale: 0
})

const displayName = computed(() => {
  if (!user.value) return '未登录'
  return (shop.value && shop.value.name) || user.value.nickname || user.value.phone || '商家'
})
const avatarText = computed(() => String(displayName.value).slice(0, 1))
const avatarUrl = computed(() => {
  const url = shop.value && shop.value.logoUrl
  return url ? absUrl(url) : ''
})

async function refresh() {
  user.value = getUser()
  if (!user.value) {
    shop.value = null
    imUnread.value = 0
    return
  }
  try {
    shop.value = await shopApi.get()
    const st = await financeApi.todayStats()
    stats.pending = st.pendingCount || 0
    stats.todayOrders = st.todayOrders || 0
    stats.withdrawable = formatMoney(st.withdrawableBalance)
    stats.onSale = st.onSaleGoods || 0
    const u = await imApi.unread({ silent: true })
    imUnread.value = Number((u && u.unreadMessages) || 0)
    refreshImBadge(3)
  } catch (e) {
    shop.value = null
  }
}

async function toggle(e) {
  if (!requireLogin() || !shop.value) return
  shop.value = await shopApi.update({
    openStatus: e.detail.value ? 1 : 0,
    notice: shop.value.notice || ''
  })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}
function goOrders(status) {
  uni.setStorageSync('merchant_order_filter', status || '')
  uni.switchTab({ url: '/pages/order/list' })
}
function goIm() {
  uni.navigateTo({ url: '/pages/im/list' })
}
function goGoods() {
  uni.switchTab({ url: '/pages/goods/list' })
}
function goShop() {
  uni.navigateTo({ url: '/pages/shop/index' })
}
function goApply() {
  uni.navigateTo({ url: '/pages/apply/index' })
}
function goFinance() {
  uni.navigateTo({ url: '/pages/finance/index' })
}
function logout() {
  clearLogin()
  user.value = null
  shop.value = null
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
  overflow: hidden;
  flex-shrink: 0;
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
.stat { flex: 1; text-align: center; }
.stat-num {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
}
.stat-num.accent { color: var(--accent); }
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
</style>
