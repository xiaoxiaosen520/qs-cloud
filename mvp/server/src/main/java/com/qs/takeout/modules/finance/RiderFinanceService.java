package com.qs.takeout.modules.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.auth.entity.RiderAccount;
import com.qs.takeout.modules.auth.mapper.RiderAccountMapper;
import com.qs.takeout.modules.finance.dto.RiderWalletVO;
import com.qs.takeout.modules.finance.dto.SettlementUpdateRequest;
import com.qs.takeout.modules.finance.dto.WithdrawApplyRequest;
import com.qs.takeout.modules.finance.dto.WithdrawReviewRequest;
import com.qs.takeout.modules.finance.entity.RiderBillingRecord;
import com.qs.takeout.modules.finance.entity.RiderWithdrawRecord;
import com.qs.takeout.modules.finance.mapper.RiderBillingRecordMapper;
import com.qs.takeout.modules.finance.mapper.RiderWithdrawRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RiderFinanceService {

    private final RiderAccountMapper riderAccountMapper;
    private final RiderBillingRecordMapper billingRecordMapper;
    private final RiderWithdrawRecordMapper withdrawRecordMapper;
    private final RiderSettleService settleService;

    private RiderAccount requireRider() {
        AuthUser user = AuthContext.require();
        if (!Roles.RIDER.equals(user.getRole())) {
            throw new BizException(403, "仅骑手可操作");
        }
        RiderAccount rider = riderAccountMapper.selectById(user.getId());
        if (rider == null || rider.getStatus() != null && rider.getStatus() == 0) {
            throw new BizException("骑手账号无效");
        }
        return rider;
    }

    public RiderWalletVO wallet() {
        RiderAccount r = reload(requireRider());
        BigDecimal w = nz(r.getWithdrawableBalance());
        BigDecimal f = nz(r.getFrozenBalance());
        return RiderWalletVO.builder()
                .withdrawableBalance(w)
                .frozenBalance(f)
                .totalBalance(w.add(f))
                .name(nullToEmpty(r.getName()))
                .realName(nullToEmpty(r.getRealName()))
                .bankCard(nullToEmpty(r.getBankCard()))
                .alipayAccount(nullToEmpty(r.getAlipayAccount()))
                .wechatAccount(nullToEmpty(r.getWechatAccount()))
                .withdrawFee(settleService.withdrawFee())
                .build();
    }

    @Transactional
    public RiderWalletVO updateSettlement(SettlementUpdateRequest req) {
        RiderAccount r = reload(requireRider());
        if (req.getRealName() != null) {
            r.setRealName(req.getRealName().trim());
        }
        if (req.getBankCard() != null) {
            r.setBankCard(req.getBankCard().trim());
        }
        if (req.getAlipayAccount() != null) {
            r.setAlipayAccount(req.getAlipayAccount().trim());
        }
        if (req.getWechatAccount() != null) {
            r.setWechatAccount(req.getWechatAccount().trim());
        }
        riderAccountMapper.updateById(r);
        return wallet();
    }

    public List<RiderBillingRecord> billings() {
        RiderAccount r = requireRider();
        return billingRecordMapper.selectList(new LambdaQueryWrapper<RiderBillingRecord>()
                .eq(RiderBillingRecord::getRiderId, r.getId())
                .orderByDesc(RiderBillingRecord::getId)
                .last("limit 100"));
    }

    public List<RiderWithdrawRecord> withdraws() {
        RiderAccount r = requireRider();
        return withdrawRecordMapper.selectList(new LambdaQueryWrapper<RiderWithdrawRecord>()
                .eq(RiderWithdrawRecord::getRiderId, r.getId())
                .orderByDesc(RiderWithdrawRecord::getId)
                .last("limit 50"));
    }

    @Transactional
    public RiderWithdrawRecord applyWithdraw(WithdrawApplyRequest req) {
        RiderAccount r = reload(requireRider());
        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = settleService.withdrawFee();
        if (amount.compareTo(fee) <= 0) {
            throw new BizException("提现金额需大于手续费 " + fee + " 元");
        }
        if (nz(r.getWithdrawableBalance()).compareTo(amount) < 0) {
            throw new BizException("可提现余额不足");
        }
        String mode = req.getPaymentMode().trim().toUpperCase();
        String snapshot = accountSnapshot(r, mode);

        r.setWithdrawableBalance(nz(r.getWithdrawableBalance()).subtract(amount));
        r.setFrozenBalance(nz(r.getFrozenBalance()).add(amount));
        riderAccountMapper.updateById(r);

        RiderWithdrawRecord rec = new RiderWithdrawRecord();
        rec.setRiderId(r.getId());
        rec.setWithdrawNo(nextWithdrawNo());
        rec.setAmount(amount);
        rec.setFee(fee);
        rec.setActualAmount(amount.subtract(fee).max(BigDecimal.ZERO));
        rec.setPaymentMode(mode);
        rec.setAccountSnapshot(snapshot);
        rec.setAuditStatus("PENDING");
        rec.setAuditReason("");
        withdrawRecordMapper.insert(rec);

        RiderBillingRecord bill = new RiderBillingRecord();
        bill.setRiderId(r.getId());
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

    public List<RiderWithdrawRecord> adminListWithdraws(String status) {
        LambdaQueryWrapper<RiderWithdrawRecord> q = new LambdaQueryWrapper<RiderWithdrawRecord>()
                .orderByDesc(RiderWithdrawRecord::getId)
                .last("limit 100");
        if (StringUtils.hasText(status)) {
            q.eq(RiderWithdrawRecord::getAuditStatus, status);
        }
        return withdrawRecordMapper.selectList(q);
    }

    @Transactional
    public RiderWithdrawRecord reviewWithdraw(Long id, WithdrawReviewRequest req) {
        RiderWithdrawRecord rec = withdrawRecordMapper.selectById(id);
        if (rec == null) {
            throw new BizException("提现单不存在");
        }
        if (!"PENDING".equals(rec.getAuditStatus())) {
            throw new BizException("该提现单已审核");
        }
        RiderAccount r = reload(riderAccountMapper.selectById(rec.getRiderId()));
        BigDecimal amount = nz(rec.getAmount());
        if (nz(r.getFrozenBalance()).compareTo(amount) < 0) {
            throw new BizException("冻结余额异常");
        }

        if (req.isApproved()) {
            r.setFrozenBalance(nz(r.getFrozenBalance()).subtract(amount));
            riderAccountMapper.updateById(r);
            rec.setAuditStatus("APPROVED");
            rec.setAuditReason(StringUtils.hasText(req.getReason()) ? req.getReason() : "审核通过，请线下打款");
        } else {
            r.setFrozenBalance(nz(r.getFrozenBalance()).subtract(amount));
            r.setWithdrawableBalance(nz(r.getWithdrawableBalance()).add(amount));
            riderAccountMapper.updateById(r);

            RiderBillingRecord bill = new RiderBillingRecord();
            bill.setRiderId(r.getId());
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

    private String accountSnapshot(RiderAccount r, String mode) {
        return switch (mode) {
            case "BANK" -> {
                if (!StringUtils.hasText(r.getBankCard())) {
                    throw new BizException("请先完善银行卡收款信息");
                }
                yield "BANK|" + nullToEmpty(r.getRealName()) + "|" + r.getBankCard();
            }
            case "ALIPAY" -> {
                if (!StringUtils.hasText(r.getAlipayAccount())) {
                    throw new BizException("请先完善支付宝收款账号");
                }
                yield "ALIPAY|" + nullToEmpty(r.getRealName()) + "|" + r.getAlipayAccount();
            }
            case "WECHAT" -> {
                if (!StringUtils.hasText(r.getWechatAccount())) {
                    throw new BizException("请先完善微信收款账号");
                }
                yield "WECHAT|" + nullToEmpty(r.getRealName()) + "|" + r.getWechatAccount();
            }
            default -> throw new BizException("不支持的收款方式");
        };
    }

    private String nextWithdrawNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int r = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "RW" + ts + r;
    }

    private RiderAccount reload(RiderAccount r) {
        if (r == null || r.getId() == null) {
            throw new BizException("骑手账号无效");
        }
        RiderAccount latest = riderAccountMapper.selectById(r.getId());
        if (latest == null) {
            throw new BizException("骑手账号无效");
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
