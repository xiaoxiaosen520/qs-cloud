package com.qs.takeout.modules.cart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.cart.dto.CartAddRequest;
import com.qs.takeout.modules.cart.dto.CartUpdateRequest;
import com.qs.takeout.modules.cart.dto.CartViewVO;
import com.qs.takeout.modules.cart.entity.CartItem;
import com.qs.takeout.modules.cart.mapper.CartItemMapper;
import com.qs.takeout.modules.goods.entity.Goods;
import com.qs.takeout.modules.goods.entity.GoodsSku;
import com.qs.takeout.modules.goods.mapper.GoodsMapper;
import com.qs.takeout.modules.goods.mapper.GoodsSkuMapper;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final GoodsSkuMapper goodsSkuMapper;
    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;

    public CartViewVO view() {
        Long userId = AuthContext.require().getId();
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByAsc(CartItem::getId));
        if (items.isEmpty()) {
            return CartViewVO.builder()
                    .items(List.of())
                    .goodsAmount(BigDecimal.ZERO)
                    .minOrderAmount(BigDecimal.ZERO)
                    .deliveryFee(BigDecimal.ZERO)
                    .packingFee(BigDecimal.ZERO)
                    .payAmount(BigDecimal.ZERO)
                    .meetMinOrder(false)
                    .build();
        }
        Long shopId = items.get(0).getShopId();
        Shop shop = shopMapper.selectById(shopId);
        List<CartViewVO.Line> lines = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO;
        for (CartItem item : items) {
            GoodsSku sku = goodsSkuMapper.selectById(item.getSkuId());
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (sku == null || goods == null) {
                continue;
            }
            BigDecimal line = sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            amount = amount.add(line);
            lines.add(CartViewVO.Line.builder()
                    .id(item.getId())
                    .goodsId(goods.getId())
                    .skuId(sku.getId())
                    .goodsName(goods.getName())
                    .skuName(sku.getName())
                    .coverUrl(goods.getCoverUrl() == null ? "" : goods.getCoverUrl())
                    .price(sku.getPrice())
                    .stock(sku.getStock())
                    .quantity(item.getQuantity())
                    .lineAmount(line)
                    .build());
        }
        BigDecimal minOrder = shop == null || shop.getMinOrderAmount() == null
                ? BigDecimal.ZERO : shop.getMinOrderAmount();
        BigDecimal delivery = shop == null || shop.getDeliveryFee() == null
                ? BigDecimal.ZERO : shop.getDeliveryFee();
        BigDecimal packing = shop == null || shop.getPackingFee() == null
                ? BigDecimal.ZERO : shop.getPackingFee();
        BigDecimal pay = amount.add(delivery).add(packing);
        return CartViewVO.builder()
                .shopId(shopId)
                .shopName(shop == null ? "" : shop.getName())
                .shopLogoUrl(shop == null || shop.getLogoUrl() == null ? "" : shop.getLogoUrl())
                .items(lines)
                .goodsAmount(amount)
                .minOrderAmount(minOrder)
                .deliveryFee(delivery)
                .packingFee(packing)
                .payAmount(pay)
                .meetMinOrder(amount.compareTo(minOrder) >= 0)
                .build();
    }

    @Transactional
    public CartViewVO add(CartAddRequest req) {
        Long userId = AuthContext.require().getId();
        GoodsSku sku = goodsSkuMapper.selectById(req.getSkuId());
        if (sku == null || sku.getStatus() == null || sku.getStatus() == 0) {
            throw new BizException("规格不可用");
        }
        Goods goods = goodsMapper.selectById(sku.getGoodsId());
        if (goods == null || goods.getStatus() == null || goods.getStatus() == 0) {
            throw new BizException("商品已下架");
        }
        Shop shop = shopMapper.selectById(sku.getShopId());
        if (shop == null || shop.getStatus() == null || shop.getStatus() == 0) {
            throw new BizException("店铺不可用");
        }
        if (req.getQuantity() > sku.getStock()) {
            throw new BizException("库存不足");
        }

        // 换店清空
        List<CartItem> existing = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        boolean otherShop = existing.stream().anyMatch(i -> !i.getShopId().equals(sku.getShopId()));
        if (otherShop) {
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        }

        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, sku.getId()));
        if (item == null) {
            item = new CartItem();
            item.setUserId(userId);
            item.setShopId(sku.getShopId());
            item.setGoodsId(sku.getGoodsId());
            item.setSkuId(sku.getId());
            item.setQuantity(req.getQuantity());
            cartItemMapper.insert(item);
        } else {
            int q = item.getQuantity() + req.getQuantity();
            if (q > sku.getStock()) {
                throw new BizException("库存不足");
            }
            item.setQuantity(q);
            cartItemMapper.updateById(item);
        }
        return view();
    }

    public CartViewVO update(Long id, CartUpdateRequest req) {
        CartItem item = requireOwn(id);
        GoodsSku sku = goodsSkuMapper.selectById(item.getSkuId());
        if (sku == null || req.getQuantity() > sku.getStock()) {
            throw new BizException("库存不足");
        }
        item.setQuantity(req.getQuantity());
        cartItemMapper.updateById(item);
        return view();
    }

    public CartViewVO remove(Long id) {
        CartItem item = requireOwn(id);
        cartItemMapper.deleteById(item.getId());
        return view();
    }

    public void clear() {
        Long userId = AuthContext.require().getId();
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }

    public List<CartItem> listByShop(Long userId, Long shopId) {
        return cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getShopId, shopId));
    }

    public void clearShop(Long userId, Long shopId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getShopId, shopId));
    }

    private CartItem requireOwn(Long id) {
        Long userId = AuthContext.require().getId();
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !userId.equals(item.getUserId())) {
            throw new BizException("购物车项不存在");
        }
        return item;
    }
}
