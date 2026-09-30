import { toCoord } from './format.js'
import { getAlertSettings } from './settings.js'

/** 打开系统地图导航 */
export function openNav({ lat, lng, name, address }) {
  const latitude = toCoord(lat)
  const longitude = toCoord(lng)
  if (latitude == null || longitude == null) {
    uni.showToast({ title: '暂无坐标，无法导航', icon: 'none' })
    return
  }
  uni.openLocation({
    latitude,
    longitude,
    name: name || '目的地',
    address: address || '',
    scale: 16,
    fail: () => uni.showToast({ title: '打开地图失败', icon: 'none' })
  })
}

function speak(tip) {
  const s = getAlertSettings()
  if (!s.voice) return
  // #ifdef APP-PLUS
  try {
    if (typeof plus !== 'undefined' && plus.speech) {
      plus.speech.startSpeaking(tip)
    }
  } catch (e) { /* ignore */ }
  // #endif
}

function vibrateIfEnabled() {
  const s = getAlertSettings()
  if (!s.vibrate) return
  try {
    uni.vibrateLong({})
  } catch (e) { /* ignore */ }
}

/** 新单提醒 */
export function alertNewOrder(count) {
  const tip = count > 1 ? `有 ${count} 笔新订单待抢` : '有新订单待抢'
  vibrateIfEnabled()
  speak(tip)
  if (getAlertSettings().toast) {
    uni.showToast({ title: tip, icon: 'none', duration: 2500 })
  }
}

/** 订单取消提醒 */
export function alertOrderCancelled(count) {
  const tip = count > 1 ? `有 ${count} 笔配送单已取消` : '有配送单已被取消'
  vibrateIfEnabled()
  speak(tip)
  if (getAlertSettings().toast) {
    uni.showToast({ title: tip, icon: 'none', duration: 2800 })
  }
}

export function reportLocationOnce(apiFn) {
  return new Promise((resolve) => {
    uni.getLocation({
      type: 'wgs84',
      success: async (res) => {
        try {
          await apiFn(res.latitude, res.longitude)
        } catch (e) { /* silent */ }
        resolve(res)
      },
      fail: () => resolve(null)
    })
  })
}

/** 确认休息/下线 */
export function confirmRest() {
  return new Promise((resolve) => {
    uni.showModal({
      title: '确认休息？',
      content: '休息后将停止接收新的抢单，进行中的配送不受影响。',
      confirmText: '去休息',
      cancelText: '继续接单',
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false)
    })
  })
}
