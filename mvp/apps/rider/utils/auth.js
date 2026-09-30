const TOKEN_KEY = 'rider_token'
const USER_KEY = 'rider_user'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setLogin(data) {
  uni.setStorageSync(TOKEN_KEY, data.token || '')
  uni.setStorageSync(USER_KEY, {
    userId: data.userId,
    phone: data.phone,
    nickname: data.nickname || ''
  })
}

export function clearLogin() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

export function getUser() {
  return uni.getStorageSync(USER_KEY) || null
}

export function requireLogin() {
  if (getToken()) return true
  uni.navigateTo({ url: '/pages/login/index' })
  return false
}

export const PHASE_TEXT = {
  POOL: '待抢单',
  WAIT_PICKUP: '待取餐',
  ON_WAY: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  ACCEPTED: '待抢单',
  DELIVERING: '配送中',
  REFUNDED: '已退款'
}

export const ORDER_STATUS_TEXT = {
  ACCEPTED: '待抢单',
  DELIVERING: '配送中',
  COMPLETED: '已完成'
}

export function callPhone(phone) {
  if (!phone) {
    uni.showToast({ title: '暂无电话', icon: 'none' })
    return
  }
  uni.makePhoneCall({ phoneNumber: String(phone) })
}

export function copyText(text, tip = '已复制') {
  if (!text) return
  uni.setClipboardData({
    data: String(text),
    success: () => uni.showToast({ title: tip, icon: 'none' })
  })
}
