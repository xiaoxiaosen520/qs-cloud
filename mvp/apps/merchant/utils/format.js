export function formatMoney(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

export function shortOrderNo(no) {
  if (!no) return ''
  return no.length > 10 ? no.slice(-10) : no
}

export function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

export function parseAddress(snapshot) {
  if (!snapshot) return null
  if (typeof snapshot === 'object') return snapshot
  try {
    return JSON.parse(snapshot)
  } catch (e) {
    return null
  }
}

export function addrDetail(a) {
  return (a && (a.detail || a.address)) || ''
}

export function coverColor(name) {
  const palette = ['#1b5e45', '#2a6f6f', '#3d5a80', '#bc6c25', '#6a994e', '#386641']
  let h = 0
  const s = String(name || '')
  for (let i = 0; i < s.length; i++) h = (h + s.charCodeAt(i) * (i + 1)) % palette.length
  return palette[h]
}

export function shortName(name) {
  const s = String(name || '').trim()
  return s ? s.slice(0, 1) : '商'
}

export function isToday(t) {
  if (!t) return false
  const d = new Date(String(t).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return false
  const now = new Date()
  return (
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  )
}

export function waitMinutes(t) {
  if (!t) return null
  const d = new Date(String(t).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return null
  return Math.max(0, Math.floor((Date.now() - d.getTime()) / 60000))
}

export function formatWait(minutes) {
  if (minutes == null) return ''
  if (minutes < 1) return '刚刚下单'
  if (minutes < 60) return `已等 ${minutes} 分钟`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return `已等 ${h}小时${m}分`
}
