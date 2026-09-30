<template>
  <view class="page">
    <view v-if="loading" class="skeleton" />
    <view v-else-if="!cart || !cart.items || !cart.items.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">购物车还是空的</text>
      <text class="muted">去首页逛逛附近好店吧</text>
      <view class="btn-primary go-home" @click="goHome">去逛逛</view>
    </view>
    <template v-else>
      <!-- 店铺头 -->
      <view class="card shop-card" @click="goShop">
        <image class="shop-logo" :src="shopIcon" mode="aspectFill" />
        <view class="shop-meta">
          <view class="shop-title-row">
            <text class="shop-name">{{ cart.shopName || '店铺' }}</text>
            <text class="arrow">›</text>
          </view>
          <text class="tip">点击回店继续加购</text>
        </view>
        <text class="clear" @click.stop="clearAll">清空</text>
      </view>

      <!-- 商品列表 -->
      <view class="card lines">
        <view v-for="line in cart.items" :key="line.id" class="line">
          <image class="thumb" :src="lineCover(line)" mode="aspectFill" />
          <view class="info">
            <text class="name">{{ line.goodsName }}</text>
            <text class="sku">{{ line.skuName || '默认规格' }}</text>
            <view class="price-row">
              <view class="price-box">
                <text class="yen">¥</text>
                <text class="money">{{ line.price }}</text>
              </view>
              <view class="ops">
                <text class="step minus" @click="changeQty(line, line.quantity - 1)">−</text>
                <text class="qty">{{ line.quantity }}</text>
                <text class="step plus" @click="changeQty(line, line.quantity + 1)">+</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 费用 -->
      <view class="card fees">
        <text class="sec-title">费用明细</text>
        <view class="fee-row">
          <text class="fee-label">商品小计</text>
          <text class="fee-val">¥{{ cart.goodsAmount }}</text>
        </view>
        <view class="fee-row">
          <text class="fee-label">配送费</text>
          <text class="fee-val">¥{{ cart.deliveryFee || 0 }}</text>
        </view>
        <view class="fee-row">
          <text class="fee-label">打包费</text>
          <text class="fee-val">¥{{ cart.packingFee || 0 }}</text>
        </view>
        <view class="fee-row total-row">
          <text class="fee-label strong">预估应付</text>
          <view class="est">
            <text class="yen">¥</text>
            <text class="est-num">{{ cart.payAmount || cart.goodsAmount }}</text>
          </view>
        </view>
        <view v-if="!cart.meetMinOrder" class="min-tip">
          还差 ¥{{ remainMin }} 起送（起送 ¥{{ cart.minOrderAmount }}）
        </view>
      </view>

      <view class="safe-spacer" />

      <!-- 结算条：抬高避开 tabBar -->
      <view class="footer">
        <view class="footer-left">
          <text class="footer-label">合计</text>
          <view class="footer-price">
            <text class="yen">¥</text>
            <text class="total">{{ cart.payAmount || cart.goodsAmount }}</text>
          </view>
          <text class="item-count">共{{ itemCount }}件</text>
        </view>
        <view
          :class="['checkout', !cart.meetMinOrder && 'disabled']"
          @click="goCheckout"
        >{{ cart.meetMinOrder ? '去结算' : '未满起送' }}</view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { cartApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { refreshTabBadges } from '../../utils/badge.js'
import { goodsImage, shopImage } from '../../utils/image.js'

const cart = ref(null)
const loading = ref(false)

const remainMin = computed(() => {
  const need = Number(cart.value?.minOrderAmount || 0)
  const got = Number(cart.value?.goodsAmount || 0)
  return Math.max(0, +(need - got).toFixed(2))
})

const itemCount = computed(() =>
  (cart.value?.items || []).reduce((n, i) => n + Number(i.quantity || 0), 0)
)

const shopIcon = computed(() =>
  shopImage(cart.value?.shopLogoUrl, cart.value?.shopId || cart.value?.shopName || 'x')
)

function lineCover(line) {
  return goodsImage(line.coverUrl, 'g' + (line.goodsId || line.skuId || line.id || 'x'))
}

async function load() {
  if (!requireLogin()) {
    uni.stopPullDownRefresh()
    return
  }
  loading.value = true
  try {
    cart.value = await cartApi.view()
  } catch (e) {
    cart.value = { items: [] }
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
    refreshTabBadges()
  }
}

async function changeQty(line, q) {
  if (q < 1) await cartApi.remove(line.id)
  else await cartApi.update(line.id, q)
  await load()
}

function clearAll() {
  uni.showModal({
    title: '清空购物车',
    content: '确定清空全部商品吗？',
    success: async (res) => {
      if (!res.confirm) return
      await cartApi.clear()
      await load()
    }
  })
}

function goCheckout() {
  if (!cart.value?.shopId) return
  if (!cart.value.meetMinOrder) {
    uni.showToast({ title: `还差 ¥${remainMin.value} 起送`, icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/checkout/index?shopId=' + cart.value.shopId })
}

function goShop() {
  if (!cart.value?.shopId) return
  uni.navigateTo({ url: '/pages/shop/detail?id=' + cart.value.shopId })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

onShow(load)
onPullDownRefresh(load)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}
.shop-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
}
.shop-logo {
  width: 72rpx;
  height: 72rpx;
  border-radius: 14rpx;
  background: #f0f0f0;
  flex-shrink: 0;
}
.shop-meta { flex: 1; min-width: 0; }
.shop-title-row { display: flex; align-items: center; gap: 4rpx; }
.shop-name {
  font-size: 30rpx;
  font-weight: 800;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 420rpx;
}
.arrow { color: #ccc; font-size: 28rpx; }
.tip { display: block; margin-top: 6rpx; font-size: 22rpx; color: #999; }
.clear { color: #999; font-size: 24rpx; flex-shrink: 0; padding: 8rpx 0 8rpx 16rpx; }

.lines { padding-top: 8rpx; padding-bottom: 8rpx; }
.line {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f3f3f3;
}
.line:last-child { border-bottom: none; }
.thumb {
  width: 144rpx;
  height: 144rpx;
  border-radius: 14rpx;
  background: #f0f0f0;
  flex-shrink: 0;
}
.info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.name {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sku {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #999;
}
.price-row {
  margin-top: auto;
  padding-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.price-box { display: flex; align-items: baseline; color: #e85d04; }
.yen { font-size: 22rpx; font-weight: 700; }
.money { font-size: 34rpx; font-weight: 800; }
.ops { display: flex; align-items: center; gap: 4rpx; }
.step {
  width: 48rpx;
  height: 48rpx;
  line-height: 44rpx;
  text-align: center;
  border-radius: 50%;
  font-size: 32rpx;
  font-weight: 600;
}
.step.minus {
  background: #f3f3f3;
  color: #666;
}
.step.plus {
  background: #0f3d2e;
  color: #fff;
}
.qty {
  min-width: 48rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 700;
  color: #222;
}

.sec-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #222;
  margin-bottom: 8rpx;
}
.fee-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  font-size: 26rpx;
}
.fee-label { color: #666; }
.fee-label.strong { color: #222; font-weight: 700; }
.fee-val { color: #222; font-weight: 600; }
.total-row {
  margin-top: 8rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f3f3f3;
}
.est { display: flex; align-items: baseline; color: #e85d04; }
.est-num { font-size: 36rpx; font-weight: 800; }
.min-tip {
  margin-top: 12rpx;
  padding: 14rpx 18rpx;
  border-radius: 12rpx;
  background: #fff7ed;
  color: #9a3412;
  font-size: 22rpx;
}

.safe-spacer { height: 20rpx; }

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(16rpx + env(safe-area-inset-bottom));
  z-index: 20;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #eee;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.04);
}
.footer-left { display: flex; align-items: baseline; gap: 8rpx; flex-wrap: wrap; }
.footer-label { font-size: 24rpx; color: #666; }
.footer-price { display: flex; align-items: baseline; color: #e85d04; }
.footer-price .yen { font-size: 24rpx; }
.total { font-size: 40rpx; font-weight: 800; }
.item-count { font-size: 22rpx; color: #999; margin-left: 4rpx; }
.checkout {
  min-width: 200rpx;
  padding: 20rpx 36rpx;
  border-radius: 40rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #f48c06, #e85d04);
}
.checkout.disabled { opacity: 0.45; }
.go-home { margin: 40rpx 80rpx 0; }
</style>
