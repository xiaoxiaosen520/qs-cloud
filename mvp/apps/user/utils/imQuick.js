/** 商家快捷话术 */
export const MERCHANT_QUICK_REPLIES = [
  '您好，已收到您的留言',
  '正在加急制作，请稍候',
  '商品已备好，等待骑手取餐',
  '抱歉给您带来不便',
  '好的，没问题'
]

/** 骑手快捷话术 */
export const RIDER_QUICK_REPLIES = [
  '已取餐，正在配送中',
  '马上到，请保持电话畅通',
  '已到楼下，请下来取餐',
  '放门口了，请注意查收',
  '您好，请问方便接听电话吗'
]

/** 买家快捷话术 */
export const USER_QUICK_REPLIES = [
  '请问大概多久能送到？',
  '少放辣，谢谢',
  '放到门口即可',
  '请尽快配送，谢谢',
  '好的，收到'
]

export function quickRepliesFor(role, sessionType) {
  if (role === 'MERCHANT') return MERCHANT_QUICK_REPLIES
  if (role === 'RIDER') return RIDER_QUICK_REPLIES
  if (sessionType === 'USER_RIDER') {
    return [
      '请问多久能到？',
      '放到门口即可',
      '我在门口等您',
      '电话打不通，请留言',
      '好的，谢谢'
    ]
  }
  return USER_QUICK_REPLIES
}
