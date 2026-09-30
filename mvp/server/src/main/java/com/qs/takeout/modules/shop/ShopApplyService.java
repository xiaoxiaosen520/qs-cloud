package com.qs.takeout.modules.shop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.auth.entity.MerchantAccount;
import com.qs.takeout.modules.auth.mapper.MerchantAccountMapper;
import com.qs.takeout.modules.shop.dto.ShopApplyRequest;
import com.qs.takeout.modules.shop.dto.ShopApplyReviewRequest;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.entity.ShopApply;
import com.qs.takeout.modules.shop.mapper.ShopApplyMapper;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopApplyService {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private final ShopApplyMapper shopApplyMapper;
    private final ShopMapper shopMapper;
    private final MerchantAccountMapper merchantAccountMapper;

    @Transactional
    public ShopApply submit(ShopApplyRequest req) {
        AuthUser user = AuthContext.require();
        String phone = user.getPhone();
        if (!StringUtils.hasText(phone)) {
            throw new BizException("商家手机号缺失，请重新登录");
        }

        MerchantAccount merchant = merchantAccountMapper.selectById(user.getId());
        if (merchant == null) {
            throw new BizException("商家账号不存在");
        }
        if (merchant.getShopId() != null) {
            throw new BizException("已开通店铺，无需重复入驻");
        }

        Long pending = shopApplyMapper.selectCount(new LambdaQueryWrapper<ShopApply>()
                .eq(ShopApply::getMerchantId, merchant.getId())
                .eq(ShopApply::getStatus, PENDING));
        if (pending != null && pending > 0) {
            throw new BizException("已有待审核申请，请耐心等待");
        }

        String contactPhone = StringUtils.hasText(req.getContactPhone()) ? req.getContactPhone().trim() : phone;
        String address = req.getAddress().trim();
        if (StringUtils.hasText(req.getHouseNumber())) {
            address = address + " " + req.getHouseNumber().trim();
        }

        ShopApply apply = new ShopApply();
        apply.setMerchantId(merchant.getId());
        apply.setContactName(req.getContactName().trim());
        apply.setContactPhone(contactPhone);
        apply.setShopName(req.getShopName().trim());
        apply.setShopType(req.getShopType());
        apply.setCategoryId(req.getCategoryId() != null
                ? req.getCategoryId()
                : ("FOOD".equals(req.getShopType()) ? 1L : 2L));
        apply.setNotice(req.getNotice() == null ? "" : req.getNotice().trim());
        apply.setLogoUrl(empty(req.getLogoUrl()));
        apply.setWithinUrl(empty(req.getWithinUrl()));
        apply.setLicenseUrl(req.getLicenseUrl().trim());
        apply.setIdCardFrontUrl(req.getIdCardFrontUrl().trim());
        apply.setIdCardBackUrl(req.getIdCardBackUrl().trim());
        apply.setAddress(address);
        apply.setHouseNumber(req.getHouseNumber() == null ? "" : req.getHouseNumber().trim());
        apply.setLat(req.getLat());
        apply.setLng(req.getLng());
        apply.setStatus(PENDING);
        apply.setRejectReason("");
        shopApplyMapper.insert(apply);
        return apply;
    }

    private String empty(String v) {
        return v == null ? "" : v.trim();
    }

    public ShopApply latestStatus() {
        AuthUser user = AuthContext.require();
        return shopApplyMapper.selectOne(new LambdaQueryWrapper<ShopApply>()
                .eq(ShopApply::getMerchantId, user.getId())
                .orderByDesc(ShopApply::getId)
                .last("limit 1"));
    }

    public Page<ShopApply> pageForAdmin(String status, long page, long size) {
        LambdaQueryWrapper<ShopApply> q = new LambdaQueryWrapper<ShopApply>()
                .orderByDesc(ShopApply::getId);
        if (StringUtils.hasText(status)) {
            q.eq(ShopApply::getStatus, status);
        }
        return shopApplyMapper.selectPage(new Page<>(page, size), q);
    }

    @Transactional
    public ShopApply review(Long applyId, ShopApplyReviewRequest req) {
        ShopApply apply = shopApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException("申请不存在");
        }
        if (!PENDING.equals(apply.getStatus())) {
            throw new BizException("申请已处理");
        }

        if (Boolean.TRUE.equals(req.getApproved())) {
            return approve(apply);
        }
        if (!StringUtils.hasText(req.getRejectReason())) {
            throw new BizException("请填写拒绝原因");
        }
        apply.setStatus(REJECTED);
        apply.setRejectReason(req.getRejectReason());
        shopApplyMapper.updateById(apply);
        return apply;
    }

    private ShopApply approve(ShopApply apply) {
        if (apply.getLat() == null || apply.getLng() == null) {
            throw new BizException("申请缺少坐标，无法开店");
        }

        Shop shop = new Shop();
        shop.setName(apply.getShopName());
        shop.setShopType(apply.getShopType());
        shop.setCategoryId(apply.getCategoryId());
        shop.setLogoUrl(empty(apply.getLogoUrl()));
        shop.setWithinUrl(empty(apply.getWithinUrl()));
        shop.setLicenseUrl(empty(apply.getLicenseUrl()));
        shop.setIdCardFrontUrl(empty(apply.getIdCardFrontUrl()));
        shop.setIdCardBackUrl(empty(apply.getIdCardBackUrl()));
        shop.setNotice(empty(apply.getNotice()));
        shop.setAddress(apply.getAddress());
        shop.setLat(apply.getLat());
        shop.setLng(apply.getLng());
        shop.setPhone(apply.getContactPhone());
        shop.setMinOrderAmount(BigDecimal.ZERO);
        shop.setDeliveryFee(new BigDecimal("3.00"));
        shop.setPackingFee(BigDecimal.ZERO);
        shop.setBusinessHours("09:00-22:00");
        shop.setOpenStatus(0);
        shop.setStatus(1);
        shop.setScore(new BigDecimal("5.0"));
        shop.setMonthSales(0);
        shopMapper.insert(shop);

        MerchantAccount merchant = apply.getMerchantId() != null
                ? merchantAccountMapper.selectById(apply.getMerchantId())
                : null;
        if (merchant == null) {
            merchant = merchantAccountMapper.selectOne(new LambdaQueryWrapper<MerchantAccount>()
                    .eq(MerchantAccount::getPhone, apply.getContactPhone()));
        }
        if (merchant == null) {
            throw new BizException("申请人商家账号不存在");
        }
        merchant.setShopId(shop.getId());
        merchantAccountMapper.updateById(merchant);

        apply.setStatus(APPROVED);
        apply.setRejectReason("");
        shopApplyMapper.updateById(apply);
        return apply;
    }

    public List<ShopApply> listPending(int limit) {
        return shopApplyMapper.selectList(new LambdaQueryWrapper<ShopApply>()
                .eq(ShopApply::getStatus, PENDING)
                .orderByAsc(ShopApply::getId)
                .last("limit " + limit));
    }
}
