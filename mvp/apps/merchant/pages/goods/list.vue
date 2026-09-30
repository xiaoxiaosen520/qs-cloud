<template>
  <view class="page">
    <view class="toolbar">
      <view class="btn-ghost half" @click="addCategory">加分类</view>
      <view class="btn-accent half" @click="goCreate()">上架商品</view>
    </view>

    <scroll-view scroll-x class="cats" v-if="categories.length">
      <text :class="['cat', categoryId == null && 'on']" @click="selectCat(null)">全部</text>
      <text
        v-for="c in categories"
        :key="c.id"
        :class="['cat', categoryId === c.id && 'on']"
        @click="selectCat(c.id)"
      >{{ c.name }}</text>
    </scroll-view>

    <view v-if="loading">
      <view class="skeleton" v-for="i in 3" :key="i" />
    </view>

    <view v-else-if="!goods.length" class="empty">
      <view class="empty-icon" />
      <text class="empty-title">暂无商品</text>
      <text class="muted">先加分类，再上架商品开始售卖</text>
      <view class="btn-accent empty-btn" @click="goCreate()">去上架</view>
    </view>

    <view v-else>
      <view v-for="g in goods" :key="g.id" class="goods-card" @click="goEdit(g.id)">
        <image v-if="g.coverUrl" class="cover" :src="absUrl(g.coverUrl)" mode="aspectFill" />
        <view v-else class="cover placeholder" :style="{ background: coverColor(g.name) }">
          <text class="cover-text">{{ shortName(g.name) }}</text>
        </view>
        <view class="body">
          <view class="row">
            <text class="name">{{ g.name }}</text>
            <text class="price">¥{{ formatMoney(g.minPrice) }}</text>
          </view>
          <view class="tags">
            <text :class="['tag', g.status === 1 ? 'on' : 'off']">
              {{ g.status === 1 ? '上架中' : '已下架' }}
            </text>
            <text :class="['tag', stockClass(g.stockTotal)]">库存 {{ g.stockTotal ?? 0 }}</text>
          </view>
          <text class="muted desc" v-if="g.description">{{ g.description }}</text>
          <view class="ops" @click.stop>
            <text class="link" @click="goEdit(g.id)">编辑</text>
            <text class="link danger" v-if="g.status === 1" @click="offline(g.id)">下架</text>
            <text class="link" v-else @click="online(g.id)">上架</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { categoryApi, goodsApi, absUrl } from '../../api/http.js'
import { requireLogin, confirmModal } from '../../utils/auth.js'
import { coverColor, shortName, formatMoney } from '../../utils/format.js'

const categories = ref([])
const categoryId = ref(null)
const goods = ref([])
const loading = ref(false)

function stockClass(n) {
  const stock = Number(n || 0)
  if (stock <= 0) return 'sold'
  if (stock <= 5) return 'low'
  return 'ok'
}

async function load() {
  if (!requireLogin()) return
  loading.value = true
  try {
    categories.value = (await categoryApi.list()) || []
    goods.value = (await goodsApi.list(categoryId.value)) || []
  } catch (e) {
    categories.value = []
    goods.value = []
  } finally {
    loading.value = false
  }
}

function selectCat(id) {
  categoryId.value = id
  load()
}

async function addCategory() {
  uni.showModal({
    title: '新分类',
    editable: true,
    placeholderText: '分类名',
    success: async (res) => {
      if (!res.confirm || !res.content) return
      await categoryApi.create({ name: res.content, sort: 0 })
      await load()
    }
  })
}

function goCreate() {
  uni.navigateTo({ url: '/pages/goods/create-method' })
}

function goEdit(id) {
  uni.navigateTo({ url: '/pages/goods/edit' + (id ? '?id=' + id : '') })
}

async function offline(id) {
  const ok = await confirmModal('确认下架该商品？')
  if (!ok) return
  await goodsApi.offline(id)
  uni.showToast({ title: '已下架', icon: 'none' })
  await load()
}

async function online(id) {
  await goodsApi.online(id)
  uni.showToast({ title: '已上架', icon: 'success' })
  await load()
}

onShow(load)
onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 28rpx 8rpx;
}
.half { flex: 1; padding: 18rpx 0; font-size: 26rpx; }
.cats {
  white-space: nowrap;
  padding: 8rpx 28rpx 12rpx;
}
.cat {
  display: inline-block;
  padding: 12rpx 24rpx;
  margin-right: 12rpx;
  background: #fff;
  border-radius: 999rpx;
  color: var(--muted);
  font-size: 26rpx;
}
.cat.on {
  background: var(--brand);
  color: #fff;
  font-weight: 700;
}
.goods-card {
  display: flex;
  gap: 20rpx;
  background: #fff;
  border-radius: 20rpx;
  margin: 16rpx 28rpx;
  padding: 24rpx;
  box-shadow: 0 6rpx 18rpx rgba(20, 32, 27, 0.04);
}
.cover {
  width: 140rpx;
  height: 140rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
  background: #e8efeb;
}
.cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover-text { color: #fff; font-size: 44rpx; font-weight: 800; }
.body { flex: 1; min-width: 0; }
.row {
  display: flex;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 10rpx;
}
.name { font-weight: 700; font-size: 30rpx; flex: 1; }
.tags { display: flex; gap: 12rpx; flex-wrap: wrap; }
.tag {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  background: #eef3f0;
  color: var(--muted);
}
.tag.on { background: rgba(42, 157, 143, 0.12); color: var(--ok); }
.tag.off { background: #eee; color: #888; }
.tag.ok { background: #eef3f0; color: var(--brand-soft); }
.tag.low { background: var(--accent-soft); color: var(--accent); }
.tag.sold { background: #fde8e8; color: var(--danger); }
.desc {
  display: block;
  margin-top: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ops {
  margin-top: 14rpx;
  display: flex;
  gap: 28rpx;
}
.link { color: var(--brand); font-weight: 600; font-size: 26rpx; }
.danger { color: var(--danger); }
.empty-btn { margin: 32rpx 80rpx 0; }
</style>
