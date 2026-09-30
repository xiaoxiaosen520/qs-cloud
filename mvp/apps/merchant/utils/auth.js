const TOKEN_KEY = 'merchant_token'
const USER_KEY = 'merchant_user'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setLogin(data) {
  uni.setStorageSync(TOKEN_KEY, data.token || '')
  uni.setStorageSync(USER_KEY, {
    userId: data.userId,
    phone: data.phone,
    shopId: data.shopId || 0,
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

export const ORDER_STATUS_TEXT = {
  PENDING_PAY: '待支付',
  PAID: '待接单',
  ACCEPTED: '已接单',
  DELIVERING: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款'
}

export const APPLY_STATUS_TEXT = {
  PENDING: '审核中',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
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

export function confirmModal(content, title = '请确认') {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false)
    })
  })
}
