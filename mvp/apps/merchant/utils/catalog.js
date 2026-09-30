/** 平台常见商品库（演示用，后续可接条码查询 API） */
export const PRODUCT_CATALOG = [
  { name: '可口可乐', spec: '500ml', price: '3.50', barcode: '6901234567001' },
  { name: '百事可乐', spec: '500ml', price: '3.50', barcode: '6901234567003' },
  { name: '雪碧', spec: '500ml', price: '3.50', barcode: '6901234567004' },
  { name: '农夫山泉', spec: '550ml', price: '2.00', barcode: '6901234567005' },
  { name: '怡宝纯净水', spec: '555ml', price: '2.00', barcode: '6901234567006' },
  { name: '红牛维生素功能饮料', spec: '250ml', price: '6.00', barcode: '6901234567007' },
  { name: '脉动维生素饮料', spec: '600ml', price: '4.50', barcode: '6901234567008' },
  { name: '元气森林气泡水', spec: '480ml', price: '5.00', barcode: '6901234567009' },
  { name: '维他柠檬茶', spec: '250ml', price: '3.00', barcode: '6901234567010' },
  { name: '统一冰红茶', spec: '500ml', price: '3.50', barcode: '6901234567011' },
  { name: '旺仔牛奶', spec: '245ml', price: '4.00', barcode: '6901234567012' },
  { name: '椰树牌椰汁', spec: '245ml', price: '4.50', barcode: '6901234567013' },
  { name: '康师傅矿泉水', spec: '550ml', price: '1.50', barcode: '6901234567014' },
  { name: '东方树叶茉莉花茶', spec: '900ml', price: '5.50', barcode: '6901234567015' },
  { name: '美年达橙味汽水', spec: '500ml', price: '3.50', barcode: '6901234567016' }
]

export function findByBarcode(code) {
  const c = String(code || '').trim()
  if (!c) return null
  return PRODUCT_CATALOG.find((x) => x.barcode === c) || null
}

export function searchCatalog(keyword) {
  const k = String(keyword || '').trim()
  if (!k) return [...PRODUCT_CATALOG]
  return PRODUCT_CATALOG.filter(
    (x) => x.name.includes(k) || (x.barcode && x.barcode.includes(k))
  )
}
