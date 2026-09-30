export function addrText(a) {
  if (!a) return ''
  const name = a.contactName || ''
  const phone = a.contactPhone || ''
  const detail = a.detail || ''
  return [name, phone, detail].filter(Boolean).join(' ')
}

export function addrDetail(a) {
  return (a && a.detail) || ''
}

export function formatDistance(meters) {
  if (meters == null || meters === '') return ''
  const m = Number(meters)
  if (Number.isNaN(m)) return ''
  if (m < 1000) return `${Math.round(m)}m`
  return `${(m / 1000).toFixed(1)}km`
}

export function formatMoney(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

export function formatWait(minutes) {
  if (minutes == null) return ''
  if (minutes < 1) return '刚刚出餐'
  if (minutes < 60) return `已等 ${minutes} 分钟`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return `已等 ${h}小时${m}分`
}

export function shortOrderNo(no) {
  if (!no) return ''
  return no.length > 10 ? no.slice(-10) : no
}

export function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

export function toCoord(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}
