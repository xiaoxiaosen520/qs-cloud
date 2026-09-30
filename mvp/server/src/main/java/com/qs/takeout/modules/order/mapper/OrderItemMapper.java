package com.qs.takeout.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qs.takeout.modules.order.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {
}
