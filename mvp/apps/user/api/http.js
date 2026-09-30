/**
 * API 基址：
 * - H5 / 模拟器本机：http://127.0.0.1:8080/api
 * - 真机调试：改成电脑局域网 IP，如 http://192.168.0.6:8080/api
 */
/** 真机调试用电脑局域网 IP；本机 H5 可改回 127.0.0.1 */
export const HOST = 'http://192.168.0.6:8080'
export const BASE_URL = HOST + '/api'

/** 相对路径（如 /uploads/...）转成可访问的绝对地址 */
export function absUrl(path) {
  if (!path) return ''
  const s = String(path).trim()
  if (!s) return ''
  if (/^https?:\/\//i.test(s) || s.startsWith('data:')) return s
  return HOST + (s.startsWith('/') ? s : '/' + s)
}

export function request(path, options = {}) {
  const token = uni.getStorageSync('token') || ''
  const method = (options.method || 'GET').toUpperCase()
  let url = BASE_URL + path
  if (method === 'GET' && options.data && typeof options.data === 'object') {
    const q = Object.entries(options.data)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
      .join('&')
    if (q) url += (url.includes('?') ? '&' : '?') + q
  }

  const maxRetry = options.retry == null ? (method === 'GET' ? 1 : 0) : options.retry

  const once = (left) =>
    new Promise((resolve, reject) => {
      uni.request({
        url,
        method,
        data: method === 'GET' ? undefined : options.data,
        timeout: options.timeout || 15000,
        header: {
          Authorization: token ? `Bearer ${token}` : '',
          'Content-Type': 'application/json',
          ...(options.header || {})
        },
        success: (res) => {
          const body = res.data
          if (body && body.code === 0) {
            resolve(body.data)
            return
          }
          if (body && body.code === 401) {
            uni.removeStorageSync('token')
            uni.removeStorageSync('user')
            if (!options.silent) {
              uni.showToast({ title: '请重新登录', icon: 'none' })
            }
          } else if (!options.silent) {
            uni.showToast({ title: (body && body.message) || '请求失败', icon: 'none' })
          }
          reject(body || res)
        },
        fail: (err) => {
          if (left > 0) {
            once(left - 1).then(resolve).catch(reject)
            return
          }
          if (!options.silent) {
            uni.showToast({ title: '网络异常，请检查后重试', icon: 'none' })
          }
          reject(err)
        }
      })
    })

  return once(maxRetry)
}

export const authApi = {
  sendSms: (phone) => request('/auth/sms/send', { method: 'POST', data: { phone, scene: 'LOGIN_USER' } }),
  login: (phone, code) => request('/auth/sms/login', { method: 'POST', data: { phone, code, role: 'USER' } }),
  me: () => request('/auth/me')
}

export const profileApi = {
  get: () => request('/user/profile'),
  update: (data) => request('/user/profile', { method: 'PUT', data })
}

export function uploadFile(filePath) {
  const token = uni.getStorageSync('token') || ''
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/user/upload',
      filePath,
      name: 'file',
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (body && body.code === 0) {
            resolve(body.data)
            return
          }
          uni.showToast({ title: (body && body.message) || '上传失败', icon: 'none' })
          reject(body || res)
        } catch (e) {
          uni.showToast({ title: '上传失败', icon: 'none' })
          reject(e)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

export const shopApi = {
  nearby: (params) => request('/shops/nearby', { data: params }),
  detail: (id) => request(`/shops/${id}`),
  categories: (id) => request(`/shops/${id}/categories`),
  goods: (id, categoryId) => request(`/shops/${id}/goods`, { data: { categoryId } }),
  goodsDetail: (goodsId) => request(`/shops/goods/${goodsId}`),
  reviews: (id, limit = 20) => request(`/shops/${id}/reviews`, { data: { limit } })
}

export const bannerApi = {
  list: () => request('/banners')
}

export const favoriteApi = {
  list: () => request('/user/favorites'),
  ids: () => request('/user/favorites/ids'),
  toggle: (shopId) => request(`/user/favorites/${shopId}/toggle`, { method: 'POST', data: {} })
}

export const couponApi = {
  available: (shopId) => request('/coupons/available', { data: { shopId } }),
  claim: (id) => request(`/coupons/${id}/claim`, { method: 'POST', data: {} }),
  mine: (params) => request('/coupons/mine', { data: params || {} })
}

export const cartApi = {
  view: (opt) => request('/cart', opt || {}),
  add: (skuId, quantity = 1) => request('/cart/items', { method: 'POST', data: { skuId, quantity } }),
  update: (id, quantity) => request(`/cart/items/${id}`, { method: 'PUT', data: { quantity } }),
  remove: (id) => request(`/cart/items/${id}`, { method: 'DELETE' }),
  clear: () => request('/cart', { method: 'DELETE' })
}

export const addressApi = {
  list: () => request('/user/addresses'),
  create: (data) => request('/user/addresses', { method: 'POST', data }),
  update: (id, data) => request(`/user/addresses/${id}`, { method: 'PUT', data }),
  remove: (id) => request(`/user/addresses/${id}`, { method: 'DELETE' })
}

export const orderApi = {
  create: (data) => request('/orders', { method: 'POST', data }),
  list: (opt) => request('/orders', opt || {}),
  detail: (id) => request(`/orders/${id}`),
  mockPay: (id) => request(`/orders/${id}/mock-pay`, { method: 'POST', data: {} }),
  cancel: (id, reason) => request(`/orders/${id}/cancel`, { method: 'POST', data: { reason } }),
  refund: (id, reason) => request(`/orders/${id}/refund`, { method: 'POST', data: { reason } }),
  review: (id, data) => request(`/orders/${id}/review`, { method: 'POST', data }),
  reorder: (id) => request(`/orders/${id}/reorder`, { method: 'POST', data: {} })
}

export const payApi = {
  channels: () => request('/pay/channels'),
  prepay: (data) => request('/pay/prepay', { method: 'POST', data }),
  confirm: (orderId) => request(`/pay/confirm/${orderId}`, { method: 'POST', data: {} })
}

export const imApi = {
  sessions: () => request('/im/sessions'),
  unread: (opt) => request('/im/unread', opt || { silent: true }),
  open: (data) => request('/im/sessions/open', { method: 'POST', data }),
  detail: (id, afterId) => request(`/im/sessions/${id}`, { data: afterId ? { afterId } : {} }),
  messages: (id, afterId) =>
    request(`/im/sessions/${id}/messages`, { data: afterId ? { afterId } : {}, silent: true }),
  send: (id, contentOrPayload, msgType) => {
    const data = typeof contentOrPayload === 'object'
      ? contentOrPayload
      : { content: contentOrPayload, msgType: msgType || 'TEXT' }
    return request(`/im/sessions/${id}/messages`, { method: 'POST', data })
  },
  markRead: (id, lastMsgId) =>
    request(`/im/sessions/${id}/read`, { method: 'POST', data: lastMsgId ? { lastMsgId } : {}, silent: true })
}
