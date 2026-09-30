<template>
  <view class="page">
    <view class="card section">
      <text class="section-title">基础信息</text>
      <text class="label">分类</text>
      <picker :range="categoryNames" @change="onCat">
        <view class="input picker">{{ categoryNames[catIndex] || '请选择分类' }}</view>
      </picker>
      <text class="label">商品名称</text>
      <input class="input" v-model="form.name" placeholder="例如 鲜榨橙汁" />
      <text class="label">描述</text>
      <input class="input" v-model="form.description" placeholder="卖点、口味说明（可选）" />
    </view>

    <view class="card section">
      <text class="section-title">封面图</text>
      <view class="cover-hit" @click="chooseCover">
        <image v-if="form.coverUrl" class="preview" :src="absUrl(form.coverUrl)" mode="aspectFill" />
        <view v-else class="preview placeholder" :style="{ background: coverColor(form.name || '商') }">
          <text class="preview-text">{{ shortName(form.name || '商') }}</text>
        </view>
        <view class="cover-meta">
          <text class="cover-action">点击设置封面</text>
          <text class="muted tip">相册 / 拍照 / 图片链接</text>
        </view>
      </view>
    </view>

    <view v-if="urlPanel" class="url-mask" @click="urlPanel = false">
      <view class="url-sheet" @click.stop>
        <text class="url-title">填写图片 URL</text>
        <input class="input url-input" v-model="urlDraft" focus placeholder="https:// 或 /uploads/..." />
        <view class="url-actions">
          <view class="btn-ghost url-btn" @click="urlPanel = false">取消</view>
          <view class="btn-accent url-btn" @click="confirmUrl">确定</view>
        </view>
      </view>
    </view>

    <view class="card section">
      <text class="section-title">规格与库存</text>
      <text class="label">规格名</text>
      <input class="input" v-model="skuName" placeholder="如 500ml / 默认 / 大份" />
      <view class="row2">
        <view class="col">
          <text class="label">价格（元）</text>
          <input class="input" type="digit" v-model="skuPrice" placeholder="0.00" />
        </view>
        <view class="col">
          <text class="label">库存</text>
          <input class="input" type="number" v-model="skuStock" placeholder="100" />
        </view>
      </view>
      <text class="label">条形码</text>
      <input class="input" v-model="skuBarcode" placeholder="选填，扫码建品会自动填入" />
      <text class="hint muted">便利店建议填真实库存；餐饮可填较大数字</text>
    </view>

    <view class="safe-bottom" />
    <view class="action-bar">
      <view class="btn-accent bar-btn" @click="save">保存商品</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { categoryApi, goodsApi, uploadFile, absUrl } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { coverColor, shortName } from '../../utils/format.js'

const goodsId = ref(0)
const categories = ref([])
const catIndex = ref(0)
const form = ref({ name: '', description: '', categoryId: null, coverUrl: '' })
const skuName = ref('默认')
const skuPrice = ref('10')
const skuStock = ref('100')
const skuBarcode = ref('')
const urlPanel = ref(false)
const urlDraft = ref('')

const categoryNames = computed(() => categories.value.map((c) => c.name))

onLoad(async (q) => {
  if (!requireLogin()) return
  categories.value = (await categoryApi.list()) || []
  if (!categories.value.length) {
    uni.showToast({ title: '请先加分类', icon: 'none' })
    return
  }
  form.value.categoryId = categories.value[0].id
  if (q.id) {
    goodsId.value = Number(q.id)
    const detail = await goodsApi.detail(goodsId.value)
    form.value.name = detail.goods.name
    form.value.description = detail.goods.description || ''
    form.value.coverUrl = detail.goods.coverUrl || ''
    form.value.categoryId = detail.goods.categoryId
    catIndex.value = Math.max(0, categories.value.findIndex((c) => c.id === detail.goods.categoryId))
    if (detail.skus && detail.skus[0]) {
      skuName.value = detail.skus[0].name
      skuPrice.value = String(detail.skus[0].price)
      skuStock.value = String(detail.skus[0].stock)
      skuBarcode.value = detail.skus[0].barcode || ''
    }
  } else {
    // 扫码 / 搜索建品预填
    if (q.name) form.value.name = decodeURIComponent(q.name)
    if (q.skuName) skuName.value = decodeURIComponent(q.skuName)
    if (q.price) skuPrice.value = decodeURIComponent(q.price)
    if (q.barcode) skuBarcode.value = decodeURIComponent(q.barcode)
    if (q.mode === 'scan' && q.barcode && !form.value.name) {
      form.value.name = '扫码商品'
      skuName.value = '默认'
    }
  }
})

function onCat(e) {
  catIndex.value = Number(e.detail.value)
  form.value.categoryId = categories.value[catIndex.value].id
}

function chooseCover() {
  uni.showActionSheet({
    itemList: ['从相册选择', '拍照', '填写图片 URL'],
    success: (res) => {
      if (res.tapIndex === 0) pickCover(['album'])
      else if (res.tapIndex === 1) pickCover(['camera'])
      else openUrlPanel()
    }
  })
}

function openUrlPanel() {
  urlDraft.value = form.value.coverUrl || ''
  urlPanel.value = true
}

function confirmUrl() {
  const url = (urlDraft.value || '').trim()
  if (!url) {
    uni.showToast({ title: '请填写 URL', icon: 'none' })
    return
  }
  form.value.coverUrl = url
  urlPanel.value = false
  uni.showToast({ title: '已设置', icon: 'success' })
}

function pickCover(sourceType) {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType,
    success: async (res) => {
      const path = res.tempFilePaths && res.tempFilePaths[0]
      if (!path) return
      uni.showLoading({ title: '上传中', mask: true })
      try {
        const data = await uploadFile(path)
        form.value.coverUrl = data.url
        uni.showToast({ title: '已上传', icon: 'success' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

async function save() {
  if (!form.value.name || !form.value.categoryId) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  const payload = {
    categoryId: form.value.categoryId,
    name: form.value.name,
    description: form.value.description,
    coverUrl: form.value.coverUrl || '',
    status: 1,
    skus: [
      {
        name: skuName.value || '默认',
        price: Number(skuPrice.value || 0),
        stock: Number(skuStock.value || 0),
        barcode: skuBarcode.value || ''
      }
    ]
  }
  uni.showLoading({ title: '保存中', mask: true })
  try {
    if (goodsId.value) {
      await goodsApi.update(goodsId.value, payload)
    } else {
      await goodsApi.create(payload)
    }
    uni.showToast({ title: '已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 400)
  } finally {
    uni.hideLoading()
  }
}
</script>

<style scoped>
.section { margin-bottom: 8rpx; }
.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--brand);
  margin-bottom: 20rpx;
}
.label {
  display: block;
  font-size: 24rpx;
  color: var(--muted);
  margin-bottom: 10rpx;
}
.input {
  background: #f3f6f4;
  border-radius: 14rpx;
  padding: 22rpx 20rpx;
  margin-bottom: 20rpx;
}
.picker { color: var(--text); }
.row2 { display: flex; gap: 16rpx; }
.col { flex: 1; }
.hint { display: block; margin-top: -8rpx; }
.preview-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;
}
.cover-hit {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.preview {
  width: 112rpx;
  height: 112rpx;
  border-radius: 16rpx;
  background: #e8efeb;
  flex-shrink: 0;
  overflow: hidden;
}
.preview.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-text { color: #fff; font-weight: 800; font-size: 36rpx; }
.cover-meta { flex: 1; }
.cover-action {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--brand);
}
.tip { display: block; margin-top: 8rpx; }
.url-mask {
  position: fixed;
  inset: 0;
  background: rgba(20, 32, 27, 0.45);
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.url-sheet {
  width: 100%;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  padding: 36rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
}
.url-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 20rpx;
}
.url-input { margin-bottom: 24rpx; }
.url-actions { display: flex; gap: 16rpx; }
.url-btn { flex: 1; text-align: center; }
.safe-bottom { height: 160rpx; }
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(20, 32, 27, 0.06);
}
.bar-btn { width: 100%; }
</style>
