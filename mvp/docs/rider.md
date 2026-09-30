# 骑手配送（商用级）

对标暹罗骑手端能力（`SysLoginRider` 钱包模型 + 2.0 地图/协议/实时提醒）。

> **当前默认关闭自有骑手抢单**（`qs.rider.self-enabled=false`），平台单走蜂鸟众包，见 `docs/fengniao.md`。后期自招骑手再打开。

## 流程（自有骑手开启后）

```
用户下单(PLATFORM) → mock支付 → 商家接单(ACCEPTED)
  → 骑手上线抢单(DELIVERING / 待取餐)
  → 确认取餐(picked_up_at)
  → 确认送达(COMPLETED)
  → 配送费入账骑手钱包 / 货款入账商家（不含配送费）
```

## 接口 `/api/rider`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /profile | 骑手资料 |
| PUT | /profile | 更新昵称 `{name}` |
| GET | /stats | 今日/周/月收入、待取餐/配送中、可提现余额 |
| GET | /stats/daily | 近 N 日完成单量与收入 |
| GET | /reviews | 关联本人配送单的顾客评价 |
| POST | /online | `{online:true/false}` |
| POST | /location | `{lat,lng}` 位置上报 WGS84 |
| GET | /orders/pool | 可抢订单 |
| GET | /orders/cancelled | 近期被取消/退款的配送单（提醒用） |
| GET | /orders/mine | `phase=WAIT_PICKUP\|ON_WAY\|COMPLETED\|CANCELLED` |
| GET | /orders/{id} | 详情 |
| POST | /orders/{id}/grab | 抢单 |
| POST | /orders/{id}/pickup | 确认取餐 |
| POST | /orders/{id}/deliver | 确认送达并结算 |
| PUT | /profile/phone | 换绑手机 `{phone,code}` |
| GET | /wallet | 钱包 |
| PUT | /wallet/settlement | 收款账户 |
| GET | /billings | 资金流水 |
| GET/POST | /withdraws | 提现列表 / 申请 |
| POST | /upload | 头像等图片上传 |

登录：`role=RIDER`，`scene=LOGIN_RIDER`。

管理端审核骑手提现：`GET/POST /api/admin/withdraws?role=RIDER`。

## 库表

骑手钱包 / 流水 / 提现 / 位置字段已写入 `mvp/sql/database.sql`。初始化或重置：

```bash
bash mvp/sql/reset-db.sh
```
