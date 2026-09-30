<template>
  <view class="page">
    <web-view v-if="url" :src="url" />
    <view v-else class="empty">
      <text class="empty-title">暂无页面</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const url = ref('')

onLoad((q) => {
  if (q.title) {
    uni.setNavigationBarTitle({ title: decodeURIComponent(q.title) })
  }
  url.value = q.url ? decodeURIComponent(q.url) : ''
})
</script>
