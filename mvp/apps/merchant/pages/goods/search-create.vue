<template>
  <view class="page">
    <view class="search-bar">
      <input
        class="input"
        v-model="keyword"
        confirm-type="search"
        placeholder="搜索商品名 / 品牌"
        @confirm="filter"
      />
      <text class="go" @click="filter">搜索</text>
    </view>

    <view v-if="!list.length" class="empty">
      <text class="empty-title">未找到商品</text>
      <text class="muted">可换个关键词，或改用手动建品</text>
      <view class="btn-accent empty-btn" @click="goManual">手动建品</view>
    </view>

    <view v-else>
      <view v-for="item in list" :key="item.name" class="card item" @click="useItem(item)">
        <view class="cover" :style="{ background: coverColor(item.name) }">
          <text class="cover-text">{{ shortName(item.name) }}</text>
        </view>
        <view class="body">
          <text class="name">{{ item.name }}</text>
          <text class="muted">参考价 ¥{{ item.price }} · {{ item.spec }}</text>
          <text class="muted" v-if="item.barcode">条码 {{ item.barcode }}</text>
        </view>
        <text class="link">选用</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { coverColor, shortName } from '../../utils/format.js'
import { PRODUCT_CATALOG, searchCatalog } from '../../utils/catalog.js'

const keyword = ref('')
const list = ref([...PRODUCT_CATALOG])

onLoad(() => {
  list.value = [...PRODUCT_CATALOG]
})

function filter() {
  list.value = searchCatalog(keyword.value)
}

function goManual() {
  uni.navigateTo({ url: '/pages/goods/edit' })
}

function useItem(item) {
  const q = [
    'mode=search',
    'name=' + encodeURIComponent(item.name),
    'skuName=' + encodeURIComponent(item.spec),
    'price=' + encodeURIComponent(item.price),
    'barcode=' + encodeURIComponent(item.barcode || '')
  ].join('&')
  uni.navigateTo({ url: '/pages/goods/edit?' + q })
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 16rpx;
  align-items: center;
  padding: 20rpx 28rpx;
}
.input {
  flex: 1;
  background: #fff;
  border-radius: 14rpx;
  padding: 20rpx 22rpx;
  margin-bottom: 0;
}
.go {
  color: var(--brand);
  font-weight: 700;
  padding: 0 8rpx;
}
.item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin: 0 28rpx 16rpx;
}
.cover {
  width: 96rpx;
  height: 96rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cover-text { color: #fff; font-weight: 800; font-size: 34rpx; }
.body { flex: 1; min-width: 0; }
.name {
  display: block;
  font-weight: 700;
  font-size: 28rpx;
  margin-bottom: 6rpx;
}
.link { color: var(--accent); font-weight: 700; font-size: 26rpx; }
.empty-btn { margin: 32rpx 80rpx 0; }
</style>
