# 购物车与下单（已实现）

支付暂用 **mock**（本地联调），不接微信。

## 用户端

| 方法 | 路径 | 说明 |
|------|------|------|
| CRUD | /api/user/addresses | 收货地址 |
| GET/POST/PUT/DELETE | /api/cart[/items…] | 购物车（换店自动清空） |
| POST | /api/orders | 下单；`mockPay:true` 直接已支付 |
| POST | /api/orders/{id}/mock-pay | 待支付订单 mock 支付 |
| POST | /api/orders/{id}/cancel | 取消（恢复库存） |
| GET | /api/orders[/id] | 我的订单 |

下单示例（先登录 USER、有地址、购物车有货）：

```bash
curl -s -X POST http://127.0.0.1:8080/api/orders \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"shopId":1,"addressId":1,"mockPay":true,"remark":"少冰"}'
```

## 商家端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/merchant/orders | 默认看 PAID/ACCEPTED/DELIVERING；返回 `{ order, itemCount, itemSummary }` |
| POST | /api/merchant/orders/{id}/accept | 接单 |
| POST | /api/merchant/orders/{id}/reject | 拒单退款+回库存 |
| POST | /api/merchant/orders/{id}/complete | 自配送完成 |

浏览器：http://127.0.0.1:8080/demo
