import { getToken } from './auth.js'
import { imApi } from '../api/http.js'

/** 骑手 Tab：0抢单 1配送 2我的 */
export async function refreshImBadge(tabIndex = 2) {
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
