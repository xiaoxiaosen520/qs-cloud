<template>
  <view class="page">
    <view class="hero">
      <text class="brand">区惠商家</text>
      <text class="tagline">开店接单，就近送到</text>
    </view>

    <view class="card form">
      <text class="label">手机号</text>
      <input class="input" type="number" maxlength="11" v-model="phone" placeholder="请输入手机号" />
      <text class="label">验证码</text>
      <view class="row">
        <input class="input flex" v-model="code" placeholder="开发环境 123456" />
        <view :class="['btn-ghost', 'code', cooling && 'disabled']" @click="send">
          {{ cooling ? `${sec}s` : '获取验证码' }}
        </view>
      </view>
      <view class="btn-accent login" @click="login">登录开始营业</view>
      <text class="muted tip">登录即表示同意平台商家服务协议（演示）</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { authApi } from '../../api/http.js'
import { setLogin } from '../../utils/auth.js'

const phone = ref('13800138000')
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
  uni.showToast({ title: '验证码已发送', icon: 'none' })
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

async function login() {
  if (!/^1\d{10}$/.test(phone.value)) {
    uni.showToast({ title: '手机号不正确', icon: 'none' })
    return
  }
  if (!code.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  uni.showLoading({ title: '登录中', mask: true })
  try {
    const data = await authApi.login(phone.value, code.value)
    setLogin(data)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/home/index' }), 300)
  } finally {
    uni.hideLoading()
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg); }
.hero {
  padding: 140rpx 40rpx 60rpx;
  background:
    radial-gradient(ellipse 90% 80% at 100% 0%, rgba(232, 93, 4, 0.22), transparent 55%),
    linear-gradient(165deg, #0f3d2e 0%, #1b5e45 70%);
  color: #fff;
}
.brand {
  display: block;
  font-size: 56rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}
.tagline {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.75);
}
.form {
  margin-top: -28rpx;
  position: relative;
  z-index: 1;
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
  margin-bottom: 24rpx;
}
.row {
  display: flex;
  gap: 16rpx;
  align-items: center;
  margin-bottom: 12rpx;
}
.flex {
  flex: 1;
  margin-bottom: 0;
}
.code {
  padding: 18rpx 22rpx;
  white-space: nowrap;
  font-size: 24rpx;
}
.code.disabled { opacity: 0.5; }
.login { margin-top: 12rpx; }
.tip {
  display: block;
  text-align: center;
  margin-top: 24rpx;
}
</style>
