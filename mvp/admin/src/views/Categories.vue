<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增类目</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="shopType" label="适用类型" width="140" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="图标" width="90">
        <template #default="{ row }">
          <img v-if="row.iconUrl" class="thumb" :src="absUrl(row.iconUrl)" alt="" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑类目' : '新增类目'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.shopType" style="width: 100%">
            <el-option label="餐饮 FOOD" value="FOOD" />
            <el-option label="便利店 CONVENIENCE" value="CONVENIENCE" />
            <el-option label="全部 ALL" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="图标">
          <el-upload :show-file-list="false" :http-request="onUpload" accept="image/*">
            <el-button>上传</el-button>
          </el-upload>
          <img v-if="form.iconUrl" class="thumb" :src="absUrl(form.iconUrl)" style="margin-left: 12px" />
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
import { absUrl, uploadImage } from '../utils/http'

const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const rows = ref([])
const form = reactive({ id: null, name: '', shopType: 'FOOD', sort: 0, status: 1, iconUrl: '' })

async function load() {
  loading.value = true
  try {
    rows.value = (await adminApi.categories()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', shopType: 'FOOD', sort: 0, status: 1, iconUrl: '' })
  visible.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  visible.value = true
}

async function onUpload({ file }) {
  const res = await uploadImage(file)
  form.iconUrl = res.url
}

async function save() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name,
      shopType: form.shopType,
      sort: form.sort,
      status: form.status,
      iconUrl: form.iconUrl || ''
    }
    if (form.id) await adminApi.updateCategory(form.id, payload)
    else await adminApi.createCategory(payload)
    ElMessage.success('已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除类目「${row.name}」？`)
  await adminApi.deleteCategory(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
