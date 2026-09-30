<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="brand">区惠商家</text>
          <text class="hero-sub">
            {{ shop
              ? (shop.openStatus === 1 ? '营业中 · 新单会自动刷新' : '休息中 · 打开开关开始接单')
              : (user ? '完成入驻后即可营业' : '登录后管理店铺与订单') }}
          </text>
        </view>
        <view class="online-wrap" v-if="shop" @click.stop>
          <view :class="['dot', shop.openStatus === 1 && 'on']" />
          <switch :checked="shop.openStatus === 1" @change="toggleOpen" color="#e85d04" />
        </view>
      </view>
      <view class="mini-stats" v-if="shop">
        <view class="mini">
          <text class="mini-num">{{ stats.pending }}</text>
          <text class="mini-label">待接单</text>
        </view>
        <view class="mini">
          <text class="mini-num">{{ stats.todayOrders }}</text>
          <text class="mini-label">今日完成</text>
        </view>
        <view class="mini">
          <text class="mini-num accent">¥{{ stats.todayIncome }}</text>
          <text class="mini-label">今日入账</text>
        </view>
      </view>
    </view>

    <view v-if="loading">
      <view class="skeleton" v-for="i in 3" :key="i" />
    </view>

    <view v-else-if="!user" class="card guest">
      <text class="guest-title">登录后开始营业</text>
      <text class="muted">接单出餐、管商品库存、设置起送配送</text>
      <view class="btn-accent mt" @click="goLogin">商家登录</view>
    </view>

    <view v-else-if="!shop" class="card guest">
      <text class="guest-title">尚未开通店铺</text>
      <text class="muted">{{ applyHint }}</text>
      <view class="btn-accent mt" @click="goApply">商家入驻 / 查看状态</view>
    </view>

    <template v-else>
      <view class="section-head">
        <text class="section-title">待接订单</text>
        <text class="muted">{{ pendingCards.length }} 单 · {{ autoTip }}</text>
      </view>

      <view v-if="!pendingCards.length" class="empty compact">
        <view class="empty-icon" />
        <text class="empty-title">暂无待接单</text>
        <text class="muted">用户支付后会出现在这里</text>
      </view>

      <view
        v-for="card in pendingCards"
        :key="card.order.id"
        class="order-card"
        @click="goDetail(card.order.id)"
      >
        <view class="card-top">
          <view>
            <text class="income-label">实付金额</text>
            <text class="income-val">¥{{ formatMoney(card.order.payAmount) }}</text>
          </view>
          <view class="meta-tags">
            <text class="tag warn" v-if="waitOf(card.order)">{{ formatWait(waitOf(card.order)) }}</text>
            <text class="tag">{{ card.itemCount || 0 }} 件</text>
          </view>
        </view>

        <view class="route">
          <view class="route-row">
            <view class="pin shop" />
            <view class="route-body">
              <text class="route-title">{{ card.itemSummary || '商品明细' }}</text>
              <text class="muted">#{{ shortOrderNo(card.order.orderNo) }}</text>
            </view>
          </view>
          <view class="route-line" />
          <view class="route-row">
            <view class="pin user" />
            <view class="route-body">
              <text class="route-title">
                {{ addrName(card.order) || '顾客' }}
                <text class="phone" v-if="addrPhone(card.order)"> {{ addrPhone(card.order) }}</text>
              </text>
              <text class="muted">{{ addrDetail(parseAddress(card.order.addressSnapshot)) || '地址待完善' }}</text>
            </view>
          </view>
        </view>

        <view class="remark" v-if="card.order.remark">备注：{{ card.order.remark }}</view>

        <view class="card-foot" @click.stop>
          <text class="muted">{{ formatTime(card.order.createdAt) }}</text>
          <view class="ops">
            <view class="btn-ghost grab ghost" @click="reject(card.order.id)">拒单</view>
            <view class="btn-accent grab" @click="accept(card.order.id)">接单</view>
          </view>
        </view>
      </view>

      <view class="quick">
        <view class="quick-item" @click="goOrders('')">
          <text class="quick-title">全部进行中</text>
          <text class="muted">订单管理 ›</text>
        </view>
        <view class="quick-item" @click="goFinance">
          <text class="quick-title">资金账户</text>
          <text class="muted">可提现 ¥{{ stats.withdrawable }} ›</text>
        </view>
        <view class="quick-item" @click="goGoods">
          <text class="quick-title">商品库存</text>
          <text class="muted">在售 {{ stats.onSale }} ›</text>
        </view>
        <view class="quick-item" @click="goShop">
          <text class="quick-title">店铺设置</text>
          <text class="muted">起送 / 公告 ›</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onShow, onHide, onPullDownRefresh } from '@dcloudio/uni-app'
import { shopApi, applyApi, orderApi, financeApi } from '../../api/http.js'
import { getUser, requireLogin, APPLY_STATUS_TEXT, confirmModal } from '../../utils/auth.js'
import {
  formatMoney,
  formatTime,
  shortOrderNo,
  parseAddress,
  addrDetail,
  waitMinutes,
  formatWait
} from '../../utils/format.js'

const user = ref(null)
const shop = ref(null)
const apply = ref(null)
const loading = ref(true)
const ticking = ref(false)
const pendingCards = ref([])
const stats = reactive({
  pending: 0,
  todayOrders: 0,
  todayIncome: '0.00',
  onSale: 0,
  withdrawable: '0.00'
})
let timer = null
let lastPending = -1

const applyHint = computed(() => {
  if (!apply.value) return '提交入驻资料，等待平台审核'
  return '申请状态：' + (APPLY_STATUS_TEXT[apply.value.status] || apply.value.status)
})
const autoTip = computed(() => (ticking.value ? '自动刷新中' : '下拉刷新'))

function unwrapOrders(list) {
  return (list || []).map((row) => {
    if (row && row.order) return row
    return { order: row, itemCount: 0, itemSummary: '' }
  })
}

function addrName(order) {
  const a = parseAddress(order.addressSnapshot)
  return a && a.contactName
}
function addrPhone(order) {
  const a = parseAddress(order.addressSnapshot)
  return a && a.contactPhone
}
function waitOf(order) {
  return waitMinutes(order.paidAt || order.createdAt)
}

function notifyNewOrders(count) {
  if (lastPending >= 0 && count > lastPending) {
    try {
      uni.vibrateLong({})
    } catch (_) {}
    uni.showToast({ title: `新订单 ${count - lastPending} 笔`, icon: 'none' })
  }
  lastPending = count
}

async function refresh() {
  user.value = getUser()
  if (!user.value) {
    shop.value = null
    pendingCards.value = []
    loading.value = false
    return
  }
  loading.value = true
  try {
    shop.value = await shopApi.get()
    apply.value = null
    await loadStats()
  } catch (e) {
    shop.value = null
    pendingCards.value = []
    try {
      apply.value = await applyApi.status()
    } catch (_) {
      apply.value = null
    }
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const [pendingList, st] = await Promise.all([
    orderApi.list('PAID'),
    financeApi.todayStats().catch(() => null)
  ])
  pendingCards.value = unwrapOrders(pendingList)
  const pending = pendingCards.value.length
  stats.pending = pending
  notifyNewOrders(pending)
  if (st) {
    stats.todayOrders = st.todayOrders || 0
    stats.todayIncome = formatMoney(st.todayIncome)
    stats.onSale = st.onSaleGoods || 0
    stats.withdrawable = formatMoney(st.withdrawableBalance)
  }
}

async function toggleOpen(e) {
  const open = e.detail.value
  try {
    shop.value = await shopApi.update({
      openStatus: open ? 1 : 0,
      notice: shop.value.notice || ''
    })
    uni.showToast({ title: open ? '已营业' : '已休息', icon: 'none' })
  } catch (err) {
    await refresh()
  }
}

async function accept(id) {
  uni.showLoading({ title: '接单中', mask: true })
  try {
    await orderApi.accept(id)
    uni.showToast({ title: '已接单', icon: 'success' })
    await loadStats()
  } finally {
    uni.hideLoading()
  }
}

async function reject(id) {
  const ok = await confirmModal('拒单后将退款给用户，确认拒单？')
  if (!ok) return
  await orderApi.reject(id, '商家拒单')
  uni.showToast({ title: '已拒单', icon: 'none' })
  await loadStats()
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}
function goApply() {
  if (!requireLogin()) return
  uni.navigateTo({ url: '/pages/apply/index' })
}
function goDetail(id) {
  uni.navigateTo({ url: '/pages/order/detail?id=' + id })
}
function goOrders(status) {
  uni.setStorageSync('merchant_order_filter', status || '')
  uni.switchTab({ url: '/pages/order/list' })
}
function goGoods() {
  uni.switchTab({ url: '/pages/goods/list' })
}
function goShop() {
  uni.navigateTo({ url: '/pages/shop/index' })
}
function goFinance() {
  uni.navigateTo({ url: '/pages/finance/index' })
}

function startPoll() {
  stopPoll()
  ticking.value = true
  timer = setInterval(() => {
    if (shop.value) loadStats()
  }, 8000)
}
function stopPoll() {
  ticking.value = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onShow(() => {
  refresh()
  startPoll()
})
onHide(stopPoll)
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
.mini { flex: 1; text-align: center; }
.mini-num { display: block; font-size: 32rpx; font-weight: 700; }
.mini-num.accent { color: #ffd6a5; }
.mini-label {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.65);
}
.guest-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}
.mt { margin-top: 24rpx; }
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 12rpx 32rpx 0;
}
.section-title { font-size: 30rpx; font-weight: 700; }
.empty.compact { padding: 64rpx 48rpx; }

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
.income-label { display: block; font-size: 22rpx; color: var(--muted); }
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
.route-title { display: block; font-weight: 700; margin-bottom: 4rpx; }
.phone { font-weight: 500; color: var(--muted); font-size: 24rpx; }
.remark {
  margin-top: 16rpx;
  padding: 12rpx 16rpx;
  border-radius: 12rpx;
  background: var(--accent-soft);
  color: var(--warn);
  font-size: 24rpx;
}
.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid var(--line);
}
.ops { display: flex; gap: 12rpx; }
.grab {
  min-width: 140rpx;
  padding: 14rpx 0;
  font-size: 26rpx;
}
.grab.ghost { min-width: 120rpx; padding: 12rpx 0; }

.quick {
  margin: 12rpx 28rpx 40rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 0 28rpx;
  box-shadow: 0 6rpx 18rpx rgba(20, 32, 27, 0.04);
}
.quick-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid var(--line);
}
.quick-item:last-child { border-bottom: none; }
.quick-title { font-weight: 700; }
</style>
