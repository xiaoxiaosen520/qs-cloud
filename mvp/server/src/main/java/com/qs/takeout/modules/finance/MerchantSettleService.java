package com.qs.takeout.modules.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.auth.entity.MerchantAccount;
import com.qs.takeout.modules.auth.mapper.MerchantAccountMapper;
import com.qs.takeout.modules.finance.entity.MerchantBillingRecord;
import com.qs.takeout.modules.finance.entity.SysConfig;
import com.qs.takeout.modules.finance.mapper.MerchantBillingRecordMapper;
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
public class MerchantSettleService {

    private final MerchantAccountMapper merchantAccountMapper;
    private final MerchantBillingRecordMapper billingRecordMapper;
    private final SysConfigMapper sysConfigMapper;

    public BigDecimal commissionRate() {
        return configDecimal("commission_rate", new BigDecimal("0.06"));
    }

    public BigDecimal withdrawFee() {
        return configDecimal("merchant_withdraw_fee", new BigDecimal("1.00"));
    }

    private BigDecimal configDecimal(String key, BigDecimal def) {
        SysConfig cfg = sysConfigMapper.selectById(key);
        if (cfg == null || !StringUtils.hasText(cfg.getConfigValue())) {
            return def;
        }
        try {
            return new BigDecimal(cfg.getConfigValue().trim());
        } catch (Exception e) {
            return def;
        }
    }

    @Transactional
    public void settleOrderIncome(OrderEntity order) {
        if (order == null || order.getId() == null) {
            return;
        }
        Long exists = billingRecordMapper.selectCount(new LambdaQueryWrapper<MerchantBillingRecord>()
                .eq(MerchantBillingRecord::getOrderId, order.getId())
                .eq(MerchantBillingRecord::getType, BillingTypes.ORDER_INCOME));
        if (exists != null && exists > 0) {
            return;
        }
        MerchantAccount merchant = requireMerchantByShop(order.getShopId());
        BigDecimal pay = nz(order.getPayAmount());
        // 平台骑手配送：配送费归骑手，商家抽佣基数不含配送费
        BigDecimal deliveryFee = "PLATFORM".equals(order.getDeliveryType())
                ? nz(order.getDeliveryFee()) : BigDecimal.ZERO;
        BigDecimal base = pay.subtract(deliveryFee).max(BigDecimal.ZERO);
        BigDecimal rate = commissionRate();
        BigDecimal fee = base.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal income = base.subtract(fee).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        MerchantBillingRecord bill = new MerchantBillingRecord();
        bill.setMerchantId(merchant.getId());
        bill.setShopId(order.getShopId());
        bill.setOrderId(order.getId());
        bill.setOrderNo(order.getOrderNo() == null ? "" : order.getOrderNo());
        bill.setType(BillingTypes.ORDER_INCOME);
        bill.setOperateType(BillingTypes.ADD);
        bill.setAmount(income);
        bill.setFee(fee);
        bill.setMessage(deliveryFee.compareTo(BigDecimal.ZERO) > 0
                ? "订单完成入账，抽佣 " + fee + "，配送费 " + deliveryFee + " 归骑手"
                : "订单完成入账，抽佣 " + fee);
        billingRecordMapper.insert(bill);

        merchant.setWithdrawableBalance(nz(merchant.getWithdrawableBalance()).add(income));
        merchantAccountMapper.updateById(merchant);
    }

    @Transactional
    public void reverseOrderIncome(OrderEntity order) {
        if (order == null || order.getId() == null) {
            return;
        }
        Long refunded = billingRecordMapper.selectCount(new LambdaQueryWrapper<MerchantBillingRecord>()
                .eq(MerchantBillingRecord::getOrderId, order.getId())
                .eq(MerchantBillingRecord::getType, BillingTypes.REFUND));
        if (refunded != null && refunded > 0) {
            return;
        }
        MerchantBillingRecord income = billingRecordMapper.selectOne(new LambdaQueryWrapper<MerchantBillingRecord>()
                .eq(MerchantBillingRecord::getOrderId, order.getId())
                .eq(MerchantBillingRecord::getType, BillingTypes.ORDER_INCOME)
                .last("limit 1"));
        if (income == null) {
            // 未入账（拒单/完成前退款）无需冲正
            return;
        }
        MerchantAccount merchant = requireMerchantByShop(order.getShopId());
        BigDecimal amount = nz(income.getAmount());
        if (nz(merchant.getWithdrawableBalance()).compareTo(amount) < 0) {
            throw new BizException("商家可提现余额不足，无法冲正退款，请联系平台");
        }

        MerchantBillingRecord bill = new MerchantBillingRecord();
        bill.setMerchantId(merchant.getId());
        bill.setShopId(order.getShopId());
        bill.setOrderId(order.getId());
        bill.setOrderNo(order.getOrderNo() == null ? "" : order.getOrderNo());
        bill.setType(BillingTypes.REFUND);
        bill.setOperateType(BillingTypes.SUB);
        bill.setAmount(amount);
        bill.setFee(BigDecimal.ZERO);
        bill.setMessage("订单退款冲正");
        billingRecordMapper.insert(bill);

        merchant.setWithdrawableBalance(nz(merchant.getWithdrawableBalance()).subtract(amount));
        merchantAccountMapper.updateById(merchant);
    }

    private MerchantAccount requireMerchantByShop(Long shopId) {
        MerchantAccount merchant = merchantAccountMapper.selectOne(new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getShopId, shopId)
                .last("limit 1"));
        if (merchant == null) {
            throw new BizException("店铺未绑定商家账号，无法结算");
        }
        if (merchant.getWithdrawableBalance() == null) {
            merchant.setWithdrawableBalance(BigDecimal.ZERO);
        }
        if (merchant.getFrozenBalance() == null) {
            merchant.setFrozenBalance(BigDecimal.ZERO);
        }
        return merchant;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
