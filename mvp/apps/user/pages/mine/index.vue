<template>
  <view class="page">
    <view class="hero">
      <view class="user-row" @click="onAvatar">
        <image v-if="user?.avatarUrl" class="avatar-img" :src="avatarSrc" mode="aspectFill" />
        <view v-else class="avatar">{{ avatarText }}</view>
        <view class="user-meta">
          <text class="name">{{ user ? (user.nickname || '区惠用户') : '请登录/注册' }}</text>
          <text class="phone">{{ user ? (user.phone || '') : '登录后享受完整配送服务' }}</text>
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="asset-bar" v-if="user">
        <view class="asset" @click="goCoupon">
          <text class="asset-num">{{ counts.coupon }}</text>
          <text class="asset-label">优惠券</text>
        </view>
        <view class="asset-line" />
        <view class="asset" @click="goFav">
          <text class="asset-num">{{ counts.fav }}</text>
          <text class="asset-label">收藏</text>
        </view>
        <view class="asset-line" />
        <view class="asset" @click="goAddress">
          <text class="asset-num">{{ counts.addr }}</text>
          <text class="asset-label">地址</text>
        </view>
      </view>
    </view>

    <view class="card order-card" v-if="user">
      <view class="order-head" @click="goOrders('all')">
        <text class="order-title">我的订单</text>
        <text class="order-all">查看全部 ›</text>
      </view>
      <view class="order-grid">
        <view class="og" @click="goOrders('PENDING_PAY')">
          <view class="og-icon pay">付</view>
          <text class="og-label">待付款</text>
          <view v-if="counts.pay" class="og-badge">{{ counts.pay }}</view>
        </view>
        <view class="og" @click="goOrders('active')">
          <view class="og-icon run">送</view>
          <text class="og-label">进行中</text>
          <view v-if="counts.active" class="og-badge">{{ counts.active }}</view>
        </view>
        <view class="og" @click="goOrders('waitReview')">
          <view class="og-icon review">评</view>
          <text class="og-label">待评价</text>
          <view v-if="counts.review" class="og-badge">{{ counts.review }}</view>
        </view>
        <view class="og" @click="goOrders('afterSales')">
          <view class="og-icon refund">退</view>
          <text class="og-label">退款/售后</text>
          <view v-if="counts.refund" class="og-badge">{{ counts.refund }}</view>
        </view>
      </view>
    </view>

    <view class="card menu">
      <view class="item" @click="goIm">
        <text class="item-icon">信</text>
        <text class="item-text">订单消息</text>
        <view v-if="imUnread" class="item-badge">{{ imUnread > 99 ? '99+' : imUnread }}</view>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="goAddress">
        <text class="item-icon">址</text>
        <text class="item-text">收货地址</text>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="goFav">
        <text class="item-icon">藏</text>
        <text class="item-text">我的收藏</text>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="goCoupon">
        <text class="item-icon">券</text>
        <text class="item-text">优惠券</text>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="goService">
        <text class="item-icon">服</text>
        <text class="item-text">客服与帮助</text>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="goPrivacy">
        <text class="item-icon">协</text>
        <text class="item-text">用户协议与隐私</text>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="showAbout">
        <text class="item-icon">关</text>
        <text class="item-text">关于区惠</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="card btn-accent" v-if="!user" @click="goLogin">立即登录</view>
    <view class="card btn-ghost" v-else @click="logout">退出登录</view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { clearLogin, getToken, getUser, patchUser } from '../../utils/auth.js'
import { orderApi, couponApi, addressApi, favoriteApi, profileApi, imApi, absUrl } from '../../api/http.js'
import { listFavoriteIds } from '../../utils/favorite.js'
import { refreshImBadge } from '../../utils/imBadge.js'

const user = ref(null)
const imUnread = ref(0)
const counts = ref({
  pay: 0, active: 0, review: 0, refund: 0, fav: 0, coupon: 0, addr: 0
})

const avatarText = computed(() => {
  if (!user.value) return '访'
  return (user.value.nickname || user.value.phone || '用').slice(0, 1)
})
const avatarSrc = computed(() => absUrl(user.value?.avatarUrl || ''))

onShow(async () => {
  user.value = getUser()
  counts.value.fav = listFavoriteIds().length
  if (!getToken()) {
    counts.value = { pay: 0, active: 0, review: 0, refund: 0, fav: listFavoriteIds().length, coupon: 0, addr: 0 }
    imUnread.value = 0
    return
  }
  try {
    const profile = await profileApi.get()
    patchUser({
      nickname: profile.nickname,
      avatarUrl: profile.avatarUrl,
      phone: profile.phone,
      userId: profile.userId
    })
    user.value = getUser()
  } catch (e) { /* ignore */ }

  try {
    const [orders, coupons, addrs, favs, unread] = await Promise.all([
      orderApi.list({ silent: true }).catch(() => []),
      couponApi.mine({ status: 'UNUSED' }).catch(() => []),
      addressApi.list().catch(() => []),
      favoriteApi.ids().catch(() => listFavoriteIds()),
      imApi.unread({ silent: true }).catch(() => null)
    ])
    const statusOf = (row) => (row && row.order ? row.order.status : row && row.status)
    const reviewed = (row) => !!(row && row.reviewed)
    counts.value = {
      pay: orders.filter((o) => statusOf(o) === 'PENDING_PAY').length,
      active: orders.filter((o) => ['PAID', 'ACCEPTED', 'DELIVERING'].includes(statusOf(o))).length,
      review: orders.filter((o) => statusOf(o) === 'COMPLETED' && !reviewed(o)).length,
      refund: orders.filter((o) => ['REFUNDING', 'REFUNDED'].includes(statusOf(o))).length,
      fav: (favs || []).length,
      coupon: (coupons || []).length,
      addr: (addrs || []).length
    }
    imUnread.value = Number((unread && unread.unreadMessages) || 0)
    refreshImBadge()
  } catch (e) { /* ignore */ }
})

function onAvatar() {
  if (!user.value) return goLogin()
  uni.navigateTo({ url: '/pages/mine/profile' })
}
function goLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}
function goAddress() {
  if (!requireGate()) return
  uni.navigateTo({ url: '/pages/address/list' })
}
function goFav() {
  if (!requireGate()) return
  uni.navigateTo({ url: '/pages/favorite/index' })
}
function goCoupon() {
  if (!requireGate()) return
  uni.navigateTo({ url: '/pages/coupon/index' })
}
function goIm() {
  if (!requireGate()) return
  uni.navigateTo({ url: '/pages/im/list' })
}
function goService() {
  uni.navigateTo({ url: '/pages/mine/service' })
}
function goPrivacy() {
  uni.navigateTo({ url: '/pages/privacy/index' })
}
function goOrders(filterKey) {
  if (!requireGate()) return
  uni.setStorageSync('orderListFilter', filterKey || 'all')
  uni.switchTab({ url: '/pages/order/list' })
}
function requireGate() {
  if (getToken()) return true
  goLogin()
  return false
}
function showAbout() {
  uni.showModal({
    title: '区惠 v1.0.0',
    content: '同城外卖 / 便利店即时达。当前联调支付为 mock，正式环境将接入微信支付。',
    showCancel: false
  })
}
function logout() {
  uni.showModal({
    title: '退出登录',
    content: '确定退出当前账号？',
    success: (res) => {
      if (!res.confirm) return
      clearLogin()
      user.value = null
      counts.value = { pay: 0, active: 0, review: 0, refund: 0, fav: 0, coupon: 0, addr: 0 }
      uni.showToast({ title: '已退出', icon: 'none' })
    }
  })
}
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 40rpx; background: #f5f5f5; }
.hero {
  padding: 36rpx 28rpx 28rpx;
  background: linear-gradient(160deg, #0f3d2e, #1b5e45 70%, #245c4a);
  color: #fff;
}
.user-row { display: flex; align-items: center; gap: 20rpx; }
.avatar, .avatar-img {
  width: 108rpx; height: 108rpx; border-radius: 50%; flex-shrink: 0;
  background: rgba(255,255,255,.18);
}
.avatar {
  display: flex; align-items: center; justify-content: center;
  font-size: 42rpx; font-weight: 800;
}
.user-meta { flex: 1; min-width: 0; }
.name { display: block; font-size: 36rpx; font-weight: 800; }
.phone { display: block; margin-top: 8rpx; opacity: 0.8; font-size: 24rpx; }
.arrow { color: rgba(255,255,255,.7); font-size: 36rpx; }
.asset-bar {
  margin-top: 28rpx; display: flex; align-items: center;
  background: rgba(255,255,255,.12); border-radius: 16rpx; padding: 20rpx 0;
}
.asset { flex: 1; text-align: center; }
.asset-num { display: block; font-size: 34rpx; font-weight: 800; color: #ffd6a5; }
.asset-label { display: block; margin-top: 4rpx; font-size: 22rpx; opacity: 0.85; }
.asset-line { width: 1rpx; height: 40rpx; background: rgba(255,255,255,.25); }

.order-card { margin-top: -8rpx; }
.order-head {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx;
}
.order-title { font-size: 30rpx; font-weight: 800; color: #222; }
.order-all { font-size: 24rpx; color: #999; }
.order-grid { display: flex; padding: 12rpx 0 4rpx; }
.og {
  flex: 1; text-align: center; position: relative; padding: 12rpx 0;
}
.og-icon {
  width: 64rpx; height: 64rpx; margin: 0 auto; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 26rpx; font-weight: 700; color: #0f3d2e; background: #eaf6ef;
}
.og-icon.pay { background: #fff7ed; color: #e85d04; }
.og-icon.run { background: #e0f2fe; color: #0369a1; }
.og-icon.review { background: #fef3c7; color: #b45309; }
.og-icon.refund { background: #fee2e2; color: #b91c1c; }
.og-label { display: block; margin-top: 10rpx; font-size: 22rpx; color: #666; }
.og-badge {
  position: absolute; top: 4rpx; right: 28rpx;
  min-width: 28rpx; height: 28rpx; padding: 0 6rpx;
  border-radius: 14rpx; background: #ff4d4f; color: #fff;
  font-size: 18rpx; line-height: 28rpx;
}

.menu { padding: 0 8rpx 0 20rpx; }
.item {
  display: flex; align-items: center; gap: 16rpx;
  padding: 30rpx 20rpx 30rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.item:last-child { border-bottom: none; }
.item-icon {
  width: 48rpx; height: 48rpx; border-radius: 12rpx;
  background: #eaf6ef; color: #0f3d2e; font-size: 22rpx; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.item-text { flex: 1; font-weight: 600; color: #222; }
.item-badge {
  min-width: 32rpx; height: 32rpx; padding: 0 8rpx; border-radius: 16rpx;
  background: #e11d48; color: #fff; font-size: 20rpx; line-height: 32rpx; text-align: center;
}
.menu .arrow { color: #ccc; font-size: 32rpx; }
</style>
