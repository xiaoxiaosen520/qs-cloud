<template>
  <view class="page" v-if="shop">
    <view class="hero">
      <image v-if="shop.logoUrl" class="logo-img" :src="absUrl(shop.logoUrl)" mode="aspectFill" />
      <view v-else class="logo" :style="{ background: coverColor(shop.name) }">
        <text class="logo-text">{{ shortName(shop.name) }}</text>
      </view>
      <view class="hero-meta">
        <text class="name">{{ shop.name }}</text>
        <text class="sub">{{ typeText }} · {{ shop.address || '未填地址' }}</text>
      </view>
    </view>

    <view class="card">
      <text class="section-title">店铺头像</text>
      <view class="logo-edit">
        <image v-if="logoPreview" class="logo-preview" :src="logoPreview" mode="aspectFill" />
        <view v-else class="logo-preview empty">暂无</view>
        <view class="logo-actions">
          <view class="btn-ghost mini" @click="pickLogo">上传头像</view>
          <text class="muted tip">买家端首页/店铺页将展示</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="row">
        <view>
          <text class="row-title">营业状态</text>
          <text class="muted">关闭后用户端不可下单</text>
        </view>
        <switch :checked="form.openStatus === 1" @change="onOpen" color="#e85d04" />
      </view>
    </view>

    <view class="card">
      <text class="section-title">运营设置</text>
      <text class="label">店铺公告</text>
      <input class="input" v-model="form.notice" placeholder="如：今日满 30 减 5" />
      <text class="label">营业时间</text>
      <input class="input" v-model="form.businessHours" placeholder="如 09:00-22:00" />
      <view class="row2">
        <view class="col">
          <text class="label">起送价</text>
          <input class="input" type="digit" v-model="form.minOrderAmount" placeholder="0" />
        </view>
        <view class="col">
          <text class="label">配送费</text>
          <input class="input" type="digit" v-model="form.deliveryFee" placeholder="3" />
        </view>
        <view class="col">
          <text class="label">打包费</text>
          <input class="input" type="digit" v-model="form.packingFee" placeholder="0" />
        </view>
      </view>
    </view>

    <view class="card link-card" @click="goApply">
      <text class="row-title">入驻申请记录</text>
      <text class="muted">查看审核状态 ›</text>
    </view>

    <view class="safe-bottom" />
    <view class="action-bar">
      <view class="btn-accent bar-btn" @click="save">保存设置</view>
    </view>
  </view>

  <view v-else class="empty">
    <view class="empty-icon" />
    <text class="empty-title">暂无店铺</text>
    <text class="muted">请先完成入驻申请</text>
    <view class="btn-accent empty-btn" @click="goApply">去入驻</view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { shopApi, uploadFile, absUrl } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { coverColor, shortName } from '../../utils/format.js'

const shop = ref(null)
const form = ref({
  openStatus: 0,
  notice: '',
  businessHours: '09:00-22:00',
  minOrderAmount: '0',
  deliveryFee: '3',
  packingFee: '0',
  logoUrl: ''
})

const typeText = computed(() =>
  shop.value && shop.value.shopType === 'FOOD' ? '餐饮外卖' : '便利超市'
)
const logoPreview = computed(() =>
  form.value.logoUrl ? absUrl(form.value.logoUrl) : ''
)

async function load() {
  if (!requireLogin()) return
  try {
    shop.value = await shopApi.get()
    form.value = {
      openStatus: shop.value.openStatus,
      notice: shop.value.notice || '',
      businessHours: shop.value.businessHours || '09:00-22:00',
      minOrderAmount: String(shop.value.minOrderAmount ?? 0),
      deliveryFee: String(shop.value.deliveryFee ?? 0),
      packingFee: String(shop.value.packingFee ?? 0),
      logoUrl: shop.value.logoUrl || ''
    }
  } catch (e) {
    shop.value = null
  }
}

function onOpen(e) {
  form.value.openStatus = e.detail.value ? 1 : 0
}

function pickLogo() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const path = res.tempFilePaths && res.tempFilePaths[0]
      if (!path) return
      uni.showLoading({ title: '上传中', mask: true })
      try {
        const data = await uploadFile(path)
        form.value.logoUrl = data.url
        uni.showToast({ title: '已上传', icon: 'success' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

async function save() {
  uni.showLoading({ title: '保存中', mask: true })
  try {
    shop.value = await shopApi.update({
      openStatus: form.value.openStatus,
      notice: form.value.notice,
      businessHours: form.value.businessHours,
      logoUrl: form.value.logoUrl || '',
      minOrderAmount: Number(form.value.minOrderAmount || 0),
      deliveryFee: Number(form.value.deliveryFee || 0),
      packingFee: Number(form.value.packingFee || 0)
    })
    form.value.logoUrl = shop.value.logoUrl || ''
    uni.showToast({ title: '已保存', icon: 'success' })
  } finally {
    uni.hideLoading()
  }
}

function goApply() {
  uni.navigateTo({ url: '/pages/apply/index' })
}

onShow(load)
</script>

<style scoped>
.hero {
  display: flex;
  gap: 20rpx;
  align-items: center;
  padding: 40rpx 32rpx 48rpx;
  background:
    radial-gradient(ellipse 80% 70% at 100% 0%, rgba(232, 93, 4, 0.18), transparent 50%),
    linear-gradient(160deg, #123d2f, #1b5e45);
  color: #fff;
}
.logo {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-img {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.2);
  flex-shrink: 0;
}
.logo-text { color: #fff; font-size: 40rpx; font-weight: 800; }
.logo-edit { display: flex; gap: 20rpx; align-items: center; }
.logo-preview {
  width: 120rpx;
  height: 120rpx;
  border-radius: 20rpx;
  background: #eef3f0;
  flex-shrink: 0;
}
.logo-preview.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9aaba3;
  font-size: 24rpx;
}
.logo-actions { flex: 1; }
.mini { padding: 16rpx 0; font-size: 26rpx; margin-bottom: 8rpx; }
.tip { display: block; font-size: 22rpx; }
.hero-meta { flex: 1; }
.name { display: block; font-size: 36rpx; font-weight: 800; }
.sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.72);
}
.card { margin-top: -20rpx; position: relative; z-index: 1; }
.card + .card { margin-top: 20rpx; }
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.row-title {
  display: block;
  font-weight: 700;
  margin-bottom: 6rpx;
}
.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--brand);
  margin-bottom: 20rpx;
}
.label {
  display: block;
  font-size: 24rpx;
  color: var(--muted);
  margin-bottom: 10rpx;
}
.input {
  background: #f3f6f4;
  border-radius: 14rpx;
  padding: 22rpx 20rpx;
  margin-bottom: 20rpx;
}
.row2 { display: flex; gap: 12rpx; }
.col { flex: 1; }
.link-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.empty-btn { margin: 32rpx 80rpx 0; }
.safe-bottom { height: 160rpx; }
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(20, 32, 27, 0.06);
}
.bar-btn { width: 100%; }
</style>
