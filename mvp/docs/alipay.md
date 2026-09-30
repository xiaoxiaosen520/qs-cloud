# 支付宝 App 支付（沙箱）

当前代码已接：`alipay.trade.app.pay`（App）+ 异步通知验签；H5 预留手机网站支付表单。

## 1. 开放平台沙箱

1. [开放平台](https://open.alipay.com) → 开发助手 / **沙箱**  
2. 拿到沙箱 **APPID**、按提示配置沙箱 **RSA2 密钥**（与正式一致：上传应用公钥 → 下载支付宝公钥）  
3. 用 **沙箱支付宝 App** + 沙箱买家账号付款（正式钱包付不了沙箱单）

> 你已签约正式「App 支付」后，把 `sandbox: false` 并换成正式 APPID/密钥即可切生产。沙箱联调不依赖正式签约完成。

## 2. 本地配置

```bash
cd mvp/server
cp src/main/resources/application-local.yml.example \
   src/main/resources/application-local.yml
# 编辑 application-local.yml：APPID、私钥、支付宝公钥、notify-base-url
```

`application-local.yml` 已加入 `.gitignore`，勿提交密钥。

启动（加载 local profile）：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 3. 回调地址（必做）

支付宝服务器要能 POST 到：

`{notify-base-url}/api/pay/notify/alipay`

本机请用 ngrok / Cloudflare Tunnel / frp 等把 `8080` 暴露为 HTTPS，例如：

```yaml
qs.pay.notify-base-url: https://xxxx.ngrok-free.app
```

## 4. 用户端 App

- HBuilderX：`manifest.json` → App 模块配置勾选 **Payment(支付)** / 支付宝  
- 使用**自定义基座或云打包**后，真机调 `uni.requestPayment({ provider: 'alipay', orderInfo })`  
- **浏览器 H5 不能调 App 支付**；未签手机网站支付时，H5 请继续用「模拟支付」  
- **沙箱仅支持 Android**：支付前会 `EnvUtils.setEnv(SANDBOX)`；不切环境会报「商家订单参数异常」  
- 手机装 **支付宝沙箱版**，用沙箱买家账号；正式支付宝付不了沙箱单  
- 正式上线：`qs.pay.alipay.sandbox=false`，勿再切 SANDBOX

## 5. 联调路径

1. 后端 `qs.pay.alipay.enabled=true` 且密钥有效  
2. 买家端登录 → 下单 → 选支付宝 → 跳出沙箱支付宝付款  
3. 服务端日志 / 订单变为 `PAID`（以异步通知为准）  
4. 商家端应能看到已支付订单

## 6. 配置项摘要

| 配置 | 说明 |
|------|------|
| `qs.pay.alipay.sandbox` | `true` 走 `openapi.alipaydev.com` |
| `qs.pay.alipay.enabled` | `true` 且密钥齐全才真下单 |
| `qs.pay.alipay.dev-simulate` | 密钥未齐时是否仍显示模拟支付宝 |
| `qs.pay.notify-base-url` | 公网基址 |
