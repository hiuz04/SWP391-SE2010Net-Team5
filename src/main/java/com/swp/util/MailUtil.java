package com.swp.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Utility class để gửi email qua SMTP (Gmail).
 * Cấu hình đọc từ mail.properties trong classpath.
 */
public final class MailUtil {

    private static final Properties MAIL_PROPS = new Properties();
    private static String FROM_ADDRESS;
    private static String FROM_PASSWORD;
    private static String FROM_NAME;
    private static boolean CONFIGURED = false;

    static {
        try (InputStream in = MailUtil.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                MAIL_PROPS.load(in);
                FROM_ADDRESS = MAIL_PROPS.getProperty("mail.from", "").trim();
                FROM_PASSWORD = MAIL_PROPS.getProperty("mail.password", "").trim();
                FROM_NAME    = MAIL_PROPS.getProperty("mail.from.name", "Sport Field Booking").trim();

                // Kiểm tra đã cấu hình thật chưa
                CONFIGURED = !FROM_ADDRESS.isBlank()
                        && !FROM_PASSWORD.isBlank()
                        && !FROM_ADDRESS.equals("your_email@gmail.com")
                        && !FROM_PASSWORD.equals("your_app_password");
            }
        } catch (IOException e) {
            // Không có file → gửi mail sẽ thất bại, log error
            System.err.println("[MailUtil] Không tìm thấy mail.properties: " + e.getMessage());
        }
    }

    private MailUtil() {}

    public static boolean isConfigured() {
        return CONFIGURED;
    }

    /**
     * Gửi email HTML đến địa chỉ chỉ định.
     *
     * @param toEmail  địa chỉ email người nhận
     * @param subject  tiêu đề email
     * @param htmlBody nội dung HTML của email
     * @throws MessagingException nếu gửi thất bại
     */
    public static void sendHtml(String toEmail, String subject, String htmlBody)
            throws MessagingException, UnsupportedEncodingException {
        if (!CONFIGURED) {
            throw new IllegalStateException(
                    "Mail chưa được cấu hình. Hãy cập nhật mail.properties với thông tin Gmail thực.");
        }

        // Tạo SMTP session
        Properties smtpProps = new Properties();
        smtpProps.put("mail.smtp.host",            MAIL_PROPS.getProperty("mail.smtp.host", "smtp.gmail.com"));
        smtpProps.put("mail.smtp.port",            MAIL_PROPS.getProperty("mail.smtp.port", "587"));
        smtpProps.put("mail.smtp.auth",            MAIL_PROPS.getProperty("mail.smtp.auth", "true"));
        smtpProps.put("mail.smtp.starttls.enable", MAIL_PROPS.getProperty("mail.smtp.starttls.enable", "true"));
        smtpProps.put("mail.smtp.ssl.protocols",   "TLSv1.2 TLSv1.3");

        Session session = Session.getInstance(smtpProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_ADDRESS, FROM_PASSWORD);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_ADDRESS, FROM_NAME, "UTF-8"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject(subject, "UTF-8");
        message.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    /**
     * Tạo nội dung HTML email thông báo mật khẩu mới.
     */
    public static String buildNewPasswordEmail(String fullName, String newPassword) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; margin:0; padding:0;">
                  <div style="max-width:560px; margin:40px auto; background:#fff; border-radius:12px;
                              box-shadow:0 4px 20px rgba(0,0,0,0.08); overflow:hidden;">
                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#1a472a,#2d6a4f); padding:36px 40px; text-align:center;">
                      <h1 style="color:#fff; margin:0; font-size:26px; letter-spacing:1px;">⚽ Sport Field Booking</h1>
                      <p style="color:#a8d8b9; margin:8px 0 0; font-size:14px;">Đặt sân bóng nhanh chóng &amp; tiện lợi</p>
                    </div>
                    <!-- Body -->
                    <div style="padding:40px;">
                      <h2 style="color:#1a472a; margin-top:0;">Mật khẩu mới của bạn</h2>
                      <p style="color:#555; line-height:1.7;">
                        Xin chào <strong>%s</strong>,
                      </p>
                      <p style="color:#555; line-height:1.7;">
                        Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
                        Đây là mật khẩu mới:
                      </p>
                      <!-- Password box -->
                      <div style="background:#f0faf4; border:2px solid #2d6a4f; border-radius:8px;
                                  padding:20px; text-align:center; margin:24px 0;">
                        <span style="font-size:28px; font-weight:bold; color:#1a472a; letter-spacing:4px;">%s</span>
                      </div>
                      <p style="color:#555; line-height:1.7;">
                        Vui lòng đăng nhập bằng mật khẩu trên và <strong>đổi ngay sang mật khẩu mới</strong>
                        để bảo vệ tài khoản của bạn.
                      </p>
                      <div style="text-align:center; margin:32px 0;">
                        <a href="#" style="background:#2d6a4f; color:#fff; text-decoration:none;
                                          padding:14px 36px; border-radius:8px; font-size:16px; font-weight:bold;">
                          Đăng nhập ngay
                        </a>
                      </div>
                      <p style="color:#999; font-size:13px; line-height:1.6;">
                        Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                        Tài khoản của bạn vẫn an toàn.
                      </p>
                    </div>
                    <!-- Footer -->
                    <div style="background:#f8f9fa; padding:20px 40px; text-align:center;
                                border-top:1px solid #eee;">
                      <p style="color:#aaa; font-size:12px; margin:0;">
                        © 2025 Sport Field Booking. Mọi quyền được bảo lưu.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fullName, newPassword);
    }

    /**
     * Tạo nội dung HTML email mã OTP.
     */
    public static String buildOtpEmail(String fullName, String otpCode) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; margin:0; padding:0;">
                  <div style="max-width:560px; margin:40px auto; background:#fff; border-radius:12px;
                              box-shadow:0 4px 20px rgba(0,0,0,0.08); overflow:hidden;">
                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#1a472a,#2d6a4f); padding:36px 40px; text-align:center;">
                      <h1 style="color:#fff; margin:0; font-size:26px; letter-spacing:1px;">⚽ Sport Field Booking</h1>
                    </div>
                    <!-- Body -->
                    <div style="padding:40px;">
                      <h2 style="color:#1a472a; margin-top:0;">Mã OTP Đặt Lại Mật Khẩu</h2>
                      <p style="color:#555; line-height:1.7;">
                        Xin chào <strong>%s</strong>,
                      </p>
                      <p style="color:#555; line-height:1.7;">
                        Bạn vừa yêu cầu đặt lại mật khẩu. Vui lòng sử dụng mã OTP dưới đây để tiếp tục.
                        Mã này có hiệu lực trong vòng 5 phút.
                      </p>
                      <!-- OTP box -->
                      <div style="background:#f0faf4; border:2px dashed #2d6a4f; border-radius:8px;
                                  padding:20px; text-align:center; margin:24px 0;">
                        <span style="font-size:32px; font-weight:bold; color:#1a472a; letter-spacing:8px;">%s</span>
                      </div>
                      <p style="color:#999; font-size:13px; line-height:1.6;">
                        Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                        Không chia sẻ mã OTP này cho bất kỳ ai.
                      </p>
                    </div>
                    <!-- Footer -->
                    <div style="background:#f8f9fa; padding:20px 40px; text-align:center;
                                border-top:1px solid #eee;">
                      <p style="color:#aaa; font-size:12px; margin:0;">
                        © 2025 Sport Field Booking. Mọi quyền được bảo lưu.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fullName, otpCode);
    }
    /**
     * Tạo nội dung HTML email mã OTP cho Đăng ký tài khoản.
     */
    public static String buildRegistrationOtpEmail(String fullName, String otpCode) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; margin:0; padding:0;">
                  <div style="max-width:560px; margin:40px auto; background:#fff; border-radius:12px;
                              box-shadow:0 4px 20px rgba(0,0,0,0.08); overflow:hidden;">
                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#1a472a,#2d6a4f); padding:36px 40px; text-align:center;">
                      <h1 style="color:#fff; margin:0; font-size:26px; letter-spacing:1px;">⚽ Sport Field Booking</h1>
                    </div>
                    <!-- Body -->
                    <div style="padding:40px;">
                      <h2 style="color:#1a472a; margin-top:0;">Xác thực tài khoản mới</h2>
                      <p style="color:#555; line-height:1.7;">
                        Xin chào <strong>%s</strong>,
                      </p>
                      <p style="color:#555; line-height:1.7;">
                        Cảm ơn bạn đã đăng ký tài khoản. Vui lòng sử dụng mã OTP dưới đây để xác thực email của bạn.
                        Mã này có hiệu lực trong vòng 5 phút.
                      </p>
                      <!-- OTP box -->
                      <div style="background:#f0faf4; border:2px dashed #2d6a4f; border-radius:8px;
                                  padding:20px; text-align:center; margin:24px 0;">
                        <span style="font-size:32px; font-weight:bold; color:#1a472a; letter-spacing:8px;">%s</span>
                      </div>
                      <p style="color:#999; font-size:13px; line-height:1.6;">
                        Nếu bạn không yêu cầu tạo tài khoản, vui lòng bỏ qua email này.
                        Không chia sẻ mã OTP này cho bất kỳ ai.
                      </p>
                    </div>
                    <!-- Footer -->
                    <div style="background:#f8f9fa; padding:20px 40px; text-align:center;
                                border-top:1px solid #eee;">
                      <p style="color:#aaa; font-size:12px; margin:0;">
                        © 2025 Sport Field Booking. Mọi quyền được bảo lưu.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fullName, otpCode);
    }
}
