-- 为一线店铺上架一组饮料商品（可重复执行）
-- 用法：
--   docker exec -i mysql-old-data mysql -uroot -proot --default-character-set=utf8mb4 qs_takeout < mvp/sql/seed-drinks.sql
-- 或改 @shop_id 指定店铺

USE `qs_takeout`;

SET NAMES utf8mb4;

-- 默认取 id 最小的店铺；需要指定时改为：SET @shop_id := 1;
SET @shop_id := (SELECT `id` FROM `shop` ORDER BY `id` ASC LIMIT 1);

SELECT IF(@shop_id IS NULL, 'ERROR: 没有店铺，请先完成商家入驻审核', CONCAT('target shop_id=', @shop_id)) AS tip;

-- 确保「饮料」分类存在
SET @cat_id := (
  SELECT `id` FROM `shop_category`
  WHERE `shop_id` = @shop_id AND `name` = '饮料'
  ORDER BY `id` ASC LIMIT 1
);

INSERT INTO `shop_category` (`shop_id`, `name`, `sort`, `status`)
SELECT @shop_id, '饮料', 10, 1
WHERE @shop_id IS NOT NULL AND @cat_id IS NULL;

SET @cat_id := (
  SELECT `id` FROM `shop_category`
  WHERE `shop_id` = @shop_id AND `name` = '饮料'
  ORDER BY `id` ASC LIMIT 1
);

-- 清掉该分类下旧商品（便于重复导入）
DELETE FROM `goods_sku`
WHERE `goods_id` IN (SELECT `id` FROM `goods` WHERE `shop_id` = @shop_id AND `category_id` = @cat_id);

DELETE FROM `goods`
WHERE `shop_id` = @shop_id AND `category_id` = @cat_id;

-- 上架饮料（status=1）
INSERT INTO `goods`
(`shop_id`, `category_id`, `name`, `cover_url`, `description`, `min_price`, `status`, `sort`)
VALUES
(@shop_id, @cat_id, '可口可乐', '', '经典碳酸饮料', 3.50, 1, 100),
(@shop_id, @cat_id, '百事可乐', '', '经典碳酸饮料', 3.50, 1, 90),
(@shop_id, @cat_id, '雪碧', '', '清爽柠檬味汽水', 3.50, 1, 80),
(@shop_id, @cat_id, '农夫山泉', '', '天然矿泉水', 2.00, 1, 70),
(@shop_id, @cat_id, '怡宝纯净水', '', '纯净水', 2.00, 1, 60),
(@shop_id, @cat_id, '红牛维生素功能饮料', '', '提神醒脑', 6.00, 1, 50),
(@shop_id, @cat_id, '脉动维生素饮料', '', '青柠口味', 4.50, 1, 40),
(@shop_id, @cat_id, '元气森林气泡水', '', '白桃味，0糖', 5.00, 1, 30),
(@shop_id, @cat_id, '维他柠檬茶', '', '港式柠檬茶', 3.00, 1, 20),
(@shop_id, @cat_id, '统一冰红茶', '', '冰爽红茶', 3.50, 1, 10),
(@shop_id, @cat_id, '旺仔牛奶', '', '复原乳', 4.00, 1, 5),
(@shop_id, @cat_id, '椰树牌椰汁', '', '正宗椰子汁', 4.50, 1, 1);

-- SKU：按商品名匹配，避免依赖固定 goods.id
INSERT INTO `goods_sku` (`goods_id`, `shop_id`, `name`, `price`, `stock`, `barcode`, `status`)
SELECT g.`id`, @shop_id, s.`sku_name`, s.`price`, s.`stock`, s.`barcode`, 1
FROM `goods` g
JOIN (
  SELECT '可口可乐' AS gname, '500ml' AS sku_name, 3.50 AS price, 200 AS stock, '6901234567001' AS barcode
  UNION ALL SELECT '可口可乐', '330ml', 2.50, 150, '6901234567002'
  UNION ALL SELECT '百事可乐', '500ml', 3.50, 200, '6901234567003'
  UNION ALL SELECT '雪碧', '500ml', 3.50, 200, '6901234567004'
  UNION ALL SELECT '农夫山泉', '550ml', 2.00, 300, '6901234567005'
  UNION ALL SELECT '怡宝纯净水', '555ml', 2.00, 300, '6901234567006'
  UNION ALL SELECT '红牛维生素功能饮料', '250ml', 6.00, 120, '6901234567007'
  UNION ALL SELECT '脉动维生素饮料', '600ml', 4.50, 150, '6901234567008'
  UNION ALL SELECT '元气森林气泡水', '480ml', 5.00, 160, '6901234567009'
  UNION ALL SELECT '维他柠檬茶', '250ml', 3.00, 180, '6901234567010'
  UNION ALL SELECT '统一冰红茶', '500ml', 3.50, 180, '6901234567011'
  UNION ALL SELECT '旺仔牛奶', '245ml', 4.00, 140, '6901234567012'
  UNION ALL SELECT '椰树牌椰汁', '245ml', 4.50, 140, '6901234567013'
) s ON s.gname = g.`name`
WHERE g.`shop_id` = @shop_id AND g.`category_id` = @cat_id;

-- 保证店铺对外营业，用户端能看到
UPDATE `shop` SET `open_status` = 1, `status` = 1 WHERE `id` = @shop_id;

SELECT g.`id`, g.`name`, g.`min_price`, g.`status`, COUNT(s.`id`) AS sku_count
FROM `goods` g
LEFT JOIN `goods_sku` s ON s.`goods_id` = g.`id`
WHERE g.`shop_id` = @shop_id AND g.`category_id` = @cat_id
GROUP BY g.`id`, g.`name`, g.`min_price`, g.`status`
ORDER BY g.`sort` DESC;
