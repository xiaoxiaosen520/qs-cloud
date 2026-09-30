import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '../utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '数据中心', icon: 'DataLine' }
      },
      {
        path: 'applies',
        name: 'Applies',
        component: () => import('../views/Applies.vue'),
        meta: { title: '入驻审核', icon: 'DocumentChecked', group: '商家中心' }
      },
      {
        path: 'shops',
        name: 'Shops',
        component: () => import('../views/Shops.vue'),
        meta: { title: '店铺管理', icon: 'Shop', group: '商家中心' }
      },
      {
        path: 'merchants',
        name: 'Merchants',
        component: () => import('../views/Merchants.vue'),
        meta: { title: '商家账号', icon: 'Avatar', group: '商家中心' }
      },
      {
        path: 'withdraws',
        name: 'Withdraws',
        component: () => import('../views/Withdraws.vue'),
        meta: { title: '提现审核', icon: 'Wallet', group: '财务' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('../views/Orders.vue'),
        meta: { title: '全平台订单', icon: 'List', group: '订单' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/Users.vue'),
        meta: { title: '用户管理', icon: 'User', group: '用户与配送' }
      },
      {
        path: 'riders',
        name: 'Riders',
        component: () => import('../views/Riders.vue'),
        meta: { title: '骑手管理', icon: 'Bicycle', group: '用户与配送' }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('../views/Categories.vue'),
        meta: { title: '平台类目', icon: 'Menu', group: '运营配置' }
      },
      {
        path: 'banners',
        name: 'Banners',
        component: () => import('../views/Banners.vue'),
        meta: { title: '首页轮播', icon: 'Picture', group: '运营配置' }
      },
      {
        path: 'configs',
        name: 'Configs',
        component: () => import('../views/Configs.vue'),
        meta: { title: '系统配置', icon: 'Setting', group: '运营配置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title || '管理后台'} · 区惠外卖`
  if (to.meta.public) {
    if (isLoggedIn() && to.path === '/login') {
      next('/dashboard')
      return
    }
    next()
    return
  }
  if (!isLoggedIn()) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
