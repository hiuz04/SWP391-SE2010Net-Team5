package com.swp.util;

import com.swp.model.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class RememberMeUtil {

    private static final String COOKIE_NAME = "remember_me";
    private static final int COOKIE_MAX_AGE = 30 * 24 * 60 * 60; // 30 days
    // In production, this SECRET_KEY should be loaded from environment variables or properties
    private static final String SECRET_KEY = "S3cr3tK3yF0rR3m3mb3rM3"; 

    private RememberMeUtil() {
    }

    public static void setRememberMeCookie(HttpServletResponse response, User user) {
        String token = generateToken(user);
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // Should enable in production over HTTPS
        response.addCookie(cookie);
    }

    public static void clearRememberMeCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public static String getRememberMeCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String extractUserId(String token) {
        if (token == null || !token.contains(":")) {
            return null;
        }
        return token.split(":")[0];
    }

    public static boolean verifyToken(String token, User user) {
        if (token == null || user == null) {
            return false;
        }
        String expectedToken = generateToken(user);
        return expectedToken.equals(token);
    }

    private static String generateToken(User user) {
        String rawData = user.getUserId() + ":" + user.getPasswordHash();
        String hash = hmacSha256(rawData, SECRET_KEY);
        return user.getUserId() + ":" + hash;
    }

    private static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa Remember Me Token", e);
        }
    }
}
