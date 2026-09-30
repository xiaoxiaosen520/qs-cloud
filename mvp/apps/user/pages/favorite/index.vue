<template>
  <view class="page">
    <view v-if="loading">
      <view class="skeleton" v-for="i in 2" :key="i" />
    </view>
    <view v-else-if="!shops.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">还没有收藏</text>
      <text class="muted">在店铺页点「收藏」即可加入</text>
      <view class="btn-primary retry" @click="goHome">去首页看看</view>
    </view>
    <view v-else>
      <view
        v-for="shop in shops"
        :key="shop.id"
        class="card shop"
        @click="goShop(shop.id)"
      >
        <image class="logo" :src="logoOf(shop)" mode="aspectFill" />
        <view class="body">
          <view class="row">
            <text class="name">{{ shop.name }}</text>
            <text class="unfav" @click.stop="unfav(shop.id)">取消</text>
          </view>
          <text class="muted">★ {{ Number(shop.score || 5).toFixed(1) }} · 起送 ¥{{ shop.minOrderAmount }} · 配送 ¥{{ shop.deliveryFee || 0 }}</text>
          <text class="addr" v-if="shop.address">{{ shop.address }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { favoriteApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { shopImage } from '../../utils/image.js'

const shops = ref([])
const loading = ref(false)

function logoOf(shop) {
  return shopImage(shop.logoUrl, shop.id || shop.name)
}

async function load() {
  if (!requireLogin()) return
  loading.value = true
  try {
    shops.value = (await favoriteApi.list()) || []
  } catch (e) {
    shops.value = []
  } finally {
    loading.value = false
  }
}

async function unfav(id) {
  await favoriteApi.toggle(id)
  await load()
}
function goShop(id) {
  uni.navigateTo({ url: '/pages/shop/detail?id=' + id })
}
function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

onShow(load)
</script>

<style scoped>
.shop {
  display: flex;
  gap: 20rpx;
  box-shadow: 0 6rpx 18rpx rgba(20, 32, 27, 0.04);
}
.logo {
  width: 100rpx;
  height: 100rpx;
  border-radius: 16rpx;
  background: #f0f0f0;
  flex-shrink: 0;
}
.body { flex: 1; min-width: 0; }
.row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx; }
.name {
  font-weight: 800; font-size: 30rpx; flex: 1;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.unfav { color: #6b7c74; font-size: 24rpx; flex-shrink: 0; margin-left: 12rpx; }
.addr {
  display: block; margin-top: 8rpx; font-size: 22rpx; color: #999;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
</style>
