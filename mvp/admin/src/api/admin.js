import http from '../utils/http'

export const adminApi = {
  login: (username, password) => http.post('/admin/login', { username, password }),
  dashboard: () => http.get('/admin/dashboard'),

  applies: (params) => http.get('/admin/applies', { params }),
  reviewApply: (id, data) => http.post(`/admin/applies/${id}/review`, data),

  shops: (params) => http.get('/admin/shops', { params }),
  shop: (id) => http.get(`/admin/shops/${id}`),
  updateShop: (id, data) => http.put(`/admin/shops/${id}`, data),

  categories: () => http.get('/admin/categories'),
  createCategory: (data) => http.post('/admin/categories', data),
  updateCategory: (id, data) => http.put(`/admin/categories/${id}`, data),
  deleteCategory: (id) => http.delete(`/admin/categories/${id}`),

  banners: () => http.get('/admin/banners'),
  createBanner: (data) => http.post('/admin/banners', data),
  updateBanner: (id, data) => http.put(`/admin/banners/${id}`, data),
  deleteBanner: (id) => http.delete(`/admin/banners/${id}`),

  configs: () => http.get('/admin/configs'),
  updateConfigs: (configs) => http.put('/admin/configs', { configs }),

  orders: (params) => http.get('/admin/orders', { params }),
  orderDetail: (id) => http.get(`/admin/orders/${id}`),
  cancelOrder: (id, reason) => http.post(`/admin/orders/${id}/cancel`, { reason }),

  withdraws: (params) => http.get('/admin/withdraws', { params }),
  reviewWithdraw: (id, data, params) => http.post(`/admin/withdraws/${id}/review`, data, { params }),

  users: (params) => http.get('/admin/users', { params }),
  updateUserStatus: (id, status) => http.put(`/admin/users/${id}/status`, { status }),

  merchants: (params) => http.get('/admin/merchants', { params }),
  updateMerchantStatus: (id, status) => http.put(`/admin/merchants/${id}/status`, { status }),

  riders: (params) => http.get('/admin/riders', { params }),
  updateRiderStatus: (id, status) => http.put(`/admin/riders/${id}/status`, { status })
}
