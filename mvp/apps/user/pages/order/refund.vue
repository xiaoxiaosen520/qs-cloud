<template>
  <view class="page" v-if="detail">
    <view class="card">
      <text class="shop">{{ shopName }}</text>
      <text class="muted">订单号 {{ detail.order.orderNo }}</text>
      <view class="amt">退款金额 <text class="price">¥{{ detail.order.payAmount }}</text></view>
    </view>

    <view class="card">
      <text class="label">退款原因</text>
      <view
        v-for="r in reasons"
        :key="r"
        :class="['reason', reason === r && 'on']"
        @click="reason = r"
      >{{ r }}</view>
      <input
        v-if="reason === '其他'"
        class="input"
        v-model="custom"
        maxlength="80"
        placeholder="请补充说明（选填）"
      />
    </view>

    <view class="btn-accent submit" @click="submit">提交退款申请</view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { orderApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'

const id = ref(0)
const detail = ref(null)
const reason = ref('商品质量问题')
const custom = ref('')
const reasons = ['商品质量问题', '少送/错送', '未收到商品', '不想要了', '其他']

const shopName = computed(() => detail.value?.shop?.name || '店铺')

onLoad(async (q) => {
  if (!requireLogin()) return
  id.value = Number(q.id)
  detail.value = await orderApi.detail(id.value)
})

async function submit() {
  const text = reason.value === '其他'
    ? (custom.value.trim() || '其他原因')
    : reason.value
  await orderApi.refund(id.value, text)
  uni.showToast({ title: '已提交退款', icon: 'success' })
  setTimeout(() => {
    uni.redirectTo({ url: '/pages/order/detail?id=' + id.value })
  }, 400)
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40rpx; }
.shop { display: block; font-size: 30rpx; font-weight: 800; }
.amt { margin-top: 20rpx; font-size: 28rpx; }
.label { display: block; font-weight: 700; margin-bottom: 16rpx; }
.reason {
  padding: 22rpx 20rpx; margin-bottom: 12rpx; border-radius: 12rpx;
  background: #f5f5f5; border: 2rpx solid transparent; color: #333;
}
.reason.on { background: #eaf6ef; border-color: #0f3d2e; font-weight: 700; }
.input { margin-top: 8rpx; background: #f5f5f5; border-radius: 12rpx; padding: 20rpx; }
.submit { margin: 40rpx 28rpx; }
</style>
