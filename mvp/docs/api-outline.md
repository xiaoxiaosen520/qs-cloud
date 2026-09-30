# 接口清单（MVP）

统一前缀 `/api`，鉴权 Header：`Authorization: Bearer <token>`。  
响应：`{ code, message, data }`。

## 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /sms/send | 发验证码 |
| POST | /sms/login | 验证码登录 |
| POST | /wx/login | 微信 code 登录 |
| GET | /me | 当前用户信息 |

## 用户 `/api/user`

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | /profile | 更新资料 |
| GET/POST/PUT/DELETE | /addresses[/{id}] | 地址 CRUD |

## 店铺（C 端）`/api/shops`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /nearby | lat,lng,type?,categoryId? 附近店铺 |
| GET | /{id} | 店铺详情 |
| GET | /{id}/categories | 店内分类 |
| GET | /{id}/goods | 商品列表 |
| GET | /goods/{goodsId} | 商品详情（含 SKU） |

## 入驻与商家店 `/api/merchant`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /apply | 提交入驻 |
| GET | /apply/status | 申请状态 |
| GET/PUT | /shop | 我的店铺信息 / 营业开关 |
| CRUD | /categories | 店内分类 |
| CRUD | /goods | 商品与 SKU、库存 |
| GET | /orders | 店铺订单 |
| POST | /orders/{id}/accept | 接单 |
| POST | /orders/{id}/reject | 拒单 |
| POST | /orders/{id}/ready | 待配送 / 自配送出发 |
| POST | /orders/{id}/complete | 自配送完成 |

## 购物车 `/api/cart`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 当前购物车 |
| POST | /items | 加购 |
| PUT | /items/{id} | 改数量 |
| DELETE | /items/{id} | 删除 |
| DELETE | / | 清空 |

## 订单 `/api/orders`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | / | 创建订单（返回 payParams） |
| GET | / | 我的订单列表 |
| GET | /{id} | 订单详情 |
| POST | /{id}/cancel | 取消 |
| POST | /{id}/refund | 申请退款 |

## 支付 `/api/pay`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /notify/wechat | 微信支付回调（明文） |

## 骑手 `/api/rider`（二期）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /orders/pool | 可抢订单 |
| POST | /orders/{id}/grab | 抢单 |
| POST | /orders/{id}/pickup | 已取餐 |
| POST | /orders/{id}/deliver | 已送达 |

## 管理后台 `/api/admin`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /login | 管理员登录 |
| GET | /dashboard | 运营看板统计 |
| GET | /applies | 入驻列表（status/page/size） |
| POST | /applies/{id}/review | 入驻审核 `{approved, rejectReason?}` |
| GET | /shops | 店铺分页（keyword/shopType/status） |
| GET/PUT | /shops/{id} | 店铺详情 / 治理（上下架、配送费等） |
| GET/POST | /categories | 平台类目列表 / 新增 |
| PUT/DELETE | /categories/{id} | 类目更新 / 删除 |
| GET/POST | /banners | 轮播列表 / 新增 |
| PUT/DELETE | /banners/{id} | 轮播更新 / 删除 |
| GET/PUT | /configs | 系统配置列表 / 批量更新 `{configs:{k:v}}` |
| GET | /orders | 全平台订单分页 |
| GET | /orders/{id} | 订单详情 |
| POST | /orders/{id}/cancel | 客服强制取消 `{reason}` |
| GET | /withdraws | 提现列表（status） |
| POST | /withdraws/{id}/review | 提现审核 `{approved, reason?}` |
| GET | /users | 用户分页 |
| PUT | /users/{id}/status | 启停用户 |
| GET | /merchants | 商家账号分页 |
| PUT | /merchants/{id}/status | 启停商家 |
| GET | /riders | 骑手分页 |
| PUT | /riders/{id}/status | 启停骑手 |
| POST | /upload | 图片上传（multipart `file`） |
