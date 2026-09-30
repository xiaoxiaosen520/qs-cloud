package com.qs.takeout.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qs.takeout.modules.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    @Update("UPDATE orders SET status = 'CANCELLED', cancel_reason = #{reason}, pay_deadline_at = NULL, "
            + "updated_at = NOW() WHERE id = #{id} AND status = 'PENDING_PAY'")
    int casCancelPayTimeout(@Param("id") Long id, @Param("reason") String reason);

    @Update("UPDATE orders SET status = 'REFUNDED', cancel_reason = #{reason}, accept_deadline_at = NULL, "
            + "pay_deadline_at = NULL, updated_at = NOW() WHERE id = #{id} AND status = 'PAID'")
    int casAcceptTimeoutRefund(@Param("id") Long id, @Param("reason") String reason);

    @Update("UPDATE orders SET status = 'COMPLETED', completed_at = NOW(), "
            + "delivered_at = IFNULL(delivered_at, NOW()), auto_complete_at = NULL, updated_at = NOW() "
            + "WHERE id = #{id} AND auto_complete_at IS NOT NULL "
            + "AND status IN ('ACCEPTED', 'DELIVERING')")
    int casAutoComplete(@Param("id") Long id);

    @Update("UPDATE orders SET status = 'PAID', paid_at = NOW(), pay_deadline_at = NULL, "
            + "accept_deadline_at = #{acceptDeadline}, updated_at = NOW() "
            + "WHERE id = #{id} AND status = 'PENDING_PAY'")
    int casMarkPaid(@Param("id") Long id, @Param("acceptDeadline") java.time.LocalDateTime acceptDeadline);
}
