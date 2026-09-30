package com.qs.takeout.modules.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.auth.entity.MerchantAccount;
import com.qs.takeout.modules.auth.mapper.MerchantAccountMapper;
import com.qs.takeout.modules.finance.dto.MerchantStatsVO;
import com.qs.takeout.modules.finance.dto.MerchantWalletVO;
import com.qs.takeout.modules.finance.dto.SettlementUpdateRequest;
import com.qs.takeout.modules.finance.dto.WithdrawApplyRequest;
import com.qs.takeout.modules.finance.dto.WithdrawReviewRequest;
import com.qs.takeout.modules.finance.entity.MerchantBillingRecord;
import com.qs.takeout.modules.finance.entity.MerchantWithdrawRecord;
import com.qs.takeout.modules.finance.mapper.MerchantBillingRecordMapper;
import com.qs.takeout.modules.finance.mapper.MerchantWithdrawRecordMapper;
import com.qs.takeout.modules.goods.entity.Goods;
import com.qs.takeout.modules.goods.mapper.GoodsMapper;
import com.qs.takeout.modules.order.OrderStatus;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.mapper.OrderMapper;
import com.qs.takeout.modules.shop.MerchantShopSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MerchantFinanceService {

    private final MerchantShopSupport merchantShopSupport;
    private final MerchantAccountMapper merchantAccountMapper;
    private final MerchantBillingRecordMapper billingRecordMapper;
    private final MerchantWithdrawRecordMapper withdrawRecordMapper;
    private final MerchantSettleService settleService;
    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;

    public MerchantWalletVO wallet() {
        MerchantAccount m = reload(merchantShopSupport.requireMerchant());
        BigDecimal w = nz(m.getWithdrawableBalance());
        BigDecimal f = nz(m.getFrozenBalance());
        return MerchantWalletVO.builder()
                .withdrawableBalance(w)
                .frozenBalance(f)
                .totalBalance(w.add(f))
                .realName(nullToEmpty(m.getRealName()))
                .bankCard(nullToEmpty(m.getBankCard()))
                .alipayAccount(nullToEmpty(m.getAlipayAccount()))
                .wechatAccount(nullToEmpty(m.getWechatAccount()))
                .withdrawFee(settleService.withdrawFee())
                .commissionRate(settleService.commissionRate())
                .build();
    }

    @Transactional
    public MerchantWalletVO updateSettlement(SettlementUpdateRequest req) {
        MerchantAccount m = reload(merchantShopSupport.requireMerchant());
        if (req.getRealName() != null) {
            m.setRealName(req.getRealName().trim());
        }
        if (req.getBankCard() != null) {
            m.setBankCard(req.getBankCard().trim());
        }
        if (req.getAlipayAccount() != null) {
            m.setAlipayAccount(req.getAlipayAccount().trim());
        }
        if (req.getWechatAccount() != null) {
            m.setWechatAccount(req.getWechatAccount().trim());
        }
        merchantAccountMapper.updateById(m);
        return wallet();
    }

    public List<MerchantBillingRecord> billings() {
        MerchantAccount m = merchantShopSupport.requireMerchant();
        return billingRecordMapper.selectList(new LambdaQueryWrapper<MerchantBillingRecord>()
                .eq(MerchantBillingRecord::getMerchantId, m.getId())
                .orderByDesc(MerchantBillingRecord::getId)
                .last("limit 100"));
    }

    public List<MerchantWithdrawRecord> withdraws() {
        MerchantAccount m = merchantShopSupport.requireMerchant();
        return withdrawRecordMapper.selectList(new LambdaQueryWrapper<MerchantWithdrawRecord>()
                .eq(MerchantWithdrawRecord::getMerchantId, m.getId())
                .orderByDesc(MerchantWithdrawRecord::getId)
                .last("limit 50"));
    }

    @Transactional
    public MerchantWithdrawRecord applyWithdraw(WithdrawApplyRequest req) {
        MerchantAccount m = reload(merchantShopSupport.requireMerchant());
        if (m.getShopId() == null) {
            throw new BizException("尚未开通店铺");
        }
        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = settleService.withdrawFee();
        if (amount.compareTo(fee) <= 0) {
            throw new BizException("提现金额需大于手续费 " + fee + " 元");
        }
        if (nz(m.getWithdrawableBalance()).compareTo(amount) < 0) {
            throw new BizException("可提现余额不足");
        }
        String mode = req.getPaymentMode().trim().toUpperCase();
        String snapshot = accountSnapshot(m, mode);

        m.setWithdrawableBalance(nz(m.getWithdrawableBalance()).subtract(amount));
        m.setFrozenBalance(nz(m.getFrozenBalance()).add(amount));
        merchantAccountMapper.updateById(m);

        MerchantWithdrawRecord rec = new MerchantWithdrawRecord();
        rec.setMerchantId(m.getId());
        rec.setShopId(m.getShopId());
        rec.setWithdrawNo(nextWithdrawNo());
        rec.setAmount(amount);
        rec.setFee(fee);
        rec.setActualAmount(amount.subtract(fee).max(BigDecimal.ZERO));
        rec.setPaymentMode(mode);
        rec.setAccountSnapshot(snapshot);
        rec.setAuditStatus("PENDING");
        rec.setAuditReason("");
        withdrawRecordMapper.insert(rec);

        MerchantBillingRecord bill = new MerchantBillingRecord();
        bill.setMerchantId(m.getId());
        bill.setShopId(m.getShopId());
        bill.setOrderId(null);
        bill.setOrderNo(rec.getWithdrawNo());
        bill.setType(BillingTypes.WITHDRAW);
        bill.setOperateType(BillingTypes.SUB);
        bill.setAmount(amount);
        bill.setFee(fee);
        bill.setMessage("申请提现冻结");
        billingRecordMapper.insert(bill);
        return rec;
    }

    public List<MerchantWithdrawRecord> adminListWithdraws(String status) {
        LambdaQueryWrapper<MerchantWithdrawRecord> q = new LambdaQueryWrapper<MerchantWithdrawRecord>()
                .orderByDesc(MerchantWithdrawRecord::getId)
                .last("limit 100");
        if (StringUtils.hasText(status)) {
            q.eq(MerchantWithdrawRecord::getAuditStatus, status);
        }
        return withdrawRecordMapper.selectList(q);
    }

    @Transactional
    public MerchantWithdrawRecord reviewWithdraw(Long id, WithdrawReviewRequest req) {
        MerchantWithdrawRecord rec = withdrawRecordMapper.selectById(id);
        if (rec == null) {
            throw new BizException("提现单不存在");
        }
        if (!"PENDING".equals(rec.getAuditStatus())) {
            throw new BizException("该提现单已审核");
        }
        MerchantAccount m = merchantAccountMapper.selectById(rec.getMerchantId());
        if (m == null) {
            throw new BizException("商家不存在");
        }
        m = reload(m);
        BigDecimal amount = nz(rec.getAmount());
        if (nz(m.getFrozenBalance()).compareTo(amount) < 0) {
            throw new BizException("冻结余额异常");
        }

        if (req.isApproved()) {
            m.setFrozenBalance(nz(m.getFrozenBalance()).subtract(amount));
            merchantAccountMapper.updateById(m);
            rec.setAuditStatus("APPROVED");
            rec.setAuditReason(StringUtils.hasText(req.getReason()) ? req.getReason() : "审核通过，请线下打款");
        } else {
            m.setFrozenBalance(nz(m.getFrozenBalance()).subtract(amount));
            m.setWithdrawableBalance(nz(m.getWithdrawableBalance()).add(amount));
            merchantAccountMapper.updateById(m);

            MerchantBillingRecord bill = new MerchantBillingRecord();
            bill.setMerchantId(m.getId());
            bill.setShopId(rec.getShopId());
            bill.setOrderId(null);
            bill.setOrderNo(rec.getWithdrawNo());
            bill.setType(BillingTypes.WITHDRAW_BACK);
            bill.setOperateType(BillingTypes.ADD);
            bill.setAmount(amount);
            bill.setFee(BigDecimal.ZERO);
            bill.setMessage("提现驳回退回");
            billingRecordMapper.insert(bill);

            rec.setAuditStatus("REJECTED");
            rec.setAuditReason(StringUtils.hasText(req.getReason()) ? req.getReason() : "审核拒绝");
        }
        rec.setAuditedAt(LocalDateTime.now());
        withdrawRecordMapper.updateById(rec);
        return rec;
    }

    public MerchantStatsVO todayStats() {
        MerchantAccount m = reload(merchantShopSupport.requireMerchant());
        Long shopId = merchantShopSupport.requireShopId();
        LocalDateTime start = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        int pending = countOrders(shopId, OrderStatus.PAID, null, null);
        int accepted = countOrders(shopId, OrderStatus.ACCEPTED, null, null);
        int refunding = countOrders(shopId, OrderStatus.REFUNDING, null, null);

        List<OrderEntity> todayCompleted = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getShopId, shopId)
                .eq(OrderEntity::getStatus, OrderStatus.COMPLETED)
                .ge(OrderEntity::getCompletedAt, start)
                .le(OrderEntity::getCompletedAt, end));
        BigDecimal todayPay = todayCompleted.stream()
                .map(o -> nz(o.getPayAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MerchantBillingRecord> todayIncomeBills = billingRecordMapper.selectList(new LambdaQueryWrapper<MerchantBillingRecord>()
                .eq(MerchantBillingRecord::getMerchantId, m.getId())
                .eq(MerchantBillingRecord::getType, BillingTypes.ORDER_INCOME)
                .ge(MerchantBillingRecord::getCreatedAt, start)
                .le(MerchantBillingRecord::getCreatedAt, end));
        BigDecimal todayIncome = todayIncomeBills.stream()
                .map(b -> nz(b.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long onSale = goodsMapper.selectCount(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getShopId, shopId)
                .eq(Goods::getStatus, 1));

        return MerchantStatsVO.builder()
                .pendingCount(pending)
                .acceptedCount(accepted)
                .refundingCount(refunding)
                .todayOrders(todayCompleted.size())
                .todayPayAmount(todayPay.setScale(2, RoundingMode.HALF_UP))
                .todayIncome(todayIncome.setScale(2, RoundingMode.HALF_UP))
                .onSaleGoods(onSale == null ? 0 : onSale.intValue())
                .withdrawableBalance(nz(m.getWithdrawableBalance()))
                .build();
    }

    private int countOrders(Long shopId, String status, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<OrderEntity> q = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getShopId, shopId)
                .eq(OrderEntity::getStatus, status);
        if (start != null) {
            q.ge(OrderEntity::getCreatedAt, start);
        }
        if (end != null) {
            q.le(OrderEntity::getCreatedAt, end);
        }
        Long c = orderMapper.selectCount(q);
        return c == null ? 0 : c.intValue();
    }

    private String accountSnapshot(MerchantAccount m, String mode) {
        return switch (mode) {
            case "BANK" -> {
                if (!StringUtils.hasText(m.getBankCard())) {
                    throw new BizException("请先完善银行卡收款信息");
                }
                yield "BANK|" + nullToEmpty(m.getRealName()) + "|" + m.getBankCard();
            }
            case "ALIPAY" -> {
                if (!StringUtils.hasText(m.getAlipayAccount())) {
                    throw new BizException("请先完善支付宝收款账号");
                }
                yield "ALIPAY|" + nullToEmpty(m.getRealName()) + "|" + m.getAlipayAccount();
            }
            case "WECHAT" -> {
                if (!StringUtils.hasText(m.getWechatAccount())) {
                    throw new BizException("请先完善微信收款账号");
                }
                yield "WECHAT|" + nullToEmpty(m.getRealName()) + "|" + m.getWechatAccount();
            }
            default -> throw new BizException("不支持的收款方式");
        };
    }

    private String nextWithdrawNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int r = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "W" + ts + r;
    }

    private MerchantAccount reload(MerchantAccount m) {
        MerchantAccount latest = merchantAccountMapper.selectById(m.getId());
        if (latest == null) {
            throw new BizException("商家账号无效");
        }
        if (latest.getWithdrawableBalance() == null) {
            latest.setWithdrawableBalance(BigDecimal.ZERO);
        }
        if (latest.getFrozenBalance() == null) {
            latest.setFrozenBalance(BigDecimal.ZERO);
        }
        return latest;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
