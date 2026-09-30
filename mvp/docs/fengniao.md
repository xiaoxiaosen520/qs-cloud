# 蜂鸟众包配送

当前没有自招骑手，**平台配送走蜂鸟**，自有骑手端暂关闭。

## 流程

```
用户下单(PLATFORM) → 支付 → 商家接单(ACCEPTED)
  → 自动呼叫蜂鸟（delivery_dispatch）
  → 回调：骑手接单 DELIVERING → 到店 → 取餐 → 送达 COMPLETED
  → 商家入账（配送费归蜂鸟，不再分给自有骑手）
```

联系骑手：打蜂鸟回调里的手机号，不走自有骑手 IM。

## 配置 `application.yml`

```yaml
qs:
  rider:
    self-enabled: false   # 后期自招骑手改 true
  fengniao:
    mock: true            # 有开放平台资质后改 false
    mock-step-ms: 20000
    sandbox: true
    app-id: ""
    secret: ""
    notify-url: https://你的域名/api/delivery/fengniao/notify
```

Mock 下约每 20 秒推进一档：已呼叫 → 接单 → 到店 → 取餐 → 送达。

正式环境：到 [蜂鸟即时配送开放平台](https://open.ele.me/documents) 入驻，填 `app-id`/`secret`，回调必须 HTTPS 域名。店铺可填 `fengniao_store_code`，空则用 `SHOP_{id}`。

## 库表

`delivery_dispatch`。已有库执行：

```bash
docker exec -i mysql-old-data mysql -uroot -proot < mvp/sql/patch-fengniao.sql
```

商家端失败可点「重新呼叫蜂鸟」。
