<template>
  <div class="login-page">
    <div class="panel">
      <div class="hero">
        <h1>区惠外卖</h1>
        <p>平台调度中心</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="admin" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            size="large"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button type="danger" size="large" class="submit" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
      <p class="hint">默认账号 admin / 123456</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api/admin'
import { setAuth } from '../utils/auth'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: ''
})
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await adminApi.login(form.username, form.password)
    setAuth(data)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100%;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 20% 20%, rgba(229, 77, 66, 0.25), transparent 40%),
    radial-gradient(circle at 80% 0%, rgba(31, 45, 61, 0.35), transparent 35%),
    linear-gradient(160deg, #1f2d3d 0%, #2c3e50 45%, #e54d42 160%);
}

.panel {
  width: min(420px, 92vw);
  background: #fff;
  border-radius: 12px;
  padding: 36px 32px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.hero {
  margin-bottom: 24px;
}

.hero h1 {
  margin: 0;
  font-size: 28px;
  color: #1f2d3d;
}

.hero p {
  margin: 8px 0 0;
  color: #909399;
}

.submit {
  width: 100%;
  margin-top: 8px;
}

.hint {
  margin: 16px 0 0;
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
}
</style>
