package com.qs.takeout.modules.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.admin.dto.AdminShopUpdateRequest;
import com.qs.takeout.modules.admin.dto.BannerSaveRequest;
import com.qs.takeout.modules.admin.dto.CategorySaveRequest;
import com.qs.takeout.modules.admin.dto.DashboardVO;
import com.qs.takeout.modules.auth.entity.MerchantAccount;
import com.qs.takeout.modules.auth.entity.RiderAccount;
import com.qs.takeout.modules.auth.entity.UserAccount;
import com.qs.takeout.modules.auth.mapper.MerchantAccountMapper;
import com.qs.takeout.modules.auth.mapper.RiderAccountMapper;
import com.qs.takeout.modules.auth.mapper.UserAccountMapper;
import com.qs.takeout.modules.finance.entity.MerchantWithdrawRecord;
import com.qs.takeout.modules.finance.entity.SysConfig;
import com.qs.takeout.modules.finance.mapper.MerchantWithdrawRecordMapper;
import com.qs.takeout.modules.finance.mapper.SysConfigMapper;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.mapper.OrderMapper;
import com.qs.takeout.modules.shop.entity.Banner;
import com.qs.takeout.modules.shop.entity.PlatformCategory;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.entity.ShopApply;
import com.qs.takeout.modules.shop.mapper.BannerMapper;
import com.qs.takeout.modules.shop.mapper.PlatformCategoryMapper;
import com.qs.takeout.modules.shop.mapper.ShopApplyMapper;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminPlatformService {

    private final ShopMapper shopMapper;
    private final ShopApplyMapper shopApplyMapper;
    private final BannerMapper bannerMapper;
    private final PlatformCategoryMapper categoryMapper;
    private final SysConfigMapper sysConfigMapper;
    private final OrderMapper orderMapper;
    private final UserAccountMapper userAccountMapper;
    private final RiderAccountMapper riderAccountMapper;
    private final MerchantAccountMapper merchantAccountMapper;
    private final MerchantWithdrawRecordMapper withdrawRecordMapper;

    public DashboardVO dashboard() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        List<OrderEntity> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .ge(OrderEntity::getCreatedAt, start)
                .le(OrderEntity::getCreatedAt, end));
        BigDecimal pay = todayOrders.stream()
                .filter(o -> o.getPayAmount() != null && !"PENDING_PAY".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()))
                .map(OrderEntity::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingApply = shopApplyMapper.selectCount(new LambdaQueryWrapper<ShopApply>()
                .eq(ShopApply::getStatus, "PENDING"));
        long pendingWithdraw = withdrawRecordMapper.selectCount(new LambdaQueryWrapper<MerchantWithdrawRecord>()
                .eq(MerchantWithdrawRecord::getAuditStatus, "PENDING"));
        long onlineRider = riderAccountMapper.selectCount(new LambdaQueryWrapper<RiderAccount>()
                .eq(RiderAccount::getOnline, 1)
                .eq(RiderAccount::getStatus, 1));

        return DashboardVO.builder()
                .todayOrderCount(todayOrders.size())
                .todayPayAmount(pay)
                .pendingApplyCount(pendingApply)
                .pendingWithdrawCount(pendingWithdraw)
                .shopCount(shopMapper.selectCount(null))
                .userCount(userAccountMapper.selectCount(null))
                .riderCount(riderAccountMapper.selectCount(null))
                .onlineRiderCount(onlineRider)
                .build();
    }

    public Page<Shop> pageShops(String keyword, String shopType, Integer status, long page, long size) {
        LambdaQueryWrapper<Shop> q = new LambdaQueryWrapper<Shop>().orderByDesc(Shop::getId);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(Shop::getName, keyword).or().like(Shop::getPhone, keyword));
        }
        if (StringUtils.hasText(shopType)) {
            q.eq(Shop::getShopType, shopType);
        }
        if (status != null) {
            q.eq(Shop::getStatus, status);
        }
        return shopMapper.selectPage(new Page<>(page, size), q);
    }

    public Shop getShop(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BizException("店铺不存在");
        }
        return shop;
    }

    @Transactional
    public Shop updateShop(Long id, AdminShopUpdateRequest req) {
        Shop shop = getShop(id);
        if (req.getStatus() != null) {
            shop.setStatus(req.getStatus());
        }
        if (req.getOpenStatus() != null) {
            shop.setOpenStatus(req.getOpenStatus());
        }
        if (req.getCategoryId() != null) {
            shop.setCategoryId(req.getCategoryId());
        }
        if (req.getMinOrderAmount() != null) {
            shop.setMinOrderAmount(req.getMinOrderAmount());
        }
        if (req.getDeliveryFee() != null) {
            shop.setDeliveryFee(req.getDeliveryFee());
        }
        if (req.getPackingFee() != null) {
            shop.setPackingFee(req.getPackingFee());
        }
        if (req.getNotice() != null) {
            shop.setNotice(req.getNotice());
        }
        if (req.getBusinessHours() != null) {
            shop.setBusinessHours(req.getBusinessHours());
        }
        if (req.getPhone() != null) {
            shop.setPhone(req.getPhone());
        }
        shopMapper.updateById(shop);
        return shop;
    }

    public List<PlatformCategory> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<PlatformCategory>()
                .orderByAsc(PlatformCategory::getSort)
                .orderByDesc(PlatformCategory::getId));
    }

    @Transactional
    public PlatformCategory createCategory(CategorySaveRequest req) {
        PlatformCategory c = new PlatformCategory();
        applyCategory(c, req);
        categoryMapper.insert(c);
        return c;
    }

    @Transactional
    public PlatformCategory updateCategory(Long id, CategorySaveRequest req) {
        PlatformCategory c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BizException("类目不存在");
        }
        applyCategory(c, req);
        categoryMapper.updateById(c);
        return c;
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BizException("类目不存在");
        }
        categoryMapper.deleteById(id);
    }

    private void applyCategory(PlatformCategory c, CategorySaveRequest req) {
        c.setName(req.getName().trim());
        c.setIconUrl(req.getIconUrl() == null ? "" : req.getIconUrl());
        c.setShopType(req.getShopType().trim());
        c.setSort(req.getSort() == null ? 0 : req.getSort());
        c.setStatus(req.getStatus() == null ? 1 : req.getStatus());
    }

    public List<Banner> listBanners() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSort)
                .orderByDesc(Banner::getId));
    }

    @Transactional
    public Banner createBanner(BannerSaveRequest req) {
        Banner b = new Banner();
        applyBanner(b, req);
        bannerMapper.insert(b);
        return b;
    }

    @Transactional
    public Banner updateBanner(Long id, BannerSaveRequest req) {
        Banner b = bannerMapper.selectById(id);
        if (b == null) {
            throw new BizException("轮播不存在");
        }
        applyBanner(b, req);
        bannerMapper.updateById(b);
        return b;
    }

    @Transactional
    public void deleteBanner(Long id) {
        if (bannerMapper.selectById(id) == null) {
            throw new BizException("轮播不存在");
        }
        bannerMapper.deleteById(id);
    }

    private void applyBanner(Banner b, BannerSaveRequest req) {
        b.setTitle(req.getTitle() == null ? "" : req.getTitle());
        b.setImageUrl(req.getImageUrl());
        b.setLinkUrl(req.getLinkUrl() == null ? "" : req.getLinkUrl());
        b.setSort(req.getSort() == null ? 0 : req.getSort());
        b.setStatus(req.getStatus() == null ? 1 : req.getStatus());
    }

    public List<SysConfig> listConfigs() {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getConfigKey));
    }

    @Transactional
    public List<SysConfig> updateConfigs(Map<String, String> configs) {
        if (configs == null || configs.isEmpty()) {
            throw new BizException("配置不能为空");
        }
        for (Map.Entry<String, String> e : configs.entrySet()) {
            if (!StringUtils.hasText(e.getKey())) {
                continue;
            }
            SysConfig cfg = sysConfigMapper.selectById(e.getKey());
            if (cfg == null) {
                cfg = new SysConfig();
                cfg.setConfigKey(e.getKey());
                cfg.setConfigValue(e.getValue() == null ? "" : e.getValue());
                cfg.setRemark("");
                sysConfigMapper.insert(cfg);
            } else {
                cfg.setConfigValue(e.getValue() == null ? "" : e.getValue());
                sysConfigMapper.updateById(cfg);
            }
        }
        return listConfigs();
    }

    public Page<UserAccount> pageUsers(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<UserAccount> q = new LambdaQueryWrapper<UserAccount>().orderByDesc(UserAccount::getId);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(UserAccount::getPhone, keyword).or().like(UserAccount::getNickname, keyword));
        }
        if (status != null) {
            q.eq(UserAccount::getStatus, status);
        }
        return userAccountMapper.selectPage(new Page<>(page, size), q);
    }

    @Transactional
    public UserAccount updateUserStatus(Long id, Integer status) {
        UserAccount user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(status);
        userAccountMapper.updateById(user);
        return user;
    }

    public Page<RiderAccount> pageRiders(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<RiderAccount> q = new LambdaQueryWrapper<RiderAccount>().orderByDesc(RiderAccount::getId);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(RiderAccount::getPhone, keyword).or().like(RiderAccount::getName, keyword));
        }
        if (status != null) {
            q.eq(RiderAccount::getStatus, status);
        }
        return riderAccountMapper.selectPage(new Page<>(page, size), q);
    }

    @Transactional
    public RiderAccount updateRiderStatus(Long id, Integer status) {
        RiderAccount rider = riderAccountMapper.selectById(id);
        if (rider == null) {
            throw new BizException("骑手不存在");
        }
        rider.setStatus(status);
        if (status != null && status == 0) {
            rider.setOnline(0);
        }
        riderAccountMapper.updateById(rider);
        return rider;
    }

    public Page<MerchantAccount> pageMerchants(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<MerchantAccount> q = new LambdaQueryWrapper<MerchantAccount>().orderByDesc(MerchantAccount::getId);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(MerchantAccount::getPhone, keyword).or().like(MerchantAccount::getRealName, keyword));
        }
        if (status != null) {
            q.eq(MerchantAccount::getStatus, status);
        }
        return merchantAccountMapper.selectPage(new Page<>(page, size), q);
    }

    @Transactional
    public MerchantAccount updateMerchantStatus(Long id, Integer status) {
        MerchantAccount m = merchantAccountMapper.selectById(id);
        if (m == null) {
            throw new BizException("商家账号不存在");
        }
        m.setStatus(status);
        merchantAccountMapper.updateById(m);
        return m;
    }
}
