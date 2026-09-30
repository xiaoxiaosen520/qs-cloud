<template>
  <view class="page" v-if="shop">
    <!-- 店头：大头像 + 名称 + 指标（闪购风格） -->
    <view class="shop-head">
      <view class="head-main" @click="infoSheet = true">
        <image class="shop-avatar" :src="shopCover" mode="aspectFill" />
        <view class="head-info">
          <view class="name-row">
            <text class="shop-name">{{ shop.name }}</text>
            <text class="name-arrow">›</text>
          </view>
          <view class="metrics">
            <text class="m-item">评分 {{ Number(shop.score || 5).toFixed(1) }}</text>
            <text class="m-dot">·</text>
            <text class="m-item">月售 {{ shop.monthSales || 0 }}</text>
            <text class="m-dot">·</text>
            <text class="m-item">约{{ etaMins }}分钟</text>
          </view>
          <view class="fee-row">
            <text>起送 ¥{{ shop.minOrderAmount }}</text>
            <text class="sep">|</text>
            <text>配送 ¥{{ shop.deliveryFee || 0 }}</text>
            <text v-if="shop.businessHours" class="sep">|</text>
            <text v-if="shop.businessHours" class="hours">{{ shop.businessHours }}</text>
          </view>
          <text class="feat" v-if="shop.notice">{{ shop.notice }}</text>
        </view>
      </view>
      <text class="fav-btn" @click.stop="toggleFav">{{ favored ? '已收藏' : '收藏' }}</text>
    </view>

    <!-- 商品 / 评价 -->
    <view class="main-tabs">
      <view :class="['mtab', mainTab === 'goods' && 'on']" @click="mainTab = 'goods'">商品</view>
      <view :class="['mtab', mainTab === 'review' && 'on']" @click="mainTab = 'review'">评价</view>
    </view>

    <template v-if="mainTab === 'goods'">
      <view class="search-row">
        <input class="goods-search" v-model="goodsKw" placeholder="店内搜索商品" confirm-type="search" />
      </view>

      <view class="menu">
        <scroll-view scroll-y class="side">
          <view
            v-for="c in categories"
            :key="c.id"
            :class="['side-item', categoryId === c.id && 'on']"
            @click="selectCat(c.id)"
          >{{ c.name }}</view>
        </scroll-view>

        <scroll-view scroll-y class="main">
          <view v-if="!displayGoods.length" class="muted empty-goods">
            {{ goodsKw ? '没有匹配商品' : '该分类暂无商品' }}
          </view>
          <view v-for="g in displayGoods" :key="g.id" class="goods" @click="goGoods(g)">
            <image class="g-cover-img" :src="goodsCover(g)" mode="aspectFill" />
            <view class="g-body">
              <text class="g-name">{{ g.name }}</text>
              <text class="g-desc">{{ g.description || '新鲜供应，点击看详情' }}</text>
              <view class="g-bottom">
                <text class="price">¥{{ g.minPrice }}</text>
                <view class="plus" @click.stop="quickAdd(g)">+</view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </template>

    <view v-else class="review-pane">
      <view v-if="!reviews.length" class="empty-reviews muted">暂无评价</view>
      <view v-for="r in reviews" :key="r.id" class="review-item">
        <text class="review-stars">{{ '★'.repeat(r.score || 5) }}{{ '☆'.repeat(5 - (r.score || 5)) }}</text>
        <text class="review-content">{{ r.content || '好评' }}</text>
        <view class="review-photos" v-if="reviewImages(r).length">
          <image
            v-for="(url, i) in reviewImages(r)"
            :key="url + i"
            class="review-photo"
            :src="url"
            mode="aspectFill"
            @click="previewReview(r, i)"
          />
        </view>
        <text class="review-time" v-if="r.createdAt">{{ formatTime(r.createdAt) }}</text>
      </view>
    </view>

    <!-- 商家信息弹层 -->
    <view v-if="infoSheet" class="mask info-mask" @click="infoSheet = false">
      <view class="info-sheet" @click.stop>
        <view class="info-tabs">
          <text :class="['itab', infoTab === 'info' && 'on']" @click="infoTab = 'info'">商家信息</text>
          <text :class="['itab', infoTab === 'review' && 'on']" @click="infoTab = 'review'">评价</text>
          <text class="info-close" @click="infoSheet = false">✕</text>
        </view>

        <scroll-view scroll-y class="info-body" v-if="infoTab === 'info'">
          <view class="info-avatar-row">
            <image class="info-avatar" :src="shopCover" mode="aspectFill" />
            <view>
              <text class="info-shop-name">{{ shop.name }}</text>
              <text class="info-sub">{{ typeLabel }}</text>
            </view>
          </view>

          <text class="sec">商家服务</text>
          <view class="svc-row">
            <text class="svc-label">配送服务</text>
            <text class="svc-desc">起送 ¥{{ shop.minOrderAmount }}，配送费 ¥{{ shop.deliveryFee || 0 }}</text>
          </view>

          <text class="sec">基础信息</text>
          <view class="base-row" v-if="shop.address">
            <text class="base-ico">址</text>
            <text class="base-text">{{ shop.address }}</text>
            <view class="call-btn" v-if="shop.phone" @click="callShop">致电</view>
          </view>
          <view class="base-row">
            <text class="base-ico">类</text>
            <text class="base-text">商家品类：{{ typeLabel }}</text>
          </view>
          <view class="base-row" v-if="shop.businessHours">
            <text class="base-ico">时</text>
            <text class="base-text">营业时间：{{ shop.businessHours }}</text>
          </view>
          <view class="base-row" v-if="shop.licenseUrl" @click="previewLicense">
            <text class="base-ico">证</text>
            <text class="base-text">查看营业资质</text>
            <text class="chev">查看 ›</text>
          </view>
          <view class="base-row" @click="goService">
            <text class="base-ico">诉</text>
            <text class="base-text">投诉反馈</text>
            <text class="chev">›</text>
          </view>
        </scroll-view>

        <scroll-view scroll-y class="info-body" v-else>
          <view v-if="!reviews.length" class="empty-reviews muted">暂无评价</view>
          <view v-for="r in reviews" :key="'i' + r.id" class="review-item">
            <text class="review-stars">{{ '★'.repeat(r.score || 5) }}</text>
            <text class="review-content">{{ r.content || '好评' }}</text>
            <view class="review-photos" v-if="reviewImages(r).length">
              <image
                v-for="(url, i) in reviewImages(r)"
                :key="'s' + url + i"
                class="review-photo"
                :src="url"
                mode="aspectFill"
                @click="previewReview(r, i)"
              />
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view v-if="!skuSheet && mainTab === 'goods'" class="bar">
      <view class="cart-bubble" @click="toggleCartPanel">
        <view class="bubble-icon">车</view>
        <view class="badge" v-if="cartCount > 0">{{ cartCount > 99 ? '99+' : cartCount }}</view>
      </view>
      <view class="bar-mid" @click="toggleCartPanel">
        <text class="bar-price" v-if="cartAmount > 0">¥{{ cartAmount }}</text>
        <text class="bar-tip" v-else>未选购商品</text>
        <text class="bar-min" v-if="cartAmount > 0 && !meetMin">还差 ¥{{ remainMin }} 起送</text>
        <text class="bar-min ok" v-else-if="meetMin">配送 ¥{{ shop.deliveryFee || 0 }}</text>
        <text class="bar-min" v-else>配送 ¥{{ shop.deliveryFee || 0 }}（起送 ¥{{ shop.minOrderAmount }}）</text>
      </view>
      <view
        :class="['bar-btn', cartCount > 0 && meetMin ? 'ready' : (cartCount > 0 ? 'warn' : 'disabled')]"
        @click="goCheckout"
      >
        {{ cartCount === 0 ? `¥${shop.minOrderAmount}起送` : (meetMin ? '去结算' : '差起送') }}
      </view>
    </view>

    <view v-if="cartPanel" class="mask cart-mask" @click="cartPanel = false">
      <view class="cart-sheet" @click.stop>
        <view class="cart-sheet-head">
          <text class="cart-sheet-title">已选商品</text>
          <text class="cart-clear" v-if="cartItems.length" @click="clearShopCart">清空</text>
        </view>
        <scroll-view scroll-y class="cart-list" v-if="cartItems.length">
          <view v-for="line in cartItems" :key="line.id" class="cart-line">
            <image class="cart-thumb-img" :src="lineCover(line)" mode="aspectFill" />
            <view class="cart-info">
              <text class="cart-name">{{ line.goodsName }}</text>
              <text class="cart-sku">{{ line.skuName }}</text>
              <text class="price">¥{{ line.price }}</text>
            </view>
            <view class="qty">
              <text class="qbtn" @click="changeLineQty(line, line.quantity - 1)">-</text>
              <text class="qnum">{{ line.quantity }}</text>
              <text class="qbtn plus" @click="changeLineQty(line, line.quantity + 1)">+</text>
            </view>
          </view>
        </scroll-view>
        <view v-else class="cart-empty muted">还没有选购商品</view>
      </view>
    </view>

    <view v-if="skuSheet" class="mask sku-mask" @click="skuSheet = false">
      <view class="sheet" @click.stop>
        <view class="sheet-head">
          <image class="sheet-cover-img" :src="goodsCover(currentGoods)" mode="aspectFill" />
          <view>
            <text class="g-name">{{ currentGoods && currentGoods.name }}</text>
            <text class="price block">¥{{ selectedSku ? selectedSku.price : currentGoods.minPrice }}</text>
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
            <text class="qbtn" @click="qty++">+</text>
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
import { shopApi, cartApi, favoriteApi, absUrl } from '../../api/http.js'
import { getToken, requireLogin } from '../../utils/auth.js'
import { refreshTabBadges } from '../../utils/badge.js'
import { isFavorite, toggleFavorite } from '../../utils/favorite.js'
import { goodsImage, shopImage } from '../../utils/image.js'

const shopId = ref(0)
const shop = ref(null)
const categories = ref([])
const categoryId = ref(null)
const goods = ref([])
const goodsKw = ref('')
const skuSheet = ref(false)
const cartPanel = ref(false)
const currentGoods = ref(null)
const skus = ref([])
const selectedSkuId = ref(null)
const qty = ref(1)
const cartItems = ref([])
const cartCount = ref(0)
const cartAmount = ref(0)
const meetMin = ref(false)
const remainMin = ref(0)
const favored = ref(false)
const reviews = ref([])
const mainTab = ref('goods')
const infoSheet = ref(false)
const infoTab = ref('info')

const selectedSku = computed(() => skus.value.find((s) => s.id === selectedSkuId.value))
const shopCover = computed(() =>
  shopImage(shop.value?.logoUrl, shop.value?.id || shop.value?.name || shopId.value)
)
const typeLabel = computed(() =>
  shop.value?.shopType === 'FOOD' ? '餐饮外卖' : '便民商店/便利店'
)
const etaMins = computed(() => 25)
const displayGoods = computed(() => {
  const kw = goodsKw.value.trim()
  if (!kw) return goods.value
  return goods.value.filter((g) => (g.name || '').includes(kw) || (g.description || '').includes(kw))
})

function goodsCover(g) {
  if (!g) return goodsImage('', 'g')
  return goodsImage(g.coverUrl, 'g' + (g.id || g.name || 'x'))
}
function lineCover(line) {
  return goodsImage(line.coverUrl, 'g' + (line.goodsId || line.skuId || line.id || 'x'))
}
function reviewImages(r) {
  return (r?.imageUrls || []).map((u) => absUrl(u)).filter(Boolean)
}
function previewReview(r, index) {
  const urls = reviewImages(r)
  if (!urls.length) return
  uni.previewImage({ current: index, urls })
}
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

onLoad(async (q) => {
  shopId.value = Number(q.id)
  shop.value = await shopApi.detail(shopId.value)
  favored.value = isFavorite(shopId.value)
  if (getToken()) {
    try {
      const ids = (await favoriteApi.ids()) || []
      favored.value = ids.map(Number).includes(shopId.value)
    } catch (e) { /* ignore */ }
  }
  categories.value = (await shopApi.categories(shopId.value)) || []
  if (categories.value.length) categoryId.value = categories.value[0].id
  await loadGoods()
  try {
    reviews.value = (await shopApi.reviews(shopId.value, 20)) || []
  } catch (e) {
    reviews.value = []
  }
})

onShow(refreshCart)

async function loadGoods() {
  goods.value = (await shopApi.goods(shopId.value, categoryId.value)) || []
}

async function refreshCart() {
  if (!getToken()) {
    cartItems.value = []
    cartCount.value = 0
    cartAmount.value = 0
    meetMin.value = false
    remainMin.value = Number(shop.value?.minOrderAmount || 0)
    return
  }
  try {
    const cart = await cartApi.view()
    const sameShop = cart && cart.shopId === shopId.value
    const items = sameShop ? ((cart && cart.items) || []) : []
    cartItems.value = items
    cartCount.value = items.reduce((n, i) => n + i.quantity, 0)
    cartAmount.value = sameShop ? Number(cart.goodsAmount || 0) : 0
    meetMin.value = sameShop ? !!cart.meetMinOrder : false
    const need = Number(sameShop ? cart.minOrderAmount : (shop.value?.minOrderAmount || 0))
    remainMin.value = Math.max(0, +(need - cartAmount.value).toFixed(2))
    if (!items.length) cartPanel.value = false
  } catch (e) {
    cartItems.value = []
    cartCount.value = 0
    cartAmount.value = 0
    meetMin.value = false
  }
}

async function toggleFav() {
  if (!requireLogin()) return
  try {
    const res = await favoriteApi.toggle(shopId.value)
    favored.value = !!res.favorited
    const localOn = isFavorite(shopId.value)
    if (favored.value !== localOn) toggleFavorite(shopId.value)
    uni.showToast({ title: favored.value ? '已收藏' : '已取消收藏', icon: 'none' })
  } catch (e) { /* toast by http */ }
}

function callShop() {
  if (!shop.value?.phone) return
  uni.makePhoneCall({ phoneNumber: String(shop.value.phone) })
}

function previewLicense() {
  const url = absUrl(shop.value?.licenseUrl)
  if (!url) return
  uni.previewImage({ urls: [url] })
}

function goService() {
  infoSheet.value = false
  uni.navigateTo({ url: '/pages/mine/service' })
}

function selectCat(id) {
  categoryId.value = id
  loadGoods()
}

function goGoods(g) {
  uni.navigateTo({ url: `/pages/goods/detail?id=${g.id}&shopId=${shopId.value}` })
}

async function quickAdd(g) {
  const detail = await shopApi.goodsDetail(g.id)
  const list = detail.skus || []
  if (!list.length) {
    uni.showToast({ title: '暂无可售规格', icon: 'none' })
    return
  }
  if (list.length === 1 && (list[0].stock == null || list[0].stock > 0)) {
    currentGoods.value = g
    skus.value = list
    selectedSkuId.value = list[0].id
    qty.value = 1
    await addCart()
    return
  }
  await pickGoods(g)
}

async function pickGoods(g) {
  currentGoods.value = g
  qty.value = 1
  const detail = await shopApi.goodsDetail(g.id)
  skus.value = detail.skus || []
  selectedSkuId.value = skus.value[0] ? skus.value[0].id : null
  skuSheet.value = true
  cartPanel.value = false
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

function toggleCartPanel() {
  if (!cartCount.value) {
    uni.showToast({ title: '请先选购商品', icon: 'none' })
    return
  }
  cartPanel.value = !cartPanel.value
}

async function changeLineQty(line, q) {
  if (q < 1) await cartApi.remove(line.id)
  else await cartApi.update(line.id, q)
  await refreshCart()
  refreshTabBadges()
}

function clearShopCart() {
  uni.showModal({
    title: '清空已选',
    content: '确定清空本店已选商品吗？',
    success: async (res) => {
      if (!res.confirm) return
      await cartApi.clear()
      cartPanel.value = false
      await refreshCart()
      refreshTabBadges()
    }
  })
}

function goCheckout() {
  if (!cartCount.value) {
    uni.showToast({ title: `满 ¥${shop.value?.minOrderAmount || 0} 起送`, icon: 'none' })
    return
  }
  if (!meetMin.value) {
    cartPanel.value = true
    uni.showToast({ title: `还差 ¥${remainMin.value} 起送`, icon: 'none' })
    return
  }
  cartPanel.value = false
  uni.navigateTo({ url: '/pages/checkout/index?shopId=' + shopId.value })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 140rpx; }

.shop-head {
  display: flex; gap: 16rpx; align-items: flex-start;
  padding: 24rpx 24rpx 20rpx; background: #fff;
}
.head-main { flex: 1; display: flex; gap: 20rpx; min-width: 0; }
.shop-avatar {
  width: 128rpx; height: 128rpx; border-radius: 16rpx;
  background: #f0f0f0; flex-shrink: 0; border: 1rpx solid #eee;
}
.head-info { flex: 1; min-width: 0; }
.name-row { display: flex; align-items: center; gap: 4rpx; }
.shop-name {
  font-size: 34rpx; font-weight: 800; color: #222;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 420rpx;
}
.name-arrow { color: #ccc; font-size: 28rpx; }
.metrics {
  margin-top: 10rpx; display: flex; flex-wrap: wrap; align-items: center;
  font-size: 22rpx; color: #666;
}
.m-dot { margin: 0 8rpx; color: #ddd; }
.fee-row {
  margin-top: 8rpx; font-size: 22rpx; color: #999;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.sep { margin: 0 8rpx; color: #e5e5e5; }
.hours { color: #999; }
.feat {
  display: block; margin-top: 10rpx; font-size: 22rpx; color: #9a3412;
  background: #fff7ed; padding: 6rpx 12rpx; border-radius: 8rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.fav-btn {
  flex-shrink: 0; font-size: 22rpx; color: #e85d04;
  border: 1rpx solid #fdba74; border-radius: 24rpx; padding: 8rpx 18rpx;
  margin-top: 8rpx;
}

.main-tabs {
  display: flex; background: #fff; border-bottom: 1rpx solid #f0f0f0;
  padding: 0 24rpx;
}
.mtab {
  position: relative; padding: 22rpx 28rpx; font-size: 28rpx; color: #666;
}
.mtab.on { color: #222; font-weight: 800; }
.mtab.on::after {
  content: ''; position: absolute; left: 50%; bottom: 0;
  width: 40rpx; height: 6rpx; margin-left: -20rpx;
  background: #e85d04; border-radius: 6rpx;
}

.search-row { padding: 12rpx 24rpx; background: #fff; }
.goods-search {
  background: #f5f5f5; border-radius: 28rpx; padding: 14rpx 24rpx; font-size: 24rpx;
}

.menu { display: flex; height: calc(100vh - 420rpx); background: #fff; margin-top: 2rpx; }
.side { width: 168rpx; background: #f7f7f7; height: 100%; }
.side-item {
  padding: 28rpx 12rpx; text-align: center; color: #666; font-size: 24rpx;
  border-left: 6rpx solid transparent;
}
.side-item.on {
  background: #fff; color: #222; font-weight: 700;
  border-left-color: #e85d04;
}
.main { flex: 1; height: 100%; padding: 8rpx 20rpx 40rpx; box-sizing: border-box; }
.empty-goods { padding: 60rpx 20rpx; text-align: center; }
.goods {
  display: flex; gap: 16rpx; padding: 20rpx 0;
  border-bottom: 1rpx solid #f3f3f3;
}
.g-cover-img {
  width: 140rpx; height: 140rpx; border-radius: 12rpx; flex-shrink: 0; background: #f0f0f0;
}
.g-body { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.g-name { font-size: 28rpx; font-weight: 700; color: #222; }
.g-desc {
  margin-top: 8rpx; color: #999; font-size: 22rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.g-bottom {
  margin-top: auto; display: flex; justify-content: space-between; align-items: center;
}
.plus {
  width: 44rpx; height: 44rpx; border-radius: 50%;
  background: #e85d04; color: #fff; text-align: center; line-height: 44rpx; font-size: 32rpx;
}

.review-pane { background: #fff; padding: 8rpx 28rpx 40rpx; min-height: 40vh; }
.empty-reviews { padding: 80rpx 0; text-align: center; }
.review-item { padding: 24rpx 0; border-bottom: 1rpx solid #f3f3f3; }
.review-stars { display: block; color: #e85d04; font-size: 24rpx; }
.review-content { display: block; margin-top: 8rpx; font-size: 26rpx; color: #333; line-height: 1.5; }
.review-time { display: block; margin-top: 8rpx; font-size: 22rpx; color: #bbb; }
.review-photos { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 12rpx; }
.review-photo { width: 160rpx; height: 160rpx; border-radius: 10rpx; background: #f3f3f3; }

.info-mask { z-index: 50; }
.info-sheet {
  width: 100%; max-height: 78vh; background: #fff;
  border-radius: 24rpx 24rpx 0 0; display: flex; flex-direction: column;
}
.info-tabs {
  display: flex; align-items: center; padding: 28rpx 28rpx 0; position: relative;
}
.itab {
  margin-right: 40rpx; padding-bottom: 16rpx; font-size: 30rpx; color: #999;
}
.itab.on {
  color: #222; font-weight: 800;
  border-bottom: 4rpx solid #222;
}
.info-close {
  position: absolute; right: 24rpx; top: 24rpx;
  width: 52rpx; height: 52rpx; line-height: 52rpx; text-align: center;
  background: #f3f3f3; border-radius: 12rpx; color: #666; font-size: 28rpx;
}
.info-body { flex: 1; max-height: 62vh; padding: 24rpx 28rpx 40rpx; box-sizing: border-box; }
.info-avatar-row { display: flex; gap: 20rpx; align-items: center; margin-bottom: 28rpx; }
.info-avatar {
  width: 96rpx; height: 96rpx; border-radius: 16rpx; background: #f0f0f0;
}
.info-shop-name { display: block; font-size: 32rpx; font-weight: 800; }
.info-sub { display: block; margin-top: 6rpx; font-size: 22rpx; color: #999; }
.sec {
  display: block; font-size: 28rpx; font-weight: 800; color: #222;
  margin: 16rpx 0 16rpx;
}
.svc-row {
  display: flex; gap: 20rpx; padding: 8rpx 0 20rpx; font-size: 26rpx;
}
.svc-label { color: #222; font-weight: 600; flex-shrink: 0; }
.svc-desc { color: #666; flex: 1; }
.base-row {
  display: flex; align-items: center; gap: 16rpx;
  padding: 22rpx 0; border-bottom: 1rpx solid #f5f5f5; font-size: 26rpx;
}
.base-ico {
  width: 40rpx; height: 40rpx; border-radius: 10rpx;
  background: #f3f6f4; color: #0f3d2e; font-size: 20rpx; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.base-text { flex: 1; color: #333; line-height: 1.4; }
.call-btn {
  flex-shrink: 0; border: 1rpx solid #ddd; border-radius: 28rpx;
  padding: 8rpx 20rpx; font-size: 24rpx; color: #333;
}
.chev { color: #999; font-size: 24rpx; flex-shrink: 0; }

.bar {
  position: fixed; left: 24rpx; right: 24rpx; bottom: calc(16rpx + env(safe-area-inset-bottom));
  height: 96rpx; border-radius: 48rpx; background: #1a1a1a;
  display: flex; align-items: center; padding: 0 8rpx 0 20rpx;
  box-shadow: 0 12rpx 32rpx rgba(0,0,0,.25);
  z-index: 30;
}
.cart-bubble { position: relative; width: 72rpx; height: 72rpx; }
.bubble-icon {
  width: 72rpx; height: 72rpx; border-radius: 50%;
  background: #0f3d2e; color: #fff; text-align: center; line-height: 72rpx; font-weight: 700;
}
.badge {
  position: absolute; top: -6rpx; right: -6rpx;
  min-width: 32rpx; height: 32rpx; padding: 0 6rpx;
  background: #e85d04; color: #fff; border-radius: 16rpx;
  font-size: 18rpx; text-align: center; line-height: 32rpx;
}
.bar-mid { flex: 1; padding-left: 20rpx; color: #fff; min-width: 0; }
.bar-price { display: block; font-size: 34rpx; font-weight: 700; }
.bar-tip { color: rgba(255,255,255,.55); font-size: 24rpx; }
.bar-min { display: block; font-size: 20rpx; color: #f48c06; margin-top: 2rpx; }
.bar-min.ok { color: #95d5b2; }
.bar-btn {
  min-width: 180rpx; height: 80rpx; line-height: 80rpx; text-align: center;
  border-radius: 40rpx; font-weight: 700; color: #fff; font-size: 26rpx; padding: 0 16rpx;
}
.bar-btn.ready { background: #e85d04; }
.bar-btn.warn { background: #6b4f2a; }
.bar-btn.disabled { background: #fdba74; color: #fff; }

.mask {
  position: fixed; inset: 0; background: rgba(0,0,0,.5);
  display: flex; align-items: flex-end;
}
.cart-mask { z-index: 20; }
.sku-mask { z-index: 40; }
.cart-sheet {
  width: 100%; background: #fff; border-radius: 28rpx 28rpx 0 0;
  padding: 28rpx 28rpx calc(140rpx + env(safe-area-inset-bottom));
  max-height: 70vh;
}
.cart-sheet-head {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx;
}
.cart-sheet-title { font-size: 30rpx; font-weight: 800; }
.cart-clear { color: #6b7c74; font-size: 24rpx; }
.cart-list { max-height: 48vh; }
.cart-empty { padding: 48rpx 0; text-align: center; }
.cart-line {
  display: flex; gap: 16rpx; align-items: center;
  padding: 18rpx 0; border-bottom: 1rpx solid #eef3f0;
}
.cart-thumb-img {
  width: 88rpx; height: 88rpx; border-radius: 14rpx; flex-shrink: 0; background: #f0f0f0;
}
.cart-info { flex: 1; min-width: 0; }
.cart-name {
  display: block; font-weight: 700; font-size: 26rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.cart-sku { display: block; color: #6b7c74; font-size: 22rpx; margin: 4rpx 0 6rpx; }
.qty .qbtn.plus { background: #e85d04; color: #fff; }
.sheet {
  width: 100%; background: #fff; border-radius: 28rpx 28rpx 0 0;
  padding: 32rpx 28rpx calc(32rpx + env(safe-area-inset-bottom));
}
.sheet-head { display: flex; gap: 20rpx; margin-bottom: 24rpx; }
.sheet-cover-img {
  width: 120rpx; height: 120rpx; border-radius: 16rpx; margin-top: -60rpx;
  background: #f0f0f0; flex-shrink: 0;
}
.block { display: block; margin-top: 8rpx; }
.sku-label { font-weight: 700; color: #14201b; }
.sku-list { margin: 16rpx 0 24rpx; }
.sku {
  display: flex; justify-content: space-between; align-items: center;
  padding: 22rpx 20rpx; margin-bottom: 12rpx; border-radius: 14rpx;
  background: #f3f6f4; border: 2rpx solid transparent;
}
.sku.on { background: #fff7ed; border-color: #e85d04; }
.sku-stock { color: #6b7c74; font-size: 22rpx; }
.qty-row {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx;
}
.qty { display: flex; align-items: center; gap: 20rpx; }
.qbtn {
  width: 52rpx; height: 52rpx; line-height: 52rpx; text-align: center;
  background: #eef3f0; border-radius: 12rpx; font-size: 32rpx;
}
.qnum { min-width: 40rpx; text-align: center; font-weight: 700; }
</style>
