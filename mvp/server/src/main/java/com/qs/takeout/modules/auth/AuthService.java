package com.qs.takeout.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.common.jwt.JwtService;
import com.qs.takeout.common.sms.SmsCodeService;
import com.qs.takeout.modules.auth.dto.AdminLoginRequest;
import com.qs.takeout.modules.auth.dto.LoginResponse;
import com.qs.takeout.modules.auth.dto.SmsLoginRequest;
import com.qs.takeout.modules.auth.dto.SmsSendRequest;
import com.qs.takeout.modules.auth.entity.AdminAccount;
import com.qs.takeout.modules.auth.entity.MerchantAccount;
import com.qs.takeout.modules.auth.entity.RiderAccount;
import com.qs.takeout.modules.auth.entity.UserAccount;
import com.qs.takeout.modules.auth.mapper.AdminAccountMapper;
import com.qs.takeout.modules.auth.mapper.MerchantAccountMapper;
import com.qs.takeout.modules.auth.mapper.RiderAccountMapper;
import com.qs.takeout.modules.auth.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<String> SMS_ROLES = Set.of(Roles.USER, Roles.MERCHANT, Roles.RIDER);

    private final SmsCodeService smsCodeService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountMapper userAccountMapper;
    private final MerchantAccountMapper merchantAccountMapper;
    private final RiderAccountMapper riderAccountMapper;
    private final AdminAccountMapper adminAccountMapper;

    public void sendSms(SmsSendRequest req) {
        String scene = req.getScene().trim().toUpperCase();
        if (!scene.startsWith("LOGIN_")) {
            throw new BizException("scene 无效");
        }
        smsCodeService.send(req.getPhone(), scene);
    }

    @Transactional
    public LoginResponse smsLogin(SmsLoginRequest req) {
        String role = req.getRole().trim().toUpperCase();
        if (!SMS_ROLES.contains(role)) {
            throw new BizException("角色无效");
        }
        String scene = "LOGIN_" + role;
        smsCodeService.verifyOrThrow(req.getPhone(), scene, req.getCode());

        return switch (role) {
            case Roles.USER -> loginUser(req.getPhone());
            case Roles.MERCHANT -> loginMerchant(req.getPhone());
            case Roles.RIDER -> loginRider(req.getPhone());
            default -> throw new BizException("角色无效");
        };
    }

    private LoginResponse loginUser(String phone) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getPhone, phone));
        if (user == null) {
            user = new UserAccount();
            user.setPhone(phone);
            user.setNickname("用户" + phone.substring(7));
            user.setAvatarUrl("");
            user.setStatus(1);
            userAccountMapper.insert(user);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已禁用");
        }
        AuthUser auth = AuthUser.builder()
                .id(user.getId())
                .role(Roles.USER)
                .phone(phone)
                .build();
        return LoginResponse.builder()
                .token(jwtService.createToken(auth))
                .role(Roles.USER)
                .userId(user.getId())
                .phone(phone)
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl() == null ? "" : user.getAvatarUrl())
                .build();
    }

    private LoginResponse loginMerchant(String phone) {
        MerchantAccount merchant = merchantAccountMapper.selectOne(new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getPhone, phone));
        if (merchant == null) {
            merchant = new MerchantAccount();
            merchant.setPhone(phone);
            merchant.setStatus(1);
            merchantAccountMapper.insert(merchant);
        }
        if (merchant.getStatus() != null && merchant.getStatus() == 0) {
            throw new BizException("账号已禁用");
        }
        AuthUser auth = AuthUser.builder()
                .id(merchant.getId())
                .role(Roles.MERCHANT)
                .phone(phone)
                .shopId(merchant.getShopId())
                .build();
        return LoginResponse.builder()
                .token(jwtService.createToken(auth))
                .role(Roles.MERCHANT)
                .userId(merchant.getId())
                .shopId(merchant.getShopId())
                .phone(phone)
                .build();
    }

    private LoginResponse loginRider(String phone) {
        RiderAccount rider = riderAccountMapper.selectOne(new LambdaQueryWrapper<RiderAccount>()
                .eq(RiderAccount::getPhone, phone));
        if (rider == null) {
            rider = new RiderAccount();
            rider.setPhone(phone);
            rider.setName("骑手" + phone.substring(7));
            rider.setStatus(1);
            rider.setOnline(0);
            rider.setWithdrawableBalance(java.math.BigDecimal.ZERO);
            rider.setFrozenBalance(java.math.BigDecimal.ZERO);
            riderAccountMapper.insert(rider);
        }
        if (rider.getStatus() != null && rider.getStatus() == 0) {
            throw new BizException("账号已禁用");
        }
        AuthUser auth = AuthUser.builder()
                .id(rider.getId())
                .role(Roles.RIDER)
                .phone(phone)
                .build();
        return LoginResponse.builder()
                .token(jwtService.createToken(auth))
                .role(Roles.RIDER)
                .userId(rider.getId())
                .phone(phone)
                .nickname(rider.getName())
                .build();
    }

    public LoginResponse adminLogin(AdminLoginRequest req) {
        AdminAccount admin = adminAccountMapper.selectOne(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getUsername, req.getUsername()));
        if (admin == null || admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BizException("账号或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), admin.getPasswordHash())) {
            throw new BizException("账号或密码错误");
        }
        AuthUser auth = AuthUser.builder()
                .id(admin.getId())
                .role(Roles.ADMIN)
                .phone("")
                .build();
        return LoginResponse.builder()
                .token(jwtService.createToken(auth))
                .role(Roles.ADMIN)
                .userId(admin.getId())
                .nickname(admin.getUsername())
                .build();
    }

    public Map<String, Object> me() {
        AuthUser user = AuthContext.require();
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("role", user.getRole());
        map.put("phone", user.getPhone() == null ? "" : user.getPhone());
        map.put("shopId", user.getShopId() == null ? 0 : user.getShopId());
        if (Roles.USER.equals(user.getRole())) {
            UserAccount account = userAccountMapper.selectById(user.getId());
            if (account != null) {
                map.put("nickname", account.getNickname() == null ? "" : account.getNickname());
                map.put("avatarUrl", account.getAvatarUrl() == null ? "" : account.getAvatarUrl());
            }
        }
        return map;
    }
}
