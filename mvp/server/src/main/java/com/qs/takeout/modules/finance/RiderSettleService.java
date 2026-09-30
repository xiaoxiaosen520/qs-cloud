package com.qs.takeout.modules.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.auth.entity.RiderAccount;
import com.qs.takeout.modules.auth.mapper.RiderAccountMapper;
import com.qs.takeout.modules.finance.entity.RiderBillingRecord;
import com.qs.takeout.modules.finance.entity.SysConfig;
import com.qs.takeout.modules.finance.mapper.RiderBillingRecordMapper;
import com.qs.takeout.modules.finance.mapper.SysConfigMapper;
import com.qs.takeout.modules.order.entity.OrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RiderSettleService {

    private final RiderAccountMapper riderAccountMapper;
    private final RiderBillingRecordMapper billingRecordMapper;
    private final SysConfigMapper sysConfigMapper;

    public BigDecimal withdrawFee() {
        SysConfig cfg = sysConfigMapper.selectById("rider_withdraw_fee");
        if (cfg == null || !StringUtils.hasText(cfg.getConfigValue())) {
            return new BigDecimal("1.00");
        }
        try {
            return new BigDecimal(cfg.getConfigValue().trim());
        } catch (Exception e) {
            return new BigDecimal("1.00");
        }
    }

    @Transactional
    public void settleDeliveryIncome(OrderEntity order) {
        if (order == null || order.getId() == null || order.getRiderId() == null) {
            return;
        }
        if (!"PLATFORM".equals(order.getDeliveryType())) {
            return;
        }
        Long exists = billingRecordMapper.selectCount(new LambdaQueryWrapper<RiderBillingRecord>()
                .eq(RiderBillingRecord::getOrderId, order.getId())
                .eq(RiderBillingRecord::getType, BillingTypes.DELIVERY_INCOME));
        if (exists != null && exists > 0) {
            return;
        }
        BigDecimal income = nz(order.getDeliveryFee()).setScale(2, RoundingMode.HALF_UP);
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        RiderAccount rider = riderAccountMapper.selectById(order.getRiderId());
        if (rider == null) {
            throw new BizException("骑手不存在，无法结算配送费");
        }
        RiderBillingRecord bill = new RiderBillingRecord();
        bill.setRiderId(rider.getId());
        bill.setOrderId(order.getId());
        bill.setOrderNo(order.getOrderNo() == null ? "" : order.getOrderNo());
        bill.setType(BillingTypes.DELIVERY_INCOME);
        bill.setOperateType(BillingTypes.ADD);
        bill.setAmount(income);
        bill.setFee(BigDecimal.ZERO);
        bill.setMessage("配送完成入账");
        billingRecordMapper.insert(bill);

        rider.setWithdrawableBalance(nz(rider.getWithdrawableBalance()).add(income));
        if (rider.getFrozenBalance() == null) {
            rider.setFrozenBalance(BigDecimal.ZERO);
        }
        riderAccountMapper.updateById(rider);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
