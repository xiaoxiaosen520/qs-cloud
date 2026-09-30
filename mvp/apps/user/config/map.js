/**
 * 地图 Key — Android / iOS 双端（高德）
 * 控制台：https://console.amap.com/dev/key/app
 *
 * ── 一、先在 manifest.json 确认包名（与高德申请时一致）──
 *   Android packagename: com.quhui.user
 *   iOS     bundleid:    com.quhui.user
 *
 * ── 二、高德控制台创建 2 个 Key ──
 *
 * 【Android Key】
 *   1. 应用类型选「Android 平台」
 *   2. 发布版安全码 SHA1 + 调试版 SHA1 都要填（见下方命令）
 *   3. PackageName: com.quhui.user
 *
 *   调试 SHA1（HBuilderX 默认 debug 证书，本机执行）:
 *     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
 *
 *   云打包正式版 SHA1: 发行 → 原生 App-云打包 → 使用自有证书时，
 *   对 .keystore 执行 keytool -list -v -keystore 你的.jks
 *
 * 【iOS Key】
 *   1. 应用类型选「iOS 平台」
 *   2. Bundle ID: com.quhui.user
 *   （无需 SHA1；Apple 开发者账号与描述文件在 HBuilderX 云打包 / Xcode 里配置）
 *
 * ── 三、填入下面 + manifest.json（两处保持一致）──
 *   manifest → app-plus.distribute.sdkConfigs.maps.amap
 */
export const TENCENT_MAP_KEY = ''

export const AMAP_KEY_ANDROID = ''
export const AMAP_KEY_IOS = ''

/** 与 manifest 一致，供文档 / 脚本引用 */
export const APP_PACKAGE_ANDROID = 'com.quhui.user'
export const APP_BUNDLE_IOS = 'com.quhui.user'

export function isH5MapReady() {
  return !!String(TENCENT_MAP_KEY || '').trim()
}

export function isAppMapReady() {
  return !!(String(AMAP_KEY_ANDROID || '').trim() || String(AMAP_KEY_IOS || '').trim())
}
