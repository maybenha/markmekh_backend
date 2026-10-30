package com.panha.markmekh.security;

import com.panha.markmekh.entity.User;
import com.panha.markmekh.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails cud)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return cud.getUser();
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static boolean hasRole(String role) {
        return currentUser().getRole().name().equals(role);
    }

    public static boolean isAdminOrHr() {
        String r = currentUser().getRole().name();
        return r.equals("SUPER_ADMIN") || r.equals("HR_MANAGER");
    }
}