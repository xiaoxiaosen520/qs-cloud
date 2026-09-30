# 商家端 uni-app

对接 `mvp/server`，能力对齐 `siam-merchant` / `vue-siam-shop` 的可上线切片：接单、商品、资金账本与提现。

## 怎么跑

1. 需要时重置库：`bash mvp/sql/reset-db.sh`（导入 `sql/database.sql`）
2. 启动 API：`cd mvp/server && mvn spring-boot:run`
3. HBuilderX 打开本目录 `mvp/apps/merchant`

真机把 `api/http.js` 的 `HOST` 改成电脑局域网 IP。

## 推荐流程

1. 登录（验证码 `123456`）→ 入驻 → 管理员审核 → **重新登录**
2. 开门营业 → 加分类 → 上传封面并上架商品
3. 用户下单后，工作台接单；完成后自动入账（扣平台抽佣）
4. 「我的 → 资金账户」完善收款信息 → 申请提现 → demo 页管理员审核

## 页面

| 页面 | 作用 |
|------|------|
| pages/home | 工作台：营业开关、今日入账、待接单、新单震动提醒 |
| pages/order/* | 订单列表/详情 |
| pages/goods/* | 商品管理、本地上传封面、上下架 |
| pages/finance/* | 钱包 / 流水 / 提现（对齐 siam 账户模块） |
| pages/shop | 店铺设置 |
| pages/mine | 我的：可提现余额、快捷入口 |
| pages/apply | 商家入驻（资质/证照/照片，对齐 siam fillInformation） |
| pages/login | 商家登录 |
