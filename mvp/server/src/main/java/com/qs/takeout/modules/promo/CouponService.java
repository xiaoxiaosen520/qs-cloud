package com.qs.takeout.modules.promo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.promo.dto.UserCouponVO;
import com.qs.takeout.modules.promo.entity.Coupon;
import com.qs.takeout.modules.promo.entity.UserCoupon;
import com.qs.takeout.modules.promo.mapper.CouponMapper;
import com.qs.takeout.modules.promo.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public List<Coupon> available(Long shopId) {
        return couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .and(w -> w.isNull(Coupon::getShopId)
                        .or()
                        .eq(shopId != null, Coupon::getShopId, shopId))
                .orderByAsc(Coupon::getThreshold));
    }

    @Transactional
    public UserCoupon claim(Long couponId) {
        Long userId = AuthContext.require().getId();
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() == null || coupon.getStatus() == 0) {
            throw new BizException("优惠券不可用");
        }
        if (coupon.getClaimed() >= coupon.getTotal()) {
            throw new BizException("优惠券已领完");
        }
        Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId)
                .eq(UserCoupon::getStatus, "UNUSED"));
        if (count > 0) {
            throw new BizException("已领取过该券");
        }
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus("UNUSED");
        userCouponMapper.insert(uc);
        coupon.setClaimed(coupon.getClaimed() + 1);
        couponMapper.updateById(coupon);
        return uc;
    }

    public List<UserCouponVO> mine(String status, Long shopId, BigDecimal goodsAmount) {
        Long userId = AuthContext.require().getId();
        LambdaQueryWrapper<UserCoupon> q = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getId);
        if (status != null && !status.isBlank()) {
            q.eq(UserCoupon::getStatus, status);
        }
        List<UserCoupon> rows = userCouponMapper.selectList(q);
        List<UserCouponVO> list = new ArrayList<>();
        for (UserCoupon row : rows) {
            Coupon c = couponMapper.selectById(row.getCouponId());
            if (c == null) continue;
            boolean shopOk = c.getShopId() == null || (shopId != null && c.getShopId().equals(shopId));
            boolean amountOk = goodsAmount == null
                    || goodsAmount.compareTo(c.getThreshold() == null ? BigDecimal.ZERO : c.getThreshold()) >= 0;
            boolean usable = "UNUSED".equals(row.getStatus()) && shopOk && amountOk;
            list.add(UserCouponVO.builder()
                    .id(row.getId())
                    .couponId(c.getId())
                    .name(c.getName())
                    .shopId(c.getShopId())
                    .threshold(c.getThreshold())
                    .discount(c.getDiscount())
                    .status(row.getStatus())
                    .claimedAt(row.getClaimedAt())
                    .usable(usable)
                    .build());
        }
        return list;
    }

    /** 校验并返回减免金额；不落库 */
    public BigDecimal previewDiscount(Long userCouponId, Long shopId, BigDecimal goodsAmount) {
        if (userCouponId == null) {
            return BigDecimal.ZERO;
        }
        Long userId = AuthContext.require().getId();
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !userId.equals(uc.getUserId()) || !"UNUSED".equals(uc.getStatus())) {
            throw new BizException("优惠券不可用");
        }
        Coupon c = couponMapper.selectById(uc.getCouponId());
        if (c == null || c.getStatus() == null || c.getStatus() == 0) {
            throw new BizException("优惠券已失效");
        }
        if (c.getShopId() != null && !c.getShopId().equals(shopId)) {
            throw new BizException("优惠券不适用于该店铺");
        }
        BigDecimal threshold = c.getThreshold() == null ? BigDecimal.ZERO : c.getThreshold();
        if (goodsAmount.compareTo(threshold) < 0) {
            throw new BizException("未满优惠门槛 " + threshold);
        }
        return c.getDiscount() == null ? BigDecimal.ZERO : c.getDiscount();
    }

    @Transactional
    public void markUsed(Long userCouponId, Long orderId) {
        if (userCouponId == null) return;
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) return;
        uc.setStatus("USED");
        uc.setOrderId(orderId);
        uc.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(uc);
    }

    @Transactional
    public void restore(Long userCouponId) {
        if (userCouponId == null) return;
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !"USED".equals(uc.getStatus())) return;
        uc.setStatus("UNUSED");
        uc.setOrderId(null);
        uc.setUsedAt(null);
        userCouponMapper.updateById(uc);
    }
}
