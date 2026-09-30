<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.status" clearable placeholder="审核状态" style="width: 140px" @change="load">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
      <el-button type="primary" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="shopName" label="店铺名" min-width="140" />
      <el-table-column prop="shopType" label="类型" width="110">
        <template #default="{ row }">{{ shopTypeText(row.shopType) }}</template>
      </el-table-column>
      <el-table-column prop="contactName" label="联系人" width="100" />
      <el-table-column prop="contactPhone" label="手机号" width="120" />
      <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="success" @click="review(row, true)">通过</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="danger" @click="reject(row)">拒绝</el-button>
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

    <el-dialog v-model="detailVisible" title="入驻申请详情" width="720px">
      <el-descriptions v-if="current" :column="2" border>
        <el-descriptions-item label="店铺名">{{ current.shopName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ shopTypeText(current.shopType) }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ current.contactName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ current.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ current.address }} {{ current.houseNumber }}</el-descriptions-item>
        <el-descriptions-item label="公告" :span="2">{{ current.notice || '-' }}</el-descriptions-item>
        <el-descriptions-item label="拒绝原因" :span="2" v-if="current.rejectReason">{{ current.rejectReason }}</el-descriptions-item>
      </el-descriptions>
      <div class="imgs" v-if="current">
        <div v-for="item in imageFields" :key="item.key">
          <div class="muted">{{ item.label }}</div>
          <el-image
            v-if="current[item.key]"
            :src="absUrl(current[item.key])"
            :preview-src-list="[absUrl(current[item.key])]"
            fit="cover"
            class="preview"
          />
          <span v-else class="muted">未上传</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api/admin'
import { absUrl } from '../utils/http'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const detailVisible = ref(false)
const current = ref(null)
const query = reactive({ status: 'PENDING', page: 1, size: 20 })

const imageFields = [
  { key: 'logoUrl', label: 'Logo' },
  { key: 'withinUrl', label: '店内照' },
  { key: 'licenseUrl', label: '营业执照' },
  { key: 'idCardFrontUrl', label: '身份证正面' },
  { key: 'idCardBackUrl', label: '身份证反面' }
]

function shopTypeText(t) {
  return t === 'CONVENIENCE' ? '便利店' : t === 'FOOD' ? '餐饮外卖' : t || '-'
}
function statusText(s) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[s] || s
}
function statusType(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const page = await adminApi.applies(query)
    rows.value = page?.records || []
    total.value = page?.total || 0
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

async function review(row, approved, rejectReason) {
  await adminApi.reviewApply(row.id, { approved, rejectReason })
  ElMessage.success(approved ? '已通过' : '已拒绝')
  load()
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('请填写拒绝原因', '拒绝入驻', {
    confirmButtonText: '确认拒绝',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '请填写原因'
  })
  await review(row, false, value)
}

onMounted(load)
</script>

<style scoped>
.imgs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 16px;
}
.preview {
  width: 100%;
  height: 120px;
  border-radius: 6px;
  margin-top: 6px;
}
</style>
