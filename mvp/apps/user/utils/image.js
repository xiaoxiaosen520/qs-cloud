import { absUrl } from '../api/http.js'

/** 商品图：有封面用封面（相对路径会补全），否则用稳定占位图 */
export function goodsImage(coverUrl, seed) {
  const abs = absUrl(coverUrl)
  if (abs) return abs
  const s = encodeURIComponent(String(seed || 'goods'))
  return `https://picsum.photos/seed/${s}/400`
}

/** 店铺头像 / logo */
export function shopImage(logoUrl, seed) {
  return goodsImage(logoUrl, 'shop' + (seed || 'x'))
}
