package com.swp.util;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class GoogleConfig {

    private static final Properties PROPS = new Properties();
    private static final boolean LOADED;

    static {
        boolean loaded = false;
        try (InputStream in = GoogleConfig.class.getClassLoader().getResourceAsStream("google.properties")) {
            if (in != null) {
                PROPS.load(in);
                loaded = true;
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        LOADED = loaded;
    }

    private GoogleConfig() {
    }

    public static boolean isConfigured() {
        return LOADED && !getClientId().isBlank() && !getClientSecret().isBlank();
    }

    public static String getClientId() {
        return PROPS.getProperty("google.client.id", "").trim();
    }

    public static String getClientSecret() {
        return PROPS.getProperty("google.client.secret", "").trim();
    }

    public static String getRedirectUri() {
        return PROPS.getProperty("google.redirect.uri", "").trim();
    }

    /**
     * Ưu tiên google.redirect.uri trong file; nếu trống thì tự tạo theo context path hiện tại.
     */
    public static String resolveRedirectUri(HttpServletRequest request) {
        String configured = getRedirectUri();
        if (!configured.isBlank()) {
            return configured;
        }
        return buildCallbackUrl(request);
    }

    public static String buildCallbackUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        boolean isStandardPort = ("http".equals(request.getScheme()) && port == 80)
                || ("https".equals(request.getScheme()) && port == 443);
        if (!isStandardPort) {
            url.append(":").append(port);
        }
        url.append(request.getContextPath()).append("/auth/google/callback");
        return url.toString();
    }
}
