/**
 * 真机调试请改成电脑局域网 IP
 */
export const HOST = 'http://127.0.0.1:8080'
export const BASE_URL = HOST + '/api'

export function absUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return HOST + (path.startsWith('/') ? path : '/' + path)
}

export function request(path, options = {}) {
  const token = uni.getStorageSync('rider_token') || ''
  const method = (options.method || 'GET').toUpperCase()
  let url = BASE_URL + path
  if (method === 'GET' && options.data && typeof options.data === 'object') {
    const q = Object.entries(options.data)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
      .join('&')
    if (q) url += (url.includes('?') ? '&' : '?') + q
  }
  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method,
      data: method === 'GET' ? undefined : options.data,
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
          uni.removeStorageSync('rider_token')
          uni.removeStorageSync('rider_user')
        }
        const msg = (body && body.message) || '请求失败'
        if (!options.silent) uni.showToast({ title: msg, icon: 'none' })
        reject(body || res)
      },
      fail: (err) => {
        if (!options.silent) uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function uploadFile(filePath) {
  const token = uni.getStorageSync('rider_token') || ''
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/rider/upload',
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

export const authApi = {
  sendSms: (phone) =>
    request('/auth/sms/send', { method: 'POST', data: { phone, scene: 'LOGIN_RIDER' } }),
  login: (phone, code) =>
    request('/auth/sms/login', { method: 'POST', data: { phone, code, role: 'RIDER' } })
}

export const riderApi = {
  capability: () => request('/rider/capability', { silent: true }),
  profile: () => request('/rider/profile'),
  updateProfile: (data) => request('/rider/profile', { method: 'PUT', data }),
  changePhone: (data) => request('/rider/profile/phone', { method: 'PUT', data }),
  stats: () => request('/rider/stats'),
  dailyStats: (days) => request('/rider/stats/daily', { data: { days } }),
  reviews: (limit) => request('/rider/reviews', { data: { limit } }),
  setOnline: (online) => request('/rider/online', { method: 'POST', data: { online } }),
  reportLocation: (lat, lng) => request('/rider/location', { method: 'POST', data: { lat, lng }, silent: true }),
  pool: () => request('/rider/orders/pool'),
  cancelled: (minutes) => request('/rider/orders/cancelled', { data: { minutes }, silent: true }),
  mine: (phase) => request('/rider/orders/mine', { data: { phase } }),
  detail: (id) => request(`/rider/orders/${id}`),
  grab: (id) => request(`/rider/orders/${id}/grab`, { method: 'POST', data: {} }),
  pickup: (id) => request(`/rider/orders/${id}/pickup`, { method: 'POST', data: {} }),
  deliver: (id) => request(`/rider/orders/${id}/deliver`, { method: 'POST', data: {} })
}

export const financeApi = {
  wallet: () => request('/rider/wallet'),
  updateSettlement: (data) => request('/rider/wallet/settlement', { method: 'PUT', data }),
  billings: () => request('/rider/billings'),
  withdraws: () => request('/rider/withdraws'),
  applyWithdraw: (data) => request('/rider/withdraws', { method: 'POST', data })
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
