import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuth, getToken } from './auth'
import router from '../router'

export const HOST = ''

export function absUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return HOST + (path.startsWith('/') ? path : '/' + path)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code === 0) {
      return body.data
    }
    const msg = (body && body.message) || '请求失败'
    if (body && body.code === 401) {
      clearAuth()
      router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    ElMessage.error(msg)
    return Promise.reject(body || res)
  },
  (err) => {
    const msg = err?.response?.data?.message || err.message || '网络错误'
    if (err?.response?.status === 401) {
      clearAuth()
      router.replace('/login')
    }
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export async function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/admin/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export default http
