package com.swp.util;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Đọc cấu hình VNPay sandbox từ vnpay.properties và chuẩn hóa các URL callback.
 * Lớp này được PaymentController dùng trước khi tạo request VNPay.
 */
public final class VNPayConfig {

    private static final String CONFIG_FILE = "vnpay.properties";
    private static final Properties PROPS = new Properties();

    static {
        // Nạp cấu hình VNPay một lần từ classpath để mọi request thanh toán dùng cùng bộ cấu hình.
        try (InputStream in = VNPayConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException("Khong tim thay vnpay.properties trong classpath.");
            }
            PROPS.load(in);
        } catch (IOException e) {
            // Không đọc được cấu hình thì dừng khởi tạo để tránh tạo URL thanh toán sai.
            throw new ExceptionInInitializerError(e);
        }
    }

    private VNPayConfig() {
    }

    public static String getTmnCode() {
        return required("vnpay.tmnCode");
    }

    public static String getHashSecret() {
        return required("vnpay.hashSecret");
    }

    public static String getPayUrl() {
        return required("vnpay.payUrl");
    }

    public static String getReturnUrl() {
        return optional("vnpay.returnUrl");
    }

    public static String getIpnUrl() {
        return optional("vnpay.ipnUrl");
    }

    public static String getVersion() {
        return optionalWithDefault("vnpay.version", "2.1.0");
    }

    public static String getCommand() {
        return optionalWithDefault("vnpay.command", "pay");
    }

    public static String getOrderType() {
        return optionalWithDefault("vnpay.orderType", "other");
    }

    public static String getCurrCode() {
        return optionalWithDefault("vnpay.currCode", "VND");
    }

    public static String getLocale() {
        return optionalWithDefault("vnpay.locale", "vn");
    }

    public static String getTimeZone() {
        return optionalWithDefault("vnpay.timeZone", "Asia/Ho_Chi_Minh");
    }

    public static int getExpireMinutes() {
        String value = optionalWithDefault("vnpay.expireMinutes", "15");
        // expireMinutes phải là số nguyên dương để VNPay có thời hạn thanh toán hợp lệ.
        try {
            int minutes = Integer.parseInt(value);
            // Giá trị không dương không thể dùng làm thời gian hết hạn thanh toán.
            if (minutes <= 0) {
                throw new IllegalStateException("vnpay.expireMinutes phai lon hon 0.");
            }
            return minutes;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("vnpay.expireMinutes khong hop le.", e);
        }
    }

    /**
     * Kiểm tra các cấu hình bắt buộc trước khi tạo URL thanh toán.
     */
    public static void validateRequired() {
        getTmnCode();
        getHashSecret();
        getPayUrl();
    }

    /**
     * Lấy Return URL cấu hình sẵn hoặc tự dựng từ request hiện tại khi chạy môi trường local/demo.
     */
    public static String resolveReturnUrl(HttpServletRequest request) {
        String configured = getReturnUrl();
        // Nếu chưa cấu hình returnUrl cố định thì tự dựng từ request hiện tại.
        return configured.isBlank() ? buildPublicUrl(request, "/payment/vnpay-return") : configured;
    }

    /**
     * Lấy IPN URL cấu hình sẵn hoặc tự dựng URL public tương ứng với ứng dụng hiện tại.
     */
    public static String resolveIpnUrl(HttpServletRequest request) {
        String configured = getIpnUrl();
        // Nếu chưa cấu hình ipnUrl cố định thì tự dựng endpoint IPN của ứng dụng hiện tại.
        return configured.isBlank() ? buildPublicUrl(request, "/payment/vnpay-ipn") : configured;
    }

    /**
     * Dựng URL tuyệt đối, ưu tiên các header reverse proxy để callback vẫn đúng khi chạy sau ngrok/proxy.
     */
    private static String buildPublicUrl(HttpServletRequest request, String path) {
        String scheme = firstHeaderValue(request, "X-Forwarded-Proto");
        // Không có header proxy thì dùng scheme gốc của servlet request.
        if (scheme == null || scheme.isBlank()) {
            scheme = request.getScheme();
        }

        String host = firstHeaderValue(request, "X-Forwarded-Host");
        // Không có host từ proxy thì dựng host bằng serverName và port hiện tại.
        if (host == null || host.isBlank()) {
            StringBuilder fallback = new StringBuilder(request.getServerName());
            int port = request.getServerPort();
            boolean standardPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443);
            // Chỉ thêm port vào URL khi không phải port chuẩn của HTTP/HTTPS.
            if (!standardPort) {
                fallback.append(':').append(port);
            }
            host = fallback.toString();
        }

        return scheme + "://" + host + request.getContextPath() + path;
    }

    private static String firstHeaderValue(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        // Header không tồn tại thì caller sẽ dùng fallback từ request gốc.
        if (value == null) {
            return null;
        }
        int commaIndex = value.indexOf(',');
        // Header proxy có thể chứa nhiều giá trị, dùng giá trị đầu tiên là giá trị client-facing.
        return (commaIndex >= 0 ? value.substring(0, commaIndex) : value).trim();
    }

    private static String required(String key) {
        String value = optional(key);
        // Cấu hình bắt buộc thiếu thì không được tiếp tục tạo request VNPay.
        if (value.isBlank()) {
            throw new IllegalStateException(key + " chua duoc cau hinh trong vnpay.properties.");
        }
        return value;
    }

    private static String optionalWithDefault(String key, String defaultValue) {
        String value = optional(key);
        // Cấu hình rỗng dùng default để môi trường local vẫn chạy được.
        return value.isBlank() ? defaultValue : value;
    }

    private static String optional(String key) {
        return PROPS.getProperty(key, "").trim();
    }
}
