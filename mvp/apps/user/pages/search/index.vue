<template>
  <view class="page">
    <view class="search-bar">
      <input
        class="input"
        v-model="keyword"
        focus
        confirm-type="search"
        placeholder="搜索商家、商品"
        @confirm="search"
      />
      <text class="go" @click="search">搜索</text>
    </view>

    <view class="chips" v-if="!keyword && !searched">
      <text class="chip-label">热门搜索</text>
      <view class="chip-row">
        <text v-for="h in hots" :key="h" class="chip" @click="quick(h)">{{ h }}</text>
      </view>
    </view>

    <view v-if="loading" class="skeleton" />
    <view v-else-if="searched && !shops.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">没有找到相关商家</text>
      <text class="muted">换个关键词试试</text>
    </view>
    <view v-else class="list">
      <view v-for="item in shops" :key="item.shop.id" class="card shop" @click="goShop(item.shop.id)">
        <image class="logo" :src="logoOf(item.shop)" mode="aspectFill" />
        <view class="body">
          <text class="name">{{ item.shop.name }}</text>
          <text class="meta">★ {{ Number(item.shop.score || 5).toFixed(1) }} · 月售 {{ item.shop.monthSales || 0 }}</text>
          <text class="fee">起送 ¥{{ item.shop.minOrderAmount }} · 配送 ¥{{ item.shop.deliveryFee }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { shopApi } from '../../api/http.js'
import { shopImage } from '../../utils/image.js'

const keyword = ref('')
const shops = ref([])
const loading = ref(false)
const searched = ref(false)
const lat = ref(31.2304)
const lng = ref(121.4737)
const hots = ['便利店', '奶茶', '炸鸡', '夜宵', '超市']

onLoad((q) => {
  if (q.kw) {
    keyword.value = decodeURIComponent(q.kw)
    search()
  }
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      lat.value = res.latitude
      lng.value = res.longitude
    }
  })
})

function logoOf(shop) {
  return shopImage(shop.logoUrl, shop.id || shop.name)
}
function quick(h) {
  keyword.value = h
  search()
}

async function search() {
  const kw = keyword.value.trim()
  if (!kw) {
    uni.showToast({ title: '请输入关键词', icon: 'none' })
    return
  }
  loading.value = true
  searched.value = true
  try {
    const [food, conv] = await Promise.all([
      shopApi.nearby({ lat: lat.value, lng: lng.value, type: 'FOOD' }),
      shopApi.nearby({ lat: lat.value, lng: lng.value, type: 'CONVENIENCE' })
    ])
    const all = [...(food || []), ...(conv || [])]
    shops.value = all.filter((i) => {
      const name = i.shop?.name || ''
      const notice = i.shop?.notice || ''
      return name.includes(kw) || notice.includes(kw)
    })
  } catch (e) {
    shops.value = []
  } finally {
    loading.value = false
  }
}

function goShop(id) {
  uni.navigateTo({ url: '/pages/shop/detail?id=' + id })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; }
.search-bar {
  display: flex; gap: 12rpx; align-items: center;
  padding: 16rpx 24rpx; background: #fff; position: sticky; top: 0; z-index: 5;
}
.input {
  flex: 1; background: #f5f5f5; border-radius: 32rpx; padding: 16rpx 28rpx; font-size: 26rpx;
}
.go {
  color: #0f3d2e; font-weight: 700; padding: 8rpx 12rpx;
}
.chips { padding: 24rpx 28rpx; }
.chip-label { display: block; color: #999; font-size: 24rpx; margin-bottom: 16rpx; }
.chip-row { display: flex; flex-wrap: wrap; gap: 16rpx; }
.chip {
  padding: 12rpx 24rpx; background: #fff; border-radius: 28rpx; font-size: 24rpx; color: #333;
}
.list { padding-bottom: 24rpx; }
.shop { display: flex; gap: 20rpx; }
.logo { width: 100rpx; height: 100rpx; border-radius: 16rpx; background: #f0f0f0; flex-shrink: 0; }
.body { flex: 1; min-width: 0; }
.name { display: block; font-size: 30rpx; font-weight: 800; }
.meta, .fee { display: block; margin-top: 8rpx; font-size: 22rpx; color: #6b7c74; }
</style>
