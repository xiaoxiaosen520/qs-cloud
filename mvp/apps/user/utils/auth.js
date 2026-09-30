const TOKEN_KEY = 'token'
const USER_KEY = 'user'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setLogin(data) {
  uni.setStorageSync(TOKEN_KEY, data.token || '')
  uni.setStorageSync(USER_KEY, {
    userId: data.userId,
    phone: data.phone,
    nickname: data.nickname || '',
    avatarUrl: data.avatarUrl || ''
  })
}

export function patchUser(partial) {
  const cur = getUser() || {}
  uni.setStorageSync(USER_KEY, { ...cur, ...partial })
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

export const ORDER_STATUS_TEXT = {
  PENDING_PAY: '待支付',
  PAID: '商家待接单',
  ACCEPTED: '商家已接单',
  DELIVERING: '骑手配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款'
}

export function callPhone(phone) {
  if (!phone) {
    uni.showToast({ title: '暂无电话', icon: 'none' })
    return
  }
  uni.makePhoneCall({ phoneNumber: String(phone) })
}

export const ORDER_TIMELINE = [
  { key: 'PENDING_PAY', label: '提交订单' },
  { key: 'PAID', label: '支付成功' },
  { key: 'ACCEPTED', label: '商家接单' },
  { key: 'DELIVERING', label: '配送中' },
  { key: 'COMPLETED', label: '已送达' }
]

