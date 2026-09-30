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
  const token = uni.getStorageSync('merchant_token') || ''
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
          uni.removeStorageSync('merchant_token')
          uni.removeStorageSync('merchant_user')
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
  const token = uni.getStorageSync('merchant_token') || ''
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/merchant/upload',
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
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

export const authApi = {
  sendSms: (phone) =>
    request('/auth/sms/send', { method: 'POST', data: { phone, scene: 'LOGIN_MERCHANT' } }),
  login: (phone, code) =>
    request('/auth/sms/login', { method: 'POST', data: { phone, code, role: 'MERCHANT' } }),
  me: () => request('/auth/me')
}

export const applyApi = {
  submit: (data) => request('/merchant/apply', { method: 'POST', data }),
  status: () => request('/merchant/apply/status', { silent: true })
}

export const shopApi = {
  get: () => request('/merchant/shop', { silent: true }),
  update: (data) => request('/merchant/shop', { method: 'PUT', data })
}

export const categoryApi = {
  list: () => request('/merchant/categories'),
  create: (data) => request('/merchant/categories', { method: 'POST', data }),
  update: (id, data) => request(`/merchant/categories/${id}`, { method: 'PUT', data }),
  remove: (id) => request(`/merchant/categories/${id}`, { method: 'DELETE' })
}

export const goodsApi = {
  list: (categoryId) => request('/merchant/goods', { data: { categoryId } }),
  detail: (id) => request(`/merchant/goods/${id}`),
  create: (data) => request('/merchant/goods', { method: 'POST', data }),
  update: (id, data) => request(`/merchant/goods/${id}`, { method: 'PUT', data }),
  offline: (id) => request(`/merchant/goods/${id}/offline`, { method: 'POST', data: {} }),
  online: (id) => request(`/merchant/goods/${id}/online`, { method: 'POST', data: {} })
}

export const orderApi = {
  list: (status) => request('/merchant/orders', { data: { status } }),
  detail: (id) => request(`/merchant/orders/${id}`),
  accept: (id) => request(`/merchant/orders/${id}/accept`, { method: 'POST', data: {} }),
  dispatchFengNiao: (id) => request(`/merchant/orders/${id}/fengniao/dispatch`, { method: 'POST', data: {} }),
  reject: (id, reason) =>
    request(`/merchant/orders/${id}/reject`, { method: 'POST', data: { reason } }),
  complete: (id) => request(`/merchant/orders/${id}/complete`, { method: 'POST', data: {} }),
  approveRefund: (id) =>
    request(`/merchant/orders/${id}/refund/approve`, { method: 'POST', data: {} }),
  rejectRefund: (id, reason) =>
    request(`/merchant/orders/${id}/refund/reject`, { method: 'POST', data: { reason } })
}

export const financeApi = {
  wallet: () => request('/merchant/wallet'),
  updateSettlement: (data) => request('/merchant/wallet/settlement', { method: 'PUT', data }),
  billings: () => request('/merchant/billings'),
  withdraws: () => request('/merchant/withdraws'),
  applyWithdraw: (data) => request('/merchant/withdraws', { method: 'POST', data }),
  todayStats: () => request('/merchant/stats/today', { silent: true })
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
