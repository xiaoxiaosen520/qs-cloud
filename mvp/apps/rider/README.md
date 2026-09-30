# 骑手端 uni-app（商用级）

对标万岳骑手能力（不含任务地图 / 送达码 / 转单）：抢单、详情地图、取消提醒、统计、评价、设置、钱包。

## 怎么跑

1. 启动 API：`cd mvp/server && mvn spring-boot:run`
2. 数据库用 `mvp/sql/database.sql`（或 `bash mvp/sql/reset-db.sh`）
3. HBuilderX 打开本目录
4. 真机改 `api/http.js` 的 `HOST`

## 页面要点

| 页面 | 作用 |
|------|------|
| home | 抢单、休息确认、新单/取消提醒、位置上报 |
| order/* | 配送列表（含已取消）、详情内嵌地图 |
| stats | 近 7/14/30 日订单统计 |
| reviews | 顾客评价 |
| set/* | 提醒开关、资料、换绑手机 |
| finance/* | 钱包流水提现 |
| im/* | 联系顾客 |
| webview | H5 帮助页 |
