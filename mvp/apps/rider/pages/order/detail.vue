<template>
  <view class="page" v-if="detail">
    <view class="status-card">
      <text class="status">{{ phaseText(detail.phase) }}</text>
      <text class="muted">订单号 {{ detail.order.orderNo }}</text>
      <view class="status-row">
        <text class="income">收入 ¥{{ formatMoney(detail.order.deliveryFee) }}</text>
        <text class="muted" v-if="detail.distanceMeters != null">
          配送距 {{ formatDistance(detail.distanceMeters) }}
        </text>
      </view>
    </view>

    <view class="card map-card" v-if="showMap">
      <view class="map-head">
        <text class="title" style="margin:0">配送地图</text>
        <text class="muted">店→客{{ meLat != null ? '→我' : '' }}</text>
      </view>
      <map
        class="delivery-map"
        :latitude="mapCenter.lat"
        :longitude="mapCenter.lng"
        :scale="14"
        :markers="mapMarkers"
        :polyline="mapLines"
        show-location
        enable-scroll
        enable-zoom
      />
      <view class="map-legend">
        <text class="leg shop">取</text><text class="leg-t">取餐</text>
        <text class="leg user">送</text><text class="leg-t">送达</text>
      </view>
    </view>

    <view class="card route-card">
      <view class="route-row">
        <view class="pin shop" />
        <view class="route-body">
          <view class="row">
            <text class="route-title">取餐 · {{ shop.name || '商家' }}</text>
            <view class="links">
              <text class="link" @click="navShop">导航</text>
              <text class="link" @click="callPhone(shop.phone)">打电话</text>
            </view>
          </view>
          <text class="muted" @longpress="copyText(shop.address)">{{ shop.address || '暂无地址' }}</text>
        </view>
      </view>
      <view class="route-line" />
      <view class="route-row">
        <view class="pin user" />
        <view class="route-body">
          <view class="row">
            <text class="route-title">
              送达 · {{ address.contactName || '顾客' }}
            </text>
            <view class="links">
              <text class="link" @click="navUser">导航</text>
              <text class="link" @click="callPhone(address.contactPhone)">打电话</text>
            </view>
          </view>
          <text class="muted" @longpress="copyText(address.detail)">
            {{ address.contactPhone || '' }} {{ address.detail || '' }}
          </text>
        </view>
      </view>
    </view>

    <view class="card" v-if="detail.order.remark">
      <text class="title">备注</text>
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
    </view>

    <view class="card" v-if="detail.logs && detail.logs.length">
      <text class="title">配送进度</text>
      <view v-for="(log, idx) in detail.logs" :key="log.id" class="log">
        <view :class="['log-dot', idx === detail.logs.length - 1 && 'on']" />
        <view class="log-body">
          <text class="log-text">{{ log.remark || log.toStatus }}</text>
          <text class="muted">{{ log.createdAt || '' }}</text>
        </view>
      </view>
    </view>

    <view class="safe-bottom" />

    <view class="action-bar" v-if="canGrab || detail.phase === 'WAIT_PICKUP' || detail.phase === 'ON_WAY'">
      <view v-if="canGrab" class="btn-accent bar-btn" @click="grab">立即抢单</view>
      <template v-else-if="detail.phase === 'WAIT_PICKUP'">
        <view class="btn-ghost bar-third" @click="goChatUser">发消息</view>
        <view class="btn-ghost bar-third" @click="callPhone(shop.phone)">打电话</view>
        <view class="btn-accent bar-third" @click="pickup">确认取餐</view>
      </template>
      <template v-else-if="detail.phase === 'ON_WAY'">
        <view class="btn-ghost bar-third" @click="goChatUser">发消息</view>
        <view class="btn-ghost bar-third" @click="callPhone(address.contactPhone)">打电话</view>
        <view class="btn-accent bar-third" @click="deliver">确认送达</view>
      </template>
    </view>
  </view>

  <view v-else-if="loading" class="page">
    <view class="skeleton" v-for="i in 4" :key="i" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow, onHide } from '@dcloudio/uni-app'
import { riderApi } from '../../api/http.js'
import { PHASE_TEXT, callPhone, copyText } from '../../utils/auth.js'
import { formatDistance, formatMoney, toCoord } from '../../utils/format.js'
import { openNav, alertOrderCancelled } from '../../utils/nav.js'

const id = ref(0)
const detail = ref(null)
const loading = ref(true)
const meLat = ref(null)
const meLng = ref(null)
let pollTimer = null

const shop = computed(() => detail.value?.shop || {})
const address = computed(() => detail.value?.address || {})
const canGrab = computed(() => detail.value?.phase === 'POOL')
const isActive = computed(() =>
  detail.value && ['WAIT_PICKUP', 'ON_WAY', 'POOL'].includes(detail.value.phase)
)

const mapMarkers = computed(() => {
  const markers = []
  let mid = 1
  const s = shop.value
  const a = address.value
  if (toCoord(s.lat) != null && toCoord(s.lng) != null) {
    markers.push({
      id: mid++,
      latitude: toCoord(s.lat),
      longitude: toCoord(s.lng),
      title: '取餐',
      width: 28,
      height: 28,
      callout: { content: s.name || '取餐点', display: 'BYCLICK', padding: 6, borderRadius: 6 }
    })
  }
  if (toCoord(a.lat) != null && toCoord(a.lng) != null) {
    markers.push({
      id: mid++,
      latitude: toCoord(a.lat),
      longitude: toCoord(a.lng),
      title: '送达',
      width: 28,
      height: 28,
      callout: { content: a.detail || '送达点', display: 'BYCLICK', padding: 6, borderRadius: 6 }
    })
  }
  if (meLat.value != null && meLng.value != null) {
    markers.push({
      id: mid++,
      latitude: meLat.value,
      longitude: meLng.value,
      title: '我',
      width: 26,
      height: 26,
      callout: { content: '我的位置', display: 'ALWAYS', padding: 6, borderRadius: 6 }
    })
  }
  return markers
})

const showMap = computed(() => mapMarkers.value.length >= 2)

const mapCenter = computed(() => {
  const m = mapMarkers.value
  if (!m.length) return { lat: 31.231, lng: 121.474 }
  if (meLat.value != null) return { lat: meLat.value, lng: meLng.value }
  const lat = m.reduce((sum, p) => sum + p.latitude, 0) / m.length
  const lng = m.reduce((sum, p) => sum + p.longitude, 0) / m.length
  return { lat, lng }
})

const mapLines = computed(() => {
  const pts = []
  const s = shop.value
  const a = address.value
  if (toCoord(s.lat) != null) pts.push({ latitude: toCoord(s.lat), longitude: toCoord(s.lng) })
  if (toCoord(a.lat) != null) pts.push({ latitude: toCoord(a.lat), longitude: toCoord(a.lng) })
  if (pts.length < 2) return []
  return [{ points: pts, color: '#e85d04', width: 4, dottedLine: true }]
})

function phaseText(p) {
  return PHASE_TEXT[p] || p
}

function goChatUser() {
  uni.navigateTo({ url: `/pages/im/chat?orderId=${id.value}&type=USER_RIDER&role=RIDER` })
}

function navShop() {
  openNav({
    lat: shop.value.lat,
    lng: shop.value.lng,
    name: shop.value.name || '取餐点',
    address: shop.value.address || ''
  })
}

function navUser() {
  openNav({
    lat: address.value.lat,
    lng: address.value.lng,
    name: address.value.contactName || '送达点',
    address: address.value.detail || ''
  })
}

async function load() {
  loading.value = true
  try {
    detail.value = await riderApi.detail(id.value)
    if (detail.value?.phase === 'CANCELLED') {
      alertOrderCancelled(1)
    }
  } finally {
    loading.value = false
  }
}

async function refreshLoc() {
  try {
    const p = await riderApi.profile()
    if (p?.lat != null) {
      meLat.value = Number(p.lat)
      meLng.value = Number(p.lng)
    }
  } catch (e) { /* ignore */ }
  uni.getLocation({
    type: 'wgs84',
    success: (res) => {
      meLat.value = res.latitude
      meLng.value = res.longitude
      riderApi.reportLocation(res.latitude, res.longitude).catch(() => {})
    }
  })
}

function startPoll() {
  stopPoll()
  if (!isActive.value) return
  pollTimer = setInterval(async () => {
    try {
      const d = await riderApi.detail(id.value)
      const prev = detail.value?.phase
      detail.value = d
      if (d?.phase === 'CANCELLED' && prev !== 'CANCELLED') {
        alertOrderCancelled(1)
        stopPoll()
      }
    } catch (e) { /* ignore */ }
  }, 10000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onLoad(async (q) => {
  id.value = Number(q.id)
  await load()
  await refreshLoc()
})

onShow(() => {
  if (id.value) startPoll()
})
onHide(stopPoll)

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

async function grab() {
  uni.showLoading({ title: '抢单中', mask: true })
  try {
    detail.value = await riderApi.grab(id.value)
    uni.showToast({ title: '抢单成功', icon: 'success' })
    startPoll()
  } finally {
    uni.hideLoading()
  }
}

async function pickup() {
  const ok = await confirm('确认已到店取餐？')
  if (!ok) return
  detail.value = await riderApi.pickup(id.value)
  uni.showToast({ title: '已取餐', icon: 'none' })
}

async function deliver() {
  const ok = await confirm('确认已送达顾客？')
  if (!ok) return
  detail.value = await riderApi.deliver(id.value)
  uni.showToast({ title: '配送完成', icon: 'success' })
  stopPoll()
}
</script>

<style scoped>
.status-card {
  margin: 0 0 8rpx;
  padding: 36rpx 32rpx 28rpx;
  background: linear-gradient(160deg, #123d2f 0%, #1b5e45 100%);
  color: #fff;
}
.status {
  display: block;
  font-size: 40rpx;
  font-weight: 800;
  margin-bottom: 8rpx;
}
.status-card .muted { color: rgba(255, 255, 255, 0.7); }
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
.map-card { padding-bottom: 20rpx; }
.map-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.delivery-map {
  width: 100%;
  height: 360rpx;
  border-radius: 16rpx;
  overflow: hidden;
}
.map-legend {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 12rpx;
}
.leg {
  width: 36rpx;
  height: 36rpx;
  border-radius: 10rpx;
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
  text-align: center;
  line-height: 36rpx;
}
.leg.shop { background: var(--brand-soft); }
.leg.user { background: var(--accent); }
.leg-t { font-size: 22rpx; color: var(--muted); margin-right: 12rpx; }
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
.links { display: flex; gap: 20rpx; }
.title {
  display: block;
  font-weight: 700;
  margin-bottom: 12rpx;
}
.remark {
  color: var(--warn);
  line-height: 1.5;
}
.line {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  border-top: 1rpx solid var(--line);
}
.goods { flex: 1; padding-right: 16rpx; }
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
