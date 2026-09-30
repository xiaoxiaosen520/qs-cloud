<template>
  <view class="page">
    <view v-if="loading">
      <view class="skeleton" v-for="i in 3" :key="i" />
    </view>
    <view v-else-if="!list.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">暂无评价</text>
      <text class="muted">顾客完成订单评价后会显示在这里</text>
    </view>
    <view v-else>
      <view v-for="r in list" :key="r.orderId" class="card">
        <view class="row">
          <text class="shop">{{ r.shopName || '订单评价' }}</text>
          <text class="score">★ {{ r.score }}</text>
        </view>
        <text class="content">{{ r.content || '用户未填写文字评价' }}</text>
        <view class="photos" v-if="(r.imageUrls || []).length">
          <image
            v-for="(url, i) in r.imageUrls"
            :key="url + i"
            class="photo"
            :src="absUrl(url)"
            mode="aspectFill"
            @click="preview(r.imageUrls, i)"
          />
        </view>
        <text class="muted">#{{ shortOrderNo(r.orderNo) }} · {{ formatTime(r.createdAt) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { riderApi, absUrl } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { formatTime, shortOrderNo } from '../../utils/format.js'

const list = ref([])
const loading = ref(true)

async function load() {
  if (!requireLogin()) return
  loading.value = true
  try {
    list.value = (await riderApi.reviews(50)) || []
  } finally {
    loading.value = false
  }
}

onShow(load)

function preview(urls, index) {
  uni.previewImage({
    current: index,
    urls: (urls || []).map((u) => absUrl(u))
  })
}
</script>

<style scoped>
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}
.shop { font-weight: 700; }
.score { color: var(--accent); font-weight: 800; }
.content {
  display: block;
  margin-bottom: 10rpx;
  line-height: 1.5;
}
.photos { display: flex; flex-wrap: wrap; gap: 12rpx; margin-bottom: 12rpx; }
.photo { width: 160rpx; height: 160rpx; border-radius: 10rpx; background: #f3f3f3; }
</style>
