<template>
  <view class="page" v-if="detail">
    <view class="card">
      <text class="shop">{{ shopName }}</text>
      <text class="muted">订单已完成，给一次真实评价吧</text>
    </view>

    <view class="card">
      <text class="label">评分</text>
      <view class="stars">
        <text
          v-for="n in 5"
          :key="n"
          :class="['star', score >= n && 'on']"
          @click="score = n"
        >★</text>
      </view>
      <text class="score-tip">{{ scoreTips[score] }}</text>
      <textarea
        class="textarea"
        v-model="content"
        maxlength="200"
        placeholder="味道如何？配送是否准时？（选填）"
      />

      <text class="label photo-label">照片（选填，最多 {{ MAX }} 张）</text>
      <view class="photos">
        <view v-for="(url, i) in photos" :key="url + i" class="photo">
          <image class="photo-img" :src="absUrl(url)" mode="aspectFill" @click="preview(i)" />
          <text class="photo-del" @click.stop="remove(i)">×</text>
        </view>
        <view v-if="photos.length < MAX" class="photo add" @click="pickPhoto">
          <text class="add-ico">+</text>
          <text class="add-txt">拍照/相册</text>
        </view>
      </view>
    </view>

    <view class="btn-accent submit" @click="submit">提交评价</view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { orderApi, uploadFile, absUrl } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'

const MAX = 9
const id = ref(0)
const detail = ref(null)
const score = ref(5)
const content = ref('')
const photos = ref([])
const uploading = ref(false)
const scoreTips = { 1: '很差', 2: '较差', 3: '一般', 4: '满意', 5: '超赞' }
const shopName = computed(() => detail.value?.shop?.name || '店铺')

onLoad(async (q) => {
  if (!requireLogin()) return
  id.value = Number(q.id)
  detail.value = await orderApi.detail(id.value)
  if (detail.value?.reviewed) {
    uni.showToast({ title: '已评价过', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 500)
  }
})

function pickPhoto() {
  const left = MAX - photos.value.length
  if (left <= 0) return
  uni.showActionSheet({
    itemList: ['拍照', '从相册选择'],
    success: (res) => {
      if (res.tapIndex === 0) chooseAndUpload(['camera'], 1)
      else if (res.tapIndex === 1) chooseAndUpload(['album'], left)
    }
  })
}

function chooseAndUpload(sourceType, count) {
  uni.chooseImage({
    count,
    sizeType: ['compressed'],
    sourceType,
    success: async (res) => {
      const paths = res.tempFilePaths || []
      if (!paths.length) return
      uploading.value = true
      uni.showLoading({ title: '上传中', mask: true })
      try {
        for (const path of paths) {
          if (photos.value.length >= MAX) break
          const data = await uploadFile(path)
          if (data && data.url) photos.value.push(data.url)
        }
      } catch (e) { /* toast by upload */ }
      finally {
        uploading.value = false
        uni.hideLoading()
      }
    }
  })
}

function preview(index) {
  uni.previewImage({
    current: index,
    urls: photos.value.map((u) => absUrl(u))
  })
}

function remove(index) {
  photos.value.splice(index, 1)
}

async function submit() {
  if (uploading.value) {
    uni.showToast({ title: '图片上传中', icon: 'none' })
    return
  }
  await orderApi.review(id.value, {
    score: score.value,
    content: content.value,
    imageUrls: photos.value
  })
  uni.showToast({ title: '评价成功', icon: 'success' })
  setTimeout(() => {
    uni.redirectTo({ url: '/pages/order/detail?id=' + id.value })
  }, 400)
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; }
.shop { display: block; font-size: 30rpx; font-weight: 800; margin-bottom: 8rpx; }
.label { display: block; font-weight: 700; margin-bottom: 12rpx; }
.photo-label { margin-top: 28rpx; }
.stars { display: flex; gap: 16rpx; }
.star { font-size: 52rpx; color: #ddd; }
.star.on { color: #e85d04; }
.score-tip { display: block; margin: 12rpx 0 20rpx; color: #e85d04; font-size: 24rpx; }
.textarea {
  width: 100%; min-height: 180rpx; background: #f5f5f5; border-radius: 12rpx;
  padding: 20rpx; box-sizing: border-box;
}
.photos {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.photo {
  position: relative;
  width: 196rpx;
  height: 196rpx;
  border-radius: 12rpx;
  overflow: hidden;
  background: #f5f5f5;
}
.photo-img { width: 100%; height: 100%; }
.photo-del {
  position: absolute;
  top: 6rpx;
  right: 6rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 36rpx;
  text-align: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 28rpx;
}
.photo.add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed #ddd;
  background: #fafafa;
}
.add-ico { font-size: 48rpx; color: #bbb; line-height: 1; }
.add-txt { margin-top: 8rpx; font-size: 20rpx; color: #999; }
.submit { margin: 40rpx 28rpx; }
</style>
