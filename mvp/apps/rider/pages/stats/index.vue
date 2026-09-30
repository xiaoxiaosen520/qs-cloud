<template>
  <view class="page">
    <view class="hero">
      <text class="label">近 {{ days }} 日完成</text>
      <text class="num">{{ totalDone }} 单 · ¥{{ formatMoney(totalIncome) }}</text>
    </view>

    <view class="tabs">
      <text :class="['tab', days === 7 && 'on']" @click="setDays(7)">近7日</text>
      <text :class="['tab', days === 14 && 'on']" @click="setDays(14)">近14日</text>
      <text :class="['tab', days === 30 && 'on']" @click="setDays(30)">近30日</text>
    </view>

    <view v-if="loading">
      <view class="skeleton" v-for="i in 4" :key="i" />
    </view>
    <view v-else>
      <view v-for="d in list" :key="d.date" class="card row">
        <view>
          <text class="date">{{ d.date }}</text>
          <text class="muted">完成 {{ d.completed || 0 }} 单</text>
        </view>
        <text class="price">¥{{ formatMoney(d.income) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { riderApi } from '../../api/http.js'
import { requireLogin } from '../../utils/auth.js'
import { formatMoney } from '../../utils/format.js'

const days = ref(7)
const list = ref([])
const loading = ref(true)

const totalDone = computed(() => list.value.reduce((s, d) => s + (d.completed || 0), 0))
const totalIncome = computed(() =>
  list.value.reduce((s, d) => s + Number(d.income || 0), 0)
)

async function load() {
  if (!requireLogin()) return
  loading.value = true
  try {
    list.value = (await riderApi.dailyStats(days.value)) || []
  } finally {
    loading.value = false
  }
}

function setDays(n) {
  days.value = n
  load()
}

onShow(load)
</script>

<style scoped>
.hero {
  padding: 36rpx 32rpx;
  background: linear-gradient(160deg, #123d2f, #1b5e45);
  color: #fff;
}
.label { display: block; font-size: 24rpx; opacity: 0.75; }
.num { display: block; margin-top: 8rpx; font-size: 36rpx; font-weight: 800; }
.tabs {
  display: flex;
  gap: 8rpx;
  padding: 16rpx 24rpx;
  background: #fff;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  border-radius: 12rpx;
  background: #f3f6f4;
  color: var(--muted);
  font-size: 26rpx;
}
.tab.on {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 700;
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.date { display: block; font-weight: 700; margin-bottom: 4rpx; }
</style>
