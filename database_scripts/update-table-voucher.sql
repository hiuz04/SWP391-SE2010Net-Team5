ALTER TABLE vouchers
    ADD target_user VARCHAR(20) NOT NULL DEFAULT 'ALL'
        CONSTRAINT CK_Voucher_TargetUser
            CHECK (target_user IN ('ALL', 'MEMBER'));

ALTER TABLE vouchers
    ADD exchange_points INT NOT NULL DEFAULT 0;

INSERT INTO vouchers
(code, name, discount_type, discount_value, min_order, quantity, used,
 exchange_points, start_date, end_date, status, created_at, updated_at, target_user)
VALUES

-- Voucher dành cho tất cả
('WELCOME10', N'Giảm 10% cho mọi khách hàng', 'PERCENT', 10, 200000, 500, 35,
 500, '2026-07-01', '2026-12-31', 'ACTIVE', GETDATE(), NULL, 'ALL'),

('SAVE50K', N'Giảm 50.000đ', 'FIXED', 50000, 500000, 300, 18,
 800, '2026-07-01', '2026-10-31', 'ACTIVE', GETDATE(), NULL, 'ALL'),

('SUMMER15', N'Khuyến mãi hè giảm 15%', 'PERCENT', 15, 300000, 200, 72,
 1000, '2026-07-15', '2026-08-31', 'ACTIVE', GETDATE(), NULL, 'ALL'),

-- Voucher dành cho hội viên
('MEMBER10', N'Hội viên giảm 10%', 'PERCENT', 10, 150000, 300, 56,
 400, '2026-07-01', '2026-12-31', 'ACTIVE', GETDATE(), NULL, 'MEMBER'),

('MEMBER20', N'Hội viên giảm 20%', 'PERCENT', 20, 500000, 150, 41,
 1200, '2026-07-01', '2026-09-30', 'ACTIVE', GETDATE(), NULL, 'MEMBER'),

('VIP100K', N'Hội viên giảm 100.000đ', 'FIXED', 100000, 1000000, 80, 15,
 1800, '2026-07-01', '2026-12-31', 'ACTIVE', GETDATE(), NULL, 'MEMBER'),

('MEMBER200', N'Hội viên giảm 200.000đ', 'FIXED', 200000, 1500000, 50, 5,
 2500, '2026-07-10', '2026-08-31', 'ACTIVE', GETDATE(), NULL, 'MEMBER');