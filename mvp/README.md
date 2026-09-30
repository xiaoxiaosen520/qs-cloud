# QS Takeout MVP

精简重写：多商户入驻 · 外卖 + 便利店 · uni-app（App / 微信小程序）+ Spring Boot 单体。

与根目录暹罗微服务隔离，本目录为可独立演进的新工程骨架。

## 目录

```
mvp/
├── docs/                 # 模块与接口说明
├── sql/database.sql      # 整库建表（Navicat 风格，重导清空业务数据）
├── sql/reset-db.sh       # 一键重置数据库
├── server/               # Spring Boot API（单体）
├── admin/                # 平台管理后台（Vue3 + Element Plus）
└── apps/
    ├── user/             # 用户端 uni-app
    ├── merchant/         # 商家端 uni-app
    └── rider/            # 骑手端 uni-app
```

## 模块一览

| 模块 | 职责 |
|------|------|
| auth | 登录注册、JWT、角色（user/merchant/rider/admin） |
| user | 用户资料、收货地址 |
| shop | 商家入驻、审核、店铺、营业状态、配送规则 |
| goods | 店内分类、商品、SKU、库存（便利店） |
| cart | 购物车（单店下单） |
| order | 下单、接单、取消、售后基础 |
| pay | 微信支付下单与回调 |
| delivery | 骑手、抢单/派单、配送状态 |
| admin | 平台审核、类目、抽佣、轮播配置 |

详细说明见 [docs/modules.md](docs/modules.md)，接口清单见 [docs/api-outline.md](docs/api-outline.md)。

**已实现**：JWT 登录 + 商家入驻审核，联调见 [docs/auth-apply.md](docs/auth-apply.md)。  
**已实现**：商品/SKU + 附近店铺，见 [docs/goods-shops.md](docs/goods-shops.md)。  
**已实现**：购物车 + 下单 + mock 支付 + 商家接单，见 [docs/cart-order.md](docs/cart-order.md)。  
**用户端 uni-app**：见 [apps/user/README.md](apps/user/README.md)（HBuilderX 打开联调）。  
**商家端 uni-app**：见 [apps/merchant/README.md](apps/merchant/README.md)。  
**骑手端 uni-app**：见 [apps/rider/README.md](apps/rider/README.md)；接口见 [docs/rider.md](docs/rider.md)。  
**管理后台**：见 [admin/README.md](admin/README.md)（Vite 开发服，对齐暹罗调度中心）。  
**本地启动**：复用已有 Docker MySQL，见 [docs/local-start.md](docs/local-start.md)。  
**浏览器联调 API**：http://127.0.0.1:8080/demo

## 建议开发顺序

1. 导入 / 重置 `sql/database.sql`（会清空业务数据）
2. 跑通 `server`：auth → shop 审核 → goods → cart → order → pay
3. 用户端页面：首页附近店 → 进店 → 购物车 → 下单
4. 商家端：入驻 → 商品 → 接单
5. 管理后台：审核商家 / 提现 / 配置
6. 骑手端 / App 打包 / 小程序提审

## 店铺类型

- `FOOD`：餐饮外卖（规格口味为主，库存可选）
- `CONVENIENCE`：便利店（SKU + 强制库存/售罄）
