-- SQL Script to seed test shifts and bookings for Night Shift operations
-- Target Date: 2026-07-24 (Today)
-- Night shift slot: 10:00 PM yesterday to 6:00 AM today (specifically active at 1:00 AM right now)

-- 1. CLEAN UP PREVIOUS TEST DATA FOR TARGET DATE OVERNIGHT HOURS
DELETE FROM dbo.shift_assignments 
WHERE shift_id IN (SELECT shift_id FROM dbo.work_shifts WHERE shift_date = '2026-07-24' AND start_time = '00:00:00');

DELETE FROM dbo.work_shifts WHERE shift_date = '2026-07-24' AND start_time = '00:00:00';

-- Temporary table to hold target booking IDs
IF OBJECT_ID('tempdb..#TargetBookings') IS NOT NULL DROP TABLE #TargetBookings;
SELECT booking_id INTO #TargetBookings 
FROM dbo.bookings 
WHERE CAST(start_time AS DATE) = '2026-07-24' 
  AND CAST(start_time AS TIME) BETWEEN '00:00:00' AND '06:00:00';

-- Delete from referencing tables first to satisfy foreign key constraints
DELETE FROM dbo.payments WHERE booking_id IN (SELECT booking_id FROM #TargetBookings);
DELETE FROM dbo.invoices WHERE booking_id IN (SELECT booking_id FROM #TargetBookings);
DELETE FROM dbo.checkins WHERE booking_id IN (SELECT booking_id FROM #TargetBookings);
DELETE FROM dbo.booking_status_logs WHERE booking_id IN (SELECT booking_id FROM #TargetBookings);
DELETE FROM dbo.booking_promotions WHERE booking_id IN (SELECT booking_id FROM #TargetBookings);
DELETE FROM dbo.voucher_usages WHERE booking_id IN (SELECT booking_id FROM #TargetBookings);
DELETE FROM dbo.feedbacks WHERE booking_id IN (SELECT booking_id FROM #TargetBookings);

-- Delete from main bookings table
DELETE FROM dbo.bookings WHERE booking_id IN (SELECT booking_id FROM #TargetBookings);

DROP TABLE #TargetBookings;

GO

-- 2. SEED ACTIVE NIGHT SHIFT (00:00:00 to 08:00:00 on July 24, 2026)
-- Sân Mỹ Đình Arena (complex_id = 1)
INSERT INTO dbo.work_shifts (complex_id, shift_name, shift_date, start_time, end_time, created_at)
VALUES 
(1, N'Ca Đêm Mỹ Đình (Overnight)', '2026-07-24', '00:00:00', '08:00:00', GETDATE());

GO

-- Assign night shift to staff.hanoi1 (user_id = 4)
DECLARE @NightShiftID INT = (SELECT shift_id FROM dbo.work_shifts WHERE complex_id = 1 AND shift_name = N'Ca Đêm Mỹ Đình (Overnight)' AND shift_date = '2026-07-24');
INSERT INTO dbo.shift_assignments (shift_id, staff_id, status) 
VALUES (@NightShiftID, 4, 'ASSIGNED');

GO

-- 3. SEED BOOKINGS FOR THE OVERNIGHT HOUR TESTING (July 24, 2026 between 00:00 and 06:00)
-- Booking 1: Confirmed/Awaiting Check-in (Active at 1:00 AM right now)
INSERT INTO dbo.bookings (booking_code, customer_id, complex_id, field_id, start_time, end_time, original_price, discount_amount, total_amount, deposit_amount, status, created_at, updated_at, final_amount)
VALUES 
('BK2607240001', 6, 1, 1, '2026-07-24 01:00:00', '2026-07-24 02:30:00', 450000.00, 0.00, 450000.00, 150000.00, 'CONFIRMED', GETDATE(), GETDATE(), 450000.00);

-- Booking 2: Checked-in/Currently Playing (Ready for Checkout test)
INSERT INTO dbo.bookings (booking_code, customer_id, complex_id, field_id, start_time, end_time, original_price, discount_amount, total_amount, deposit_amount, status, created_at, updated_at, final_amount)
VALUES 
('BK2607240002', 7, 1, 2, '2026-07-24 00:30:00', '2026-07-24 02:00:00', 450000.00, 50000.00, 400000.00, 200000.00, 'CHECKED_IN', GETDATE(), GETDATE(), 400000.00);

-- Booking 3: Confirmed/Awaiting Check-in (Starts later at 2:00 AM)
INSERT INTO dbo.bookings (booking_code, customer_id, complex_id, field_id, start_time, end_time, original_price, discount_amount, total_amount, deposit_amount, status, created_at, updated_at, final_amount)
VALUES 
('BK2607240003', 8, 1, 3, '2026-07-24 02:00:00', '2026-07-24 03:30:00', 450000.00, 0.00, 450000.00, 150000.00, 'CONFIRMED', GETDATE(), GETDATE(), 450000.00);

-- Booking 4: Confirmed/Awaiting Check-in (Late check-in window testing)
INSERT INTO dbo.bookings (booking_code, customer_id, complex_id, field_id, start_time, end_time, original_price, discount_amount, total_amount, deposit_amount, status, created_at, updated_at, final_amount)
VALUES 
('BK2607240004', 9, 1, 2, '2026-07-24 00:00:00', '2026-07-24 01:30:00', 450000.00, 0.00, 450000.00, 150000.00, 'CONFIRMED', GETDATE(), GETDATE(), 450000.00);

GO

PRINT 'Night Shift and Overnight Bookings for 2026-07-24 seeded successfully!';
