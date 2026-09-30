import { getToken } from './auth.js'
import { imApi } from '../api/http.js'

function safeBadge(fn) {
  try {
    fn({ fail: () => {}, complete: () => {} })
  } catch (e) {}
}

/** 买家 Tab：0首页 1订单 2我的 — 未读消息挂在「我的」 */
export async function refreshImBadge(tabIndex = 2) {
  if (!getToken()) {
    safeBadge((opt) => uni.removeTabBarBadge({ index: tabIndex, ...opt }))
    return 0
  }
  try {
    const data = await imApi.unread({ silent: true })
    const n = Number(data?.unreadMessages || 0)
    if (n > 0) {
      safeBadge((opt) =>
        uni.setTabBarBadge({
          index: tabIndex,
          text: n > 99 ? '99+' : String(n),
          ...opt
        })
      )
    } else {
      safeBadge((opt) => uni.removeTabBarBadge({ index: tabIndex, ...opt }))
    }
    return n
  } catch (e) {
    return 0
  }
}
