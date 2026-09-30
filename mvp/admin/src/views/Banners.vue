<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增轮播</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="图片" width="160">
        <template #default="{ row }">
          <img class="thumb-lg" :src="absUrl(row.imageUrl)" alt="" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="140" />
      <el-table-column prop="linkUrl" label="跳转链接" min-width="160" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑轮播' : '新增轮播'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="图片" required>
          <el-upload :show-file-list="false" :http-request="onUpload" accept="image/*">
            <el-button>上传图片</el-button>
          </el-upload>
          <img v-if="form.imageUrl" class="thumb-lg" :src="absUrl(form.imageUrl)" style="display:block;margin-top:8px" />
        </el-form-item>
        <el-form-item label="跳转链接"><el-input v-model="form.linkUrl" placeholder="可空" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
const form = reactive({ id: null, title: '', imageUrl: '', linkUrl: '', sort: 0, status: 1 })

async function load() {
  loading.value = true
  try {
    rows.value = (await adminApi.banners()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, title: '', imageUrl: '', linkUrl: '', sort: 0, status: 1 })
  visible.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  visible.value = true
}

async function onUpload({ file }) {
  const res = await uploadImage(file)
  form.imageUrl = res.url
}

async function save() {
  if (!form.imageUrl) {
    ElMessage.warning('请上传图片')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title || '',
      imageUrl: form.imageUrl,
      linkUrl: form.linkUrl || '',
      sort: form.sort,
      status: form.status
    }
    if (form.id) await adminApi.updateBanner(form.id, payload)
    else await adminApi.createBanner(payload)
    ElMessage.success('已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除该轮播？')
  await adminApi.deleteBanner(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
