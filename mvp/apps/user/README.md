# 用户端 uni-app（商用向）

参考仓库内暹罗用户端（`wxapplet-siam-user`）信息架构，对接 `mvp/server`：

首页附近店 → 搜索 → 进店加购 → 购物车结算 → mock 支付 → 订单 / 退款 / 评价。

品牌：**区惠**（墨绿 + 橙色）。

## 商用能力（相对早期 MVP）

- TabBar 图标、个人中心资产条与订单宫格（对齐暹罗「我的」）
- 独立搜索页、退款申请页、评价页、客服帮助、个人资料
- 优惠券 / 收藏 / 地址 / 订单搜索 / 换店清车确认 / Banner 跳转
- 定位权限声明（`manifest.json`）；支付仍为 mock（真微信待接）

未照搬：积分商城、会员余额充值、邀请返佣（需额外后端，本期不做）。

## 怎么跑

1. API：`cd mvp/server && mvn spring-boot:run`
2. HBuilderX 打开本目录，运行到浏览器 / 微信开发者工具
3. 真机：改 `api/http.js` 的 `BASE_URL` 为局域网 IP

联调验证码：`123456`（演示号可预填）。

## 主要页面

| 页面 | 作用 |
|------|------|
| pages/index | 附近店、Banner、品类切换 |
| pages/search | 商家搜索 |
| pages/shop/detail | 点餐、评价、联系商家 |
| pages/cart / checkout | 购物车、结算用券 |
| pages/order/* | 列表 / 详情 / 退款 / 评价 |
| pages/mine/* | 个人中心 / 资料 / 客服 |
| pages/coupon · favorite · address | 券 / 收藏 / 地址 |
