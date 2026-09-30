/**
 * IM WebSocket：实时收消息；断线自动重连；业务层仍可轮询兜底。
 */
let socketTask = null
let reconnectTimer = null
let heartbeatTimer = null
let intentionalClose = false
const listeners = new Set()
let getTokenFn = () => ''
let hostFn = () => 'http://127.0.0.1:8080'
let activeSessionId = 0

export function configureImSocket({ getToken, host }) {
  if (typeof getToken === 'function') getTokenFn = getToken
  if (typeof host === 'function') hostFn = host
}

export function setActiveImSession(sessionId) {
  activeSessionId = Number(sessionId || 0)
}

export function getActiveImSession() {
  return activeSessionId
}

export function onImSocketEvent(handler) {
  listeners.add(handler)
  return () => listeners.delete(handler)
}

function emit(evt) {
  listeners.forEach((fn) => {
    try { fn(evt) } catch (e) { /* ignore */ }
  })
}

function wsUrl() {
  const token = getTokenFn()
  if (!token) return ''
  const host = String(hostFn() || '').replace(/\/$/, '')
  const wsHost = host.replace(/^http/, 'ws')
  return `${wsHost}/ws/im?token=${encodeURIComponent(token)}`
}

export function connectImSocket() {
  const url = wsUrl()
  if (!url) {
    disconnectImSocket()
    return
  }
  intentionalClose = false
  if (socketTask) {
    try { socketTask.close({}) } catch (e) {}
    socketTask = null
  }
  socketTask = uni.connectSocket({
    url,
    complete: () => {}
  })
  socketTask.onOpen(() => {
    clearReconnect()
    startHeartbeat()
    emit({ type: 'im.socket_open' })
  })
  socketTask.onMessage((res) => {
    let data = res.data
    try {
      if (typeof data === 'string') data = JSON.parse(data)
    } catch (e) {
      return
    }
    if (!data || !data.type) return
    if (data.type === 'pong') return
    emit(data)
  })
  socketTask.onClose(() => {
    stopHeartbeat()
    socketTask = null
    emit({ type: 'im.socket_close' })
    if (!intentionalClose) scheduleReconnect()
  })
  socketTask.onError(() => {
    // onClose 会跟着重连
  })
}

export function disconnectImSocket() {
  intentionalClose = true
  clearReconnect()
  stopHeartbeat()
  if (socketTask) {
    try { socketTask.close({}) } catch (e) {}
    socketTask = null
  }
}

function scheduleReconnect() {
  clearReconnect()
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    if (getTokenFn()) connectImSocket()
  }, 3000)
}

function clearReconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    try {
      if (socketTask) socketTask.send({ data: JSON.stringify({ type: 'ping' }) })
    } catch (e) {}
  }, 25000)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}
