package com.swp.util;

import java.util.regex.Pattern;

public final class RegisterValidator {

    public static final String DEFAULT_ROLE = "CUSTOMER";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9,10}$");
    private static final Pattern FULL_NAME_PATTERN =
            Pattern.compile("^[\\p{L}][\\p{L}\\s'.]{1,98}[\\p{L}.]$|^[\\p{L}]{2,}$");

    private RegisterValidator() {
    }

    public static ValidationResult validate(String fullName, String phone, String email,
                                           String password, String confirmPassword) {
        ValidationResult result = new ValidationResult();

        if (fullName == null || fullName.isBlank()) {
            result.addFieldError("fullName", "Họ tên không được để trống.");
        } else if (fullName.length() < 2 || fullName.length() > 100) {
            result.addFieldError("fullName", "Họ tên phải từ 2 đến 100 ký tự.");
        } else if (!FULL_NAME_PATTERN.matcher(fullName).matches()) {
            result.addFieldError("fullName", "Họ tên chỉ được chứa chữ cái và khoảng trắng.");
        }

        if (email == null || email.isBlank()) {
            result.addFieldError("email", "Email không được để trống.");
        } else if (email.length() > 100) {
            result.addFieldError("email", "Email không được vượt quá 100 ký tự.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            result.addFieldError("email", "Email không đúng định dạng (vd: name@example.com).");
        }

        if (phone == null || phone.isBlank()) {
            result.addFieldError("phone", "Số điện thoại không được để trống.");
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            result.addFieldError("phone", "Số điện thoại phải bắt đầu bằng 0 và có 10–11 chữ số.");
        }

        String passwordError = PasswordUtil.validatePassword(password);
        if (passwordError != null) {
            result.addFieldError("password", passwordError);
        }

        if (confirmPassword == null || confirmPassword.isBlank()) {
            result.addFieldError("confirmPassword", "Vui lòng xác nhận mật khẩu.");
        } else if (password != null && !password.equals(confirmPassword)) {
            result.addFieldError("confirmPassword", "Mật khẩu xác nhận không khớp.");
        }

        return result;
    }

}
