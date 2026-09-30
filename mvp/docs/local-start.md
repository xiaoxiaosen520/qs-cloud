# 本地启动（复用已有 Docker）

你 Docker 里已有别的项目的 `mysql-old-data:3306`、`redis:6379`，**不必再起一套**。本 MVP 目前只用 MySQL。

## 1. 导入 / 重置数据库

主脚本：`mvp/sql/database.sql`（风格同 zb 的 Navicat 导出：`DROP TABLE` → `CREATE` → 种子 `INSERT`）。  
重新导入会清空全部业务表数据。

```bash
# 仓库根目录
bash mvp/sql/reset-db.sh

# 或
docker exec -i mysql-old-data mysql -uroot -proot < mvp/sql/database.sql
```

种子数据：平台类目、轮播、示例优惠券、管理员占位。  
启动 API 后管理员：`admin` / `123456`。

密码不是 `root` 时：

```bash
QS_MYSQL_PASSWORD=你的密码 bash mvp/sql/reset-db.sh
# 并改 mvp/server/src/main/resources/application.yml
```

## 2. 启动 API

```bash
cd mvp/server
mvn spring-boot:run
```

默认：`http://127.0.0.1:8080`

支付宝沙箱见 [alipay.md](alipay.md)：复制 `application-local.yml.example` 后：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

微信支付见 [wechat-pay.md](wechat-pay.md)（需真实商户号，无公开沙箱）。

## 3. 快速验活

```bash
curl -s -X POST http://127.0.0.1:8080/api/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}'
```

完整入驻联调见 [auth-apply.md](auth-apply.md)。

## 4. 管理后台

```bash
cd mvp/admin
npm install
npm run dev
```

打开 http://127.0.0.1:5180 ，账号 `admin` / `123456`。说明见 [../admin/README.md](../admin/README.md)。

## 端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 现有 MySQL | 3306 | 共用，库名 `qs_takeout` |
| 本 API | **8080** | 占用则改 `application.yml` |
| 买家端 | **5173** | uni-app / H5 常用 |
| 管理后台 | **5180** | Vite 开发服，代理 `/api` |

## 可选：独立 MySQL

```bash
cd mvp
docker compose down -v
docker compose up -d
cd server
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

首次启动会自动执行 `sql/database.sql`（端口 **3307**）。
