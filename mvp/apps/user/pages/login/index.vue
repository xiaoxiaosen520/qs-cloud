<template>
  <view class="page">
    <view class="hero">
      <image class="brand-logo" src="/static/logo.png" mode="aspectFill" />
      <text class="brand">区惠</text>
      <text class="sub">同城外卖 · 便利店即时达</text>
    </view>

    <view class="card panel">
      <view class="mode-tabs">
        <text :class="['mode', mode === 'sms' && 'on']" @click="mode = 'sms'">手机号登录</text>
        <text :class="['mode', mode === 'wx' && 'on']" @click="mode = 'wx'">微信登录</text>
      </view>

      <template v-if="mode === 'sms'">
        <text class="label">手机号</text>
        <input class="input" type="number" maxlength="11" v-model="phone" placeholder="请输入手机号" />
        <text class="label">验证码</text>
        <view class="row">
          <input class="input flex" v-model="code" placeholder="开发环境填 123456" />
          <view :class="['code-btn', countdown > 0 && 'disabled']" @click="send">
            {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
          </view>
        </view>
      </template>
      <template v-else>
        <view class="wx-box">
          <text class="wx-title">微信一键登录</text>
          <text class="muted">正式环境将接入微信授权；联调请使用手机号登录。</text>
          <view class="btn-ghost wx-btn" @click="wxTip">微信登录（即将开放）</view>
        </view>
      </template>

      <view class="agree" @click="agreed = !agreed">
        <view :class="['check', agreed && 'on']" />
        <text class="agree-text">
          我已阅读并同意
          <text class="link" @click.stop="goPrivacy">《用户协议与隐私政策》</text>
        </text>
      </view>

      <view v-if="mode === 'sms'" class="btn-accent" @click="login">登录 / 注册</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onUnload } from '@dcloudio/uni-app'
import { authApi } from '../../api/http.js'
import { setLogin } from '../../utils/auth.js'
import { refreshTabBadges } from '../../utils/badge.js'

const phone = ref('13700002222')
const code = ref('123456')
const agreed = ref(false)
const mode = ref('sms')
const countdown = ref(0)
let timer = null

async function send() {
  if (countdown.value > 0) return
  if (!/^1\d{10}$/.test(phone.value)) {
    uni.showToast({ title: '手机号不正确', icon: 'none' })
    return
  }
  await authApi.sendSms(phone.value)
  uni.showToast({ title: '验证码已发送', icon: 'none' })
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function login() {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }
  const data = await authApi.login(phone.value, code.value)
  setLogin(data)
  await refreshTabBadges()
  uni.showToast({ title: '登录成功', icon: 'success' })
  setTimeout(() => {
    uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
  }, 300)
}

function wxTip() {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }
  uni.showToast({ title: '微信登录即将开放，请用手机号', icon: 'none' })
  mode.value = 'sms'
}

function goPrivacy() {
  uni.navigateTo({ url: '/pages/privacy/index' })
}

onUnload(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f3f6f4; }
.hero {
  padding: 56rpx 36rpx 80rpx;
  background:
    radial-gradient(circle at 85% 20%, rgba(232, 93, 4, 0.28), transparent 42%),
    linear-gradient(160deg, #0f3d2e, #1b5e45);
  color: #fff;
}
.brand-logo {
  width: 128rpx;
  height: 128rpx;
  border-radius: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.18);
}
.brand { display: block; font-size: 52rpx; font-weight: 800; }
.sub { display: block; margin-top: 12rpx; opacity: 0.8; }
.panel { margin-top: -40rpx; box-shadow: 0 12rpx 30rpx rgba(15, 61, 46, 0.08); }
.mode-tabs {
  display: flex; gap: 32rpx; margin-bottom: 28rpx; border-bottom: 1rpx solid #eef3f0;
}
.mode {
  padding-bottom: 16rpx; color: #6b7c74; font-size: 28rpx; position: relative;
}
.mode.on {
  color: #0f3d2e; font-weight: 800;
}
.mode.on::after {
  content: ''; position: absolute; left: 0; right: 0; bottom: 0;
  height: 4rpx; background: #e85d04; border-radius: 4rpx;
}
.label { display: block; font-weight: 600; margin: 8rpx 0 12rpx; color: #33453d; }
.input {
  background: #f3f6f4; border-radius: 14rpx; padding: 22rpx; margin-bottom: 20rpx;
}
.row { display: flex; gap: 16rpx; align-items: center; margin-bottom: 12rpx; }
.flex { flex: 1; margin-bottom: 0; }
.code-btn {
  white-space: nowrap; padding: 20rpx 18rpx; border-radius: 14rpx;
  background: #eaf6ef; color: #0f3d2e; font-size: 24rpx; font-weight: 600;
}
.code-btn.disabled { opacity: 0.5; }
.wx-box { padding: 12rpx 0 8rpx; }
.wx-title { display: block; font-size: 30rpx; font-weight: 700; margin-bottom: 12rpx; }
.wx-btn { margin-top: 28rpx; }
.agree { display: flex; gap: 12rpx; align-items: flex-start; margin: 8rpx 0 28rpx; }
.check {
  width: 28rpx; height: 28rpx; border-radius: 6rpx; margin-top: 4rpx;
  border: 2rpx solid #9aaba3; flex-shrink: 0;
}
.check.on { background: #0f3d2e; border-color: #0f3d2e; }
.agree-text { font-size: 22rpx; color: #6b7c74; line-height: 1.5; }
.link { color: #0f3d2e; font-weight: 600; }
</style>
