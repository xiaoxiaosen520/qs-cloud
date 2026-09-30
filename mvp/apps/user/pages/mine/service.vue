<template>
  <view class="page">
    <view class="card">
      <text class="title">常见问题</text>
      <view class="faq" v-for="f in faqs" :key="f.q" @click="toggle(f)">
        <view class="faq-q">
          <text>{{ f.q }}</text>
          <text class="arrow">{{ f.open ? '▾' : '›' }}</text>
        </view>
        <text class="faq-a" v-if="f.open">{{ f.a }}</text>
      </view>
    </view>

    <view class="card">
      <text class="title">联系我们</text>
      <view class="line" @click="callPlatform">
        <text>平台客服热线</text>
        <text class="link">400-000-0000</text>
      </view>
      <view class="line" @click="copyNo">
        <text>复制客服微信号</text>
        <text class="link">qs_kefu_demo</text>
      </view>
    </view>

    <view class="hint">联调环境为演示客服，正式上线后将接入企业客服。</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const faqs = ref([
  { q: '未收到商品怎么办？', a: '请先在订单详情联系骑手或商家确认暂存位置；仍未解决可申请售后退款。', open: false },
  { q: '如何申请退款？', a: '订单在商家已接单或配送中时可申请退款，商家同意后款项将原路退回（联调为 mock）。', open: false },
  { q: '优惠券如何使用？', a: '结算页可选择可用优惠券；也可在「我的-优惠券」领取后下单使用。', open: false },
  { q: '配送时效如何计算？', a: '首页展示的送达时间为基于距离的估算，实际以骑手配送为准。', open: false }
])

function toggle(f) {
  f.open = !f.open
}
function callPlatform() {
  uni.makePhoneCall({ phoneNumber: '4000000000' })
}
function copyNo() {
  uni.setClipboardData({
    data: 'qs_kefu_demo',
    success: () => uni.showToast({ title: '已复制', icon: 'none' })
  })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40rpx; }
.title { display: block; font-size: 30rpx; font-weight: 800; margin-bottom: 8rpx; }
.faq { padding: 20rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.faq:last-child { border-bottom: none; }
.faq-q { display: flex; justify-content: space-between; font-weight: 600; color: #222; }
.arrow { color: #ccc; }
.faq-a { display: block; margin-top: 12rpx; color: #666; font-size: 24rpx; line-height: 1.5; }
.line {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid #f0f0f0; font-size: 28rpx;
}
.line:last-child { border-bottom: none; }
.link { color: #0f3d2e; font-weight: 600; }
.hint { margin: 24rpx 32rpx; color: #999; font-size: 22rpx; }
</style>
