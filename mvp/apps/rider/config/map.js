/**
 * 骑手端地图 Key（高德）— 与用户端申请方式相同，包名用骑手端
 * Android: com.qs.takeout.rider
 * iOS:     com.qs.takeout.rider
 */
export const TENCENT_MAP_KEY = ''
export const AMAP_KEY_ANDROID = ''
export const AMAP_KEY_IOS = ''
export const APP_PACKAGE_ANDROID = 'com.qs.takeout.rider'
export const APP_BUNDLE_IOS = 'com.qs.takeout.rider'

export function isAppMapReady() {
  return !!(String(AMAP_KEY_ANDROID || '').trim() || String(AMAP_KEY_IOS || '').trim())
}
