import { getToken } from './auth.js'
import { imApi } from '../api/http.js'

/** 商家 Tab：0工作台 1订单 2商品 3我的 */
export async function refreshImBadge(tabIndex = 3) {
  if (!getToken()) {
    try { uni.removeTabBarBadge({ index: tabIndex }) } catch (e) {}
    return 0
  }
  try {
    const data = await imApi.unread({ silent: true })
    const n = Number(data?.unreadMessages || 0)
    if (n > 0) {
      uni.setTabBarBadge({ index: tabIndex, text: n > 99 ? '99+' : String(n) })
    } else {
      uni.removeTabBarBadge({ index: tabIndex })
    }
    return n
  } catch (e) {
    return 0
  }
}
