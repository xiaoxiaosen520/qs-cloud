package com.qs.takeout.modules.goods.dto;

import com.qs.takeout.modules.goods.entity.Goods;
import com.qs.takeout.modules.goods.entity.GoodsSku;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GoodsDetailVO {

    private Goods goods;
    private List<GoodsSku> skus;
}
