<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" clearable placeholder="店名/电话" style="width: 200px" @keyup.enter="search" />
      <el-select v-model="query.shopType" clearable placeholder="类型" style="width: 140px">
        <el-option label="餐饮外卖" value="FOOD" />
        <el-option label="便利店" value="CONVENIENCE" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="平台状态" style="width: 140px">
        <el-option label="正常" :value="1" />
        <el-option label="已下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="店铺" min-width="180">
        <template #default="{ row }">
          <div class="shop-cell">
            <img class="thumb" :src="absUrl(row.logoUrl) || placeholder" alt="" />
            <div>
              <div>{{ row.name }}</div>
              <div class="muted">{{ row.phone }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="shopType" label="类型" width="100">
        <template #default="{ row }">{{ row.shopType === 'CONVENIENCE' ? '便利店' : '餐饮' }}</template>
      </el-table-column>
      <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
      <el-table-column label="营业" width="90">
        <template #default="{ row }">
          <el-tag :type="row.openStatus === 1 ? 'success' : 'info'" size="small">
            {{ row.openStatus === 1 ? '营业中' : '休息' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="平台" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deliveryFee" label="配送费" width="90" />
      <el-table-column prop="minOrderAmount" label="起送" width="90" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="edit(row)">编辑</el-button>
          <el-button
            link
            :type="row.status === 1 ? 'danger' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '下架' : '上架' }}
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

    <el-dialog v-model="visible" title="编辑店铺" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="店铺名"><el-input :model-value="form.name" disabled /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="营业时段"><el-input v-model="form.businessHours" /></el-form-item>
        <el-form-item label="起送价"><el-input-number v-model="form.minOrderAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="配送费"><el-input-number v-model="form.deliveryFee" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="打包费"><el-input-number v-model="form.packingFee" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="公告"><el-input v-model="form.notice" type="textarea" /></el-form-item>
        <el-form-item label="营业状态">
          <el-switch v-model="form.openStatus" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="平台状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api/admin'
import { absUrl } from '../utils/http'

const placeholder = 'https://picsum.photos/seed/shop/80'
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', shopType: '', status: undefined, page: 1, size: 20 })
const form = reactive({})

async function load() {
  loading.value = true
  try {
    const page = await adminApi.shops(query)
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

function edit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    phone: row.phone,
    businessHours: row.businessHours,
    minOrderAmount: Number(row.minOrderAmount || 0),
    deliveryFee: Number(row.deliveryFee || 0),
    packingFee: Number(row.packingFee || 0),
    notice: row.notice,
    openStatus: row.openStatus,
    status: row.status
  })
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    await adminApi.updateShop(form.id, {
      phone: form.phone,
      businessHours: form.businessHours,
      minOrderAmount: form.minOrderAmount,
      deliveryFee: form.deliveryFee,
      packingFee: form.packingFee,
      notice: form.notice,
      openStatus: form.openStatus,
      status: form.status
    })
    ElMessage.success('已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(next === 0 ? '确认平台下架该店铺？' : '确认重新上架？', '提示')
  await adminApi.updateShop(row.id, { status: next })
  ElMessage.success('已更新')
  load()
}

onMounted(load)
</script>

<style scoped>
.shop-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
