<template>
  <view class="page">
    <view class="card addr" @click="pickAddress">
      <view v-if="address" class="addr-body">
        <text class="tag">收货</text>
        <view>
          <text class="name">{{ address.contactName }} {{ address.contactPhone }}</text>
          <text class="detail">{{ address.detail }}</text>
        </view>
      </view>
      <view v-else class="addr-empty">
        <text class="name">选择收货地址</text>
        <text class="muted">点击添加或选择</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <view class="card" v-if="cart">
      <text class="shop">{{ cart.shopName }}</text>
      <view v-for="line in cart.items" :key="line.id" class="line">
        <image class="line-img" :src="lineCover(line)" mode="aspectFill" />
        <view class="line-mid">
          <text class="item">{{ line.goodsName }}</text>
          <text class="sku">{{ line.skuName }} ×{{ line.quantity }}</text>
        </view>
        <text class="price">¥{{ line.lineAmount }}</text>
      </view>
      <view class="fee-row"><text class="muted">商品小计</text><text>¥{{ cart.goodsAmount }}</text></view>
      <view class="fee-row"><text class="muted">配送费</text><text>¥{{ cart.deliveryFee || 0 }}</text></view>
      <view class="fee-row"><text class="muted">打包费</text><text>¥{{ cart.packingFee || 0 }}</text></view>
      <view class="fee-row" v-if="discount > 0">
        <text class="muted">优惠券</text>
        <text class="price">-¥{{ discount }}</text>
      </view>
      <view class="fee-row pay">
        <text>应付合计</text>
        <text class="price big">¥{{ payAmount }}</text>
      </view>
      <view v-if="!cart.meetMinOrder" class="warn">未满起送价 ¥{{ cart.minOrderAmount }}，请返回加购</view>
    </view>

    <view class="card coupon" @click="pickCoupon">
      <text class="label">优惠券</text>
      <text class="coupon-val">{{ couponLabel }}</text>
      <text class="arrow">›</text>
    </view>

    <view class="card">
      <text class="label">订单备注</text>
      <input class="input" v-model="remark" maxlength="80" placeholder="如：放门口、少放辣（选填）" />
    </view>

    <view class="card pay-card">
      <text class="label">支付方式</text>
      <view class="pay-list">
        <view
          v-for="ch in payChannels"
          :key="ch.code"
          :class="['pay-item', payChannel === ch.code && 'on', !ch.enabled && 'disabled']"
          @click="selectChannel(ch)"
        >
          <text class="pay-icon">{{ channelIcon(ch.code) }}</text>
          <view class="pay-meta">
            <text class="pay-name">{{ ch.name }}</text>
            <text class="pay-desc">{{ ch.desc }}</text>
          </view>
          <view :class="['radio', payChannel === ch.code && 'on']" />
        </view>
      </view>
    </view>

    <view class="card tip-card">
      <text class="tip-title">支付说明</text>
      <text class="muted">未配置微信/支付宝商户时，将走模拟支付完成联调；正式上线在服务端 application.yml 配置商户参数。</text>
    </view>

    <view class="footer">
      <view class="sum">
        <text class="muted">合计</text>
        <text class="price total">¥{{ payAmount }}</text>
      </view>
      <view
        :class="['btn-accent submit', (!canSubmit || submitting) && 'disabled']"
        @click="submit"
      >{{ submitting ? '提交中…' : '提交并支付' }}</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { cartApi, addressApi, orderApi, couponApi, payApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { goodsImage } from '../../utils/image.js'
import { payOrder } from '../../utils/pay.js'

const shopId = ref(0)
const cart = ref(null)
const address = ref(null)
const remark = ref('')
const submitting = ref(false)
const coupons = ref([])
const selectedCouponId = ref(null)
const payChannels = ref([])
const payChannel = ref('MOCK')

function lineCover(line) {
  return goodsImage(line.coverUrl, 'g' + (line.goodsId || line.skuId || line.id || 'x'))
}

const selectedCoupon = computed(() =>
  coupons.value.find((c) => c.id === selectedCouponId.value && c.usable)
)

const discount = computed(() => Number(selectedCoupon.value?.discount || 0))

const payAmount = computed(() => {
  const base = Number(cart.value?.payAmount || cart.value?.goodsAmount || 0)
  return Math.max(0, +(base - discount.value).toFixed(2))
})

const couponLabel = computed(() => {
  if (selectedCoupon.value) return `${selectedCoupon.value.name} -¥${selectedCoupon.value.discount}`
  const usable = coupons.value.filter((c) => c.usable).length
  return usable ? `${usable} 张可用` : '暂无可用'
})

const canSubmit = computed(() =>
  !!address.value && !!cart.value?.shopId && cart.value.meetMinOrder && (cart.value.items || []).length > 0
)

onLoad((q) => {
  shopId.value = Number(q.shopId || 0)
})

onShow(async () => {
  if (!requireLogin()) return
  cart.value = await cartApi.view()
  const list = (await addressApi.list()) || []
  const picked = uni.getStorageSync('selectedAddressId')
  address.value = list.find((a) => a.id === picked) || list.find((a) => a.isDefault === 1) || list[0] || null
  coupons.value = (await couponApi.mine({
    status: 'UNUSED',
    shopId: cart.value?.shopId,
    goodsAmount: cart.value?.goodsAmount
  })) || []
  if (selectedCouponId.value && !selectedCoupon.value) {
    selectedCouponId.value = null
  }
  try {
    payChannels.value = (await payApi.channels()) || []
    const enabled = payChannels.value.filter((c) => c.enabled)
    if (!enabled.find((c) => c.code === payChannel.value)) {
      payChannel.value = (enabled[0] && enabled[0].code) || 'MOCK'
    }
  } catch (e) {
    payChannels.value = [{ code: 'MOCK', name: '模拟支付', enabled: true, desc: '联调' }]
  }
})

function pickAddress() {
  uni.navigateTo({ url: '/pages/address/list?select=1' })
}

function pickCoupon() {
  const usable = coupons.value.filter((c) => c.usable)
  if (!usable.length) {
    uni.showToast({ title: '暂无可用优惠券', icon: 'none' })
    return
  }
  uni.showActionSheet({
    itemList: ['不使用优惠券', ...usable.map((c) => `${c.name}（满${c.threshold}减${c.discount}）`)],
    success: (res) => {
      if (res.tapIndex === 0) selectedCouponId.value = null
      else selectedCouponId.value = usable[res.tapIndex - 1].id
    }
  })
}

function channelIcon(code) {
  if (code === 'WECHAT') return '微'
  if (code === 'ALIPAY') return '支'
  return '测'
}

function selectChannel(ch) {
  if (!ch.enabled) {
    uni.showToast({ title: ch.desc || '暂未开通', icon: 'none' })
    return
  }
  payChannel.value = ch.code
}

async function submit() {
  if (submitting.value || !canSubmit.value) {
    if (!address.value) uni.showToast({ title: '请先选择地址', icon: 'none' })
    else if (!cart.value?.meetMinOrder) uni.showToast({ title: '未满起送价', icon: 'none' })
    return
  }
  uni.showModal({
    title: '确认支付',
    content: `应付 ¥${payAmount.value}，确认提交订单？`,
    success: async (res) => {
      if (!res.confirm) return
      submitting.value = true
      uni.showLoading({ title: '下单中' })
      try {
        const detail = await orderApi.create({
          shopId: cart.value.shopId,
          addressId: address.value.id,
          remark: remark.value,
          userCouponId: selectedCouponId.value || undefined,
          payChannel: payChannel.value,
          mockPay: false
        })
        uni.showLoading({ title: '支付中' })
        try {
          await payOrder(detail.order.id, payChannel.value)
          uni.hideLoading()
          uni.showToast({ title: '支付成功', icon: 'success' })
          setTimeout(() => {
            uni.redirectTo({ url: '/pages/order/detail?id=' + detail.order.id })
          }, 400)
        } catch (pe) {
          uni.hideLoading()
          if (pe && pe.message !== 'cancel') {
            uni.showToast({ title: '已下单，可在订单里继续支付', icon: 'none' })
            setTimeout(() => {
              uni.redirectTo({ url: '/pages/order/detail?id=' + detail.order.id })
            }, 600)
          }
        }
      } catch (e) {
        uni.hideLoading()
      } finally {
        submitting.value = false
      }
    }
  })
}
</script>

<style scoped>
.page { padding-bottom: 200rpx; }
.addr { display: flex; align-items: center; gap: 16rpx; }
.addr-body { flex: 1; display: flex; gap: 16rpx; align-items: flex-start; }
.tag {
  background: #0f3d2e; color: #fff; font-size: 20rpx; padding: 6rpx 10rpx;
  border-radius: 8rpx; margin-top: 4rpx;
}
.name { display: block; font-weight: 700; font-size: 30rpx; }
.detail { display: block; margin-top: 8rpx; color: #52796f; font-size: 24rpx; }
.addr-empty { flex: 1; }
.arrow { font-size: 40rpx; color: #9aaba3; }
.shop { display: block; font-weight: 700; margin-bottom: 16rpx; }
.line { display: flex; align-items: center; gap: 16rpx; padding: 14rpx 0; border-bottom: 1rpx solid #f3f3f3; }
.line:last-of-type { border-bottom: none; }
.line-img { width: 88rpx; height: 88rpx; border-radius: 12rpx; background: #f0f0f0; flex-shrink: 0; }
.line-mid { flex: 1; min-width: 0; }
.item { display: block; color: #222; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sku { display: block; margin-top: 6rpx; font-size: 22rpx; color: #999; }
.fee-row { display: flex; justify-content: space-between; margin-top: 12rpx; }
.fee-row.pay { margin-top: 20rpx; padding-top: 16rpx; border-top: 1rpx solid #eef3f0; font-weight: 700; }
.big { font-size: 36rpx; }
.warn {
  margin-top: 16rpx; padding: 12rpx 16rpx; background: #fff7ed; color: #9a3412;
  border-radius: 10rpx; font-size: 22rpx;
}
.coupon { display: flex; align-items: center; gap: 12rpx; }
.coupon .label { font-weight: 700; }
.coupon-val { flex: 1; text-align: right; color: #e85d04; font-size: 24rpx; }
.pay-card { padding-bottom: 8rpx; }
.pay-list { margin-top: 8rpx; }
.pay-item {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 0; border-bottom: 1rpx solid #f3f3f3;
}
.pay-item:last-child { border-bottom: none; }
.pay-item.disabled { opacity: 0.45; }
.pay-item.on .pay-name { color: #0f3d2e; font-weight: 800; }
.pay-icon {
  width: 56rpx; height: 56rpx; line-height: 56rpx; text-align: center;
  border-radius: 14rpx; background: #f3f6f4; font-weight: 800; color: #0f3d2e;
}
.pay-meta { flex: 1; min-width: 0; }
.pay-name { display: block; font-size: 28rpx; }
.pay-desc { display: block; margin-top: 4rpx; font-size: 22rpx; color: #999; }
.radio {
  width: 32rpx; height: 32rpx; border-radius: 50%; border: 2rpx solid #ccc;
}
.radio.on { border-color: #0f3d2e; background: radial-gradient(circle, #0f3d2e 45%, transparent 46%); }
.label { display: block; font-weight: 700; margin-bottom: 12rpx; }
.input {
  background: #f3f6f4; border-radius: 12rpx; padding: 20rpx;
}
.tip-card { background: #f0faf4; }
.tip-title { display: block; font-weight: 700; margin-bottom: 8rpx; color: #0f3d2e; }
.footer {
  position: fixed; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; gap: 20rpx;
  padding: 16rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1rpx solid #e6eee9;
}
.sum { flex: 1; }
.total { font-size: 40rpx; margin-left: 8rpx; }
.submit { min-width: 260rpx; padding: 20rpx 0; }
.submit.disabled { opacity: 0.45; }
</style>
