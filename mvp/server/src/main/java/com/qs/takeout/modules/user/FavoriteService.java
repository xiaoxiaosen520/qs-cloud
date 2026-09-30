package com.qs.takeout.modules.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import com.qs.takeout.modules.user.entity.UserFavoriteShop;
import com.qs.takeout.modules.user.mapper.UserFavoriteShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteShopMapper favoriteShopMapper;
    private final ShopMapper shopMapper;

    public List<Shop> list() {
        Long userId = AuthContext.require().getId();
        List<UserFavoriteShop> rows = favoriteShopMapper.selectList(new LambdaQueryWrapper<UserFavoriteShop>()
                .eq(UserFavoriteShop::getUserId, userId)
                .orderByDesc(UserFavoriteShop::getId));
        List<Shop> shops = new ArrayList<>();
        for (UserFavoriteShop row : rows) {
            Shop shop = shopMapper.selectById(row.getShopId());
            if (shop != null && shop.getStatus() != null && shop.getStatus() == 1) {
                shops.add(shop);
            }
        }
        return shops;
    }

    public List<Long> ids() {
        Long userId = AuthContext.require().getId();
        return favoriteShopMapper.selectList(new LambdaQueryWrapper<UserFavoriteShop>()
                        .eq(UserFavoriteShop::getUserId, userId)
                        .select(UserFavoriteShop::getShopId))
                .stream()
                .map(UserFavoriteShop::getShopId)
                .toList();
    }

    public Map<String, Object> toggle(Long shopId) {
        Long userId = AuthContext.require().getId();
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || shop.getStatus() == null || shop.getStatus() == 0) {
            throw new BizException("店铺不存在");
        }
        UserFavoriteShop exist = favoriteShopMapper.selectOne(new LambdaQueryWrapper<UserFavoriteShop>()
                .eq(UserFavoriteShop::getUserId, userId)
                .eq(UserFavoriteShop::getShopId, shopId));
        if (exist != null) {
            favoriteShopMapper.deleteById(exist.getId());
            return Map.of("favorited", false);
        }
        UserFavoriteShop row = new UserFavoriteShop();
        row.setUserId(userId);
        row.setShopId(shopId);
        favoriteShopMapper.insert(row);
        return Map.of("favorited", true);
    }

    public boolean isFavorite(Long shopId) {
        Long userId = AuthContext.require().getId();
        return favoriteShopMapper.selectCount(new LambdaQueryWrapper<UserFavoriteShop>()
                .eq(UserFavoriteShop::getUserId, userId)
                .eq(UserFavoriteShop::getShopId, shopId)) > 0;
    }
}
