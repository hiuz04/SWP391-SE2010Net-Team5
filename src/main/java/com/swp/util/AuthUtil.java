package com.swp.util;

/**
 * Tiện ích xác thực và phân quyền người dùng.
 * Cung cấp các hàm helper dùng chung cho các servlet.
 */
public final class AuthUtil {

    /** Ngăn khởi tạo instance (utility class). */
    private AuthUtil() {
    }

    /**
     * Chuyển đổi tên vai trò từ database sang chuỗi navRole
     * dùng để điều hướng menu trong giao diện (app.js).
     *
     * <p>
     * Mapping:
     * </p>
     * <ul>
     * <li>ADMIN → "admin"</li>
     * <li>STAFF → "staff"</li>
     * <li>OWNER → "owner"</li>
     * <li>khác → "customer"</li>
     * </ul>
     *
     * @param roleName tên vai trò lấy từ DB (ví dụ: "ADMIN", "STAFF")
     * @return chuỗi navRole tương ứng dùng cho frontend
     */
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

    public static String dashboardPath(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "/";
        }
        return switch (roleName.toUpperCase()) {
            case "ADMIN" -> "/admin/dashboard";
            case "OWNER" -> "/owner/dashboard";
            case "STAFF" -> "/staff/dashboard";
            case "CUSTOMER" -> "/search";
            default -> "/";
        };
    }
}
