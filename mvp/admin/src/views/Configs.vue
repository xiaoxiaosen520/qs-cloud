<template>
  <div class="page-card">
    <el-form v-loading="loading" :model="form" label-width="180px" style="max-width: 640px">
      <el-divider content-position="left">平台抽佣与配送</el-divider>
      <el-form-item label="平台抽佣比例">
        <el-input v-model="form.commission_rate">
          <template #append>小数，如 0.06 = 6%</template>
        </el-input>
      </el-form-item>
      <el-form-item label="默认配送费">
        <el-input v-model="form.default_delivery_fee">
          <template #append>元</template>
        </el-input>
      </el-form-item>
      <el-form-item label="商家提现手续费">
        <el-input v-model="form.merchant_withdraw_fee">
          <template #append>元/笔</template>
        </el-input>
      </el-form-item>
      <el-form-item label="未支付取消时间">
        <el-input v-model="form.pay_timeout_minutes">
          <template #append>分钟</template>
        </el-input>
      </el-form-item>
      <el-form-item label="待接单自动退款">
        <el-input v-model="form.accept_timeout_minutes">
          <template #append>分钟</template>
        </el-input>
      </el-form-item>
      <el-form-item label="履约自动完成">
        <el-input v-model="form.auto_complete_minutes">
          <template #append>分钟（配送中/自配）</template>
        </el-input>
      </el-form-item>

      <el-divider content-position="left">其他配置项</el-divider>
      <el-form-item v-for="key in extraKeys" :key="key" :label="key">
        <el-input v-model="form[key]" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
        <el-button @click="load">重置</el-button>
      </el-form-item>
    </el-form>
    <p class="muted">配置写入 sys_config，结算与提现会实时读取抽佣/手续费。</p>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api/admin'

const known = [
  'commission_rate',
  'default_delivery_fee',
  'merchant_withdraw_fee',
  'pay_timeout_minutes',
  'accept_timeout_minutes',
  'auto_complete_minutes'
]
const loading = ref(false)
const saving = ref(false)
const form = reactive({})
const allKeys = ref([])

const extraKeys = computed(() => allKeys.value.filter((k) => !known.includes(k)))

async function load() {
  loading.value = true
  try {
    const list = (await adminApi.configs()) || []
    allKeys.value = list.map((i) => i.configKey)
    known.forEach((k) => {
      if (form[k] === undefined) form[k] = ''
    })
    list.forEach((i) => {
      form[i.configKey] = i.configValue
    })
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const configs = {}
    Object.keys(form).forEach((k) => {
      configs[k] = form[k] == null ? '' : String(form[k])
    })
    await adminApi.updateConfigs(configs)
    ElMessage.success('配置已保存')
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
