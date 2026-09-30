<template>
  <view class="page" v-if="detail">
    <!-- 状态头 -->
    <view class="status-block">
      <view class="status-row">
        <text class="status-title">{{ statusHeadline }}</text>
        <text class="status-arrow">›</text>
      </view>
      <text class="status-desc">{{ statusDesc }}</text>
      <text class="countdown" v-if="countdownText">{{ countdownText }}</text>
      <view class="addr-line" v-if="addressDetail">
        <text class="addr-label">送至</text>
        <text class="addr-text">{{ addressDetail }}</text>
      </view>
      <text class="addr-contact" v-if="addressContact">{{ addressContact }}</text>
      <text class="poll-hint" v-if="polling">状态自动刷新中…</text>
    </view>

    <!-- 配送地图 -->
    <view class="card map-card" v-if="showMap">
      <view class="map-head">
        <text class="sec-title" style="margin:0">配送追踪</text>
        <text class="rider-call" v-if="riderPhone" @click="callRider">联系骑手</text>
      </view>
      <text class="map-tip" v-if="riderName">{{ riderName }} 配送中</text>
      <text class="map-tip muted" v-else-if="dispatchText">{{ dispatchText }}</text>
      <text class="map-tip muted" v-else-if="detail.order.status === 'PAID'">等待商家接单</text>
      <text class="map-tip muted" v-else>商家备货中，正在呼叫蜂鸟骑手</text>
      <map
        class="delivery-map"
        :latitude="mapCenter.lat"
        :longitude="mapCenter.lng"
        :scale="mapScale"
        :markers="mapMarkers"
        :show-location="false"
        enable-scroll
        enable-zoom
      />
      <view class="map-legend">
        <text class="leg">商</text><text class="leg-t">商家</text>
        <text class="leg dest">收</text><text class="leg-t">收货地</text>
        <text class="leg rider" v-if="hasRiderMarker">骑</text><text class="leg-t" v-if="hasRiderMarker">骑手</text>
      </view>
    </view>

    <!-- 快捷操作 -->
    <view class="card actions-card">
      <view class="action-grid">
        <view
          v-for="a in quickActions"
          :key="a.key"
          :class="['action-item', a.accent && 'accent']"
          @click="onAction(a.key)"
        >
          <view :class="['action-icon', a.accent && 'accent']">{{ a.icon }}</view>
          <text class="action-label">{{ a.label }}</text>
        </view>
      </view>
    </view>

    <!-- 进度（进行中） -->
    <view class="card" v-if="!isClosed && timeline.length">
      <text class="sec-title">订单进度</text>
      <view
        v-for="(step, idx) in timeline"
        :key="step.key"
        class="step"
      >
        <view class="rail">
          <view :class="['dot', step.done && 'on', step.current && 'cur']" />
          <view v-if="idx < timeline.length - 1" :class="['rail-line', step.done && 'on']" />
        </view>
        <view class="step-body">
          <text :class="['step-title', step.done && 'on']">{{ step.label }}</text>
          <text class="step-time" v-if="step.time">{{ step.time }}</text>
        </view>
      </view>
    </view>

    <!-- 店铺 + 商品 -->
    <view class="card">
      <view class="shop-row" @click="goShop">
        <text class="shop-tag">区惠</text>
        <text class="shop-name">{{ shopName || '店铺' }}</text>
        <text class="arrow">›</text>
      </view>

      <view v-for="it in detail.items" :key="it.id" class="goods-row">
        <image class="g-img" :src="itemCover(it)" mode="aspectFill" />
        <view class="g-mid">
          <text class="g-name">{{ it.goodsName }}</text>
          <text class="g-sku">{{ it.skuName || '默认规格' }}</text>
          <text class="g-qty">x{{ it.quantity }}</text>
        </view>
        <view class="g-right">
          <text class="g-pay">实付 ¥{{ linePay(it) }}</text>
          <view class="add-cart" @click.stop="addOne(it)">加入购物车</view>
        </view>
      </view>

      <view class="fee-row">
        <text class="fee-label">配送费</text>
        <view class="fee-val">
          <text class="fee-now">¥{{ detail.order.deliveryFee || 0 }}</text>
        </view>
      </view>
      <view class="fee-row">
        <text class="fee-label">打包费</text>
        <text class="fee-now">¥{{ detail.order.packingFee || 0 }}</text>
      </view>
      <view class="fee-row" v-if="Number(detail.order.discountAmount) > 0">
        <text class="fee-label">优惠券</text>
        <text class="fee-disc">-¥{{ detail.order.discountAmount }}</text>
      </view>

      <view class="price-box">
        <text class="sec-title">价格明细</text>
        <view class="price-sum">
          <text v-if="Number(detail.order.discountAmount) > 0" class="disc-sum">
            总优惠 ¥{{ detail.order.discountAmount }}
          </text>
          <text class="pay-sum">实付款 <text class="pay-num">¥{{ detail.order.payAmount }}</text></text>
        </view>
        <view class="price-detail">
          <view class="pd-row"><text>商品小计</text><text>¥{{ detail.order.goodsAmount }}</text></view>
          <view class="pd-row"><text>配送费</text><text>¥{{ detail.order.deliveryFee || 0 }}</text></view>
          <view class="pd-row"><text>打包费</text><text>¥{{ detail.order.packingFee || 0 }}</text></view>
          <view class="pd-row" v-if="Number(detail.order.discountAmount) > 0">
            <text>优惠</text><text class="fee-disc">-¥{{ detail.order.discountAmount }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="card info-card">
      <view class="info-row" v-if="addressDetail">
        <text class="info-label">收货地址</text>
        <view class="info-right">
          <text class="info-main">{{ addressDetail }}</text>
          <text class="info-sub" v-if="addressContact">{{ addressContact }}</text>
        </view>
      </view>
      <view class="info-row">
        <text class="info-label">送达时间</text>
        <text class="info-main">尽快送达</text>
      </view>
      <view class="info-row">
        <text class="info-label">配送方式</text>
        <text class="info-main">{{ deliveryText }}</text>
      </view>
      <view class="info-row" v-if="detail.order.remark">
        <text class="info-label">备注</text>
        <text class="info-main">{{ detail.order.remark }}</text>
      </view>
      <view class="info-row" v-if="detail.order.cancelReason">
        <text class="info-label">取消原因</text>
        <text class="info-main">{{ detail.order.cancelReason }}</text>
      </view>
      <view class="info-row" v-if="detail.order.refundReason">
        <text class="info-label">退款说明</text>
        <text class="info-main">{{ detail.order.refundReason }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">订单号</text>
        <view class="info-right row">
          <text class="info-main mono">{{ detail.order.orderNo }}</text>
          <text class="copy-btn" @click="copyNo">复制</text>
        </view>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-main">{{ formatTime(detail.order.createdAt) }}</text>
      </view>
    </view>

    <!-- 评价入口 -->
    <view class="card" v-if="detail.order.status === 'COMPLETED' && !detail.reviewed" @click="goReviewPage">
      <view class="review-entry">
        <text class="sec-title" style="margin:0">评价本次订单</text>
        <text class="arrow">去评价 ›</text>
      </view>
    </view>
    <view class="card" v-if="detail.review">
      <text class="sec-title">我的评价</text>
      <text class="stars-static">{{ '★'.repeat(detail.review.score) }}{{ '☆'.repeat(5 - detail.review.score) }}</text>
      <text class="muted" v-if="detail.review.content">{{ detail.review.content }}</text>
      <view class="review-photos" v-if="reviewPhotos.length">
        <image
          v-for="(url, i) in reviewPhotos"
          :key="url + i"
          class="review-photo"
          :src="url"
          mode="aspectFill"
          @click="previewReview(i)"
        />
      </view>
    </view>

    <!-- 常见问题 -->
    <view class="card faq-card">
      <text class="sec-title">常见问题</text>
      <view class="faq-chips">
        <text class="chip" @click="toastFaq">未收到商品</text>
        <text class="chip" @click="toastFaq">少送商品</text>
        <text class="chip" @click="toastFaq">商品送错</text>
      </view>
    </view>

    <view class="safe-bottom" />
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { onLoad, onShow, onUnload, onHide } from '@dcloudio/uni-app'
import { orderApi, cartApi, absUrl } from '../../api/http.js'
import { ORDER_STATUS_TEXT, ORDER_TIMELINE } from '../../utils/auth.js'
import { refreshTabBadges } from '../../utils/badge.js'
import { goodsImage } from '../../utils/image.js'
import { payOrderAuto } from '../../utils/pay.js'

const id = ref(0)
const detail = ref(null)
const polling = ref(false)
const nowTick = ref(Date.now())
let timer = null
let countdownTimer = null

function parseDeadline(v) {
  if (!v) return 0
  const t = Date.parse(String(v).replace(' ', 'T'))
  return Number.isFinite(t) ? t : 0
}

const activeDeadline = computed(() => {
  const order = detail.value?.order
  if (!order) return null
  if (order.status === 'PENDING_PAY' && order.payDeadlineAt) {
    return { label: '支付剩余', at: order.payDeadlineAt }
  }
  if (order.status === 'PAID' && order.acceptDeadlineAt) {
    return { label: '等待接单剩余', at: order.acceptDeadlineAt }
  }
  return null
})

const countdownText = computed(() => {
  const d = activeDeadline.value
  if (!d) return ''
  const end = parseDeadline(d.at)
  if (!end) return ''
  const left = Math.max(0, Math.floor((end - nowTick.value) / 1000))
  const mm = String(Math.floor(left / 60)).padStart(2, '0')
  const ss = String(left % 60).padStart(2, '0')
  if (left <= 0) return d.label + ' 即将超时处理'
  return `${d.label} ${mm}:${ss}`
})

function startCountdownTick() {
  stopCountdownTick()
  countdownTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
}
function stopCountdownTick() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const isClosed = computed(() =>
  ['CANCELLED', 'REFUNDED', 'REFUNDING', 'COMPLETED'].includes(detail.value?.order?.status)
)
const canRefund = computed(() =>
  ['ACCEPTED', 'DELIVERING'].includes(detail.value?.order?.status)
)
const shopName = computed(() => detail.value?.shop?.name || '')
const reviewPhotos = computed(() =>
  (detail.value?.review?.imageUrls || []).map((u) => absUrl(u)).filter(Boolean)
)
function previewReview(index) {
  if (!reviewPhotos.value.length) return
  uni.previewImage({ current: index, urls: reviewPhotos.value })
}
const shopPhone = computed(() => detail.value?.shop?.phone || '')
const shopId = computed(() => detail.value?.shop?.id || detail.value?.order?.shopId)

const addressObj = computed(() => {
  if (detail.value?.address) return detail.value.address
  const snap = detail.value?.order?.addressSnapshot
  if (!snap) return null
  try {
    return typeof snap === 'string' ? JSON.parse(snap) : snap
  } catch (e) {
    return null
  }
})
const rider = computed(() => detail.value?.rider || null)
const riderName = computed(() => rider.value?.name || '')
const riderPhone = computed(() => rider.value?.phone || '')

const mapMarkers = computed(() => {
  const markers = []
  let id = 1
  const shop = detail.value?.shop
  const addr = addressObj.value
  const r = rider.value
  if (shop?.lat != null && shop?.lng != null) {
    markers.push({
      id: id++,
      latitude: Number(shop.lat),
      longitude: Number(shop.lng),
      title: '商家',
      width: 24,
      height: 24,
      callout: { content: '商家', display: 'BYCLICK', padding: 6, borderRadius: 6 }
    })
  }
  if (addr?.lat != null && addr?.lng != null) {
    markers.push({
      id: id++,
      latitude: Number(addr.lat),
      longitude: Number(addr.lng),
      title: '收货地',
      width: 24,
      height: 24,
      callout: { content: '收货地', display: 'BYCLICK', padding: 6, borderRadius: 6 }
    })
  }
  if (r?.lat != null && r?.lng != null) {
    markers.push({
      id: id++,
      latitude: Number(r.lat),
      longitude: Number(r.lng),
      title: '骑手',
      width: 28,
      height: 28,
      callout: { content: riderName.value || '骑手', display: 'ALWAYS', padding: 6, borderRadius: 6 }
    })
  }
  return markers
})
const hasRiderMarker = computed(() => mapMarkers.value.some((m) => m.title === '骑手'))
const showMap = computed(() => {
  const s = detail.value?.order?.status
  return ['PAID', 'ACCEPTED', 'DELIVERING'].includes(s) && mapMarkers.value.length >= 2
})
const mapCenter = computed(() => {
  const m = mapMarkers.value
  if (!m.length) return { lat: 31.231, lng: 121.474 }
  const s = detail.value?.order?.status
  if (s === 'DELIVERING' && rider.value?.lat != null) {
    return { lat: Number(rider.value.lat), lng: Number(rider.value.lng) }
  }
  const lat = m.reduce((sum, p) => sum + p.latitude, 0) / m.length
  const lng = m.reduce((sum, p) => sum + p.longitude, 0) / m.length
  return { lat, lng }
})
const mapScale = computed(() => (detail.value?.order?.status === 'DELIVERING' ? 15 : 13))
const addressDetail = computed(() => (addressObj.value && addressObj.value.detail) || '')
const addressContact = computed(() => {
  const a = addressObj.value
  if (!a) return ''
  return `${a.contactName || ''} ${a.contactPhone || ''}`.trim()
})

const statusHeadline = computed(() => {
  const s = detail.value?.order?.status
  const map = {
    PENDING_PAY: '等待支付',
    PAID: '商家待接单',
    ACCEPTED: '商家已接单',
    DELIVERING: '骑手配送中',
    COMPLETED: '订单已送达',
    CANCELLED: '订单已取消',
    REFUNDING: '退款处理中',
    REFUNDED: '已退款'
  }
  return map[s] || ORDER_STATUS_TEXT[s] || s
})
const statusDesc = computed(() => {
  const s = detail.value?.order?.status
  const map = {
    PENDING_PAY: '请尽快完成支付，超时订单将自动取消',
    PAID: '已通知商家，超时未接单将自动退款',
    ACCEPTED: '商家已接单，正在呼叫蜂鸟骑手',
    DELIVERING: '蜂鸟骑手正在赶往收货地址',
    COMPLETED: '感谢下单，欢迎再次光临',
    CANCELLED: '订单已取消',
    REFUNDING: '商家处理中，请耐心等待',
    REFUNDED: '退款已完成（联调为 mock）'
  }
  return map[s] || ''
})
const deliveryText = computed(() => {
  const t = detail.value?.order?.deliveryType
  return t === 'SELF' ? '商家自配送' : '蜂鸟众包配送'
})
const dispatchText = computed(() => detail.value?.dispatch?.statusText || '')

const quickActions = computed(() => {
  const s = detail.value?.order?.status
  const list = []
  if (s === 'PENDING_PAY') {
    list.push({ key: 'pay', label: '去支付', icon: '付', accent: true })
    list.push({ key: 'cancel', label: '取消订单', icon: '消' })
  } else if (s === 'PAID') {
    list.push({ key: 'cancel', label: '取消订单', icon: '消' })
  } else if (canRefund.value) {
    list.push({ key: 'refund', label: '申请售后', icon: '售' })
  }
  if (s === 'COMPLETED' && !detail.value?.reviewed) {
    list.push({ key: 'review', label: '评价', icon: '评' })
  }
  list.push({ key: 'reorder', label: '再买一单', icon: '再', accent: true })
  if (['PAID', 'ACCEPTED', 'DELIVERING', 'REFUNDING', 'COMPLETED'].includes(s)) {
    list.push({ key: 'chatShop', label: '联系商家', icon: '店' })
  }
  if (s === 'DELIVERING' && (detail.value?.rider || detail.value?.dispatch?.riderPhone)) {
    list.push({ key: 'chatRider', label: '联系骑手', icon: '骑' })
  }
  list.push({ key: 'cs', label: '联系客服', icon: '服' })
  return list.slice(0, 5)
})

const timeline = computed(() => {
  const order = detail.value?.order
  if (!order) return []
  const rank = { PENDING_PAY: 0, PAID: 1, ACCEPTED: 2, DELIVERING: 3, COMPLETED: 4 }
  const cur = rank[order.status]
  if (cur == null) return []
  const logs = detail.value.logs || []
  const timeOf = (key) => {
    if (key === 'PENDING_PAY') return formatTime(order.createdAt)
    if (key === 'PAID') return formatTime(order.paidAt)
    if (key === 'ACCEPTED') return formatTime(order.acceptedAt)
    if (key === 'DELIVERING') {
      const log = logs.find((l) => l.toStatus === 'DELIVERING')
      return formatTime(log && log.createdAt)
    }
    if (key === 'COMPLETED') return formatTime(order.completedAt || order.deliveredAt)
    return ''
  }
  return ORDER_TIMELINE.map((s, i) => ({
    ...s,
    done: i <= cur,
    current: i === cur,
    time: timeOf(s.key)
  }))
})

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}
function itemCover(it) {
  return goodsImage(it.coverUrl, 'g' + (it.goodsId || it.id || 'x'))
}
function linePay(it) {
  return (Number(it.price) * Number(it.quantity || 1)).toFixed(2)
}

async function load() {
  detail.value = await orderApi.detail(id.value)
  syncPoll()
}
function syncPoll() {
  stopPoll()
  const s = detail.value?.order?.status
  const active = ['PENDING_PAY', 'PAID', 'ACCEPTED', 'DELIVERING', 'REFUNDING'].includes(s)
  polling.value = active
  if (!active) return
  timer = setInterval(async () => {
    try {
      detail.value = await orderApi.detail(id.value)
      const ns = detail.value?.order?.status
      if (!['PENDING_PAY', 'PAID', 'ACCEPTED', 'DELIVERING', 'REFUNDING'].includes(ns)) {
        stopPoll()
        refreshTabBadges()
      }
    } catch (e) { /* ignore */ }
  }, 5000)
}
function stopPoll() {
  polling.value = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function callRider() {
  if (!riderPhone.value) return
  uni.makePhoneCall({ phoneNumber: String(riderPhone.value) })
}

function copyNo() {
  const no = detail.value?.order?.orderNo
  if (!no) return
  uni.setClipboardData({
    data: String(no),
    success: () => uni.showToast({ title: '已复制', icon: 'none' })
  })
}
function goShop() {
  if (!shopId.value) return
  uni.navigateTo({ url: '/pages/shop/detail?id=' + shopId.value })
}
function toastFaq() {
  uni.navigateTo({ url: '/pages/mine/service' })
}

function goReviewPage() {
  uni.navigateTo({ url: '/pages/order/review?id=' + id.value })
}
function goRefundPage() {
  uni.navigateTo({ url: '/pages/order/refund?id=' + id.value })
}

function onAction(key) {
  if (key === 'pay') return pay()
  if (key === 'cancel') return cancel()
  if (key === 'refund') return goRefundPage()
  if (key === 'reorder') return reorder()
  if (key === 'review') return goReviewPage()
  if (key === 'shop') {
    if (shopPhone.value) {
      uni.makePhoneCall({ phoneNumber: String(shopPhone.value) })
    } else {
      goShop()
    }
    return
  }
  if (key === 'chatShop') {
    uni.navigateTo({ url: `/pages/im/chat?orderId=${id.value}&type=USER_MERCHANT` })
    return
  }
  if (key === 'chatRider') {
    const phone = detail.value?.rider?.phone || detail.value?.dispatch?.riderPhone
    if (phone) {
      uni.makePhoneCall({ phoneNumber: String(phone) })
      return
    }
    uni.showToast({ title: '暂无骑手电话', icon: 'none' })
    return
  }
  if (key === 'cs') toastFaq()
}

async function addOne(it) {
  if (!it.skuId) {
    uni.showToast({ title: '无法加购', icon: 'none' })
    return
  }
  try {
    await cartApi.add(it.skuId, 1)
    await refreshTabBadges()
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } catch (e) { /* http toast */ }
}

onLoad(async (q) => {
  id.value = Number(q.id)
  await load()
  startCountdownTick()
})
onShow(() => {
  refreshTabBadges()
  if (detail.value) syncPoll()
  startCountdownTick()
})
onHide(() => {
  stopPoll()
  stopCountdownTick()
})
onUnload(() => {
  stopPoll()
  stopCountdownTick()
})
onUnmounted(() => {
  stopPoll()
  stopCountdownTick()
})

async function pay() {
  try {
    uni.showLoading({ title: '支付中' })
    await payOrderAuto(id.value)
    uni.hideLoading()
    detail.value = await orderApi.detail(id.value)
    uni.showToast({ title: '支付成功', icon: 'success' })
    refreshTabBadges()
    syncPoll()
  } catch (e) {
    uni.hideLoading()
  }
}
function cancel() {
  uni.showModal({
    title: '取消订单',
    content: '确定取消该订单吗？',
    success: async (res) => {
      if (!res.confirm) return
      detail.value = await orderApi.cancel(id.value, '用户取消')
      uni.showToast({ title: '已取消', icon: 'none' })
      refreshTabBadges()
      stopPoll()
    }
  })
}
async function reorder() {
  uni.showLoading({ title: '加入购物车' })
  try {
    const res = await orderApi.reorder(id.value)
    uni.hideLoading()
    await refreshTabBadges()
    uni.showToast({ title: '已加入购物车', icon: 'success' })
    setTimeout(() => {
      if (res?.shopId) {
        uni.navigateTo({ url: '/pages/shop/detail?id=' + res.shopId })
      }
    }, 400)
  } catch (e) {
    uni.hideLoading()
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}
.status-block {
  background: #fff;
  padding: 32rpx 28rpx 28rpx;
}
.status-row { display: flex; align-items: center; gap: 4rpx; }
.status-title { font-size: 40rpx; font-weight: 800; color: #222; }
.status-arrow { font-size: 36rpx; color: #999; }
.status-desc { display: block; margin-top: 10rpx; color: #666; font-size: 26rpx; }
.countdown {
  display: inline-block;
  margin-top: 14rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  background: rgba(232, 93, 4, 0.12);
  color: #e85d04;
  font-size: 24rpx;
  font-weight: 600;
}
.addr-line {
  margin-top: 20rpx; display: flex; gap: 12rpx; align-items: flex-start;
}
.addr-label {
  flex-shrink: 0; font-size: 22rpx; color: #999; background: #f3f3f3;
  padding: 2rpx 10rpx; border-radius: 6rpx; margin-top: 2rpx;
}
.addr-text { flex: 1; font-size: 26rpx; color: #333; line-height: 1.4; }
.addr-contact { display: block; margin-top: 8rpx; font-size: 24rpx; color: #999; padding-left: 72rpx; }
.poll-hint { display: block; margin-top: 12rpx; font-size: 22rpx; color: #e85d04; }

.map-card { padding-bottom: 16rpx; }
.map-head {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx;
}
.rider-call { font-size: 24rpx; color: #2563eb; font-weight: 600; }
.map-tip { display: block; font-size: 24rpx; color: #333; margin-bottom: 12rpx; }
.delivery-map {
  width: 100%; height: 360rpx; border-radius: 16rpx; overflow: hidden;
}
.map-legend {
  display: flex; align-items: center; gap: 8rpx; margin-top: 12rpx; flex-wrap: wrap;
}
.leg {
  width: 28rpx; height: 28rpx; line-height: 28rpx; text-align: center;
  border-radius: 8rpx; font-size: 18rpx; font-weight: 700; color: #fff;
  background: #0f3d2e;
}
.leg.dest { background: #e85d04; }
.leg.rider { background: #2563eb; }
.leg-t { font-size: 22rpx; color: #666; margin-right: 16rpx; }

.card {
  background: #fff;
  margin: 16rpx 20rpx;
  border-radius: 20rpx;
  padding: 24rpx 24rpx 20rpx;
}
.actions-card { padding-bottom: 8rpx; }
.action-grid {
  display: flex; flex-wrap: wrap;
}
.action-item {
  width: 20%;
  display: flex; flex-direction: column; align-items: center;
  padding: 12rpx 0 20rpx;
}
.action-icon {
  width: 64rpx; height: 64rpx; border-radius: 50%;
  background: #f3f6f4; color: #33453d;
  display: flex; align-items: center; justify-content: center;
  font-size: 26rpx; font-weight: 700;
}
.action-icon.accent { background: #fff7ed; color: #e85d04; }
.action-label { margin-top: 10rpx; font-size: 22rpx; color: #333; }
.action-item.accent .action-label { color: #e85d04; font-weight: 700; }

.sec-title { display: block; font-size: 28rpx; font-weight: 700; color: #222; margin-bottom: 16rpx; }
.step { display: flex; gap: 16rpx; min-height: 64rpx; }
.rail { width: 24rpx; display: flex; flex-direction: column; align-items: center; }
.dot {
  width: 16rpx; height: 16rpx; border-radius: 50%; background: #ddd; margin-top: 8rpx;
}
.dot.on { background: #0f3d2e; }
.dot.cur { background: #e85d04; box-shadow: 0 0 0 6rpx rgba(232, 93, 4, 0.18); }
.rail-line { width: 4rpx; flex: 1; background: #eee; margin: 4rpx 0; }
.rail-line.on { background: #0f3d2e; }
.step-title { display: block; font-size: 26rpx; color: #999; }
.step-title.on { color: #222; font-weight: 700; }
.step-time { display: block; font-size: 20rpx; color: #bbb; margin-top: 2rpx; }

.shop-row {
  display: flex; align-items: center; gap: 10rpx; margin-bottom: 20rpx;
}
.shop-tag {
  font-size: 20rpx; color: #e85d04; border: 1rpx solid #fdba74;
  border-radius: 6rpx; padding: 0 8rpx;
}
.shop-name { font-size: 28rpx; font-weight: 700; color: #222; flex: 1; }
.arrow { color: #ccc; font-size: 28rpx; }

.goods-row {
  display: flex; gap: 16rpx; padding: 16rpx 0;
  border-bottom: 1rpx solid #f3f3f3;
}
.g-img {
  width: 120rpx; height: 120rpx; border-radius: 12rpx; background: #f3f3f3; flex-shrink: 0;
}
.g-mid { flex: 1; min-width: 0; }
.g-name {
  display: block; font-size: 28rpx; font-weight: 700; color: #222;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.g-sku { display: block; margin-top: 8rpx; font-size: 22rpx; color: #999; }
.g-qty { display: block; margin-top: 10rpx; font-size: 24rpx; color: #666; }
.g-right { flex-shrink: 0; width: 180rpx; text-align: right; }
.g-pay { display: block; font-size: 26rpx; font-weight: 700; color: #222; }
.add-cart {
  display: inline-block; margin-top: 16rpx; padding: 8rpx 16rpx;
  border: 1rpx solid #ddd; border-radius: 24rpx; font-size: 22rpx; color: #333;
}

.fee-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14rpx 0; font-size: 26rpx;
}
.fee-label { color: #666; }
.fee-now { color: #222; font-weight: 600; }
.fee-disc { color: #e11d48; font-weight: 600; }

.price-box { margin-top: 8rpx; padding-top: 8rpx; border-top: 1rpx solid #f3f3f3; }
.price-sum {
  display: flex; justify-content: flex-end; align-items: baseline; gap: 20rpx; margin-bottom: 12rpx;
}
.disc-sum { color: #e11d48; font-size: 24rpx; }
.pay-sum { font-size: 26rpx; color: #333; }
.pay-num { font-size: 34rpx; font-weight: 800; color: #222; }
.price-detail {
  background: #f7f7f7; border-radius: 12rpx; padding: 16rpx 20rpx;
}
.pd-row {
  display: flex; justify-content: space-between; padding: 8rpx 0;
  font-size: 24rpx; color: #666;
}

.info-card { padding-top: 8rpx; }
.info-row {
  display: flex; gap: 20rpx; padding: 18rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}
.info-row:last-child { border-bottom: none; }
.info-label { width: 140rpx; flex-shrink: 0; color: #999; font-size: 26rpx; }
.info-right { flex: 1; min-width: 0; }
.info-right.row { display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.info-main { display: block; text-align: right; color: #333; font-size: 26rpx; line-height: 1.4; }
.info-sub { display: block; text-align: right; color: #999; font-size: 24rpx; margin-top: 6rpx; }
.mono { font-size: 24rpx; }
.copy-btn { color: #2563eb; font-size: 24rpx; flex-shrink: 0; }

.stars-static { display: block; color: #e85d04; margin-bottom: 8rpx; }
.review-photos { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 12rpx; }
.review-photo { width: 160rpx; height: 160rpx; border-radius: 10rpx; background: #f3f3f3; }
.review-entry {
  display: flex; justify-content: space-between; align-items: center;
}
.muted { color: #999; font-size: 24rpx; }

.faq-chips { display: flex; flex-wrap: wrap; gap: 16rpx; }
.chip {
  padding: 12rpx 22rpx; border-radius: 28rpx; border: 1rpx solid #e5e5e5;
  font-size: 24rpx; color: #333; background: #fff;
}
.safe-bottom { height: 40rpx; }
</style>
