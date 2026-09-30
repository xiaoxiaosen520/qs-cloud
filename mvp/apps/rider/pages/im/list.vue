<template>
  <view class="page">
    <view v-if="loading">
      <view class="skeleton" v-for="i in 3" :key="i" />
    </view>
    <view v-else-if="!list.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">暂无顾客消息</text>
      <text class="muted">顾客在配送中点「联系骑手」后会出现在这里，未读会显示红点</text>
    </view>
    <view
      v-for="item in list"
      :key="item.sessionId"
      class="card item"
      @click="openChat(item)"
    >
      <view class="row">
        <view class="title-wrap">
          <view v-if="item.unreadCount" class="dot" />
          <text :class="['title', item.unreadCount && 'bold']">{{ item.title }}</text>
        </view>
        <text class="time">{{ shortTime(item.updatedAt) }}</text>
      </view>
      <view class="preview-row">
        <text :class="['preview', item.unreadCount && 'bold']">{{ preview(item) }}</text>
        <view v-if="item.unreadCount" class="badge">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</view>
      </view>
      <view class="foot">
        <text class="tag" v-if="item.canSend">配送中</text>
        <text class="tag off" v-else>已结束</text>
        <text class="go">进入会话 ›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onHide, onPullDownRefresh } from '@dcloudio/uni-app'
import { imApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { refreshImBadge } from '../../utils/imBadge.js'

const list = ref([])
const loading = ref(false)
let pollTimer = null
let lastUnread = 0

async function load(silent) {
  if (!requireLogin()) return
  if (!silent) loading.value = true
  try {
    list.value = (await imApi.sessions()) || []
    const total = list.value.reduce((n, i) => n + Number(i.unreadCount || 0), 0)
    if (silent && total > lastUnread) {
      uni.vibrateShort({})
      uni.showToast({ title: '收到新消息', icon: 'none' })
    }
    lastUnread = total
    refreshImBadge(2)
  } catch (e) {
    if (!silent) list.value = []
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function preview(item) {
  const c = item.lastContent || ''
  if (!c) return '暂无消息'
  const who = item.lastSenderRole === 'USER' ? '顾客：'
    : item.lastSenderRole === 'SYSTEM' ? ''
      : '我：'
  return who + c
}

function shortTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(5, 16)
}

function openChat(item) {
  uni.navigateTo({
    url: `/pages/im/chat?orderId=${item.orderId}&type=${item.sessionType}&role=RIDER`
  })
}

function startPoll() {
  stopPoll()
  pollTimer = setInterval(() => load(true), 5000)
}
function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onShow(() => {
  load(false)
  startPoll()
})
onHide(stopPoll)
onPullDownRefresh(() => load(false))
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40rpx; }
.item { margin: 16rpx 24rpx; }
.row { display: flex; justify-content: space-between; align-items: center; }
.title-wrap { display: flex; align-items: center; gap: 10rpx; min-width: 0; flex: 1; }
.dot {
  width: 14rpx; height: 14rpx; border-radius: 50%; background: #e11d48; flex-shrink: 0;
}
.title {
  font-size: 28rpx; font-weight: 600; color: #222;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 420rpx;
}
.title.bold { font-weight: 800; }
.time { font-size: 22rpx; color: #999; flex-shrink: 0; margin-left: 12rpx; }
.preview-row { display: flex; align-items: center; gap: 12rpx; margin-top: 12rpx; }
.preview {
  flex: 1; font-size: 26rpx; color: #666;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.preview.bold { color: #222; font-weight: 700; }
.badge {
  min-width: 32rpx; height: 32rpx; padding: 0 8rpx; border-radius: 16rpx;
  background: #e11d48; color: #fff; font-size: 20rpx; text-align: center; line-height: 32rpx;
  flex-shrink: 0;
}
.foot { display: flex; align-items: center; margin-top: 16rpx; }
.tag {
  font-size: 20rpx; color: #e85d04; background: #fff7ed;
  padding: 4rpx 12rpx; border-radius: 8rpx;
}
.tag.off { color: #999; background: #f3f3f3; }
.go { margin-left: auto; font-size: 24rpx; color: #0f3d2e; font-weight: 600; }
</style>
