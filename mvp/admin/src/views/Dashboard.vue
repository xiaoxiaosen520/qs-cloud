<template>
  <div class="page-card">
    <el-row :gutter="16">
      <el-col v-for="item in cards" :key="item.label" :xs="12" :sm="8" :md="6">
        <div class="stat" :style="{ borderTopColor: item.color }">
          <div class="label">{{ item.label }}</div>
          <div class="value">{{ item.value }}</div>
          <div class="sub">{{ item.sub }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="quick">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>待办</template>
          <div class="todo" @click="$router.push('/applies')">
            入驻待审 <el-tag type="danger">{{ data.pendingApplyCount || 0 }}</el-tag>
          </div>
          <div class="todo" @click="$router.push('/withdraws')">
            提现待审 <el-tag type="warning">{{ data.pendingWithdrawCount || 0 }}</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>运营提示</template>
          <p class="muted">对齐暹罗调度中心能力：审核入驻、治理店铺、提现打款、订单客服、轮播与抽佣配置。</p>
          <p class="muted">今日成交额按「已支付且未取消」订单实付金额汇总。</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { adminApi } from '../api/admin'

const data = reactive({
  todayOrderCount: 0,
  todayPayAmount: 0,
  pendingApplyCount: 0,
  pendingWithdrawCount: 0,
  shopCount: 0,
  userCount: 0,
  riderCount: 0,
  onlineRiderCount: 0
})

const cards = computed(() => [
  { label: '今日订单', value: data.todayOrderCount, sub: '含未支付', color: '#e54d42' },
  { label: '今日成交额', value: `¥${Number(data.todayPayAmount || 0).toFixed(2)}`, sub: '已支付', color: '#67c23a' },
  { label: '店铺数', value: data.shopCount, sub: '全平台', color: '#409eff' },
  { label: '用户数', value: data.userCount, sub: 'C 端账号', color: '#909399' },
  { label: '骑手数', value: data.riderCount, sub: `在线 ${data.onlineRiderCount}`, color: '#e6a23c' },
  { label: '待审入驻', value: data.pendingApplyCount, sub: '商家申请', color: '#f56c6c' },
  { label: '待审提现', value: data.pendingWithdrawCount, sub: '线下打款', color: '#f56c6c' }
])

onMounted(async () => {
  const res = await adminApi.dashboard()
  Object.assign(data, res || {})
})
</script>

<style scoped>
.stat {
  background: #fff;
  border: 1px solid #ebeef5;
  border-top: 3px solid #e54d42;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.label {
  color: #909399;
  font-size: 13px;
}

.value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
}

.sub {
  margin-top: 4px;
  color: #c0c4cc;
  font-size: 12px;
}

.quick {
  margin-top: 8px;
}

.todo {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  cursor: pointer;
  border-bottom: 1px dashed #ebeef5;
}

.todo:last-child {
  border-bottom: none;
}
</style>
