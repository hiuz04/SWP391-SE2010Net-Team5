package com.swp.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Tiện ích hỗ trợ băm (hash) và kiểm tra mật khẩu.
 * Sử dụng thuật toán BCrypt để bảo mật mật khẩu.
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * Băm mật khẩu sử dụng thuật toán BCrypt.
     *
     * @param plainPassword mật khẩu gốc (chưa băm)
     * @return mật khẩu đã được băm (hash)
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Kiểm tra xem mật khẩu gốc có khớp với mật khẩu đã băm hay không.
     *
     * @param plainPassword mật khẩu người dùng nhập vào
     * @param hashedPassword mật khẩu đã băm lưu trong DB
     * @return {@code true} nếu khớp, ngược lại {@code false}
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Trường hợp hashedPassword trong DB đang là plain text (chưa băm trước đây)
            // thì BCrypt.checkpw sẽ ném lỗi. Fallback về so sánh chuỗi thường tạm thời
            // để hỗ trợ tài khoản cũ. Trong dự án thực tế nên migrate toàn bộ DB.
            return plainPassword.equals(hashedPassword);
        }
    }
}
