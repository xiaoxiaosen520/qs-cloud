import { getToken } from './auth.js'
import { orderApi } from '../api/http.js'

function safeBadge(fn) {
  try {
    fn({ fail: () => {}, complete: () => {} })
  } catch (e) {}
}

/** Tab：0 首页 | 1 订单 | 2 我的 */
export async function refreshTabBadges() {
  if (!getToken()) {
    safeBadge((opt) => uni.removeTabBarBadge({ index: 1, ...opt }))
    return
  }
  try {
    const orders = (await orderApi.list({ silent: true })) || []
    const active = orders.filter((row) => {
      const s = row && row.order ? row.order.status : row && row.status
      return ['PENDING_PAY', 'PAID', 'ACCEPTED', 'DELIVERING'].includes(s)
    }).length
    if (active > 0) {
      safeBadge((opt) =>
        uni.setTabBarBadge({ index: 1, text: active > 99 ? '99+' : String(active), ...opt })
      )
    } else {
      safeBadge((opt) => uni.removeTabBarBadge({ index: 1, ...opt }))
    }
  } catch (e) {
    safeBadge((opt) => uni.removeTabBarBadge({ index: 1, ...opt }))
  }
}
