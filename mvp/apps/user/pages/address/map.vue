<template>
  <view class="page">
    <map
      v-if="mapReady"
      id="pickMap"
      class="map"
      :latitude="lat"
      :longitude="lng"
      :scale="scale"
      show-location
      enable-scroll
      enable-zoom
      @regionchange="onRegionChange"
    />
    <view v-if="mapReady" class="center-pin" />

    <view v-else class="fallback">
      <text class="fallback-title">{{ fallbackTitle }}</text>
      <text class="fallback-desc">{{ fallbackDesc }}</text>
      <view class="coord-box">
        <text class="coord-label">经纬度</text>
        <text class="coord-val">{{ lat }}, {{ lng }}</text>
      </view>
      <view class="btn-primary loc-btn" @click="useCurrent">定位到当前位置</view>
      <text class="key-hint" v-if="keyHint">{{ keyHint }}</text>
    </view>

    <view class="panel">
      <input
        v-model="detail"
        class="addr-input"
        placeholder="详细地址（小区/楼栋/门牌号）"
        confirm-type="done"
      />
      <view class="panel-actions">
        <view class="ghost-btn" @click="useCurrent">重新定位</view>
        <view class="btn-accent confirm-btn" @click="confirm">确认位置</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onReady } from '@dcloudio/uni-app'
import { isH5MapReady, isAppMapReady } from '../../config/map.js'

const lat = ref(31.231)
const lng = ref(121.474)
const scale = ref(16)
const detail = ref('')
const mapReady = ref(true)
const fallbackTitle = ref('')
const fallbackDesc = ref('')
const keyHint = ref('')
let eventChannel = null
let mapCtx = null

// #ifdef MP-WEIXIN
mapReady.value = true
// #endif
// #ifdef H5
mapReady.value = isH5MapReady()
fallbackTitle.value = '浏览器预览模式'
fallbackDesc.value = '未配置腾讯地图 Key，地图瓦片无法加载。可先定位并填写详细地址。'
keyHint.value = '配置 config/map.js 的 TENCENT_MAP_KEY 后可显示地图'
// #endif
// #ifdef APP-PLUS
mapReady.value = isAppMapReady()
fallbackTitle.value = 'App 地图未配置'
fallbackDesc.value = '未配置高德地图 Key，无法加载地图。可先定位获取坐标并填写详细地址。'
keyHint.value = '在 config/map.js 填写 AMAP_KEY_ANDROID / AMAP_KEY_IOS，并同步 manifest.json'
// #endif

onLoad((q) => {
  if (q.lat) lat.value = Number(q.lat)
  if (q.lng) lng.value = Number(q.lng)
  if (q.detail) detail.value = decodeURIComponent(q.detail)
  const pages = getCurrentPages()
  const cur = pages[pages.length - 1]
  eventChannel = cur.getOpenerEventChannel && cur.getOpenerEventChannel()
  // #ifdef APP-PLUS
  ensureAppLocationPermission().then(() => {
    if (!q.lat && !q.lng) useCurrent({ silent: true })
  })
  // #endif
  // #ifndef APP-PLUS
  if (!q.lat && !q.lng) useCurrent({ silent: true })
  // #endif
})

onReady(() => {
  if (!mapReady.value) return
  try {
    mapCtx = uni.createMapContext('pickMap')
  } catch (e) { /* ignore */ }
})

function ensureAppLocationPermission() {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    if (plus.os.name === 'Android') {
      plus.android.requestPermissions(
        ['android.permission.ACCESS_FINE_LOCATION', 'android.permission.ACCESS_COARSE_LOCATION'],
        () => resolve(true),
        () => resolve(false)
      )
      return
    }
    // #endif
    resolve(true)
  })
}

function onRegionChange(e) {
  const d = e.detail || {}
  if (e.type !== 'end') return
  if (d.centerLocation) {
    lat.value = Number(d.centerLocation.latitude.toFixed(6))
    lng.value = Number(d.centerLocation.longitude.toFixed(6))
  }
}

function useCurrent(opt = {}) {
  const silent = opt && opt.silent
  if (!silent) uni.showLoading({ title: '定位中' })
  uni.getLocation({
    type: 'gcj02',
    isHighAccuracy: true,
    success: (res) => {
      lat.value = Number(res.latitude.toFixed(6))
      lng.value = Number(res.longitude.toFixed(6))
      if (mapCtx && mapCtx.moveToLocation) {
        mapCtx.moveToLocation({ latitude: lat.value, longitude: lng.value })
      }
      if (!silent) uni.showToast({ title: '已更新位置', icon: 'none' })
    },
    fail: (err) => {
      if (!silent) {
        const msg = (err && err.errMsg) || ''
        if (msg.includes('auth deny') || msg.includes('authorize')) {
          uni.showModal({
            title: '需要定位权限',
            content: '请在系统设置中允许「区惠」使用位置信息',
            confirmText: '知道了',
            showCancel: false
          })
        } else {
          uni.showToast({ title: '定位失败，请手动填写地址', icon: 'none' })
        }
      }
    },
    complete: () => {
      if (!silent) uni.hideLoading()
    }
  })
}

function confirm() {
  const text = (detail.value || '').trim()
  if (!text) {
    uni.showToast({ title: '请填写详细地址', icon: 'none' })
    return
  }
  const payload = { lat: lat.value, lng: lng.value, detail: text }
  if (eventChannel) eventChannel.emit('pick', payload)
  uni.navigateBack()
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
  position: relative;
}
.map {
  width: 100%;
  height: calc(100vh - 220rpx);
}
.center-pin {
  position: absolute;
  left: 50%;
  top: calc((100vh - 220rpx) / 2 - 36rpx);
  width: 24rpx;
  height: 48rpx;
  margin-left: -12rpx;
  background: #e85d04;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  box-shadow: 0 4rpx 12rpx rgba(232, 93, 4, 0.45);
  pointer-events: none;
  z-index: 2;
}
.fallback {
  height: calc(100vh - 220rpx);
  padding: 48rpx 40rpx;
  box-sizing: border-box;
  background: #fff;
}
.fallback-title {
  display: block;
  font-size: 32rpx;
  font-weight: 800;
  color: #222;
}
.fallback-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}
.key-hint {
  display: block;
  margin-top: 20rpx;
  font-size: 22rpx;
  color: #999;
  line-height: 1.5;
}
.coord-box {
  margin-top: 32rpx;
  padding: 24rpx;
  background: #f3f6f4;
  border-radius: 16rpx;
}
.coord-label {
  display: block;
  font-size: 22rpx;
  color: #999;
}
.coord-val {
  display: block;
  margin-top: 8rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #0f3d2e;
}
.loc-btn { margin-top: 28rpx; }

.panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -8rpx 24rpx rgba(0, 0, 0, 0.06);
  z-index: 10;
}
.addr-input {
  background: #f3f6f4;
  border-radius: 14rpx;
  padding: 22rpx;
  font-size: 28rpx;
}
.panel-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 16rpx;
}
.ghost-btn {
  flex: 1;
  text-align: center;
  padding: 22rpx 0;
  border-radius: 14rpx;
  border: 2rpx solid rgba(15, 61, 46, 0.25);
  color: #0f3d2e;
  font-weight: 700;
}
.confirm-btn {
  flex: 2;
  padding: 22rpx 0;
  border-radius: 14rpx;
}
</style>
