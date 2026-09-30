const KEY = 'rider_alert_settings'

const DEFAULTS = {
  vibrate: true,
  voice: true,
  toast: true
}

export function getAlertSettings() {
  try {
    const raw = uni.getStorageSync(KEY)
    if (!raw || typeof raw !== 'object') return { ...DEFAULTS }
    return { ...DEFAULTS, ...raw }
  } catch (e) {
    return { ...DEFAULTS }
  }
}

export function setAlertSettings(partial) {
  const next = { ...getAlertSettings(), ...partial }
  uni.setStorageSync(KEY, next)
  return next
}

const SEEN_CANCEL_KEY = 'rider_seen_cancelled_ids'

export function getSeenCancelledIds() {
  try {
    const arr = uni.getStorageSync(SEEN_CANCEL_KEY)
    return Array.isArray(arr) ? arr.map(Number) : []
  } catch (e) {
    return []
  }
}

export function markCancelledSeen(ids) {
  const set = new Set(getSeenCancelledIds())
  ;(ids || []).forEach((id) => set.add(Number(id)))
  const list = Array.from(set).slice(-80)
  uni.setStorageSync(SEEN_CANCEL_KEY, list)
  return list
}
