package com.qs.takeout.modules.goods;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.goods.dto.GoodsDetailVO;
import com.qs.takeout.modules.goods.entity.Goods;
import com.qs.takeout.modules.goods.entity.GoodsSku;
import com.qs.takeout.modules.goods.entity.ShopCategory;
import com.qs.takeout.modules.goods.mapper.GoodsMapper;
import com.qs.takeout.modules.goods.mapper.GoodsSkuMapper;
import com.qs.takeout.modules.goods.mapper.ShopCategoryMapper;
import com.qs.takeout.modules.shop.ShopQueryService;
import com.qs.takeout.modules.shop.entity.Shop;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserGoodsService {

    private final ShopQueryService shopQueryService;
    private final ShopCategoryMapper shopCategoryMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsSkuMapper goodsSkuMapper;

    public List<ShopCategory> categories(Long shopId) {
        ensureShop(shopId);
        return shopCategoryMapper.selectList(new LambdaQueryWrapper<ShopCategory>()
                .eq(ShopCategory::getShopId, shopId)
                .eq(ShopCategory::getStatus, 1)
                .orderByAsc(ShopCategory::getSort)
                .orderByAsc(ShopCategory::getId));
    }

    public List<Goods> goodsList(Long shopId, Long categoryId) {
        ensureShop(shopId);
        LambdaQueryWrapper<Goods> q = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getShopId, shopId)
                .eq(Goods::getStatus, 1)
                .orderByAsc(Goods::getSort)
                .orderByDesc(Goods::getId);
        if (categoryId != null) {
            q.eq(Goods::getCategoryId, categoryId);
        }
        return goodsMapper.selectList(q);
    }

    public GoodsDetailVO goodsDetail(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() == 0) {
            throw new BizException("商品不存在");
        }
        ensureShop(goods.getShopId());
        List<GoodsSku> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSku>()
                .eq(GoodsSku::getGoodsId, goodsId)
                .eq(GoodsSku::getStatus, 1)
                .orderByAsc(GoodsSku::getId));
        return GoodsDetailVO.builder().goods(goods).skus(skus).build();
    }

    private Shop ensureShop(Long shopId) {
        return shopQueryService.detail(shopId);
    }
}
