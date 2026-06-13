package com.libuke.evidence.security;

import com.libuke.evidence.common.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthContext {

    private final ThreadLocal<AdminPrincipal> principalHolder = new ThreadLocal<>();

    public AdminPrincipal currentUser() {
        AdminPrincipal principal = principalHolder.get();
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return principal;
    }

    public void set(AdminPrincipal principal) {
        principalHolder.set(principal);
    }

    public void clear() {
        principalHolder.remove();
    }
}
