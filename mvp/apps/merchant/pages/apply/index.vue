<template>
  <view class="page">
    <view class="hero">
      <text class="brand">商家入驻</text>
      <text class="tagline">提交资料，平台审核通过后即可开店</text>
    </view>

    <view class="card status-card" v-if="status">
      <view class="status-row">
        <text class="title">当前状态</text>
        <text :class="['pill', status.status]">{{ statusText }}</text>
      </view>
      <text class="muted" v-if="status.rejectReason">原因：{{ status.rejectReason }}</text>
      <text class="muted tip" v-if="status.status === 'APPROVED'">
        已通过。请返回「我的」退出并重新登录，以刷新店铺绑定。
      </text>
    </view>

    <view v-if="!status || status.status === 'REJECTED'" class="form-wrap">
      <view class="card">
        <text class="section">基本信息</text>
        <text class="label">门店名称</text>
        <input class="input" v-model="form.shopName" placeholder="线下牌匾 / 对外展示名" />
        <text class="label">联系人姓名</text>
        <input class="input" v-model="form.contactName" placeholder="请填写联系人姓名" />
        <text class="label">联系电话</text>
        <input class="input" type="number" maxlength="11" v-model="form.contactPhone" placeholder="外卖联系电话" />
        <text class="label">经营类型</text>
        <view class="types">
          <text :class="['type', form.shopType === 'FOOD' && 'on']" @click="setType('FOOD')">餐饮外卖</text>
          <text :class="['type', form.shopType === 'CONVENIENCE' && 'on']" @click="setType('CONVENIENCE')">便利店</text>
        </view>
        <text class="label">营业类目</text>
        <input class="input" v-model="form.managePrimary" placeholder="如：中式快餐 / 零食饮料" />
        <text class="label">门店公告</text>
        <input class="input" v-model="form.notice" placeholder="选填，如：欢迎光临" />
      </view>

      <view class="card">
        <text class="section">门店地址</text>
        <text class="label">详细地址</text>
        <input class="input" v-model="form.address" placeholder="街道门店地址" />
        <text class="label">门牌号</text>
        <input class="input" v-model="form.houseNumber" placeholder="如：1 栋 108 室" />
        <text class="hint muted">演示环境使用默认坐标，上线后可接地图选点</text>
      </view>

      <view class="card">
        <text class="section">门店照片</text>
        <view class="upload-block">
          <text class="label">门店头像</text>
          <view class="upload-box" @click="pick('logoUrl')">
            <image v-if="form.logoUrl" class="preview" :src="absUrl(form.logoUrl)" mode="aspectFill" />
            <text v-else class="plus">+</text>
          </view>
        </view>
        <view class="upload-block">
          <text class="label">店内照片</text>
          <view class="upload-box wide" @click="pick('withinUrl')">
            <image v-if="form.withinUrl" class="preview" :src="absUrl(form.withinUrl)" mode="aspectFill" />
            <text v-else class="plus">+</text>
          </view>
        </view>
      </view>

      <view class="card">
        <text class="section">资质材料</text>
        <view class="upload-block">
          <text class="label">营业执照 <text class="req">必传</text></text>
          <view class="upload-box wide" @click="pick('licenseUrl')">
            <image v-if="form.licenseUrl" class="preview" :src="absUrl(form.licenseUrl)" mode="aspectFill" />
            <text v-else class="plus">+</text>
          </view>
        </view>
        <view class="upload-row">
          <view class="upload-block half">
            <text class="label">身份证正面 <text class="req">必传</text></text>
            <view class="upload-box" @click="pick('idCardFrontUrl')">
              <image v-if="form.idCardFrontUrl" class="preview" :src="absUrl(form.idCardFrontUrl)" mode="aspectFill" />
              <text v-else class="plus">+</text>
            </view>
          </view>
          <view class="upload-block half">
            <text class="label">身份证反面 <text class="req">必传</text></text>
            <view class="upload-box" @click="pick('idCardBackUrl')">
              <image v-if="form.idCardBackUrl" class="preview" :src="absUrl(form.idCardBackUrl)" mode="aspectFill" />
              <text v-else class="plus">+</text>
            </view>
          </view>
        </view>
      </view>

      <view class="safe-bottom" />
      <view class="action-bar">
        <view class="btn-accent bar-btn" @click="submit">提交入驻</view>
      </view>
    </view>

    <view class="card pending" v-else-if="status.status === 'PENDING'">
      <view class="empty-icon mini" />
      <text class="empty-title">资料审核中</text>
      <text class="muted">可用 demo 页管理员账号审核通过</text>
      <view class="btn-ghost mt" @click="load">刷新状态</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { applyApi, uploadFile, absUrl } from '../../api/http.js'
import { requireLogin, getUser, APPLY_STATUS_TEXT } from '../../utils/auth.js'

const status = ref(null)
const form = ref({
  contactName: '',
  contactPhone: '',
  shopName: '',
  shopType: 'CONVENIENCE',
  categoryId: 2,
  managePrimary: '',
  notice: '',
  logoUrl: '',
  withinUrl: '',
  licenseUrl: '',
  idCardFrontUrl: '',
  idCardBackUrl: '',
  address: '',
  houseNumber: '',
  lat: 31.2304,
  lng: 121.4737
})

const statusText = computed(() =>
  status.value ? APPLY_STATUS_TEXT[status.value.status] || status.value.status : ''
)

function setType(t) {
  form.value.shopType = t
  form.value.categoryId = t === 'FOOD' ? 1 : 2
}

async function load() {
  if (!requireLogin()) return
  const user = getUser()
  if (user && user.phone && !form.value.contactPhone) {
    form.value.contactPhone = user.phone
  }
  try {
    status.value = await applyApi.status()
    if (status.value && status.value.status === 'REJECTED') {
      // 驳回后允许重填，预填上次内容
      form.value.shopName = status.value.shopName || form.value.shopName
      form.value.contactName = status.value.contactName || form.value.contactName
      form.value.contactPhone = status.value.contactPhone || form.value.contactPhone
      form.value.shopType = status.value.shopType || form.value.shopType
      form.value.address = status.value.address || form.value.address
      form.value.notice = status.value.notice || ''
      form.value.logoUrl = status.value.logoUrl || ''
      form.value.withinUrl = status.value.withinUrl || ''
      form.value.licenseUrl = status.value.licenseUrl || ''
      form.value.idCardFrontUrl = status.value.idCardFrontUrl || ''
      form.value.idCardBackUrl = status.value.idCardBackUrl || ''
      form.value.houseNumber = status.value.houseNumber || ''
    }
  } catch (e) {
    status.value = null
  }
}

function pick(field) {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const path = res.tempFilePaths && res.tempFilePaths[0]
      if (!path) return
      uni.showLoading({ title: '上传中', mask: true })
      try {
        const data = await uploadFile(path)
        form.value[field] = data.url
        uni.showToast({ title: '已上传', icon: 'success' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

async function submit() {
  const f = form.value
  if (!f.shopName || !f.contactName || !f.contactPhone || !f.address) {
    uni.showToast({ title: '请填写基本信息', icon: 'none' })
    return
  }
  if (!/^1\d{10}$/.test(f.contactPhone)) {
    uni.showToast({ title: '联系电话不正确', icon: 'none' })
    return
  }
  if (!f.licenseUrl || !f.idCardFrontUrl || !f.idCardBackUrl) {
    uni.showToast({ title: '请上传营业执照与身份证', icon: 'none' })
    return
  }
  uni.showLoading({ title: '提交中', mask: true })
  try {
    status.value = await applyApi.submit({
      contactName: f.contactName,
      contactPhone: f.contactPhone,
      shopName: f.shopName,
      shopType: f.shopType,
      categoryId: f.categoryId,
      notice: f.notice || f.managePrimary || '',
      logoUrl: f.logoUrl,
      withinUrl: f.withinUrl,
      licenseUrl: f.licenseUrl,
      idCardFrontUrl: f.idCardFrontUrl,
      idCardBackUrl: f.idCardBackUrl,
      address: f.address,
      houseNumber: f.houseNumber,
      lat: f.lat,
      lng: f.lng
    })
    uni.showToast({ title: '已提交', icon: 'success' })
  } finally {
    uni.hideLoading()
  }
}

onShow(load)
</script>

<style scoped>
.hero {
  padding: 48rpx 36rpx 56rpx;
  background:
    radial-gradient(ellipse 90% 80% at 100% 0%, rgba(232, 93, 4, 0.2), transparent 55%),
    linear-gradient(165deg, #0f3d2e 0%, #1b5e45 70%);
  color: #fff;
}
.brand {
  display: block;
  font-size: 44rpx;
  font-weight: 800;
  letter-spacing: 1rpx;
}
.tagline {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.75);
}
.status-card,
.form-wrap .card:first-child,
.pending {
  margin-top: -24rpx;
  position: relative;
  z-index: 1;
}
.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}
.title { font-weight: 700; font-size: 30rpx; }
.pill {
  font-size: 22rpx;
  font-weight: 700;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #eef3f0;
  color: var(--muted);
}
.pill.PENDING { background: var(--accent-soft); color: var(--accent); }
.pill.APPROVED { background: rgba(42, 157, 143, 0.12); color: var(--ok); }
.pill.REJECTED { background: #fde8e8; color: var(--danger); }
.tip { display: block; margin-top: 12rpx; }
.section {
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
.req { color: var(--danger); font-size: 22rpx; }
.input {
  background: #f3f6f4;
  border-radius: 14rpx;
  padding: 22rpx 20rpx;
  margin-bottom: 20rpx;
}
.types { display: flex; gap: 16rpx; margin-bottom: 20rpx; }
.type {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  border-radius: 14rpx;
  background: #f0f3f1;
  color: var(--muted);
  font-weight: 600;
}
.type.on { background: var(--brand); color: #fff; }
.hint { display: block; margin-top: -8rpx; margin-bottom: 8rpx; }
.upload-block { margin-bottom: 20rpx; }
.upload-row { display: flex; gap: 16rpx; }
.half { flex: 1; }
.upload-box {
  width: 200rpx;
  height: 200rpx;
  border-radius: 16rpx;
  background: #f3f6f4;
  border: 2rpx dashed #c9d5ce;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.upload-box.wide { width: 100%; height: 220rpx; }
.preview { width: 100%; height: 100%; }
.plus { font-size: 56rpx; color: #9aada3; font-weight: 300; }
.pending { text-align: center; padding: 48rpx 28rpx; }
.mini { width: 80rpx; height: 80rpx; margin: 0 auto 16rpx; }
.mt { margin-top: 24rpx; }
.safe-bottom { height: 160rpx; }
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(20, 32, 27, 0.06);
  z-index: 10;
}
.bar-btn { width: 100%; }
</style>
