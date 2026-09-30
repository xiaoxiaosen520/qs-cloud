# 模块职责与边界

单体工程按 `modules/*` 分包，表与接口归属如下。跨模块调用用同进程 Service，不拆微服务。

## auth

- 手机号验证码 / 微信登录换 token
- JWT 载荷：`userId`、`role`（USER / MERCHANT / RIDER / ADMIN）
- 商家账号绑定 `merchant_id`，骑手绑定 `rider_id`

## user

- 用户昵称头像
- 收货地址 CRUD，下单时快照到订单

## shop

- `shop_apply`：入驻申请（营业执照、类目、联系人）
- 平台审核通过后写 `shop` + `merchant` 账号
- 店铺：类型 `FOOD` | `CONVENIENCE`、坐标、起送价、配送费、营业时段、营业中/休息
- 平台类目 `platform_category`（外卖 / 便利店入口）

## goods

- 店内分类 `shop_category`
- 商品 `goods` + SKU `goods_sku`
- `CONVENIENCE`：下单扣减 `stock`，库存 ≤0 售罄
- `FOOD`：可不强校验库存，或用大库存占位

## cart

- 按「用户 + 店铺」隔离，换店清空或提示
- 结算前校验商品上架状态与库存

## order

状态机（MVP）：

```
PENDING_PAY → PAID → ACCEPTED → DELIVERING → COMPLETED
                ↘ CANCELLED
         PAID/ACCEPTED → REFUNDING → REFUNDED（基础退款）
```

- 创建订单：锁库存（便利店）→ 写订单/明细 → 调支付
- 商家：接单 / 拒单（拒单退款）
- 超时未支付自动取消（定时扫表，`pay_deadline_at`）
- 已支付待接单超时自动退款（`accept_deadline_at`）
- 配送中/自配履约超时自动完成并入账（`auto_complete_at`）

## pay

- 统一下单（微信小程序 / App）
- 支付回调验签，更新订单为 PAID
- 退款接口供取消/售后调用

## delivery

- **默认 `PLATFORM` = 蜂鸟众包**：商家接单后自动发单，回调驱动 DELIVERING / COMPLETED
- 本地 `qs.fengniao.mock=true` 模拟接单/到店/取餐/送达；正式改 `false` 并填 app-id/secret
- 自有骑手抢单 `qs.rider.self-enabled=false`（暂未自招，后期再开）
- `SELF` 商家自配仍由商家点送达

## admin

- 审核入驻、上下架店铺、启停商家/用户/骑手
- 平台类目、轮播、抽佣比例、配送全局默认值
- 商家提现审核（线下打款）
- 全平台订单查询与客服强制取消
- 管理端 Web：`mvp/admin`（Vue3 + Element Plus）

## finance（对齐 siam-merchant 可上线切片）

- 订单完成按 `commission_rate` 入账到商家 `withdrawable_balance`
- 退款冲正；提现冻结/审核/驳回退回
- `merchant_billing_record` 流水、`merchant_withdraw_record` 提现单
- 图片本地上传 `/api/merchant/upload` → `/uploads/**`
- 工作台 `GET /api/merchant/stats/today`

## 非目标（本期不做）

积分商城、复杂优惠券后台、分站、拼团、云打印机、微服务注册中心、ELK。
