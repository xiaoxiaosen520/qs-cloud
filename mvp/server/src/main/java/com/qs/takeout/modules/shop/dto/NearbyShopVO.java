package com.qs.takeout.modules.shop.dto;

import com.qs.takeout.modules.shop.entity.Shop;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NearbyShopVO {

    private Shop shop;
    /** 距离（米） */
    private Integer distanceMeters;
}
