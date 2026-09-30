<template>
  <view class="page">
    <view class="hero">
      <view class="loc">
        <view class="loc-dot" />
        <view class="loc-text">
          <text class="loc-label">配送至</text>
          <text class="loc-name">{{ locName }}</text>
        </view>
        <text class="loc-btn" @click="refreshLoc">重新定位</text>
      </view>
      <text class="brand">区惠</text>
      <view class="search-wrap" @click="goSearch">
        <text class="search-ph">搜索附近商家、商品</text>
        <text class="search-go">搜索</text>
      </view>
    </view>

    <swiper v-if="banners.length" class="banner" circular autoplay indicator-dots>
      <swiper-item v-for="b in banners" :key="b.id" @click="openBanner(b)">
        <image class="banner-img" :src="bannerSrc(b)" mode="aspectFill" />
      </swiper-item>
    </swiper>

    <view class="type-bar">
      <view :class="['type', type === 'FOOD' && 'on']" @click="switchType('FOOD')">
        <text class="type-emoji">餐</text>
        <view>
          <text class="type-title">美食外卖</text>
          <text class="type-desc">热乎到家</text>
        </view>
      </view>
      <view :class="['type', type === 'CONVENIENCE' && 'on']" @click="switchType('CONVENIENCE')">
        <text class="type-emoji">超</text>
        <view>
          <text class="type-title">便利超市</text>
          <text class="type-desc">即时达</text>
        </view>
      </view>
    </view>

    <view class="sort-bar">
      <text
        v-for="s in sorts"
        :key="s.key"
        :class="['sort', sortKey === s.key && 'on']"
        @click="changeSort(s.key)"
      >{{ s.label }}</text>
      <text :class="['sort', onlyFav && 'on']" @click="onlyFav = !onlyFav">收藏</text>
    </view>

    <view v-if="loading">
      <view class="skeleton" v-for="i in 3" :key="i" />
    </view>

    <view v-else-if="error" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">加载失败</text>
      <text class="muted">{{ error }}</text>
      <view class="btn-primary retry" @click="load">重新加载</view>
    </view>

    <view v-else-if="!displayShops.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">{{ onlyFav ? '暂无收藏商家' : (keyword ? '没有匹配的商家' : '附近暂无营业店铺') }}</text>
      <text class="muted">{{ onlyFav ? '去店铺页点收藏后再看这里' : (keyword ? '换个关键词试试' : '下拉刷新，或先用商家端开店上架') }}</text>
    </view>

    <view v-else class="list">
      <view
        v-for="item in displayShops"
        :key="item.shop.id"
        class="shop-card"
        @click="goShop(item.shop.id)"
      >
        <image
          class="logo-img"
          :src="shopLogo(item.shop)"
          mode="aspectFill"
        />
        <view class="body">
          <view class="top">
            <text class="name">{{ item.shop.name }}</text>
            <text class="dist">{{ formatDistance(item.distanceMeters) }}</text>
          </view>
          <view class="meta">
            <text class="score">★ {{ Number(item.shop.score || 5).toFixed(1) }}</text>
            <text class="dot">·</text>
            <text>月售 {{ item.shop.monthSales || 0 }}</text>
            <text class="dot">·</text>
            <text>{{ etaText(item.distanceMeters) }}</text>
          </view>
          <view class="fees">
            <text>起送 ¥{{ item.shop.minOrderAmount }}</text>
            <text class="sep">|</text>
            <text>配送 ¥{{ item.shop.deliveryFee }}</text>
          </view>
          <view class="notice" v-if="item.shop.notice">{{ item.shop.notice }}</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { shopApi, bannerApi, favoriteApi } from '../../api/http.js'
import { refreshTabBadges } from '../../utils/badge.js'
import { listFavoriteIds } from '../../utils/favorite.js'
import { getToken } from '../../utils/auth.js'
import { shopImage, goodsImage } from '../../utils/image.js'

const type = ref('CONVENIENCE')
const shops = ref([])
const banners = ref([])
const favIds = ref([])
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const sortKey = ref('distance')
const onlyFav = ref(false)
const lat = ref(31.2304)
const lng = ref(121.4737)
const locName = ref('演示坐标 · 附近商家')
const sorts = [
  { key: 'distance', label: '距离' },
  { key: 'sales', label: '销量' },
  { key: 'score', label: '评分' },
  { key: 'delivery', label: '配送费' }
]

const displayShops = computed(() => {
  let list = [...shops.value]
  const kw = keyword.value.trim()
  if (kw) {
    list = list.filter((i) => (i.shop.name || '').includes(kw) || (i.shop.notice || '').includes(kw))
  }
  if (onlyFav.value) {
    const ids = new Set(favIds.value.length ? favIds.value : listFavoriteIds())
    list = list.filter((i) => ids.has(Number(i.shop.id)))
  }
  list.sort((a, b) => {
    if (sortKey.value === 'sales') return (b.shop.monthSales || 0) - (a.shop.monthSales || 0)
    if (sortKey.value === 'score') return Number(b.shop.score || 0) - Number(a.shop.score || 0)
    if (sortKey.value === 'delivery') return Number(a.shop.deliveryFee || 0) - Number(b.shop.deliveryFee || 0)
    return (a.distanceMeters || 0) - (b.distanceMeters || 0)
  })
  return list
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [shopList, bannerList] = await Promise.all([
      shopApi.nearby({ lat: lat.value, lng: lng.value, type: type.value }),
      bannerApi.list().catch(() => [])
    ])
    shops.value = shopList || []
    banners.value = bannerList || []
    if (getToken()) {
      try {
        favIds.value = ((await favoriteApi.ids()) || []).map(Number)
      } catch (e) {
        favIds.value = listFavoriteIds()
      }
    } else {
      favIds.value = listFavoriteIds()
    }
  } catch (e) {
    shops.value = []
    error.value = (e && e.message) || '网络异常'
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function switchType(t) {
  type.value = t
  load()
}
function changeSort(k) {
  sortKey.value = k
}
function applyFilter() {
  // computed 已响应 keyword
}
function goSearch() {
  uni.navigateTo({ url: '/pages/search/index' })
}

function refreshLoc() {
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      lat.value = res.latitude
      lng.value = res.longitude
      locName.value = `当前位置 · ${res.latitude.toFixed(4)}, ${res.longitude.toFixed(4)}`
      load()
    },
    fail: () => {
      uni.showToast({ title: '定位失败，仍用演示坐标', icon: 'none' })
      locName.value = '演示坐标 · 附近商家'
      load()
    }
  })
}

function formatDistance(m) {
  if (m == null) return ''
  if (m < 1000) return m + 'm'
  return (m / 1000).toFixed(1) + 'km'
}
function etaText(m) {
  const mins = Math.max(20, Math.round((m || 800) / 80) + 15)
  return `约${mins}分钟`
}
function shopLogo(shop) {
  return shopImage(shop?.logoUrl, shop?.id || shop?.name || 'x')
}
function bannerSrc(b) {
  return goodsImage(b?.imageUrl, 'banner' + (b?.id || 'x'))
}
function goShop(id) {
  uni.navigateTo({ url: '/pages/shop/detail?id=' + id })
}
function openBanner(b) {
  const link = (b && b.linkUrl) ? String(b.linkUrl).trim() : ''
  if (!link) return
  // 站内店铺：shop:123 或 /pages/shop/detail?id=123
  const shopMatch = link.match(/^(?:shop:|\/pages\/shop\/detail\?id=)(\d+)$/i)
  if (shopMatch) {
    goShop(Number(shopMatch[1]))
    return
  }
  if (link.startsWith('/pages/')) {
    if (link.includes('/pages/index') || link.includes('/pages/cart') || link.includes('/pages/order/list') || link.includes('/pages/mine')) {
      uni.switchTab({ url: link.split('?')[0] })
    } else {
      uni.navigateTo({ url: link })
    }
    return
  }
  // 外链：H5 可用；小程序可提示
  // #ifdef H5
  window.open(link, '_blank')
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: link,
    success: () => uni.showToast({ title: '链接已复制', icon: 'none' })
  })
  // #endif
}

onShow(() => {
  refreshTabBadges()
  load()
})
onPullDownRefresh(load)
</script>

<style scoped>
.page { padding-bottom: 40rpx; min-height: 100vh; }
.hero {
  padding: 24rpx 32rpx 40rpx;
  background:
    radial-gradient(circle at 90% 10%, rgba(232, 93, 4, 0.25), transparent 40%),
    linear-gradient(165deg, #0f3d2e 0%, #1b5e45 55%, #245c4a 100%);
  color: #fff;
}
.loc { display: flex; align-items: center; gap: 12rpx; margin-bottom: 20rpx; }
.loc-dot {
  width: 16rpx; height: 16rpx; border-radius: 50%;
  background: #f48c06; box-shadow: 0 0 0 8rpx rgba(244, 140, 6, 0.25);
}
.loc-text { flex: 1; min-width: 0; }
.loc-label { display: block; font-size: 20rpx; opacity: 0.75; }
.loc-name {
  display: block; font-size: 26rpx; font-weight: 600;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.loc-btn {
  font-size: 22rpx; padding: 8rpx 16rpx; border-radius: 20rpx;
  background: rgba(255,255,255,.16);
}
.brand {
  display: block; font-size: 48rpx; font-weight: 800; letter-spacing: 2rpx; margin-bottom: 20rpx;
}
.banner {
  height: 220rpx; margin: -12rpx 28rpx 8rpx; border-radius: 16rpx; overflow: hidden;
}
.banner-img { width: 100%; height: 220rpx; }
.search-wrap {
  display: flex; align-items: center; gap: 12rpx;
  background: #fff; border-radius: 16rpx; padding: 8rpx 8rpx 8rpx 24rpx;
}
.search-ph { flex: 1; color: #9aaba3; height: 64rpx; line-height: 64rpx; font-size: 26rpx; }
.search-go {
  background: #0f3d2e; color: #fff; padding: 14rpx 22rpx; border-radius: 12rpx; font-size: 24rpx; font-weight: 600;
}
.type-bar {
  display: flex; gap: 16rpx; margin: -20rpx 28rpx 8rpx; position: relative; z-index: 2;
}
.type {
  flex: 1; background: #fff; border-radius: 20rpx; padding: 22rpx 20rpx;
  border: 2rpx solid transparent; box-shadow: 0 8rpx 24rpx rgba(15, 61, 46, 0.06);
  display: flex; align-items: center; gap: 16rpx;
}
.type.on { border-color: #0f3d2e; background: #f0faf4; }
.type-emoji {
  width: 64rpx; height: 64rpx; border-radius: 16rpx;
  background: #eaf6ef; color: #0f3d2e; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
}
.type-title { display: block; font-size: 30rpx; font-weight: 700; color: #0f3d2e; }
.type-desc { display: block; margin-top: 6rpx; font-size: 22rpx; color: #6b7c74; }
.sort-bar { display: flex; gap: 28rpx; padding: 16rpx 32rpx 8rpx; }
.sort { color: #6b7c74; font-size: 24rpx; }
.sort.on { color: #0f3d2e; font-weight: 700; }
.list { padding-top: 4rpx; }
.shop-card {
  display: flex; gap: 20rpx; margin: 16rpx 28rpx; padding: 24rpx;
  background: #fff; border-radius: 20rpx; box-shadow: 0 6rpx 20rpx rgba(20, 32, 27, 0.04);
}
.logo, .logo-img {
  width: 112rpx; height: 112rpx; border-radius: 18rpx; flex-shrink: 0;
}
.logo {
  display: flex; align-items: center; justify-content: center;
}
.logo-text { color: #fff; font-size: 44rpx; font-weight: 700; }
.body { flex: 1; min-width: 0; }
.top { display: flex; justify-content: space-between; gap: 12rpx; align-items: flex-start; }
.name {
  font-size: 32rpx; font-weight: 700; flex: 1;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.dist { color: #6b7c74; font-size: 22rpx; flex-shrink: 0; }
.meta { margin-top: 8rpx; color: #6b7c74; font-size: 22rpx; }
.score { color: #e85d04; font-weight: 600; }
.dot { margin: 0 6rpx; }
.fees { margin-top: 10rpx; font-size: 22rpx; color: #52796f; }
.sep { margin: 0 10rpx; color: #d0dbd5; }
.notice {
  margin-top: 12rpx; padding: 8rpx 12rpx; background: #fff7ed; color: #9a3412;
  font-size: 22rpx; border-radius: 8rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
</style>
