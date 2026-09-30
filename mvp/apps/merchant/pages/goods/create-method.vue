<template>
  <view class="page">
    <view class="sheet">
      <view class="head">
        <text class="title">选择建品方式</text>
        <text class="close" @click="close">✕</text>
      </view>

      <view
        v-for="item in methods"
        :key="item.key"
        class="row"
        @click="onPick(item)"
      >
        <view class="icon" :style="{ background: item.bg }">
          <text class="icon-text">{{ item.icon }}</text>
        </view>
        <view class="meta">
          <view class="name-row">
            <text class="name">{{ item.title }}</text>
            <text v-if="item.tag" class="badge">{{ item.tag }}</text>
          </view>
          <text class="desc">{{ item.desc }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { findByBarcode } from '../../utils/catalog.js'

const methods = [
  {
    key: 'ai',
    title: 'AI智能拍照建品',
    desc: '拍商品、货架、表格清单（进销存单/销售单等）',
    tag: '支持批量',
    icon: '拍',
    bg: 'linear-gradient(135deg, #ffe8d6, #ffd6a8)'
  },
  {
    key: 'scan',
    title: '扫码建品',
    desc: '扫描商品条形码，智能填写信息',
    tag: '推荐',
    icon: '码',
    bg: 'linear-gradient(135deg, #d8f3e8, #b8e8d4)'
  },
  {
    key: 'search',
    title: '搜索建品',
    desc: '从平台库搜索常见商品，快速建品',
    icon: '搜',
    bg: 'linear-gradient(135deg, #d6e8ff, #b8d4ff)'
  },
  {
    key: 'copy',
    title: '一键搬品',
    desc: '帮你推荐标杆店，快速复制商品清单',
    icon: '搬',
    bg: 'linear-gradient(135deg, #ffe0cc, #ffc9a8)'
  },
  {
    key: 'manual',
    title: '手动建品',
    desc: '适合创建没有条形码且搜索不到的商品',
    icon: '写',
    bg: 'linear-gradient(135deg, #e8d6ff, #d4b8ff)'
  },
  {
    key: 'hot',
    title: '爆品推荐',
    desc: '帮你智能选品，同城热卖、稀缺商品推荐',
    icon: '爆',
    bg: 'linear-gradient(135deg, #ffd6e0, #ffb8c9)'
  }
]

function close() {
  uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/goods/list' }) })
}

function goManual(query = '') {
  uni.navigateTo({ url: '/pages/goods/edit' + (query ? '?' + query : '') })
}

function onPick(item) {
  if (item.key === 'manual') {
    goManual()
    return
  }
  if (item.key === 'scan') {
    uni.scanCode({
      onlyFromCamera: false,
      scanType: ['barCode', 'qrCode'],
      success: (res) => {
        const code = (res.result || '').trim()
        if (!code) {
          uni.showToast({ title: '未识别到条码', icon: 'none' })
          return
        }
        const hit = findByBarcode(code)
        const parts = ['mode=scan', 'barcode=' + encodeURIComponent(code)]
        if (hit) {
          parts.push('name=' + encodeURIComponent(hit.name))
          parts.push('skuName=' + encodeURIComponent(hit.spec))
          parts.push('price=' + encodeURIComponent(hit.price))
          uni.showToast({ title: '已匹配商品库', icon: 'success' })
        } else {
          uni.showToast({ title: '已填入条码，请补全信息', icon: 'none' })
        }
        goManual(parts.join('&'))
      },
      fail: () => {
        uni.showToast({ title: '已取消扫码', icon: 'none' })
      }
    })
    return
  }
  if (item.key === 'search') {
    uni.navigateTo({ url: '/pages/goods/search-create' })
    return
  }
  uni.showToast({ title: '该方式即将开放，请先用手动/扫码/搜索建品', icon: 'none' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f3f6f4;
  padding: calc(24rpx + env(safe-area-inset-top)) 28rpx 24rpx;
  box-sizing: border-box;
}
.sheet {
  background: #fff;
  border-radius: 24rpx;
  padding: 8rpx 8rpx 20rpx;
  box-shadow: 0 10rpx 30rpx rgba(20, 32, 27, 0.06);
}
.head {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 28rpx 24rpx 12rpx;
}
.title {
  font-size: 34rpx;
  font-weight: 800;
  color: #1a2e26;
}
.close {
  position: absolute;
  right: 20rpx;
  top: 24rpx;
  width: 56rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  font-size: 32rpx;
  color: #8a9a92;
}
.row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 24rpx;
  border-top: 1rpx solid #eef2ef;
}
.row:active { background: #f7faf8; }
.icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.icon-text {
  font-size: 30rpx;
  font-weight: 800;
  color: #3d4f46;
}
.meta { flex: 1; min-width: 0; }
.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}
.name {
  font-size: 30rpx;
  font-weight: 700;
  color: #1a2e26;
}
.badge {
  font-size: 20rpx;
  font-weight: 700;
  color: #fff;
  background: #e85d04;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
}
.desc {
  display: block;
  font-size: 24rpx;
  color: #8a9a92;
  line-height: 1.4;
}
.arrow {
  font-size: 40rpx;
  color: #c2cdc7;
  font-weight: 300;
}
</style>
