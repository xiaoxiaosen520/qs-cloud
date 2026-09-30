<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.orderNo" clearable placeholder="订单号" style="width: 200px" @keyup.enter="search" />
      <el-input v-model="query.shopId" clearable placeholder="店铺ID" style="width: 120px" />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 150px">
        <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="orderNo" label="订单号" min-width="170" />
      <el-table-column prop="shopId" label="店铺" width="80" />
      <el-table-column prop="userId" label="用户" width="80" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="payAmount" label="实付" width="90" />
      <el-table-column prop="deliveryType" label="配送" width="100" />
      <el-table-column prop="createdAt" label="下单时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="canCancel(row.status)"
            link
            type="danger"
            @click="cancel(row)"
          >强制取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>

    <el-drawer v-model="drawer" title="订单详情" size="480px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.order.status }}</el-descriptions-item>
          <el-descriptions-item label="店铺">{{ detail.shop?.name || detail.order.shopId }}</el-descriptions-item>
          <el-descriptions-item label="实付">¥{{ detail.order.payAmount }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.order.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取消原因">{{ detail.order.cancelReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址快照">{{ detail.order.addressSnapshot }}</el-descriptions-item>
        </el-descriptions>
        <h4>商品</h4>
        <el-table :data="detail.items || []" size="small">
          <el-table-column prop="goodsName" label="商品" />
          <el-table-column prop="skuName" label="规格" width="100" />
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column prop="price" label="单价" width="80" />
        </el-table>
        <h4>状态轨迹</h4>
        <el-timeline>
          <el-timeline-item v-for="log in detail.logs || []" :key="log.id" :timestamp="log.createdAt">
            {{ log.fromStatus || '-' }} → {{ log.toStatus }}（{{ log.operatorType }}）{{ log.remark }}
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api/admin'

const statuses = ['PENDING_PAY', 'PAID', 'ACCEPTED', 'DELIVERING', 'COMPLETED', 'CANCELLED', 'REFUNDING', 'REFUNDED']
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const drawer = ref(false)
const detail = ref(null)
const query = reactive({ orderNo: '', shopId: '', status: '', page: 1, size: 20 })

function canCancel(status) {
  return !['CANCELLED', 'REFUNDED', 'COMPLETED'].includes(status)
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (params.shopId === '') delete params.shopId
    const page = await adminApi.orders(params)
    rows.value = page?.records || []
    total.value = page?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

async function openDetail(row) {
  detail.value = await adminApi.orderDetail(row.id)
  drawer.value = true
}

async function cancel(row) {
  const { value } = await ElMessageBox.prompt('请填写强制取消原因', '客服强制取消', {
    inputPattern: /\S+/,
    inputErrorMessage: '请填写原因'
  })
  await adminApi.cancelOrder(row.id, value)
  ElMessage.success('已取消')
  load()
}

onMounted(load)
</script>
