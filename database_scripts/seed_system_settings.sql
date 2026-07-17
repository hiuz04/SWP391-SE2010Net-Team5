-- SQL Script to insert default system settings if they don't exist

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'MAINTENANCE_MODE')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('MAINTENANCE_MODE', 'false', N'Bật/Tắt chế độ bảo trì', GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'CONTACT_EMAIL')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('CONTACT_EMAIL', 'support@sportfield.vn', N'Email hỗ trợ khách hàng', GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'CONTACT_PHONE')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('CONTACT_PHONE', '1900 1234', N'Số điện thoại hỗ trợ', GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'MAX_BOOKING_DAYS_AHEAD')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('MAX_BOOKING_DAYS_AHEAD', '30', N'Số ngày tối đa cho phép đặt sân trước', GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'MIN_CANCELLATION_HOURS')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('MIN_CANCELLATION_HOURS', '24', N'Số giờ tối thiểu cho phép hủy sân không mất phí', GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'PLATFORM_FEE_PERCENTAGE')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('PLATFORM_FEE_PERCENTAGE', '5.0', N'Tỷ lệ phần trăm phí nền tảng thu từ Chủ sân', GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'VIP_SUBSCRIPTION_PRICE_MONTHLY')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('VIP_SUBSCRIPTION_PRICE_MONTHLY', '100000', N'Giá tiền để đăng ký/nâng cấp gói hội viên VIP 1 tháng', GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'VIP_DISCOUNT_PERCENTAGE')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('VIP_DISCOUNT_PERCENTAGE', '5', N'Phần trăm giảm giá dành riêng cho hội viên VIP mỗi khi đặt sân', GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'DEPOSIT_PERCENTAGE')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('DEPOSIT_PERCENTAGE', '30', N'Tỉ lệ phần trăm số tiền khách phải cọc khi đặt sân', GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'BOOKING_HOLD_MINUTES')
BEGIN
    INSERT INTO system_settings (setting_key, setting_value, description, updated_at) 
    VALUES ('BOOKING_HOLD_MINUTES', '15', N'Thời gian chờ khách hàng thanh toán trước khi tự động hủy hóa đơn (phút)', GETDATE());
END
