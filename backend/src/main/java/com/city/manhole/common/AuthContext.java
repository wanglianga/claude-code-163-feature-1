package com.city.manhole.common;

import com.city.manhole.model.User;

import java.util.Arrays;

public class AuthContext {
    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    public static void set(User user) {
        CURRENT.set(user);
    }

    public static User get() {
        return CURRENT.get();
    }

    public static User requireLogin() {
        User u = CURRENT.get();
        if (u == null) {
            throw new BizException("未登录或登录已过期");
        }
        return u;
    }

    public static User requireRole(String... roles) {
        User u = requireLogin();
        if (Arrays.stream(roles).noneMatch(r -> r.equals(u.getRole()))) {
            throw new BizException("当前角色无权执行此操作");
        }
        return u;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
