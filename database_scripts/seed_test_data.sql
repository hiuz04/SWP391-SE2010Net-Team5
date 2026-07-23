-- SQL Script to seed comprehensive test data for Staff Operations and Customer Matchmaking
-- Target Date: 2026-07-23 (Today)

-- 1. CLEAN UP EXISTING TEST DATA FOR TARGET DATE TO PREVENT OVERLAPS/DUPLICATES
-- Delete shift assignments for target date shifts
DELETE FROM dbo.shift_assignments 
WHERE shift_id IN (SELECT shift_id FROM dbo.work_shifts WHERE shift_date = '2026-07-23');

-- Delete work shifts for target date
DELETE FROM dbo.work_shifts WHERE shift_date = '2026-07-23';

-- Delete matchmaking post responses
DELETE FROM dbo.matchmaking_post_responses;

-- Delete matchmaking posts
DELETE FROM dbo.matchmaking_posts;

-- Delete bookings for target date
DELETE FROM dbo.bookings WHERE CAST(start_time AS DATE) = '2026-07-23';

GO

-- 2. SEED WORK SHIFTS & ASSIGNMENTS FOR STAFF (Target Date: 2026-07-23)
-- Insert shifts for Complex 1 (Mỹ Đình Arena)
INSERT INTO dbo.work_shifts (complex_id, shift_name, shift_date, start_time, end_time, created_at)
VALUES 
(1, N'Ca Sáng Mỹ Đình', '2026-07-23', '08:00:00', '12:00:00', GETDATE()),
(1, N'Ca Chiều Mỹ Đình', '2026-07-23', '12:00:00', '18:00:00', GETDATE()),
(1, N'Ca Tối Mỹ Đình', '2026-07-23', '18:00:00', '22:00:00', GETDATE());

-- Insert shifts for Complex 2 (Cầu Giấy Sport)
INSERT INTO dbo.work_shifts (complex_id, shift_name, shift_date, start_time, end_time, created_at)
VALUES 
(2, N'Ca Gay Cầu Giấy', '2026-07-23', '12:00:00', '22:00:00', GETDATE());

GO

-- Assign shifts to staff
-- Staff Hanoi 1 (user_id = 4) assigned to Ca Sáng & Ca Chiều Mỹ Đình
DECLARE @ShiftMorningID INT = (SELECT shift_id FROM dbo.work_shifts WHERE complex_id = 1 AND shift_name = N'Ca Sáng Mỹ Đình' AND shift_date = '2026-07-23');
DECLARE @ShiftAfternoonID INT = (SELECT shift_id FROM dbo.work_shifts WHERE complex_id = 1 AND shift_name = N'Ca Chiều Mỹ Đình' AND shift_date = '2026-07-23');
DECLARE @ShiftEveningID INT = (SELECT shift_id FROM dbo.work_shifts WHERE complex_id = 1 AND shift_name = N'Ca Tối Mỹ Đình' AND shift_date = '2026-07-23');
DECLARE @ShiftCauGiayID INT = (SELECT shift_id FROM dbo.work_shifts WHERE complex_id = 2 AND shift_name = N'Ca Gay Cầu Giấy' AND shift_date = '2026-07-23');

INSERT INTO dbo.shift_assignments (shift_id, staff_id, status) VALUES 
(@ShiftMorningID, 4, 'ASSIGNED'),
(@ShiftAfternoonID, 4, 'ASSIGNED'),
(@ShiftEveningID, 5, 'ASSIGNED'),
(@ShiftCauGiayID, 5, 'ASSIGNED');

GO

-- 3. SEED BOOKINGS FOR TARGET DATE (2026-07-23)
-- Customer 6 (Nguyễn Minh Quân), Customer 7 (Trần Thu Huyền), Customer 8 (Lê Hoàng Nam), Customer 9 (Phạm Ngọc Anh), Customer 11 (Mạnh Nguyễn)
-- Complex 1 (Mỹ Đình Arena) has fields 1, 2, 3

-- Completed bookings (Morning & early afternoon)
INSERT INTO dbo.bookings (booking_code, customer_id, complex_id, field_id, start_time, end_time, original_price, discount_amount, total_amount, deposit_amount, status, created_at, updated_at, final_amount)
VALUES 
('BK2607230001', 6, 1, 1, '2026-07-23 08:30:00', '2026-07-23 10:00:00', 300000.00, 0.00, 300000.00, 100000.00, 'COMPLETED', GETDATE(), GETDATE(), 300000.00),
('BK2607230002', 7, 1, 2, '2026-07-23 09:00:00', '2026-07-23 10:30:00', 300000.00, 50000.00, 250000.00, 100000.00, 'COMPLETED', GETDATE(), GETDATE(), 250000.00),
('BK2607230003', 8, 1, 3, '2026-07-23 10:30:00', '2026-07-23 12:00:00', 300000.00, 0.00, 300000.00, 100000.00, 'COMPLETED', GETDATE(), GETDATE(), 300000.00),
('BK2607230004', 9, 1, 1, '2026-07-23 13:00:00', '2026-07-23 14:30:00', 350000.00, 0.00, 350000.00, 150000.00, 'COMPLETED', GETDATE(), GETDATE(), 350000.00);

-- Currently playing (CHECKED_IN)
INSERT INTO dbo.bookings (booking_code, customer_id, complex_id, field_id, start_time, end_time, original_price, discount_amount, total_amount, deposit_amount, status, created_at, updated_at, final_amount)
VALUES 
('BK2607230005', 11, 1, 2, '2026-07-23 15:00:00', '2026-07-23 16:30:00', 350000.00, 0.00, 350000.00, 150000.00, 'CHECKED_IN', GETDATE(), GETDATE(), 350000.00),
('BK2607230006', 6, 1, 3, '2026-07-23 15:30:00', '2026-07-23 17:00:00', 350000.00, 0.00, 350000.00, 150000.00, 'CHECKED_IN', GETDATE(), GETDATE(), 350000.00);

-- Awaiting check-in (CONFIRMED) - For testing Staff Check-in Flow
INSERT INTO dbo.bookings (booking_code, customer_id, complex_id, field_id, start_time, end_time, original_price, discount_amount, total_amount, deposit_amount, status, created_at, updated_at, final_amount)
VALUES 
('BK2607230007', 7, 1, 1, '2026-07-23 17:00:00', '2026-07-23 18:30:00', 400000.00, 0.00, 400000.00, 150000.00, 'CONFIRMED', GETDATE(), GETDATE(), 400000.00),
('BK2607230008', 8, 1, 2, '2026-07-23 17:30:00', '2026-07-23 19:00:00', 400000.00, 0.00, 400000.00, 150000.00, 'CONFIRMED', GETDATE(), GETDATE(), 400000.00),
('BK2607230009', 9, 1, 3, '2026-07-23 18:30:00', '2026-07-23 20:00:00', 400000.00, 0.00, 400000.00, 150000.00, 'CONFIRMED', GETDATE(), GETDATE(), 400000.00),
('BK2607230010', 11, 1, 1, '2026-07-23 19:00:00', '2026-07-23 20:30:00', 400000.00, 0.00, 400000.00, 150000.00, 'CONFIRMED', GETDATE(), GETDATE(), 400000.00);

GO

-- 4. SEED MATCHMAKING POSTS (Customer Tìm Đối)
-- Seed variety of active posts with status = 'OPEN'
INSERT INTO dbo.matchmaking_posts (author_id, post_type, title, description, skill_level, expected_time, complex_id, contact_name, contact_phone, status, created_at, updated_at)
VALUES 
(6, 'FIND_OPPONENT', N'Cần tìm đối mềm đá sân Mỹ Đình chiều tối nay', N'Đội mình gồm các anh em văn phòng, đá nhẹ nhàng cọ xát dưỡng sinh. Sân đã đặt 17h30 ở Mỹ Đình Arena.', 'BEGINNER', '2026-07-23 17:30:00', 1, N'Anh Quân', '0911000001', 'OPEN', GETDATE(), GETDATE()),
(7, 'FIND_OPPONENT', N'Tìm đối cứng sân Cầu Giấy 19h tối nay', N'FC Cầu Giấy cần đối cứng 7x7 giao hữu học hỏi. Sân 19h00 chia tiền sân nước 50/50.', 'INTERMEDIATE', '2026-07-23 19:00:00', 2, N'Huyền Trần', '0911000002', 'OPEN', GETDATE(), GETDATE()),
(8, 'FIND_TEAMMATE', N'Cần tuyển 2 chân chạy cánh đá sân Đà Nẵng tối mai', N'Đội mình thiếu người chạy cánh cho trận tối mai lúc 18h ở sân Đà Nẵng Futsal Center. Anh em nào tham gia giao lưu nhé.', 'INTERMEDIATE', '2026-07-24 18:00:00', 3, N'Hoàng Nam', '0911000003', 'OPEN', GETDATE(), GETDATE()),
(9, 'FIND_OPPONENT', N'FC Mỹ Đình tìm đối tác giao hữu tối 24/7', N'Tìm các đội đá sân 7 cứng cáp giao lưu cọ sát. Đá fair-play không va chạm mạnh. Liên hệ đặt kèo.', 'ADVANCED', '2026-07-24 20:00:00', 1, N'Ngọc Anh', '0911000004', 'OPEN', GETDATE(), GETDATE()),
(11, 'FIND_TEAMMATE', N'Tuyển gấp thủ môn cho giải đấu nội bộ sân Cầu Giấy', N'FC Phong Trào cần tìm 1 thủ môn bắt chính cho trận đấu chiều thứ 7 lúc 17h30. Có bồi dưỡng nước nôi.', 'BEGINNER', '2026-07-25 17:30:00', 2, N'Mạnh Nguyễn', '0988888888', 'OPEN', GETDATE(), GETDATE()),
(6, 'FIND_OPPONENT', N'Cần tìm đối giao lưu sân Cầu Giấy sáng chủ nhật', N'Kèo sáng chủ nhật mát mẻ dưỡng sinh. Anh em văn phòng vui vẻ là chính.', 'BEGINNER', '2026-07-26 08:30:00', 2, N'Anh Quân', '0911000001', 'OPEN', GETDATE(), GETDATE()),
(8, 'FIND_OPPONENT', N'Tìm đối đá kèo 7x7 sân Mỹ Đình tối thứ 7', N'Cần đối giao hữu 7x7 tối thứ 7 tuần này. Đội hình trung bình khá. Chia đôi chi phí.', 'INTERMEDIATE', '2026-07-25 19:00:00', 1, N'Hoàng Nam', '0911000003', 'OPEN', GETDATE(), GETDATE());

GO

-- 5. SEED MATCHMAKING POST RESPONSES
-- Seed some responses from customers to the open posts
DECLARE @Post1ID INT = (SELECT MIN(post_id) FROM dbo.matchmaking_posts WHERE author_id = 6 AND complex_id = 1);
DECLARE @Post2ID INT = (SELECT MIN(post_id) FROM dbo.matchmaking_posts WHERE author_id = 7 AND complex_id = 2);

INSERT INTO dbo.matchmaking_post_responses (post_id, responder_id, message, status, created_at)
VALUES 
(@Post1ID, 7, N'Team mình cũng toàn anh em văn phòng, cho mình giao lưu trận này nhé!', 'PENDING', GETDATE()),
(@Post1ID, 8, N'Đã inbox bạn nhé, hi vọng được cọ xát.', 'ACCEPTED', GETDATE()),
(@Post2ID, 11, N'Kèo này còn không bạn ơi? Cho mình đăng ký đá đối.', 'PENDING', GETDATE());

GO

-- Print success message
PRINT 'Comprehensive test data for Target Date 2026-07-23 (Shifts, Assignments, Bookings, Matchmaking) seeded successfully!';
