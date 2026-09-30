# 平台管理后台

Vue 3 + Vite + Element Plus，对齐暹罗 `vue-siam-admin` 的商用调度能力（精简版）。

## 本地启动

```bash
# 先启动 API：mvp/server （默认 8080）
cd mvp/admin
npm install
npm run dev
```

浏览器打开 http://127.0.0.1:5180（避开买家端常用的 5173）  
默认账号：`admin` / `123456`

开发态通过 Vite 代理转发 `/api`、`/uploads` 到 `http://127.0.0.1:8080`。

## 功能菜单

| 模块 | 路由 | 说明 |
|------|------|------|
| 数据中心 | `/dashboard` | 今日订单/成交额、待审入驻与提现 |
| 入驻审核 | `/applies` | 商家开店申请通过/拒绝 |
| 店铺管理 | `/shops` | 上下架、配送费/起送价等治理 |
| 商家账号 | `/merchants` | 启停商家账号 |
| 提现审核 | `/withdraws` | 线下打款确认 / 驳回退回 |
| 全平台订单 | `/orders` | 查询详情、客服强制取消 |
| 用户管理 | `/users` | 启停 C 端用户 |
| 骑手管理 | `/riders` | 启停骑手 |
| 平台类目 | `/categories` | 首页类目 CRUD |
| 首页轮播 | `/banners` | 海报 CRUD + 上传 |
| 系统配置 | `/configs` | 抽佣、配送费、提现手续费等 |

接口见 `docs/api-outline.md` → `/api/admin/*`。

## 构建

```bash
npm run build
```

产物在 `dist/`，可挂到 Nginx 或由网关静态托管；生产环境把 API 反代到同源 `/api`。
