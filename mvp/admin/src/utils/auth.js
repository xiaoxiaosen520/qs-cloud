const TOKEN_KEY = 'admin_token'
const USER_KEY = 'admin_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export function setAuth(loginData) {
  localStorage.setItem(TOKEN_KEY, loginData.token || '')
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      userId: loginData.userId,
      role: loginData.role,
      nickname: loginData.nickname || '管理员'
    })
  )
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}
