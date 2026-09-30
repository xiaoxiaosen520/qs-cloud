package com.qs.takeout.modules.goods;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.goods.dto.CategorySaveRequest;
import com.qs.takeout.modules.goods.dto.GoodsDetailVO;
import com.qs.takeout.modules.goods.dto.GoodsSaveRequest;
import com.qs.takeout.modules.goods.dto.MerchantGoodsItemVO;
import com.qs.takeout.modules.goods.entity.Goods;
import com.qs.takeout.modules.goods.entity.GoodsSku;
import com.qs.takeout.modules.goods.entity.ShopCategory;
import com.qs.takeout.modules.goods.mapper.GoodsMapper;
import com.qs.takeout.modules.goods.mapper.GoodsSkuMapper;
import com.qs.takeout.modules.goods.mapper.ShopCategoryMapper;
import com.qs.takeout.modules.shop.MerchantShopSupport;
import com.qs.takeout.modules.shop.entity.Shop;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantGoodsService {

    private final MerchantShopSupport merchantShopSupport;
    private final ShopCategoryMapper shopCategoryMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsSkuMapper goodsSkuMapper;

    public List<ShopCategory> listCategories() {
        Long shopId = merchantShopSupport.requireShopId();
        return shopCategoryMapper.selectList(new LambdaQueryWrapper<ShopCategory>()
                .eq(ShopCategory::getShopId, shopId)
                .orderByAsc(ShopCategory::getSort)
                .orderByAsc(ShopCategory::getId));
    }

    public ShopCategory createCategory(CategorySaveRequest req) {
        Long shopId = merchantShopSupport.requireShopId();
        ShopCategory c = new ShopCategory();
        c.setShopId(shopId);
        c.setName(req.getName().trim());
        c.setSort(req.getSort() == null ? 0 : req.getSort());
        c.setStatus(1);
        shopCategoryMapper.insert(c);
        return c;
    }

    public ShopCategory updateCategory(Long id, CategorySaveRequest req) {
        ShopCategory c = requireOwnCategory(id);
        c.setName(req.getName().trim());
        if (req.getSort() != null) {
            c.setSort(req.getSort());
        }
        shopCategoryMapper.updateById(c);
        return c;
    }

    public void deleteCategory(Long id) {
        ShopCategory c = requireOwnCategory(id);
        Long count = goodsMapper.selectCount(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getCategoryId, c.getId()));
        if (count != null && count > 0) {
            throw new BizException("分类下还有商品，无法删除");
        }
        shopCategoryMapper.deleteById(id);
    }

    public List<MerchantGoodsItemVO> listGoods(Long categoryId) {
        Long shopId = merchantShopSupport.requireShopId();
        LambdaQueryWrapper<Goods> q = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getShopId, shopId)
                .orderByAsc(Goods::getSort)
                .orderByDesc(Goods::getId);
        if (categoryId != null) {
            q.eq(Goods::getCategoryId, categoryId);
        }
        List<Goods> goodsList = goodsMapper.selectList(q);
        if (goodsList.isEmpty()) {
            return List.of();
        }
        List<Long> goodsIds = goodsList.stream().map(Goods::getId).toList();
        List<GoodsSku> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSku>()
                .in(GoodsSku::getGoodsId, goodsIds));
        Map<Long, Integer> stockMap = new HashMap<>();
        for (GoodsSku sku : skus) {
            stockMap.merge(sku.getGoodsId(), sku.getStock() == null ? 0 : sku.getStock(), Integer::sum);
        }
        return goodsList.stream()
                .map(g -> MerchantGoodsItemVO.builder()
                        .id(g.getId())
                        .categoryId(g.getCategoryId())
                        .name(g.getName())
                        .coverUrl(g.getCoverUrl())
                        .description(g.getDescription())
                        .minPrice(g.getMinPrice())
                        .status(g.getStatus())
                        .stockTotal(stockMap.getOrDefault(g.getId(), 0))
                        .build())
                .collect(Collectors.toList());
    }

    public GoodsDetailVO goodsDetail(Long goodsId) {
        Goods goods = requireOwnGoods(goodsId);
        List<GoodsSku> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSku>()
                .eq(GoodsSku::getGoodsId, goodsId)
                .orderByAsc(GoodsSku::getId));
        return GoodsDetailVO.builder().goods(goods).skus(skus).build();
    }

    @Transactional
    public GoodsDetailVO createGoods(GoodsSaveRequest req) {
        Long shopId = merchantShopSupport.requireShopId();
        requireOwnCategory(req.getCategoryId());
        Shop shop = merchantShopSupport.requireShop();

        Goods goods = new Goods();
        goods.setShopId(shopId);
        goods.setCategoryId(req.getCategoryId());
        goods.setName(req.getName().trim());
        goods.setCoverUrl(StringUtils.hasText(req.getCoverUrl()) ? req.getCoverUrl() : "");
        goods.setDescription(req.getDescription() == null ? "" : req.getDescription());
        goods.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        goods.setSort(req.getSort() == null ? 0 : req.getSort());
        goods.setMinPrice(minSkuPrice(req.getSkus()));
        goodsMapper.insert(goods);

        saveSkus(shop, goods.getId(), shopId, req.getSkus());
        return goodsDetail(goods.getId());
    }

    @Transactional
    public GoodsDetailVO updateGoods(Long goodsId, GoodsSaveRequest req) {
        Goods goods = requireOwnGoods(goodsId);
        requireOwnCategory(req.getCategoryId());
        Shop shop = merchantShopSupport.requireShop();

        goods.setCategoryId(req.getCategoryId());
        goods.setName(req.getName().trim());
        if (req.getCoverUrl() != null) {
            goods.setCoverUrl(req.getCoverUrl());
        }
        if (req.getDescription() != null) {
            goods.setDescription(req.getDescription());
        }
        if (req.getStatus() != null) {
            goods.setStatus(req.getStatus());
        }
        if (req.getSort() != null) {
            goods.setSort(req.getSort());
        }
        goods.setMinPrice(minSkuPrice(req.getSkus()));
        goodsMapper.updateById(goods);

        // 简单策略：先删后插（MVP）
        goodsSkuMapper.delete(new LambdaQueryWrapper<GoodsSku>().eq(GoodsSku::getGoodsId, goodsId));
        saveSkus(shop, goodsId, goods.getShopId(), req.getSkus());
        return goodsDetail(goodsId);
    }

    public void offlineGoods(Long goodsId) {
        Goods goods = requireOwnGoods(goodsId);
        goods.setStatus(0);
        goodsMapper.updateById(goods);
    }

    public void onlineGoods(Long goodsId) {
        Goods goods = requireOwnGoods(goodsId);
        goods.setStatus(1);
        goodsMapper.updateById(goods);
    }

    private void saveSkus(Shop shop, Long goodsId, Long shopId, List<GoodsSaveRequest.SkuItem> items) {
        for (GoodsSaveRequest.SkuItem item : items) {
            if (item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("价格不能为负");
            }
            if (item.getStock() < 0) {
                throw new BizException("库存不能为负");
            }
            GoodsSku sku = new GoodsSku();
            sku.setGoodsId(goodsId);
            sku.setShopId(shopId);
            sku.setName(item.getName().trim());
            sku.setPrice(item.getPrice());
            sku.setStock(item.getStock());
            sku.setBarcode(item.getBarcode() == null ? "" : item.getBarcode());
            sku.setStatus(item.getStatus() == null ? 1 : item.getStatus());
            goodsSkuMapper.insert(sku);
        }
    }

    private BigDecimal minSkuPrice(List<GoodsSaveRequest.SkuItem> skus) {
        return skus.stream()
                .map(GoodsSaveRequest.SkuItem::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    private ShopCategory requireOwnCategory(Long categoryId) {
        Long shopId = merchantShopSupport.requireShopId();
        ShopCategory c = shopCategoryMapper.selectById(categoryId);
        if (c == null || !shopId.equals(c.getShopId())) {
            throw new BizException("分类不存在");
        }
        return c;
    }

    private Goods requireOwnGoods(Long goodsId) {
        Long shopId = merchantShopSupport.requireShopId();
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || !shopId.equals(goods.getShopId())) {
            throw new BizException("商品不存在");
        }
        return goods;
    }
}
