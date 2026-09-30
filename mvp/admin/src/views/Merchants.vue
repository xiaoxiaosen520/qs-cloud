<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" clearable placeholder="手机号/姓名" style="width: 200px" @keyup.enter="search" />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px">
        <el-option label="正常" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="shopId" label="店铺ID" width="100" />
      <el-table-column prop="withdrawableBalance" label="可提现" width="110" />
      <el-table-column prop="frozenBalance" label="冻结" width="110" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggle(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: undefined, page: 1, size: 20 })

async function load() {
  loading.value = true
  try {
    const page = await adminApi.merchants(query)
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
async function toggle(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(next === 0 ? '确认停用该商家账号？' : '确认启用？')
  await adminApi.updateMerchantStatus(row.id, next)
  ElMessage.success('已更新')
  load()
}
onMounted(load)
</script>
