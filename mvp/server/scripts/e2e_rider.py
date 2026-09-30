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
        payload = json.load(resp)
    if payload.get("code") != 0:
        raise RuntimeError(path + " => " + json.dumps(payload, ensure_ascii=False))
    return payload["data"]

mt = call("POST", "/api/auth/sms/login", {"phone": "13900001111", "code": "123456", "role": "MERCHANT"})["token"]
shop_id = call("GET", "/api/merchant/shop", token=mt)["id"]
call("PUT", "/api/merchant/shop", {"openStatus": 1}, mt)
goods = call("GET", f"/api/shops/{shop_id}/goods")
sku_id = call("GET", f"/api/shops/goods/{goods[0]['id']}")["skus"][0]["id"]

ut = call("POST", "/api/auth/sms/login", {"phone": "13700004444", "code": "123456", "role": "USER"})["token"]
addr = call("POST", "/api/user/addresses", {
    "contactName": "测", "contactPhone": "13700004444", "detail": "路1号",
    "lat": 31.231, "lng": 121.474, "isDefault": True
}, ut)
call("POST", "/api/cart/items", {"skuId": sku_id, "quantity": 1}, ut)
order = call("POST", "/api/orders", {"shopId": shop_id, "addressId": addr["id"], "mockPay": True}, ut)["order"]
assert order["deliveryType"] == "PLATFORM"
oid = order["id"]
call("POST", f"/api/merchant/orders/{oid}/accept", {}, mt)

rt = call("POST", "/api/auth/sms/login", {"phone": "15500001111", "code": "123456", "role": "RIDER"})["token"]
call("POST", "/api/rider/online", {"online": True}, rt)
pool = call("GET", "/api/rider/orders/pool", token=rt)
print("pool", len(pool))
grabbed = call("POST", f"/api/rider/orders/{oid}/grab", {}, rt)
print("grab", grabbed["order"]["status"])
call("POST", f"/api/rider/orders/{oid}/pickup", {}, rt)
done = call("POST", f"/api/rider/orders/{oid}/deliver", {}, rt)
print("deliver", done["order"]["status"])
