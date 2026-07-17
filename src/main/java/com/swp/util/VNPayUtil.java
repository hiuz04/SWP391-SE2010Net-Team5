package com.swp.util;

import com.swp.model.Payment;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

public final class VNPayUtil {

    private static final DateTimeFormatter VNPAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private VNPayUtil() {
    }

    public static String buildPaymentUrl(Payment payment, long bookingId, HttpServletRequest request) {
        return buildPaymentUrlDebug(payment, bookingId, request).paymentUrl();
    }

    public static PaymentUrlDebug buildPaymentUrlDebug(Payment payment, long bookingId, HttpServletRequest request) {
        String transactionRef = payment.getTransactionRef();
        String orderInfo = "Thanh toan booking " + bookingId + " - " + transactionRef;

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", VNPayConfig.getVersion());
        params.put("vnp_Command", VNPayConfig.getCommand());
        params.put("vnp_TmnCode", VNPayConfig.getTmnCode());
        params.put("vnp_Amount", toVNPayAmount(payment.getAmount()));
        params.put("vnp_CurrCode", VNPayConfig.getCurrCode());
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", VNPayConfig.getOrderType());
        params.put("vnp_Locale", VNPayConfig.getLocale());
        params.put("vnp_ReturnUrl", VNPayConfig.resolveReturnUrl(request));
        params.put("vnp_IpAddr", getClientIp(request));
        LocalDateTime createDate = LocalDateTime.now(ZoneId.of(VNPayConfig.getTimeZone()));
        params.put("vnp_CreateDate", formatDate(createDate));
        params.put("vnp_ExpireDate", formatDate(createDate.plusMinutes(VNPayConfig.getExpireMinutes())));

        String query = buildQueryString(params);
        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(VNPayConfig.getHashSecret(), hashData);
        String paymentUrl = VNPayConfig.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
        return new PaymentUrlDebug(paymentUrl, new TreeMap<>(params), query, hashData, secureHash);
    }

    public static boolean verifySignature(Map<String, String> requestParams) {
        return verifySignatureDebug(requestParams).valid();
    }

    public static SignatureDebug verifySignatureDebug(Map<String, String> requestParams) {
        String receivedHash = requestParams.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return new SignatureDebug(false, "", "", "");
        }

        Map<String, String> signedParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith("vnp_")) {
                continue;
            }
            if ("vnp_SecureHash".equalsIgnoreCase(key) || "vnp_SecureHashType".equalsIgnoreCase(key)) {
                continue;
            }
            String value = entry.getValue();
            if (value != null && !value.isBlank()) {
                signedParams.put(key, value);
            }
        }

        String payload = buildHashData(signedParams);
        String expectedHash = hmacSHA512(VNPayConfig.getHashSecret(), payload);
        return new SignatureDebug(expectedHash.equalsIgnoreCase(receivedHash), payload, expectedHash, receivedHash);
    }

    public static String hmacSHA512(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hash.append(String.format("%02x", b & 0xff));
            }
            return hash.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Khong the tao chu ky VNPay.", e);
        }
    }

    public static Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0 && values[0] != null) {
                params.put(entry.getKey(), values[0]);
            }
        }
        return params;
    }

    public static String buildRawPayload(Map<String, String> params) {
        return buildQueryString(new TreeMap<>(params));
    }

    public static String toVNPayAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("So tien thanh toan khong hop le.");
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    public static String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return normalizeClientIp(commaIndex >= 0 ? forwardedFor.substring(0, commaIndex) : forwardedFor);
        }
        return normalizeClientIp(request.getRemoteAddr());
    }

    private static String formatDate(LocalDateTime dateTime) {
        return dateTime.format(VNPAY_DATE_FORMATTER);
    }

    private static String buildQueryString(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(entry.getValue()));
        }
        return query.toString();
    }

    private static String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!hashData.isEmpty()) {
                hashData.append('&');
            }
            hashData.append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(entry.getValue()));
        }
        return hashData.toString();
    }

    private static String normalizeClientIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "127.0.0.1";
        }
        String normalized = ipAddress.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if ("0:0:0:0:0:0:0:1".equals(normalized)
                || "::1".equals(normalized)
                || "localhost".equalsIgnoreCase(normalized)) {
            return "127.0.0.1";
        }
        return normalized;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record PaymentUrlDebug(
            String paymentUrl,
            Map<String, String> sortedParams,
            String query,
            String hashData,
            String secureHash
    ) {
    }

    public record SignatureDebug(
            boolean valid,
            String hashData,
            String expectedHash,
            String receivedHash
    ) {
    }
}
