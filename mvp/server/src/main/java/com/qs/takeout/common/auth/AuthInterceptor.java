package com.qs.takeout.common.auth;

import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.common.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        AuthUser user = null;
        if (header != null && header.startsWith("Bearer ")) {
            user = jwtService.parse(header.substring(7).trim());
            AuthContext.set(user);
        }

        RequireRole requireRole = method.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null) {
            if (user == null) {
                throw new BizException(401, "未登录");
            }
            boolean ok = Arrays.asList(requireRole.value()).contains(user.getRole());
            if (!ok) {
                throw new BizException(403, "无权限");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
