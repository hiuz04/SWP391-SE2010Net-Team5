package com.swp.util;

public final class AuthUtil {

    private AuthUtil() {
    }

    public static String toNavRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "guest";
        }
        return switch (roleName.toUpperCase()) {
            case "ADMIN" -> "admin";
            case "STAFF" -> "staff";
            case "OWNER" -> "owner";
            default -> "customer";
        };
    }
}
