# 微信支付（App · API v3）

微信**没有**像支付宝那样好用的公开沙箱；需要真实商户号才能真下单。代码已接好，填参数即可测。

## 你需要准备的账号与材料

1. [微信开放平台](https://open.weixin.qq.com)  
   - 创建 **移动应用**（区惠买家端）→ 拿到 **AppID**  
   - Android 包名：`com.quhui.user`（与 `manifest.json` 一致）  
   - 填应用签名（打包后用微信签名工具算出）  
   - iOS 填 Bundle ID：`com.quhui.user`

2. [微信支付商户平台](https://pay.weixin.qq.com)  
   - 开通 **App 支付** 产品  
   - 把开放平台移动应用 **关联到商户号**  
   - **API 安全**：
     - 设置 **APIv3 密钥**（32 位）  
     - 申请 / 下载 **商户 API 证书** → 得到 `apiclient_key.pem`  
     - 复制 **证书序列号**

3. 回调公网地址（与支付宝共用穿透即可）  
   `{notify-base-url}/api/pay/notify/wechat`  
   例：`http://uba99367.natappfree.cc/api/pay/notify/wechat`  
   （正式环境建议 HTTPS）

## 配置 `application.yml`

```yaml
qs.pay:
  notify-base-url: http://你的穿透域名
  wechat:
    enabled: true
    dev-simulate: false
    app-id: "wx........"
    mch-id: "1........"
    api-v3-key: "32位密钥"
    serial-no: "证书序列号"
    # 推荐文件路径，勿把私钥提交 git
    private-key-path: /绝对路径/apiclient_key.pem
    # 或内联（与 path 二选一）
    # private-key: |
    #   -----BEGIN PRIVATE KEY-----
    #   ...
    #   -----END PRIVATE KEY-----
```

改完重启：`mvn spring-boot:run`

## 用户端

- HBuilderX `manifest` → App 模块勾选 **Payment**，微信支付填开放平台 **AppID**  
- **自定义基座 / 云打包**（正式微信 App 已安装）  
- 真机下单选微信支付  

## 联调顺序

1. 商户平台能看到 App 支付已开通且应用已绑定  
2. 后端 `enabled=true` 且密钥有效  
3. 穿透通着，health 公网可达  
4. App 下单 → 调起微信 → 付款 → 订单变 PAID  

## 与支付宝差异

| | 支付宝 | 微信 |
|--|--------|------|
| 沙箱 | 有（可先测） | 基本靠正式商户小额测 |
| 本仓库已接 | App + 回调 | App + 回调 |
| 金额单位 | 元 | **分**（服务端已换算） |
