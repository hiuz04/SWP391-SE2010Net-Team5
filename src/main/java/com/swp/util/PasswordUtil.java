/*
 * Author: Tran Bao Long
 * 31/5/2026
 */
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

    /**
     * Kiểm tra tính hợp lệ của mật khẩu (độ dài, ký tự chữ và số).
     *
     * @param password mật khẩu cần kiểm tra
     * @return null nếu hợp lệ, ngược lại trả về chuỗi thông báo lỗi
     */
    public static String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            return "Mật khẩu không được để trống.";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }
        if (password.length() > 64) {
            return "Mật khẩu không được vượt quá 64 ký tự.";
        }
        if (!containsLetter(password) || !containsDigit(password)) {
            return "Mật khẩu phải có ít nhất 1 chữ cái và 1 chữ số.";
        }
        return null;
    }

    private static boolean containsLetter(String value) {
        for (char c : value.toCharArray()) {
            if (Character.isLetter(c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDigit(String value) {
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }
}
