<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="brand" @click="$router.push('/dashboard')">
        <span v-if="!collapsed" class="brand-text">区惠调度中心</span>
        <span v-else class="brand-short">青</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="active"
          :collapse="collapsed"
          background-color="#1f2d3d"
          text-color="#bfcbd9"
          active-text-color="#ffffff"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataLine /></el-icon>
            <span>数据中心</span>
          </el-menu-item>

          <el-sub-menu index="merchant">
            <template #title>
              <el-icon><Shop /></el-icon>
              <span>商家中心</span>
            </template>
            <el-menu-item index="/applies">入驻审核</el-menu-item>
            <el-menu-item index="/shops">店铺管理</el-menu-item>
            <el-menu-item index="/merchants">商家账号</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="finance">
            <template #title>
              <el-icon><Wallet /></el-icon>
              <span>财务</span>
            </template>
            <el-menu-item index="/withdraws">提现审核</el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/orders">
            <el-icon><List /></el-icon>
            <span>全平台订单</span>
          </el-menu-item>

          <el-sub-menu index="people">
            <template #title>
              <el-icon><User /></el-icon>
              <span>用户与配送</span>
            </template>
            <el-menu-item index="/users">用户管理</el-menu-item>
            <el-menu-item index="/riders">骑手管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="ops">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>运营配置</span>
            </template>
            <el-menu-item index="/categories">平台类目</el-menu-item>
            <el-menu-item index="/banners">首页轮播</el-menu-item>
            <el-menu-item index="/configs">系统配置</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <span class="page-title">{{ $route.meta.title }}</span>
        </div>
        <div class="right">
          <el-dropdown>
            <span class="user">
              {{ user?.nickname || '管理员' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const user = getUser()
const active = computed(() => route.path)

function logout() {
  clearAuth()
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #1f2d3d;
  transition: width 0.2s;
  overflow: hidden;
}

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-text {
  font-size: 16px;
}

.brand-short {
  font-size: 20px;
  color: #e54d42;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  height: 56px;
}

.left,
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.user {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.main {
  padding: 16px;
}
</style>
