<template>
  <view class="page">
    <view class="card">
      <view class="avatar-wrap" @click="pickAvatar">
        <image v-if="avatarUrl" class="avatar" :src="absUrl(avatarUrl)" mode="aspectFill" />
        <view v-else class="avatar placeholder">{{ short }}</view>
        <text class="muted tip">点击更换头像</text>
      </view>
      <text class="label">昵称</text>
      <input class="input" v-model="name" placeholder="骑手昵称" />
      <text class="label">真实姓名</text>
      <input class="input" v-model="realName" placeholder="用于收款核对" />
      <view class="btn-accent" @click="save">保存</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { absUrl, riderApi, uploadFile } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'

const name = ref('')
const realName = ref('')
const avatarUrl = ref('')

const short = computed(() => (name.value || '骑').slice(0, 1))

async function load() {
  if (!requireLogin()) return
  const p = await riderApi.profile()
  name.value = p.name || ''
  realName.value = p.realName || ''
  avatarUrl.value = p.avatarUrl || ''
}

async function pickAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const path = res.tempFilePaths?.[0]
      if (!path) return
      uni.showLoading({ title: '上传中', mask: true })
      try {
        const data = await uploadFile(path)
        avatarUrl.value = data.url || data.path || ''
        uni.showToast({ title: '已上传', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

async function save() {
  await riderApi.updateProfile({
    name: name.value,
    realName: realName.value,
    avatarUrl: avatarUrl.value
  })
  uni.showToast({ title: '已保存', icon: 'success' })
}

onShow(load)
</script>

<style scoped>
.avatar-wrap { text-align: center; margin-bottom: 24rpx; }
.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 32rpx;
  margin: 0 auto 12rpx;
  display: block;
}
.avatar.placeholder {
  background: linear-gradient(145deg, #1b5e45, #0f3d2e);
  color: #fff;
  font-size: 48rpx;
  font-weight: 800;
  line-height: 140rpx;
  text-align: center;
}
.tip { display: block; }
.label {
  display: block;
  font-size: 24rpx;
  color: var(--muted);
  margin: 12rpx 0 8rpx;
}
.input {
  background: #f3f6f4;
  border-radius: 14rpx;
  padding: 22rpx 20rpx;
  margin-bottom: 8rpx;
}
</style>
