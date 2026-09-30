#!/usr/bin/env python3
import json, urllib.request

BASE = "http://127.0.0.1:8080"

def call(method, path, body=None, token=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req) as resp:
        return json.load(resp)

m = call("POST", "/api/auth/sms/login", {"phone": "13900001111", "code": "123456", "role": "MERCHANT"})
mt = m["data"]["token"]
shop = call("GET", "/api/merchant/shop", token=mt)["data"]
shop_id = shop["id"]
call("PUT", "/api/merchant/shop", {"openStatus": 1}, mt)
goods = call("GET", f"/api/shops/{shop_id}/goods")["data"]
sku_id = call("GET", f"/api/shops/goods/{goods[0]['id']}")["data"]["skus"][0]["id"]
u = call("POST", "/api/auth/sms/login", {"phone": "13700003333", "code": "123456", "role": "USER"})
ut = u["data"]["token"]
addr = call("POST", "/api/user/addresses", {
    "contactName": "测", "contactPhone": "13700003333", "detail": "地址1",
    "lat": 31.231, "lng": 121.474, "isDefault": True
}, ut)["data"]
call("POST", "/api/cart/items", {"skuId": sku_id, "quantity": 2}, ut)
order = call("POST", "/api/orders", {"shopId": shop_id, "addressId": addr["id"], "mockPay": True}, ut)
oid = order["data"]["order"]["id"]
print("order", order["code"], order["data"]["order"]["status"], order["data"]["order"]["payAmount"])
acc = call("POST", f"/api/merchant/orders/{oid}/accept", {}, mt)
print("accept", acc["code"], acc["data"]["order"]["status"])
cmp_ = call("POST", f"/api/merchant/orders/{oid}/complete", {}, mt)
print("complete", cmp_["code"], cmp_["data"]["order"]["status"])
