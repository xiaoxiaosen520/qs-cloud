<template>
  <view class="page">
    <view class="card add" @click="openCreate">+ 新增收货地址</view>

    <view v-if="!list.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">还没有地址</text>
      <text class="muted">添加后下单更快</text>
    </view>

    <view
      v-for="a in list"
      :key="a.id"
      class="card item"
      @click="select(a)"
    >
      <view class="row">
        <text class="name">{{ a.contactName }} {{ a.contactPhone }}</text>
        <text v-if="a.isDefault === 1" class="def">默认</text>
      </view>
      <text class="detail">{{ a.detail }}</text>
      <view class="ops" @click.stop>
        <text class="op" v-if="a.isDefault !== 1" @click="setDefault(a)">设默认</text>
        <text class="op" @click="openEdit(a)">编辑</text>
        <text class="op danger" @click="remove(a)">删除</text>
        <text v-if="selectMode" class="op primary" @click="select(a)">使用</text>
      </view>
    </view>

    <view v-if="showForm" class="mask" @click="showForm = false">
      <view class="sheet" @click.stop>
        <text class="sheet-title">{{ editingId ? '编辑地址' : '新增地址' }}</text>
        <input v-model="form.contactName" class="input" placeholder="联系人" />
        <input v-model="form.contactPhone" class="input" type="number" maxlength="11" placeholder="手机号" />
        <view class="map-pick" @click="chooseOnMap">
          <view class="map-pick-left">
            <text class="map-pick-title">地图选点</text>
            <text class="map-pick-addr">{{ form.detail || '点击在地图上选择收货位置' }}</text>
          </view>
          <text class="map-pick-go">去选点 ›</text>
        </view>
        <input v-model="form.detail" class="input" placeholder="门牌号、楼层等补充信息" />
        <view class="loc-row" @click="fillLocation">
          <text class="loc-btn">使用当前 GPS 坐标</text>
          <text class="loc-tip">{{ form.lat }}, {{ form.lng }}</text>
        </view>
        <view class="default-row" @click="form.isDefault = !form.isDefault">
          <view :class="['check', form.isDefault && 'on']" />
          <text>设为默认地址</text>
        </view>
        <view class="btn-accent" @click="save">保存</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { addressApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'

const list = ref([])
const selectMode = ref(false)
const showForm = ref(false)
const editingId = ref(null)
const form = ref(emptyForm())

function emptyForm() {
  return {
    contactName: '',
    contactPhone: '',
    detail: '',
    lat: 31.231,
    lng: 121.474,
    isDefault: true
  }
}

onLoad((q) => {
  selectMode.value = q.select === '1'
})

async function load() {
  if (!requireLogin()) return
  list.value = (await addressApi.list()) || []
}

function select(a) {
  if (!selectMode.value) return
  uni.setStorageSync('selectedAddressId', a.id)
  uni.navigateBack()
}

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  form.value.isDefault = list.value.length === 0
  showForm.value = true
}

function openEdit(a) {
  editingId.value = a.id
  form.value = {
    contactName: a.contactName,
    contactPhone: a.contactPhone,
    detail: a.detail,
    lat: a.lat || 31.231,
    lng: a.lng || 121.474,
    isDefault: a.isDefault === 1
  }
  showForm.value = true
}

function chooseOnMap() {
  uni.navigateTo({
    url: `/pages/address/map?lat=${form.value.lat}&lng=${form.value.lng}&detail=${encodeURIComponent(form.value.detail || '')}`,
    events: {
      pick: (data) => {
        if (data.detail) form.value.detail = data.detail
        if (data.lat != null) form.value.lat = data.lat
        if (data.lng != null) form.value.lng = data.lng
      }
    }
  })
}

function fillLocation() {
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      form.value.lat = Number(res.latitude.toFixed(6))
      form.value.lng = Number(res.longitude.toFixed(6))
      uni.showToast({ title: '已填入坐标', icon: 'none' })
    },
    fail: () => {
      uni.showToast({ title: '定位失败，可手填地址', icon: 'none' })
    }
  })
}

async function save() {
  if (!form.value.contactName || !form.value.contactPhone || !form.value.detail) {
    uni.showToast({ title: '请填完整', icon: 'none' })
    return
  }
  if (!/^1\d{10}$/.test(form.value.contactPhone)) {
    uni.showToast({ title: '手机号格式错误', icon: 'none' })
    return
  }
  const payload = { ...form.value }
  if (editingId.value) {
    await addressApi.update(editingId.value, payload)
  } else {
    await addressApi.create(payload)
  }
  showForm.value = false
  await load()
}

async function setDefault(a) {
  await addressApi.update(a.id, {
    contactName: a.contactName,
    contactPhone: a.contactPhone,
    detail: a.detail,
    lat: a.lat,
    lng: a.lng,
    isDefault: true
  })
  uni.showToast({ title: '已设为默认', icon: 'none' })
  await load()
}

function remove(a) {
  uni.showModal({
    title: '删除地址',
    content: '确定删除该收货地址？',
    success: async (res) => {
      if (!res.confirm) return
      await addressApi.remove(a.id)
      const picked = uni.getStorageSync('selectedAddressId')
      if (picked === a.id) uni.removeStorageSync('selectedAddressId')
      await load()
    }
  })
}

onShow(load)
</script>

<style scoped>
.add {
  text-align: center; color: #0f3d2e; font-weight: 700;
  border: 2rpx dashed rgba(15, 61, 46, 0.3);
  background: #f0faf4;
}
.item { box-shadow: 0 6rpx 18rpx rgba(20, 32, 27, 0.04); }
.row { display: flex; justify-content: space-between; align-items: center; }
.name { font-weight: 700; font-size: 30rpx; }
.def {
  color: #e85d04; background: #fff7ed; font-size: 20rpx;
  padding: 4rpx 10rpx; border-radius: 8rpx;
}
.detail { display: block; margin-top: 10rpx; color: #52796f; }
.ops { display: flex; gap: 28rpx; margin-top: 18rpx; }
.op { font-size: 24rpx; color: #52796f; }
.op.primary { color: #0f3d2e; font-weight: 700; }
.op.danger { color: #d62828; }
.mask {
  position: fixed; inset: 0; background: rgba(0,0,0,.45);
  display: flex; align-items: flex-end; z-index: 20;
}
.sheet {
  width: 100%; background: #fff; border-radius: 28rpx 28rpx 0 0;
  padding: 32rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
}
.sheet-title { display: block; font-size: 32rpx; font-weight: 800; margin-bottom: 20rpx; }
.input {
  background: #f3f6f4; border-radius: 14rpx; padding: 22rpx; margin-bottom: 16rpx;
}
.map-pick {
  display: flex; align-items: center; justify-content: space-between;
  background: #f0faf4; border-radius: 14rpx; padding: 22rpx 20rpx; margin-bottom: 16rpx;
  border: 2rpx dashed rgba(15, 61, 46, 0.2);
}
.map-pick-left { flex: 1; min-width: 0; padding-right: 16rpx; }
.map-pick-title { display: block; font-size: 26rpx; font-weight: 700; color: #0f3d2e; }
.map-pick-addr {
  display: block; margin-top: 8rpx; font-size: 24rpx; color: #52796f;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.map-pick-go { flex-shrink: 0; font-size: 24rpx; color: #0f3d2e; font-weight: 700; }
.loc-row {
  display: flex; justify-content: space-between; align-items: center;
  margin: 0 0 16rpx; padding: 12rpx 0;
}
.loc-btn { color: #0f3d2e; font-size: 24rpx; font-weight: 700; }
.loc-tip { font-size: 20rpx; color: #999; max-width: 360rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.default-row {
  display: flex; align-items: center; gap: 12rpx; margin: 8rpx 0 24rpx; color: #33453d;
}
.check {
  width: 32rpx; height: 32rpx; border-radius: 8rpx; border: 2rpx solid #9aaba3;
}
.check.on { background: #0f3d2e; border-color: #0f3d2e; }
</style>
