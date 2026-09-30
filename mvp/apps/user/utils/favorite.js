const KEY = 'favoriteShopIds'

export function listFavoriteIds() {
  const raw = uni.getStorageSync(KEY)
  return Array.isArray(raw) ? raw.map(Number).filter(Boolean) : []
}

export function isFavorite(shopId) {
  return listFavoriteIds().includes(Number(shopId))
}

export function toggleFavorite(shopId) {
  const id = Number(shopId)
  if (!id) return false
  let ids = listFavoriteIds()
  if (ids.includes(id)) {
    ids = ids.filter((x) => x !== id)
    uni.setStorageSync(KEY, ids)
    return false
  }
  ids.unshift(id)
  uni.setStorageSync(KEY, ids.slice(0, 50))
  return true
}

export function listFavoriteShops(allNearbyItems) {
  const ids = new Set(listFavoriteIds())
  return (allNearbyItems || []).filter((i) => ids.has(Number(i.shop && i.shop.id)))
}
