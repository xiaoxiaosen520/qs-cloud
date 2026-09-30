package com.qs.takeout.modules.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderGrabMapper {

    @Update("UPDATE orders SET rider_id = #{riderId}, status = 'DELIVERING', updated_at = NOW() "
            + "WHERE id = #{orderId} AND rider_id IS NULL AND status = 'ACCEPTED' AND delivery_type = 'PLATFORM'")
    int grab(@Param("orderId") Long orderId, @Param("riderId") Long riderId);
}
