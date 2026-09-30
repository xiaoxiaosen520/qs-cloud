package com.qs.takeout.common.auth;

public final class AuthContext {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser get() {
        return HOLDER.get();
    }

    public static AuthUser require() {
        AuthUser user = HOLDER.get();
        if (user == null) {
            throw new com.qs.takeout.common.exception.BizException(401, "未登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
