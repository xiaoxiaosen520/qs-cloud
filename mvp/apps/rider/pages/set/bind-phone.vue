<template>
  <view class="page">
    <view class="card">
      <text class="muted">验证新手机号后即可换绑</text>
      <text class="label">新手机号</text>
      <input class="input" type="number" maxlength="11" v-model="phone" placeholder="请输入手机号" />
      <text class="label">验证码</text>
      <view class="row">
        <input class="input flex" v-model="code" placeholder="开发环境 123456" />
        <view :class="['btn-ghost', 'code', cooling && 'disabled']" @click="send">
          {{ cooling ? `${sec}s` : '获取验证码' }}
        </view>
      </view>
      <view class="btn-accent" @click="submit">确认更换</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { authApi, riderApi } from '../../api/http.js'
import { requireLogin, setLogin, getUser } from '../../utils/auth.js'

const phone = ref('')
const code = ref('123456')
const cooling = ref(false)
const sec = ref(60)
let timer = null

async function send() {
  if (cooling.value) return
  if (!/^1\d{10}$/.test(phone.value)) {
    uni.showToast({ title: '手机号不正确', icon: 'none' })
    return
  }
  await authApi.sendSms(phone.value)
  uni.showToast({ title: '已发送', icon: 'none' })
  cooling.value = true
  sec.value = 60
  timer = setInterval(() => {
    sec.value -= 1
    if (sec.value <= 0) {
      clearInterval(timer)
      cooling.value = false
    }
  }, 1000)
}

async function submit() {
  if (!requireLogin()) return
  const p = await riderApi.changePhone({ phone: phone.value, code: code.value })
  const u = getUser() || {}
  setLogin({
    token: uni.getStorageSync('rider_token'),
    userId: p.id || u.userId,
    phone: p.phone,
    nickname: p.name || u.nickname
  })
  uni.showToast({ title: '已更换', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 400)
}
</script>

<style scoped>
.label {
  display: block;
  font-size: 24rpx;
  color: var(--muted);
  margin: 16rpx 0 8rpx;
}
.input {
  background: #f3f6f4;
  border-radius: 14rpx;
  padding: 22rpx 20rpx;
  margin-bottom: 8rpx;
}
.row { display: flex; gap: 16rpx; align-items: center; }
.flex { flex: 1; margin-bottom: 0; }
.code { padding: 18rpx 20rpx; white-space: nowrap; font-size: 24rpx; }
.code.disabled { opacity: 0.5; }
</style>
