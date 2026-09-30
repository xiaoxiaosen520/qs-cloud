<script>
import { refreshTabBadges } from './utils/badge.js'
import { refreshImBadge } from './utils/imBadge.js'
import { getToken } from './utils/auth.js'
import { HOST } from './api/http.js'
import {
  configureImSocket,
  connectImSocket,
  disconnectImSocket,
  onImSocketEvent,
  getActiveImSession
} from './utils/imSocket.js'

configureImSocket({ getToken, host: () => HOST })

let off = null

export default {
  onLaunch() {
    off = onImSocketEvent((evt) => {
      if (evt.type !== 'im.message') return
      const sid = Number(evt.sessionId)
      if (sid && sid === getActiveImSession()) return
      const preview = evt.preview || (evt.message && evt.message.content) || '新消息'
      try { uni.vibrateShort({}) } catch (e) {}
      uni.showToast({ title: String(preview).slice(0, 20), icon: 'none' })
      refreshImBadge()
    })
  },
  onShow() {
    if (getToken()) connectImSocket()
    else disconnectImSocket()
    refreshTabBadges()
    refreshImBadge()
  }
}
</script>

<style>
page {
  --brand: #0f3d2e;
  --brand-soft: #1b5e45;
  --accent: #e85d04;
  --bg: #f3f6f4;
  --card: #ffffff;
  --text: #14201b;
  --muted: #6b7c74;
  --line: #e6eee9;
  --danger: #d62828;
  background: var(--bg);
  color: var(--text);
  font-size: 28rpx;
  font-family: "PingFang SC", "Helvetica Neue", sans-serif;
}

.btn-primary {
  background: linear-gradient(135deg, #1b5e45, #0f3d2e);
  color: #fff;
  border-radius: 16rpx;
  text-align: center;
  padding: 24rpx 0;
  font-weight: 600;
  letter-spacing: 1rpx;
}
.btn-primary:active { opacity: 0.9; }
.btn-accent {
  background: linear-gradient(135deg, #f48c06, #e85d04);
  color: #fff;
  border-radius: 16rpx;
  text-align: center;
  padding: 24rpx 0;
  font-weight: 600;
}
.btn-ghost {
  background: #fff;
  color: var(--brand);
  border: 2rpx solid rgba(15, 61, 46, 0.25);
  border-radius: 16rpx;
  text-align: center;
  padding: 22rpx 0;
  font-weight: 600;
}
.card {
  background: var(--card);
  border-radius: 20rpx;
  padding: 28rpx;
  margin: 20rpx 28rpx;
}
.muted { color: var(--muted); font-size: 24rpx; }
.price { color: var(--accent); font-weight: 700; }
.empty {
  padding: 120rpx 48rpx;
  text-align: center;
}
.empty-icon {
  width: 120rpx;
  height: 120rpx;
  margin: 0 auto 24rpx;
  border-radius: 28rpx;
  background: linear-gradient(145deg, #d8f3dc, #b7e4c7);
}
.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 12rpx;
}
.skeleton {
  height: 160rpx;
  margin: 20rpx 28rpx;
  border-radius: 20rpx;
  background: linear-gradient(90deg, #e8efeb 25%, #f5f8f6 37%, #e8efeb 63%);
  background-size: 400% 100%;
  animation: shimmer 1.2s ease infinite;
}
@keyframes shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: 0 0; }
}
.retry {
  margin: 24rpx 80rpx 0;
}
</style>
