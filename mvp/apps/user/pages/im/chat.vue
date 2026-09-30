<template>
  <view class="page">
    <view class="order-bar" v-if="vo">
      <view class="ob-main">
        <view class="ob-left">
          <text class="ob-title">{{ vo.title }}</text>
          <text class="ob-sub">订单号 {{ vo.orderNo }}</text>
        </view>
        <view class="ob-actions">
          <view :class="['status-pill', statusClass(vo.orderStatus)]">{{ statusText(vo.orderStatus) }}</view>
          <view v-if="vo.peerPhone" class="call-btn" @click="callPeer">电话</view>
        </view>
      </view>
      <text class="ob-tip" v-if="!vo.canSend">会话已关闭，仅可查看历史消息</text>
      <text class="live" v-else>{{ wsOk ? '实时已连接' : '同步中…' }}</text>
    </view>

    <scroll-view
      scroll-y
      class="msgs"
      :style="{ top: barH + 'px', bottom: composerH + 'px' }"
      :scroll-into-view="scrollInto"
      scroll-with-animation
    >
      <view v-for="m in messages" :key="m.id" :id="'m' + m.id" :class="['row', bubbleClass(m)]">
        <view v-if="m.senderRole === 'SYSTEM'" class="sys">{{ sysText(m.content) }}</view>
        <template v-else>
          <view v-if="m.senderRole !== myRole" class="avatar">{{ peerAvatar }}</view>
          <view class="bubble-wrap">
            <view class="bubble" :class="isImage(m) && 'img-bubble'">
              <image
                v-if="isImage(m)"
                class="img"
                :src="imgSrc(m.content)"
                mode="widthFix"
                @click="previewImg(m.content)"
              />
              <text v-else class="txt">{{ m.content }}</text>
            </view>
            <view class="meta">
              <text class="time">{{ formatTime(m.createdAt) }}</text>
              <text v-if="m.senderRole === myRole" :class="['read-flag', isPeerRead(m) ? 'on' : '']">
                {{ isPeerRead(m) ? '已读' : '未读' }}
              </text>
            </view>
          </view>
        </template>
      </view>
      <view class="msgs-tail" />
    </scroll-view>

    <view class="composer-wrap" :style="{ paddingBottom: safeBottom + 'px' }">
      <scroll-view v-if="vo?.canSend" scroll-x class="quick">
        <view
          v-for="(q, i) in quickList"
          :key="i"
          class="quick-item"
          @click="sendQuick(q)"
        >{{ q }}</view>
      </scroll-view>
      <view class="composer">
        <view v-if="vo?.canSend" class="plus" @click="pickImage">图</view>
        <input
          class="input"
          v-model="text"
          :disabled="!vo?.canSend"
          :placeholder="vo?.canSend ? '输入消息…' : '会话已关闭'"
          confirm-type="send"
          @confirm="send"
          @focus="onFocus"
        />
        <view :class="['send', (!vo?.canSend || !text.trim()) && 'off']" @click="send">发送</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { onLoad, onUnload, onHide, onShow, onReady } from '@dcloudio/uni-app'
import { imApi, uploadFile, absUrl, HOST } from '../../api/http.js'
import { ORDER_STATUS_TEXT, callPhone, getToken } from '../../utils/auth.js'
import { quickRepliesFor } from '../../utils/imQuick.js'
import {
  configureImSocket, connectImSocket, onImSocketEvent,
  setActiveImSession, getActiveImSession
} from '../../utils/imSocket.js'

const sessionId = ref(0)
const vo = ref(null)
const messages = ref([])
const text = ref('')
const scrollInto = ref('')
const myRole = 'USER'
const sessionType = ref('USER_MERCHANT')
const safeBottom = ref(12)
const composerH = ref(110)
const barH = ref(64)
const wsOk = ref(false)
const quickList = ref(USER_QUICK())
let pollTimer = null
let offSocket = null

function USER_QUICK() {
  return quickRepliesFor('USER', sessionType.value)
}

const peerAvatar = computed(() => (sessionType.value === 'USER_RIDER' ? '骑' : '店'))

configureImSocket({ getToken, host: () => HOST })

onReady(() => {
  const sys = uni.getSystemInfoSync()
  const inset = (sys.safeAreaInsets && sys.safeAreaInsets.bottom) || 0
  safeBottom.value = Math.max(inset, 8) + 8
  composerH.value = 100 + safeBottom.value
})

onLoad(async (q) => {
  const orderId = Number(q.orderId)
  sessionType.value = q.type || 'USER_MERCHANT'
  quickList.value = quickRepliesFor('USER', sessionType.value)
  uni.setNavigationBarTitle({
    title: sessionType.value === 'USER_RIDER' ? '联系骑手' : '联系商家'
  })
  const data = await imApi.open({ orderId, type: sessionType.value })
  applyVo(data)
  await markReadNow()
  connectImSocket()
  offSocket = onImSocketEvent(onSocketEvt)
  startPoll()
})

onShow(() => {
  if (sessionId.value) setActiveImSession(sessionId.value)
  connectImSocket()
})
onHide(() => {
  if (getActiveImSession() === sessionId.value) setActiveImSession(0)
})
onUnload(() => {
  stopPoll()
  if (offSocket) offSocket()
  if (getActiveImSession() === sessionId.value) setActiveImSession(0)
})

function onSocketEvt(evt) {
  if (evt.type === 'im.socket_open') wsOk.value = true
  if (evt.type === 'im.socket_close') wsOk.value = false
  if (evt.type === 'im.message' && Number(evt.sessionId) === sessionId.value) {
    const m = normalizeMsg(evt.message)
    if (!m || messages.value.some((x) => x.id === m.id)) return
    messages.value.push(m)
    nextTick(() => { scrollInto.value = 'm' + m.id })
    markReadNow()
    try { uni.vibrateShort({}) } catch (e) {}
  }
  if (evt.type === 'im.peer_read' && Number(evt.sessionId) === sessionId.value) {
    vo.value = { ...vo.value, peerReadMsgId: evt.peerReadMsgId }
  }
}

async function markReadNow() {
  if (!sessionId.value) return
  const last = messages.value[messages.value.length - 1]
  try {
    await imApi.markRead(sessionId.value, last ? last.id : undefined)
  } catch (e) {}
}

function applyVo(data) {
  vo.value = data
  sessionId.value = data.session.id
  setActiveImSession(sessionId.value)
  messages.value = (data.messages || []).map(normalizeMsg)
  barH.value = data.canSend ? 64 : 78
  nextTick(() => {
    const last = messages.value[messages.value.length - 1]
    if (last) scrollInto.value = 'm' + last.id
  })
}

function normalizeMsg(m) {
  return { ...m, msgType: m.msgType || m.msg_type || 'TEXT' }
}
function isImage(m) {
  return (m.msgType || m.msg_type) === 'IMAGE'
}
function imgSrc(c) {
  return absUrl(c)
}
function previewImg(c) {
  uni.previewImage({ urls: [absUrl(c)] })
}
function statusText(s) {
  return ORDER_STATUS_TEXT[s] || s || ''
}
function statusClass(s) {
  if (s === 'DELIVERING' || s === 'ACCEPTED' || s === 'PAID') return 'run'
  if (s === 'COMPLETED') return 'ok'
  if (s === 'CANCELLED' || s === 'REFUNDED') return 'off'
  return ''
}
function sysText(c) {
  if (!c) return ''
  if (c.includes('可与商家沟通') || c.includes('可与骑手沟通') || c.includes('订单临时会话')) {
    return sessionType.value === 'USER_RIDER'
      ? '订单沟通已开启，可与骑手沟通；订单结束后关闭发送'
      : '订单沟通已开启，可与商家沟通；订单结束后关闭发送'
  }
  return c
}
function bubbleClass(m) {
  if (m.senderRole === 'SYSTEM') return 'sys-row'
  return m.senderRole === myRole ? 'mine' : 'theirs'
}
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(5, 16)
}
function isPeerRead(m) {
  const peer = Number(vo.value?.peerReadMsgId || 0)
  return peer > 0 && Number(m.id) <= peer
}
function callPeer() {
  if (vo.value?.peerPhone) callPhone(vo.value.peerPhone)
}
function onFocus() {
  nextTick(() => {
    const last = messages.value[messages.value.length - 1]
    if (last) scrollInto.value = 'm' + last.id
  })
}
async function sendQuick(q) {
  if (!vo.value?.canSend) return
  try {
    const msg = normalizeMsg(await imApi.send(sessionId.value, { content: q, msgType: 'TEXT' }))
    if (!messages.value.some((x) => x.id === msg.id)) messages.value.push(msg)
    nextTick(() => { scrollInto.value = 'm' + msg.id })
  } catch (e) {}
}
async function send() {
  if (!vo.value?.canSend) return
  const content = text.value.trim()
  if (!content) return
  text.value = ''
  try {
    const msg = normalizeMsg(await imApi.send(sessionId.value, { content, msgType: 'TEXT' }))
    if (!messages.value.some((x) => x.id === msg.id)) messages.value.push(msg)
    nextTick(() => { scrollInto.value = 'm' + msg.id })
  } catch (e) {}
}
function pickImage() {
  if (!vo.value?.canSend) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const path = res.tempFilePaths && res.tempFilePaths[0]
      if (!path) return
      uni.showLoading({ title: '发送中' })
      try {
        const up = await uploadFile(path)
        const url = up && up.url
        if (!url) throw new Error('no url')
        const msg = normalizeMsg(await imApi.send(sessionId.value, { content: url, msgType: 'IMAGE' }))
        if (!messages.value.some((x) => x.id === msg.id)) messages.value.push(msg)
        nextTick(() => { scrollInto.value = 'm' + msg.id })
      } catch (e) {
        uni.showToast({ title: '图片发送失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}
function startPoll() {
  stopPoll()
  pollTimer = setInterval(async () => {
    if (!sessionId.value) return
    try {
      const lastId = messages.value.length ? messages.value[messages.value.length - 1].id : 0
      const list = await imApi.messages(sessionId.value, lastId)
      if (list && list.length) {
        for (const raw of list) {
          const m = normalizeMsg(raw)
          if (!messages.value.some((x) => x.id === m.id)) messages.value.push(m)
        }
        nextTick(() => {
          const last = messages.value[messages.value.length - 1]
          if (last) scrollInto.value = 'm' + last.id
        })
        await markReadNow()
      }
      const fresh = await imApi.detail(sessionId.value)
      vo.value = { ...fresh, messages: messages.value }
      barH.value = fresh.canSend ? 64 : 78
    } catch (e) {}
  }, wsOk.value ? 12000 : 4000)
}
function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}
</script>

<style scoped>
.page {
  position: relative;
  width: 100%;
  height: 100vh;
  background: #ededed;
  overflow: hidden;
}
.order-bar {
  position: relative;
  z-index: 2;
  padding: 16rpx 24rpx 12rpx;
  background: #fff;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.ob-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.ob-title { display: block; font-size: 30rpx; font-weight: 700; color: #1a1a1a; }
.ob-sub { display: block; margin-top: 4rpx; font-size: 22rpx; color: #8c8c8c; }
.ob-actions { display: flex; align-items: center; gap: 12rpx; flex-shrink: 0; }
.status-pill {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: #f3f3f3;
  color: #666;
}
.status-pill.run { background: #fff7ed; color: #e85d04; }
.status-pill.ok { background: #eaf6ef; color: #0f3d2e; }
.status-pill.off { background: #f5f5f5; color: #999; }
.call-btn {
  font-size: 24rpx;
  font-weight: 700;
  color: #fff;
  background: #07c160;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
}
.ob-tip { display: block; margin-top: 10rpx; font-size: 22rpx; color: #e85d04; }
.live { display: block; margin-top: 6rpx; font-size: 20rpx; color: #999; }

.msgs {
  position: absolute;
  left: 0;
  right: 0;
  padding: 24rpx 20rpx 16rpx;
  box-sizing: border-box;
}
.msgs-tail { height: 8rpx; }
.row {
  margin-bottom: 28rpx;
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}
.row.mine { justify-content: flex-end; }
.row.theirs { justify-content: flex-start; }
.row.sys-row { justify-content: center; margin-bottom: 20rpx; }
.sys {
  max-width: 86%;
  font-size: 22rpx;
  line-height: 1.4;
  color: #8c8c8c;
  background: rgba(0, 0, 0, 0.045);
  padding: 10rpx 18rpx;
  border-radius: 10rpx;
  text-align: center;
}
.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 12rpx;
  background: #0f3d2e;
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.bubble-wrap { max-width: 68%; }
.bubble {
  padding: 18rpx 22rpx;
  border-radius: 8rpx 18rpx 18rpx 18rpx;
  background: #fff;
  box-shadow: 0 1rpx 2rpx rgba(0, 0, 0, 0.04);
}
.mine .bubble {
  background: #95ec69;
  border-radius: 18rpx 8rpx 18rpx 18rpx;
  box-shadow: none;
}
.img-bubble { padding: 8rpx; background: #fff !important; }
.img { display: block; width: 360rpx; border-radius: 10rpx; }
.txt {
  display: block;
  font-size: 30rpx;
  color: #1a1a1a;
  line-height: 1.45;
  word-break: break-all;
}
.meta {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 8rpx;
}
.mine .meta { justify-content: flex-end; }
.theirs .meta { justify-content: flex-start; }
.time { font-size: 20rpx; color: #b0b0b0; }
.read-flag { font-size: 20rpx; color: #b0b0b0; }
.read-flag.on { color: #07c160; }

.composer-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  background: #f7f7f7;
  border-top: 1rpx solid #e5e5e5;
}
.quick {
  white-space: nowrap;
  padding: 12rpx 16rpx 0;
  width: 100%;
  box-sizing: border-box;
}
.quick-item {
  display: inline-block;
  margin-right: 12rpx;
  padding: 10rpx 18rpx;
  background: #fff;
  border: 1rpx solid #e8e8e8;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: #333;
}
.composer {
  display: flex;
  gap: 12rpx;
  align-items: center;
  padding: 12px 12px 0;
  box-sizing: border-box;
}
.plus {
  width: 40px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  border-radius: 8px;
  background: #fff;
  border: 1rpx solid #e8e8e8;
  font-size: 14px;
  font-weight: 700;
  color: #333;
  flex-shrink: 0;
}
.input {
  flex: 1;
  height: 40px;
  line-height: 40px;
  background: #fff;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 15px;
  border: 1rpx solid #e8e8e8;
}
.send {
  height: 40px;
  line-height: 40px;
  padding: 0 18px;
  border-radius: 8px;
  background: #07c160;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
}
.send.off { opacity: 0.35; }
</style>
