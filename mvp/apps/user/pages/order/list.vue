<template>
  <view class="page">
    <view class="tab-bar">
      <view
        v-for="t in tabs"
        :key="t.key"
        :class="['tab-item', tabHighlight(t.key) && 'on']"
        @click="filter = t.key"
      >
        <text class="tab-text">{{ t.label }}</text>
        <view v-if="tabCount(t.key) > 0" class="tab-badge">{{ tabCount(t.key) > 99 ? '99+' : tabCount(t.key) }}</view>
        <view v-if="tabHighlight(t.key)" class="tab-line" />
      </view>
    </view>

    <view class="search-row">
      <input
        class="order-search"
        v-model="keyword"
        confirm-type="search"
        placeholder="搜索订单号 / 店铺名"
        @confirm="noop"
      />
    </view>

    <view v-if="loading">
      <view class="skeleton" v-for="i in 3" :key="i" />
    </view>

    <view v-else-if="!filtered.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">暂无{{ currentTabLabel }}订单</text>
      <text class="muted">去附近好店下单试试吧</text>
      <view class="btn-primary go-home" @click="goHome">去点餐</view>
    </view>

    <view v-else class="list">
      <view
        v-for="card in filtered"
        :key="card.order.id"
        class="order-card"
        @click="goDetail(card.order.id)"
      >
        <!-- 顶栏：店标 + 店名 + 状态 -->
        <view class="shop-head">
          <view class="shop-left" @click.stop="goShop(card.shopId)">
            <image class="shop-logo" :src="shopIcon(card)" mode="aspectFill" />
            <text class="shop-name">{{ card.shopName || '店铺' }}</text>
            <text class="arrow">›</text>
          </view>
          <text :class="['status', statusClass(card.order.status)]">{{ statusText(card.order.status) }}</text>
        </view>

        <!-- 中部：商品图横滑 + 价格件数 -->
        <view class="goods-row">
          <scroll-view scroll-x class="goods-scroll" :show-scrollbar="false">
            <view class="goods-track">
              <image
                v-for="(url, idx) in goodsCovers(card)"
                :key="idx"
                class="goods-img"
                :src="url"
                mode="aspectFill"
              />
            </view>
          </scroll-view>
          <view class="price-box">
            <view class="price-line">
              <text class="yen">¥</text>
              <text class="money">{{ card.order.payAmount }}</text>
            </view>
            <text class="count">共{{ card.itemCount || 0 }}件</text>
          </view>
        </view>

        <!-- 底栏操作 -->
        <view class="ops" @click.stop>
          <template v-if="card.order.status === 'PENDING_PAY'">
            <view class="op ghost" @click="cancel(card.order.id)">取消订单</view>
            <view class="op primary" @click="pay(card.order.id)">去支付</view>
          </template>
          <template v-else-if="card.order.status === 'PAID'">
            <view class="op ghost" @click="cancel(card.order.id)">取消订单</view>
            <view class="op ghost" @click="goDetail(card.order.id)">查看详情</view>
          </template>
          <template v-else-if="card.order.status === 'ACCEPTED' || card.order.status === 'DELIVERING'">
            <view class="op ghost" @click="goRefund(card.order.id)">申请退款</view>
            <view class="op ghost" @click="goDetail(card.order.id)">查看详情</view>
          </template>
          <template v-else-if="card.order.status === 'REFUNDING'">
            <view class="op ghost" @click="goDetail(card.order.id)">退款进度</view>
          </template>
          <template v-else-if="card.order.status === 'COMPLETED'">
            <view v-if="!card.reviewed" class="op ghost" @click="goReview(card.order.id)">评价</view>
            <view class="op accent" @click="reorder(card.order.id)">再买一单</view>
          </template>
          <template v-else>
            <view class="op accent" @click="reorder(card.order.id)">再买一单</view>
          </template>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { orderApi } from '../../api/http.js'
import { requireLogin, ORDER_STATUS_TEXT } from '../../utils/auth.js'
import { refreshTabBadges } from '../../utils/badge.js'
import { goodsImage, shopImage } from '../../utils/image.js'
import { payOrderAuto } from '../../utils/pay.js'

const orders = ref([])
const loading = ref(false)
const filter = ref('all')
const keyword = ref('')

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'active', label: '进行中' },
  { key: 'waitReview', label: '待评价' },
  { key: 'afterSales', label: '退款' }
]

const currentTabLabel = computed(() => {
  const t = tabs.find((x) => x.key === filter.value)
  if (t) return t.label
  if (filter.value === 'PENDING_PAY') return '待付款'
  if (filter.value === 'COMPLETED') return '已完成'
  return ''
})

function matchTab(card, key) {
  const s = card.order?.status
  if (key === 'all') return true
  if (key === 'PENDING_PAY') return s === 'PENDING_PAY'
  if (key === 'COMPLETED') return s === 'COMPLETED'
  if (key === 'active') return ['PENDING_PAY', 'PAID', 'ACCEPTED', 'DELIVERING'].includes(s)
  if (key === 'waitReview') return s === 'COMPLETED' && !card.reviewed
  if (key === 'afterSales') return ['REFUNDING', 'REFUNDED'].includes(s)
  return false
}

/** 我的页快捷入口可能带 PENDING_PAY；进行中 Tab 高亮即可 */
function tabHighlight(key) {
  if (filter.value === key) return true
  if (filter.value === 'PENDING_PAY' && key === 'active') return true
  return false
}

const filtered = computed(() => {
  const kw = keyword.value.trim()
  return orders.value.filter((c) => {
    if (!matchTab(c, filter.value)) return false
    if (!kw) return true
    const no = String(c.order?.orderNo || '')
    const name = String(c.shopName || '')
    return no.includes(kw) || name.includes(kw)
  })
})

function noop() {}

function tabCount(key) {
  if (key === 'all') return 0
  return orders.value.filter((c) => matchTab(c, key)).length
}

function statusText(s) {
  const map = {
    ...ORDER_STATUS_TEXT,
    COMPLETED: '已送达',
    DELIVERING: '配送中',
    ACCEPTED: '商家已接单',
    PAID: '待接单',
    PENDING_PAY: '待付款'
  }
  return map[s] || s
}
function statusClass(s) {
  if (s === 'COMPLETED') return 'done'
  if (s === 'CANCELLED' || s === 'REFUNDED') return 'bad'
  if (s === 'REFUNDING') return 'warn'
  return 'hot'
}

function shopIcon(card) {
  return shopImage(card.shopLogoUrl, card.shopId || card.shopName || 'x')
}

/** 商品封面横排；无封面时按商品摘要生成占位 */
function goodsCovers(card) {
  const covers = (card.goodsCoverUrls || []).filter(Boolean).map((u) => goodsImage(u, 'g'))
  if (covers.length) return covers.slice(0, 6)
  // 兜底：至少 1 张，按订单/店铺生成
  const n = Math.min(Math.max(card.itemCount || 1, 1), 3)
  const list = []
  for (let i = 0; i < n; i++) {
    list.push(goodsImage('', `ogi${card.order?.id || 0}_${i}`))
  }
  return list
}

function unwrap(list) {
  return (list || []).map((row) => {
    if (row && row.order) {
      return { ...row, goodsCoverUrls: row.goodsCoverUrls || [] }
    }
    return {
      order: row,
      shopName: '店铺',
      shopId: row && row.shopId,
      shopLogoUrl: '',
      goodsCoverUrls: [],
      itemCount: 0,
      itemSummary: '查看商品明细',
      reviewed: false
    }
  })
}

async function load() {
  if (!requireLogin()) {
    uni.stopPullDownRefresh()
    return
  }
  loading.value = true
  try {
    orders.value = unwrap(await orderApi.list())
    refreshTabBadges()
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function goDetail(id) {
  uni.navigateTo({ url: '/pages/order/detail?id=' + id })
}
function goRefund(id) {
  uni.navigateTo({ url: '/pages/order/refund?id=' + id })
}
function goReview(id) {
  uni.navigateTo({ url: '/pages/order/review?id=' + id })
}

async function pay(id) {
  try {
    uni.showLoading({ title: '支付中' })
    await payOrderAuto(id)
    uni.hideLoading()
    uni.showToast({ title: '支付成功', icon: 'success' })
    await load()
  } catch (e) {
    uni.hideLoading()
    if (e && e.message !== 'cancel') { /* http toast */ }
  }
}
function goShop(shopId) {
  if (!shopId) return
  uni.navigateTo({ url: '/pages/shop/detail?id=' + shopId })
}
function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

async function reorder(id) {
  const res = await orderApi.reorder(id)
  await refreshTabBadges()
  uni.showToast({ title: '已加入购物车', icon: 'success' })
  setTimeout(() => {
    if (res?.shopId) {
      uni.navigateTo({ url: '/pages/shop/detail?id=' + res.shopId })
    }
  }, 400)
}

function cancel(id) {
  uni.showModal({
    title: '取消订单',
    content: '确定取消该订单吗？',
    success: async (res) => {
      if (!res.confirm) return
      await orderApi.cancel(id, '用户取消')
      uni.showToast({ title: '已取消', icon: 'none' })
      await load()
    }
  })
}

onShow(() => {
  const pending = uni.getStorageSync('orderListFilter')
  if (pending) {
    filter.value = String(pending)
    uni.removeStorageSync('orderListFilter')
  }
  load()
})
onPullDownRefresh(load)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}
.tab-bar {
  display: flex;
  background: #fff;
  height: 88rpx;
  position: sticky;
  top: 0;
  z-index: 5;
}
.tab-item {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tab-text { font-size: 28rpx; color: #666; }
.tab-item.on .tab-text { color: #e85d04; font-weight: 700; }
.tab-line {
  position: absolute; left: 50%; bottom: 0;
  width: 40rpx; height: 6rpx; margin-left: -20rpx;
  border-radius: 6rpx; background: #e85d04;
}
.tab-badge {
  position: absolute; top: 12rpx; right: 18rpx;
  min-width: 28rpx; height: 28rpx; padding: 0 6rpx;
  border-radius: 14rpx; background: #ff4d4f; color: #fff;
  font-size: 18rpx; line-height: 28rpx; text-align: center;
}

.search-row {
  padding: 12rpx 20rpx 4rpx;
  background: #fff;
}
.order-search {
  background: #f5f5f5; border-radius: 28rpx; padding: 14rpx 24rpx; font-size: 24rpx;
}

.list { padding: 12rpx 0 24rpx; }
.order-card {
  margin: 16rpx 20rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx 24rpx 20rpx;
}

.shop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.shop-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-width: 0;
  flex: 1;
}
.shop-logo {
  width: 44rpx;
  height: 44rpx;
  border-radius: 10rpx;
  background: #f0f0f0;
  flex-shrink: 0;
}
.shop-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.arrow { color: #ccc; font-size: 28rpx; flex-shrink: 0; }
.status {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #999;
}
.status.hot { color: #e85d04; font-weight: 600; }
.status.warn { color: #c2410c; font-weight: 600; }
.status.bad { color: #d62828; }
.status.done { color: #999; }

.goods-row {
  margin-top: 20rpx;
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}
.goods-scroll {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
}
.goods-track {
  display: inline-flex;
  gap: 12rpx;
}
.goods-img {
  width: 128rpx;
  height: 128rpx;
  border-radius: 12rpx;
  background: #f3f3f3;
}
.price-box {
  flex-shrink: 0;
  width: 150rpx;
  text-align: right;
  padding-top: 8rpx;
}
.price-line { color: #222; }
.yen { font-size: 22rpx; font-weight: 700; }
.money { font-size: 32rpx; font-weight: 800; }
.count {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #999;
}

.ops {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 20rpx;
  padding-top: 4rpx;
}
.op {
  min-width: 140rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  border-radius: 28rpx;
  font-size: 24rpx;
  font-weight: 600;
  padding: 0 8rpx;
}
.op.ghost {
  color: #333;
  border: 1rpx solid #ddd;
  background: #fff;
}
.op.primary {
  color: #fff;
  background: #0f3d2e;
  border: 1rpx solid #0f3d2e;
}
.op.accent {
  color: #9a3412;
  background: #fff7ed;
  border: 1rpx solid #fdba74;
}
.go-home { margin: 40rpx 80rpx 0; }
</style>
