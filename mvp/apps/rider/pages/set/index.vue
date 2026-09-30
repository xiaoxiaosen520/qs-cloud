<template>
  <view class="page">
    <view class="card">
      <text class="section">接单提醒</text>
      <view class="row">
        <text>震动提醒</text>
        <switch :checked="form.vibrate" @change="(e) => save('vibrate', e.detail.value)" color="#e85d04" />
      </view>
      <view class="row">
        <text>语音播报（App）</text>
        <switch :checked="form.voice" @change="(e) => save('voice', e.detail.value)" color="#e85d04" />
      </view>
      <view class="row">
        <text>弹窗提示</text>
        <switch :checked="form.toast" @change="(e) => save('toast', e.detail.value)" color="#e85d04" />
      </view>
    </view>

    <view class="card menu">
      <view class="menu-item" @click="goProfile">
        <text>个人资料</text>
        <text class="muted">头像 / 昵称 ›</text>
      </view>
      <view class="menu-item" @click="goBind">
        <text>更换手机号</text>
        <text class="muted">›</text>
      </view>
      <view class="menu-item" @click="goAgreement">
        <text>服务协议</text>
        <text class="muted">›</text>
      </view>
      <view class="menu-item" @click="goWeb">
        <text>帮助中心</text>
        <text class="muted">H5 ›</text>
      </view>
    </view>

    <view class="card" v-if="user">
      <view class="row">
        <view>
          <text class="row-title">接单状态</text>
          <text class="muted">{{ online ? '接单中' : '休息中' }}</text>
        </view>
        <switch :checked="online" @change="toggleOnline" color="#e85d04" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { riderApi } from '../../api/http.js'
import { getUser, requireLogin } from '../../utils/auth.js'
import { getAlertSettings, setAlertSettings } from '../../utils/settings.js'
import { confirmRest } from '../../utils/nav.js'

const user = ref(null)
const online = ref(false)
const form = reactive(getAlertSettings())

function save(key, val) {
  form[key] = val
  setAlertSettings({ [key]: val })
  uni.showToast({ title: '已保存', icon: 'none' })
}

async function refresh() {
  user.value = getUser()
  if (!user.value) return
  try {
    const p = await riderApi.profile()
    online.value = p.online === 1
  } catch (e) { /* ignore */ }
  Object.assign(form, getAlertSettings())
}

async function toggleOnline(e) {
  if (!requireLogin()) return
  const on = e.detail.value
  if (!on) {
    const ok = await confirmRest()
    if (!ok) {
      online.value = true
      return
    }
  }
  try {
    const p = await riderApi.setOnline(on)
    online.value = p.online === 1
  } catch (err) {
    online.value = !on
  }
}

function goProfile() {
  uni.navigateTo({ url: '/pages/set/profile' })
}
function goBind() {
  uni.navigateTo({ url: '/pages/set/bind-phone' })
}
function goAgreement() {
  uni.navigateTo({ url: '/pages/agreement/index' })
}
function goWeb() {
  uni.navigateTo({
    url: '/pages/webview/index?title=' + encodeURIComponent('帮助中心')
      + '&url=' + encodeURIComponent('https://mp.weixin.qq.com/')
  })
}

onShow(refresh)
</script>

<style scoped>
.section {
  display: block;
  font-weight: 700;
  margin-bottom: 12rpx;
  color: var(--brand);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid var(--line);
}
.row:last-child { border-bottom: none; }
.row-title { display: block; font-weight: 700; margin-bottom: 4rpx; }
.menu { padding: 0 28rpx; }
.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid var(--line);
}
.menu-item:last-child { border-bottom: none; }
</style>
