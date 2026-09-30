package com.qs.takeout.modules.goods.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsSkuStockMapper {

    @Update("UPDATE goods_sku SET stock = stock - #{qty} WHERE id = #{skuId} AND stock >= #{qty} AND status = 1")
    int deduct(@Param("skuId") Long skuId, @Param("qty") int qty);

    @Update("UPDATE goods_sku SET stock = stock + #{qty} WHERE id = #{skuId}")
    int restore(@Param("skuId") Long skuId, @Param("qty") int qty);
}
