package com.qs.takeout.modules.user;

import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.auth.entity.UserAccount;
import com.qs.takeout.modules.auth.mapper.UserAccountMapper;
import com.qs.takeout.modules.user.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@RequireRole({Roles.USER})
public class UserProfileController {

    private final UserAccountMapper userAccountMapper;

    @GetMapping
    public ApiResult<Map<String, Object>> profile() {
        return ApiResult.ok(toMap(requireUser()));
    }

    @PutMapping
    public ApiResult<Map<String, Object>> update(@Valid @RequestBody ProfileUpdateRequest request) {
        UserAccount user = requireUser();
        if (request.getNickname() != null) {
            String nick = request.getNickname().trim();
            if (!StringUtils.hasText(nick)) {
                throw new BizException("昵称不能为空");
            }
            user.setNickname(nick);
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        userAccountMapper.updateById(user);
        return ApiResult.ok(toMap(user));
    }

    private UserAccount requireUser() {
        AuthUser auth = AuthContext.require();
        UserAccount user = userAccountMapper.selectById(auth.getId());
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    private Map<String, Object> toMap(UserAccount user) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("phone", user.getPhone() == null ? "" : user.getPhone());
        map.put("nickname", user.getNickname() == null ? "" : user.getNickname());
        map.put("avatarUrl", user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
        return map;
    }
}
