-- ===============================================================
-- Script: Cho phép booking_id nhận giá trị NULL trong bảng payments
-- Mục đích: Để hỗ trợ các loại giao dịch không gắn liền với 
--           booking cụ thể (như mua gói MEMBERSHIP).
-- Xoá các constraint (nếu có) trước khi alter column. Thông thường 
-- FOREIGN KEY sẽ không cản trở việc đổi thành NULL, 
-- nhưng nếu có DEFAULT hoặc UNIQUE constraint thì sẽ cần xử lý.
-- Với khoá ngoại, nếu có vấn đề bạn hãy thử xóa khoá ngoại,
-- alter column, sau đó tạo lại khoá ngoại.

-- Lệnh chuyển booking_id thành NULLable
ALTER TABLE payments
ALTER COLUMN booking_id BIGINT NULL;
GO
