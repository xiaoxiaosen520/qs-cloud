<template>
  <view class="page">
    <view class="card">
      <view class="row avatar-row" @click="pickAvatar">
        <text class="label">头像</text>
        <view class="avatar-wrap">
          <image v-if="form.avatarUrl" class="avatar" :src="avatarSrc" mode="aspectFill" />
          <view v-else class="avatar placeholder">{{ (form.nickname || '用').slice(0, 1) }}</view>
          <text class="tip">点击更换 ›</text>
        </view>
      </view>
      <view class="row col">
        <text class="label">昵称</text>
        <input class="input" v-model="form.nickname" maxlength="20" placeholder="请输入昵称" />
      </view>
      <view class="row">
        <text class="label">手机号</text>
        <text class="val">{{ form.phone || '-' }}</text>
      </view>
    </view>
    <view class="btn-accent save" @click="save">保存</view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { profileApi, uploadFile, absUrl } from '../../api/http.js'
import { patchUser, requireLogin } from '../../utils/auth.js'

const form = ref({ nickname: '', avatarUrl: '', phone: '' })
const avatarSrc = computed(() => absUrl(form.value.avatarUrl))

onShow(async () => {
  if (!requireLogin()) return
  const p = await profileApi.get()
  form.value = {
    nickname: p.nickname || '',
    avatarUrl: p.avatarUrl || '',
    phone: p.phone || ''
  }
})

function pickAvatar() {
  uni.showActionSheet({
    itemList: ['从相册选择', '拍照'],
    success: (res) => {
      if (res.tapIndex === 0) chooseAndUpload(['album'])
      else if (res.tapIndex === 1) chooseAndUpload(['camera'])
    }
  })
}

function chooseAndUpload(sourceType) {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType,
    success: async (res) => {
      const path = res.tempFilePaths && res.tempFilePaths[0]
      if (!path) return
      uni.showLoading({ title: '上传中', mask: true })
      try {
        const data = await uploadFile(path)
        form.value.avatarUrl = data.url
        // 立即落库，个人中心同步刷新
        const p = await profileApi.update({
          nickname: form.value.nickname.trim() || form.value.phone || '区惠用户',
          avatarUrl: data.url
        })
        patchUser({ nickname: p.nickname, avatarUrl: p.avatarUrl })
        uni.showToast({ title: '头像已更新', icon: 'success' })
      } catch (e) { /* toast by upload */ }
      finally {
        uni.hideLoading()
      }
    }
  })
}

async function save() {
  if (!form.value.nickname.trim()) {
    uni.showToast({ title: '请填写昵称', icon: 'none' })
    return
  }
  const p = await profileApi.update({
    nickname: form.value.nickname.trim(),
    avatarUrl: form.value.avatarUrl || ''
  })
  patchUser({ nickname: p.nickname, avatarUrl: p.avatarUrl })
  uni.showToast({ title: '已保存', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 400)
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding: 20rpx 0 40rpx; }
.row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 28rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.row.col { flex-direction: column; align-items: stretch; gap: 12rpx; }
.row:last-child { border-bottom: none; }
.avatar-row { padding: 20rpx 0; }
.label { color: #666; font-size: 28rpx; width: 140rpx; flex-shrink: 0; }
.val { color: #222; font-size: 28rpx; }
.input {
  background: #f5f5f5; border-radius: 12rpx; padding: 20rpx;
}
.avatar-wrap { display: flex; align-items: center; gap: 16rpx; }
.avatar {
  width: 108rpx; height: 108rpx; border-radius: 50%; background: #eaf6ef;
}
.avatar.placeholder {
  display: flex; align-items: center; justify-content: center;
  font-weight: 800; color: #0f3d2e; font-size: 36rpx;
}
.tip { font-size: 24rpx; color: #999; }
.save { margin: 40rpx 28rpx; }
</style>
