# Auth + 入驻审核（已实现）

## 联调步骤

1. 导入 / 重置库：`bash mvp/sql/reset-db.sh` 或导入 `mvp/sql/database.sql`
2. 修改 `server/src/main/resources/application.yml` 数据库账号
3. 启动：

```bash
cd mvp/server && mvn spring-boot:run
```

## 接口示例

开发环境验证码可用固定值 `123456`（`qs.sms.dev-fixed-code=true`）。

### 1. 商家发验证码并登录

```bash
curl -s -X POST http://127.0.0.1:8080/api/auth/sms/send \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800138000","scene":"LOGIN_MERCHANT"}'

curl -s -X POST http://127.0.0.1:8080/api/auth/sms/login \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800138000","code":"123456","role":"MERCHANT"}'
```

记下返回的 `token`。

### 2. 提交入驻

```bash
curl -s -X POST http://127.0.0.1:8080/api/merchant/apply \
  -H "Authorization: Bearer <MERCHANT_TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{
    "contactName":"张三",
    "contactPhone":"13800138000",
    "shopName":"夜猫便利店",
    "shopType":"CONVENIENCE",
    "categoryId":2,
    "notice":"欢迎光临",
    "licenseUrl":"/uploads/demo-license.png",
    "idCardFrontUrl":"/uploads/demo-id-front.png",
    "idCardBackUrl":"/uploads/demo-id-back.png",
    "address":"测试路1号",
    "houseNumber":"108室",
    "lat":31.2304,
    "lng":121.4737
  }'
```

### 3. 管理员登录并审核

```bash
curl -s -X POST http://127.0.0.1:8080/api/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}'

curl -s 'http://127.0.0.1:8080/api/admin/applies?status=PENDING' \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

curl -s -X POST http://127.0.0.1:8080/api/admin/applies/1/review \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{"approved":true}'
```

### 4. 商家重新登录拿到 shopId

审核通过后需重新登录，JWT 中才会带上 `shopId`。

```bash
curl -s -X POST http://127.0.0.1:8080/api/auth/sms/login \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800138000","code":"123456","role":"MERCHANT"}'
```

### 用户端登录

`scene=LOGIN_USER`，`role=USER`。
