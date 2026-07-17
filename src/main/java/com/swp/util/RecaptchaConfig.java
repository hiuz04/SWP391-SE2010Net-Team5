package com.swp.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class RecaptchaConfig {
    private static final Properties PROPS = new Properties();
    private static String SITE_KEY;
    private static String SECRET_KEY;
    private static boolean CONFIGURED = false;

    static {
        try (InputStream in = RecaptchaConfig.class.getClassLoader().getResourceAsStream("recaptcha.properties")) {
            if (in != null) {
                PROPS.load(in);
                SITE_KEY = PROPS.getProperty("recaptcha.site.key", "").trim();
                SECRET_KEY = PROPS.getProperty("recaptcha.secret.key", "").trim();

                CONFIGURED = !SITE_KEY.isEmpty() && !SECRET_KEY.isEmpty()
                        && !SITE_KEY.equals("your_site_key_here")
                        && !SECRET_KEY.equals("your_secret_key_here");
            }
        } catch (IOException e) {
            System.err.println("[RecaptchaConfig] Lỗi đọc recaptcha.properties: " + e.getMessage());
        }
    }

    public static boolean isConfigured() {
        return CONFIGURED;
    }

    public static String getSiteKey() {
        return SITE_KEY;
    }

    public static String getSecretKey() {
        return SECRET_KEY;
    }
}
