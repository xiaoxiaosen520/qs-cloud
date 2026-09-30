# 商品与附近店铺（已实现）

前置：商家已入驻并通过审核，且**重新登录**拿到带 `shopId` 的 token。  
浏览器联调：http://127.0.0.1:8080/demo

## 商家端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/PUT | /api/merchant/shop | 店铺信息 / 营业开关等 |
| CRUD | /api/merchant/categories | 店内分类 |
| CRUD | /api/merchant/goods | 商品+SKU；`POST .../offline` 下架 |

创建商品示例：

```bash
TOKEN=<商家token>
curl -s -X POST http://127.0.0.1:8080/api/merchant/categories \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"饮料","sort":1}'

curl -s -X POST http://127.0.0.1:8080/api/merchant/goods \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
    "categoryId":1,
    "name":"可乐",
    "skus":[{"name":"500ml","price":3.5,"stock":100}]
  }'

curl -s -X PUT http://127.0.0.1:8080/api/merchant/shop \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"openStatus":1,"notice":"欢迎光临"}'
```

## 用户端（可匿名）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/shops/nearby?lat=&lng=&type= | 10km 内营业中店铺 |
| GET | /api/shops/{id} | 详情 |
| GET | /api/shops/{id}/categories | 分类 |
| GET | /api/shops/{id}/goods | 上架商品 |
| GET | /api/shops/goods/{goodsId} | 商品+SKU |

```bash
curl -s 'http://127.0.0.1:8080/api/shops/nearby?lat=31.2304&lng=121.4737&type=CONVENIENCE'
```
