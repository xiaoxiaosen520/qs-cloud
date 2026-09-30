<template>
  <view class="page">
    <view class="tabs">
      <text :class="['tab', tab === 'available' && 'on']" @click="tab = 'available'; load()">可领取</text>
      <text :class="['tab', tab === 'UNUSED' && 'on']" @click="tab = 'UNUSED'; load()">我的未用</text>
      <text :class="['tab', tab === 'USED' && 'on']" @click="tab = 'USED'; load()">已使用</text>
    </view>

    <view v-if="loading"><view class="skeleton" v-for="i in 2" :key="i" /></view>
    <view v-else-if="!list.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">暂无优惠券</text>
    </view>
    <view v-else>
      <view v-for="c in list" :key="c.id || c.couponId" class="card coupon">
        <view class="left">
          <text class="yen">¥</text>
          <text class="amount">{{ c.discount }}</text>
        </view>
        <view class="mid">
          <text class="name">{{ c.name }}</text>
          <text class="muted">满 {{ c.threshold }} 可用 · {{ c.shopId ? '店铺券' : '平台券' }}</text>
        </view>
        <view class="right" v-if="tab === 'available'">
          <text class="btn" @click="claim(c.id)">领取</text>
        </view>
        <view class="right" v-else-if="tab === 'UNUSED'">
          <text class="btn" @click="goUse(c)">去使用</text>
        </view>
        <view class="right" v-else>
          <text class="status">{{ statusText(c.status) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { couponApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'

const tab = ref('available')
const list = ref([])
const loading = ref(false)

function statusText(s) {
  return { UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期' }[s] || s
}

async function load() {
  if (!requireLogin()) return
  loading.value = true
  try {
    if (tab.value === 'available') {
      list.value = (await couponApi.available()) || []
    } else {
      list.value = (await couponApi.mine({ status: tab.value })) || []
    }
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function claim(id) {
  await couponApi.claim(id)
  uni.showToast({ title: '领取成功', icon: 'success' })
  tab.value = 'UNUSED'
  await load()
}

function goUse(c) {
  if (c && c.shopId) {
    uni.navigateTo({ url: '/pages/shop/detail?id=' + c.shopId })
    return
  }
  uni.switchTab({ url: '/pages/index/index' })
}

onShow(load)
</script>

<style scoped>
.tabs { display: flex; gap: 12rpx; padding: 16rpx 28rpx; }
.tab {
  padding: 12rpx 22rpx; border-radius: 20rpx; background: #fff; color: #6b7c74; font-size: 24rpx;
}
.tab.on { background: #0f3d2e; color: #fff; font-weight: 700; }
.coupon { display: flex; align-items: center; gap: 16rpx; }
.left {
  width: 140rpx; text-align: center; color: #e85d04;
}
.yen { font-size: 22rpx; }
.amount { font-size: 48rpx; font-weight: 800; }
.mid { flex: 1; min-width: 0; }
.name { display: block; font-weight: 700; margin-bottom: 6rpx; }
.btn {
  background: #0f3d2e; color: #fff; padding: 12rpx 20rpx; border-radius: 20rpx; font-size: 22rpx;
}
.status { color: #6b7c74; font-size: 22rpx; }
</style>
