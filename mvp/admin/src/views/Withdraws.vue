<template>
  <div class="page-card">
    <div class="toolbar">
      <el-radio-group v-model="query.role" @change="load">
        <el-radio-button label="MERCHANT">商家提现</el-radio-button>
        <el-radio-button label="RIDER">骑手提现</el-radio-button>
      </el-radio-group>
      <el-select v-model="query.status" clearable placeholder="审核状态" style="width: 140px" @change="load">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
      <el-button type="primary" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="withdrawNo" label="提现单号" min-width="160" />
      <el-table-column v-if="query.role === 'MERCHANT'" prop="shopId" label="店铺ID" width="90" />
      <el-table-column v-if="query.role === 'MERCHANT'" prop="merchantId" label="商家ID" width="90" />
      <el-table-column v-else prop="riderId" label="骑手ID" width="90" />
      <el-table-column prop="amount" label="申请金额" width="100" />
      <el-table-column prop="fee" label="手续费" width="90" />
      <el-table-column prop="actualAmount" label="到账" width="100" />
      <el-table-column prop="paymentMode" label="方式" width="90" />
      <el-table-column prop="accountSnapshot" label="收款账户" min-width="160" show-overflow-tooltip />
      <el-table-column prop="auditStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.auditStatus)">{{ statusText(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.auditStatus === 'PENDING'">
            <el-button link type="success" @click="review(row, true)">通过</el-button>
            <el-button link type="danger" @click="reject(row)">驳回</el-button>
          </template>
          <span v-else class="muted">{{ row.auditReason || '-' }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api/admin'

const loading = ref(false)
const rows = ref([])
const query = reactive({ status: 'PENDING', role: 'MERCHANT' })

function statusText(s) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[s] || s
}
function statusType(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    rows.value = (await adminApi.withdraws(query)) || []
  } finally {
    loading.value = false
  }
}

async function review(row, approved, reason) {
  if (approved) {
    await ElMessageBox.confirm(`确认已线下打款 ¥${row.actualAmount}？通过后将扣减冻结余额。`, '提现审核')
  }
  await adminApi.reviewWithdraw(row.id, { approved, reason }, { role: query.role })
  ElMessage.success(approved ? '已通过' : '已驳回')
  load()
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回提现', {
    inputPattern: /\S+/,
    inputErrorMessage: '请填写原因'
  })
  await review(row, false, value)
}

onMounted(load)
</script>
