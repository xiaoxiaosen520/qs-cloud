<template>
  <view class="page" v-if="goods">
    <view class="cover-wrap">
      <image class="cover" :src="cover" mode="aspectFill" />
      <text class="share-btn" @click="shareGoods">分享</text>
    </view>

    <view class="card main-card">
      <text class="name">{{ goods.name }}</text>
      <view class="price-row">
        <text class="price">¥{{ displayPrice }}</text>
        <text class="muted" v-if="skus.length > 1">起</text>
      </view>
      <text class="desc" v-if="goods.description">{{ goods.description }}</text>
      <text class="desc muted" v-else>新鲜供应，欢迎选购</text>
    </view>

    <view class="card shop-card" v-if="shop" @click="goShop">
      <image class="shop-logo" :src="shopCover" mode="aspectFill" />
      <view class="shop-meta">
        <text class="shop-name">{{ shop.name }}</text>
        <text class="shop-sub">评分 {{ Number(shop.score || 5).toFixed(1) }} · 进店逛逛</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <view class="card">
      <text class="sec-title">商品评价</text>
      <view v-if="!reviews.length" class="muted empty-reviews">暂无评价，下单后可评价</view>
      <view v-for="r in reviews" :key="r.id" class="review-item">
        <text class="stars">{{ '★'.repeat(r.score || 5) }}{{ '☆'.repeat(5 - (r.score || 5)) }}</text>
        <text class="review-text">{{ r.content || '好评' }}</text>
      </view>
    </view>

    <view class="safe-bottom" />

    <view class="footer">
      <view class="foot-shop" @click="goShop">
        <text class="foot-ico">店</text>
        <text class="foot-label">店铺</text>
      </view>
      <view class="foot-cart" @click="goShopCart">
        <text class="foot-ico">车</text>
        <text class="foot-label">购物车</text>
        <view class="foot-badge" v-if="cartCount > 0">{{ cartCount > 99 ? '99+' : cartCount }}</view>
      </view>
      <view class="foot-btn ghost" @click="openSku">选规格</view>
      <view class="foot-btn accent" @click="openSku">加入购物车</view>
    </view>

    <view v-if="skuSheet" class="mask" @click="skuSheet = false">
      <view class="sheet" @click.stop>
        <view class="sheet-head">
          <image class="sheet-cover" :src="cover" mode="aspectFill" />
          <view>
            <text class="sheet-name">{{ goods.name }}</text>
            <text class="price block">¥{{ selectedSku ? selectedSku.price : goods.minPrice }}</text>
          </view>
        </view>
        <text class="sku-label">规格</text>
        <view class="sku-list">
          <view
            v-for="s in skus"
            :key="s.id"
            :class="['sku', selectedSkuId === s.id && 'on']"
            @click="selectedSkuId = s.id"
          >
            <text>{{ s.name }}</text>
            <text class="sku-stock">库存 {{ s.stock }}</text>
          </view>
        </view>
        <view class="qty-row">
          <text>数量</text>
          <view class="qty">
            <text class="qbtn" @click="qty > 1 && qty--">-</text>
            <text class="qnum">{{ qty }}</text>
            <text class="qbtn plus" @click="qty++">+</text>
          </view>
        </view>
        <view class="btn-accent" @click="addCart">加入购物车</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { shopApi, cartApi } from '../../api/http.js'
import { getToken, requireLogin } from '../../utils/auth.js'
import { refreshTabBadges } from '../../utils/badge.js'
import { goodsImage, shopImage } from '../../utils/image.js'

const goodsId = ref(0)
const shopId = ref(0)
const goods = ref(null)
const skus = ref([])
const shop = ref(null)
const reviews = ref([])
const skuSheet = ref(false)
const selectedSkuId = ref(null)
const qty = ref(1)
const cartCount = ref(0)

const selectedSku = computed(() => skus.value.find((s) => s.id === selectedSkuId.value))
const cover = computed(() => goodsImage(goods.value?.coverUrl, 'g' + (goodsId.value || 'x')))
const shopCover = computed(() => shopImage(shop.value?.logoUrl, shopId.value || 's'))
const displayPrice = computed(() => {
  if (selectedSku.value) return selectedSku.value.price
  return goods.value?.minPrice
})

onLoad(async (q) => {
  goodsId.value = Number(q.id)
  shopId.value = Number(q.shopId || 0)
  const detail = await shopApi.goodsDetail(goodsId.value)
  goods.value = detail.goods
  skus.value = detail.skus || []
  selectedSkuId.value = skus.value[0] ? skus.value[0].id : null
  if (!shopId.value && goods.value?.shopId) shopId.value = goods.value.shopId
  if (shopId.value) {
    shop.value = await shopApi.detail(shopId.value)
    try {
      reviews.value = (await shopApi.reviews(shopId.value, 5)) || []
    } catch (e) {
      reviews.value = []
    }
  }
})

onShow(refreshCart)

async function refreshCart() {
  if (!getToken() || !shopId.value) {
    cartCount.value = 0
    return
  }
  try {
    const cart = await cartApi.view({ silent: true })
    const items = cart && cart.shopId === shopId.value ? (cart.items || []) : []
    cartCount.value = items.reduce((n, i) => n + i.quantity, 0)
  } catch (e) {
    cartCount.value = 0
  }
}

function goShop() {
  if (!shopId.value) return
  uni.navigateTo({ url: '/pages/shop/detail?id=' + shopId.value })
}

function goShopCart() {
  goShop()
}

function openSku() {
  if (!skus.value.length) {
    uni.showToast({ title: '暂无可售规格', icon: 'none' })
    return
  }
  qty.value = 1
  skuSheet.value = true
}

function confirmSwitchShop(otherName) {
  return new Promise((resolve) => {
    uni.showModal({
      title: '切换店铺',
      content: `购物车里还有「${otherName || '其他店铺'}」的商品，加购将清空原购物车，是否继续？`,
      confirmText: '清空并加购',
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false)
    })
  })
}

async function addCart() {
  if (!requireLogin()) return
  if (!selectedSkuId.value) return
  try {
    const cart = await cartApi.view({ silent: true })
    if (cart?.shopId && cart.shopId !== shopId.value && (cart.items || []).length) {
      const ok = await confirmSwitchShop(cart.shopName)
      if (!ok) return
    }
  } catch (e) { /* ignore */ }
  await cartApi.add(selectedSkuId.value, qty.value)
  skuSheet.value = false
  uni.showToast({ title: '已加入购物车', icon: 'success' })
  await refreshCart()
  refreshTabBadges()
}

function shareGoods() {
  const title = goods.value?.name || '区惠好物'
  const path = `/pages/goods/detail?id=${goodsId.value}&shopId=${shopId.value}`
  // #ifdef MP-WEIXIN
  uni.showShareMenu({ withShareTicket: true, menus: ['shareAppMessage', 'shareTimeline'] })
  uni.showToast({ title: '请点击右上角分享', icon: 'none' })
  // #endif
  // #ifndef MP-WEIXIN
  const link = path
  uni.setClipboardData({
    data: `${title} ${link}`,
    success: () => uni.showToast({ title: '链接已复制', icon: 'none' })
  })
  // #endif
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding-bottom: calc(140rpx + env(safe-area-inset-bottom)); }
.cover-wrap { position: relative; width: 100%; height: 560rpx; background: #f0f0f0; }
.cover { width: 100%; height: 100%; }
.share-btn {
  position: absolute; right: 24rpx; top: 24rpx;
  background: rgba(0,0,0,.45); color: #fff; font-size: 24rpx;
  padding: 10rpx 22rpx; border-radius: 28rpx;
}
.main-card { margin-top: -32rpx; position: relative; z-index: 2; }
.name { display: block; font-size: 36rpx; font-weight: 800; color: #222; line-height: 1.35; }
.price-row { display: flex; align-items: baseline; gap: 8rpx; margin-top: 16rpx; }
.price { font-size: 44rpx; font-weight: 800; color: #e85d04; }
.desc { display: block; margin-top: 16rpx; font-size: 26rpx; color: #666; line-height: 1.5; }

.shop-card {
  display: flex; align-items: center; gap: 16rpx;
}
.shop-logo {
  width: 72rpx; height: 72rpx; border-radius: 12rpx; background: #f0f0f0; flex-shrink: 0;
}
.shop-meta { flex: 1; min-width: 0; }
.shop-name {
  display: block; font-size: 28rpx; font-weight: 700;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.shop-sub { display: block; margin-top: 6rpx; font-size: 22rpx; color: #999; }
.arrow { color: #ccc; font-size: 32rpx; }

.sec-title { display: block; font-size: 28rpx; font-weight: 700; margin-bottom: 12rpx; }
.empty-reviews { padding: 24rpx 0; text-align: center; }
.review-item { padding: 18rpx 0; border-top: 1rpx solid #f3f3f3; }
.stars { display: block; color: #e85d04; font-size: 24rpx; }
.review-text { display: block; margin-top: 8rpx; font-size: 26rpx; color: #333; }

.safe-bottom { height: 20rpx; }
.footer {
  position: fixed; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; gap: 12rpx;
  padding: 12rpx 20rpx calc(12rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1rpx solid #eee; z-index: 10;
}
.foot-shop, .foot-cart {
  display: flex; flex-direction: column; align-items: center;
  width: 80rpx; position: relative;
}
.foot-ico {
  width: 44rpx; height: 44rpx; line-height: 44rpx; text-align: center;
  background: #f3f6f4; border-radius: 12rpx; font-size: 22rpx; font-weight: 700; color: #0f3d2e;
}
.foot-label { font-size: 18rpx; color: #666; margin-top: 4rpx; }
.foot-badge {
  position: absolute; top: -4rpx; right: 4rpx;
  min-width: 28rpx; height: 28rpx; padding: 0 6rpx;
  background: #e85d04; color: #fff; border-radius: 14rpx;
  font-size: 18rpx; text-align: center; line-height: 28rpx;
}
.foot-btn {
  flex: 1; text-align: center; padding: 22rpx 0;
  border-radius: 40rpx; font-weight: 700; font-size: 26rpx;
}
.foot-btn.ghost {
  border: 2rpx solid #0f3d2e; color: #0f3d2e; background: #fff;
}
.foot-btn.accent {
  background: linear-gradient(135deg, #f48c06, #e85d04); color: #fff;
}

.mask {
  position: fixed; inset: 0; background: rgba(0,0,0,.5);
  display: flex; align-items: flex-end; z-index: 30;
}
.sheet {
  width: 100%; background: #fff; border-radius: 28rpx 28rpx 0 0;
  padding: 32rpx 28rpx calc(32rpx + env(safe-area-inset-bottom));
}
.sheet-head { display: flex; gap: 20rpx; margin-bottom: 24rpx; }
.sheet-cover {
  width: 120rpx; height: 120rpx; border-radius: 16rpx; margin-top: -60rpx;
  background: #f0f0f0; flex-shrink: 0;
}
.sheet-name { font-size: 30rpx; font-weight: 700; }
.block { display: block; margin-top: 8rpx; }
.sku-label { font-weight: 700; }
.sku-list { margin: 16rpx 0 24rpx; }
.sku {
  display: flex; justify-content: space-between; align-items: center;
  padding: 22rpx 20rpx; margin-bottom: 12rpx; border-radius: 14rpx;
  background: #f3f6f4; border: 2rpx solid transparent;
}
.sku.on { background: #fff7ed; border-color: #e85d04; }
.sku-stock { color: #999; font-size: 22rpx; }
.qty-row {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx;
}
.qty { display: flex; align-items: center; gap: 20rpx; }
.qbtn {
  width: 52rpx; height: 52rpx; line-height: 52rpx; text-align: center;
  background: #eef3f0; border-radius: 12rpx; font-size: 32rpx;
}
.qbtn.plus { background: #e85d04; color: #fff; }
.qnum { min-width: 40rpx; text-align: center; font-weight: 700; }
</style>
