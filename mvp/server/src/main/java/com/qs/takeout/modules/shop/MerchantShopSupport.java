package com.qs.takeout.modules.shop;

import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.auth.entity.MerchantAccount;
import com.qs.takeout.modules.auth.mapper.MerchantAccountMapper;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantShopSupport {

    private final MerchantAccountMapper merchantAccountMapper;
    private final ShopMapper shopMapper;

    public MerchantAccount requireMerchant() {
        AuthUser user = AuthContext.require();
        MerchantAccount merchant = merchantAccountMapper.selectById(user.getId());
        if (merchant == null || merchant.getStatus() != null && merchant.getStatus() == 0) {
            throw new BizException("商家账号无效");
        }
        return merchant;
    }

    public Long requireShopId() {
        MerchantAccount merchant = requireMerchant();
        if (merchant.getShopId() == null) {
            throw new BizException("尚未开通店铺，请先入驻并通过审核");
        }
        return merchant.getShopId();
    }

    public Shop requireShop() {
        Shop shop = shopMapper.selectById(requireShopId());
        if (shop == null || shop.getStatus() != null && shop.getStatus() == 0) {
            throw new BizException("店铺不可用");
        }
        return shop;
    }
}
