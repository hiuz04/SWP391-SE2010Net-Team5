-- SQL Script to seed matchmaking (tìm đối) test data
-- Rerun-safe (idempotent) script

-- 1. CLEAN UP PREVIOUS MATCHMAKING DATA
IF OBJECT_ID('dbo.matchmaking_post_responses', 'U') IS NOT NULL
BEGIN
    DELETE FROM dbo.matchmaking_post_responses;
END
GO

IF OBJECT_ID('dbo.matchmaking_posts', 'U') IS NOT NULL
BEGIN
    DELETE FROM dbo.matchmaking_posts;
END
GO

-- 2. ENSURE PREREQUISITE DATA EXISTS (USERS & COMPLEX)
-- Ensure 'roles' table has Customer role (ID = 4)
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE role_id = 4)
BEGIN
    SET IDENTITY_INSERT dbo.roles ON;
    INSERT INTO dbo.roles (role_id, role_name) VALUES (4, 'CUSTOMER');
    SET IDENTITY_INSERT dbo.roles OFF;
END
GO

-- Ensure test users exist (user_id = 6, 7, 8, 9)
-- Check and insert User ID 6 (Customer - Nguyễn Văn Khánh)
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE user_id = 6)
BEGIN
    SET IDENTITY_INSERT dbo.users ON;
    INSERT INTO dbo.users (user_id, role_id, full_name, email, phone, password_hash, status, created_at, updated_at)
    VALUES (6, 4, N'Nguyễn Văn Khánh', 'khanh.customer@gmail.com', '0912345678', '$2a$10$R9hZaf.U0R4z2tX.7XzFxe2d9X6UfpxC7e8Z3o7f2E2h6h3S0D3oK', 'ACTIVE', GETDATE(), GETDATE());
    SET IDENTITY_INSERT dbo.users OFF;
END
GO

-- Check and insert User ID 7 (Customer - Trần Anh Tú)
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE user_id = 7)
BEGIN
    SET IDENTITY_INSERT dbo.users ON;
    INSERT INTO dbo.users (user_id, role_id, full_name, email, phone, password_hash, status, created_at, updated_at)
    VALUES (7, 4, N'Trần Anh Tú', 'tu.customer@gmail.com', '0987654321', '$2a$10$R9hZaf.U0R4z2tX.7XzFxe2d9X6UfpxC7e8Z3o7f2E2h6h3S0D3oK', 'ACTIVE', GETDATE(), GETDATE());
    SET IDENTITY_INSERT dbo.users OFF;
END
GO

-- Check and insert User ID 8 (Customer - Lê Hoàng Nam)
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE user_id = 8)
BEGIN
    SET IDENTITY_INSERT dbo.users ON;
    INSERT INTO dbo.users (user_id, role_id, full_name, email, phone, password_hash, status, created_at, updated_at)
    VALUES (8, 4, N'Lê Hoàng Nam', 'nam.customer@gmail.com', '0901234567', '$2a$10$R9hZaf.U0R4z2tX.7XzFxe2d9X6UfpxC7e8Z3o7f2E2h6h3S0D3oK', 'ACTIVE', GETDATE(), GETDATE());
    SET IDENTITY_INSERT dbo.users OFF;
END
GO

-- Check and insert User ID 9 (Customer - Phạm Minh Đức)
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE user_id = 9)
BEGIN
    SET IDENTITY_INSERT dbo.users ON;
    INSERT INTO dbo.users (user_id, role_id, full_name, email, phone, password_hash, status, created_at, updated_at)
    VALUES (9, 4, N'Phạm Minh Đức', 'duc.customer@gmail.com', '0934567890', '$2a$10$R9hZaf.U0R4z2tX.7XzFxe2d9X6UfpxC7e8Z3o7f2E2h6h3S0D3oK', 'ACTIVE', GETDATE(), GETDATE());
    SET IDENTITY_INSERT dbo.users OFF;
END
GO

-- Ensure at least one football complex exists (complex_id = 1)
IF NOT EXISTS (SELECT 1 FROM dbo.football_complexes WHERE complex_id = 1)
BEGIN
    SET IDENTITY_INSERT dbo.football_complexes ON;
    INSERT INTO dbo.football_complexes (
        complex_id, complex_name, description, address, ward, district, city, 
        latitude, longitude, hotline, opening_time, closing_time, general_rules, 
        status, featured, created_at, updated_at
    ) VALUES (
        1, N'Sân bóng đá Mỹ Đình', N'Hệ thống sân cỏ nhân tạo tiêu chuẩn quốc tế', 
        N'Số 2 Lê Đức Thọ', N'Mỹ Đình 1', N'Nam Từ Liêm', N'Hà Nội', 
        21.0205, 105.7739, '0981234567', '05:00:00', '22:00:00', 
        N'Đi giày đinh dăm, giữ vệ sinh chung', 'ACTIVE', 1, GETDATE(), GETDATE()
    );
    SET IDENTITY_INSERT dbo.football_complexes OFF;
END
GO

-- Ensure a second football complex exists (complex_id = 2) for filter variety
IF NOT EXISTS (SELECT 1 FROM dbo.football_complexes WHERE complex_id = 2)
BEGIN
    SET IDENTITY_INSERT dbo.football_complexes ON;
    INSERT INTO dbo.football_complexes (
        complex_id, complex_name, description, address, ward, district, city, 
        latitude, longitude, hotline, opening_time, closing_time, general_rules, 
        status, featured, created_at, updated_at
    ) VALUES (
        2, N'Sân bóng Cầu Giấy', N'Sân bóng văn phòng trung tâm Cầu Giấy', 
        N'Số 10 Dịch Vọng Hậu', N'Dịch Vọng Hậu', N'Cầu Giấy', N'Hà Nội', 
        21.0278, 105.7889, '0987654321', '06:00:00', '22:00:00', 
        N'Không hút thuốc tại khu vực sân cỏ', 'ACTIVE', 0, GETDATE(), GETDATE()
    );
    SET IDENTITY_INSERT dbo.football_complexes OFF;
END
GO


-- 3. SEED MATCHMAKING POSTS (BÀI ĐĂNG TÌM ĐỐI / ĐỒNG ĐỘI)
-- Temporary table to hold generated posts and map their IDs
CREATE TABLE #TempPostMap (
    post_index INT,
    post_id BIGINT
);

-- Post 1: Open Find Opponent, Intermediate level, expected in 2 days
INSERT INTO dbo.matchmaking_posts (
    author_id, post_type, title, description, skill_level, expected_time, 
    complex_id, contact_name, contact_phone, status, created_at, updated_at
) VALUES (
    6, 'FIND_OPPONENT', 
    N'Tìm đối mềm giao lưu tối mai - cá nhẹ nước nôi sân Mỹ Đình', 
    N'Đội văn phòng độ tuổi 25-30, trình độ trung bình (Intermediate) muốn tìm đối giao lưu vui vẻ. Đá sân 7, có chia tiền sân nước nôi.', 
    'INTERMEDIATE', DATEADD(day, 1, GETDATE()), 1, N'Khánh Nguyễn', '0912345678', 'OPEN', GETDATE(), GETDATE()
);
INSERT INTO #TempPostMap VALUES (1, SCOPE_IDENTITY());

-- Post 2: Open Find Teammate, Advanced level, expected in 3 days
INSERT INTO dbo.matchmaking_posts (
    author_id, post_type, title, description, skill_level, expected_time, 
    complex_id, contact_name, contact_phone, status, created_at, updated_at
) VALUES (
    7, 'FIND_TEAMMATE', 
    N'Tuyển thêm 2 chân chạy cánh đá tối thứ 7 tuần này', 
    N'Đội chuyên đá các giải phủi cần tuyển thêm 2 chân chạy cánh/tiền vệ cánh thể lực tốt, đá nhiệt tình, có trách nhiệm. Trình độ khá trở lên.', 
    'ADVANCED', DATEADD(day, 2, GETDATE()), 1, N'Anh Tú', '0987654321', 'OPEN', GETDATE(), GETDATE()
);
INSERT INTO #TempPostMap VALUES (2, SCOPE_IDENTITY());

-- Post 3: Open Find Opponent, Beginner level, expected in 5 days
INSERT INTO dbo.matchmaking_posts (
    author_id, post_type, title, description, skill_level, expected_time, 
    complex_id, contact_name, contact_phone, status, created_at, updated_at
) VALUES (
    8, 'FIND_OPPONENT', 
    N'Giao lưu dưỡng sinh vui vẻ chiều chủ nhật', 
    N'Đội mới lập, đa số anh em lâu ngày không vận động, đá dưỡng sinh giao lưu nhẹ nhàng. Không cá độ ăn thua, tìm đối cùng trình độ.', 
    'BEGINNER', DATEADD(day, 4, GETDATE()), 2, N'Nam Lê', '0901234567', 'OPEN', GETDATE(), GETDATE()
);
INSERT INTO #TempPostMap VALUES (3, SCOPE_IDENTITY());

-- Post 4: Closed/Expired Find Opponent, Intermediate level, past expected time
INSERT INTO dbo.matchmaking_posts (
    author_id, post_type, title, description, skill_level, expected_time, 
    complex_id, contact_name, contact_phone, status, created_at, updated_at
) VALUES (
    9, 'FIND_OPPONENT', 
    N'[ĐÃ ĐÁ] Cần tìm đối giao hữu khẩn cấp chiều hôm qua', 
    N'Tìm đối giao hữu khẩn cấp lúc 18h00 hôm qua, đá vui vẻ chia sân 5/5.', 
    'INTERMEDIATE', DATEADD(day, -1, GETDATE()), 1, N'Đức Phạm', '0934567890', 'CLOSED', DATEADD(day, -2, GETDATE()), DATEADD(day, -2, GETDATE())
);
INSERT INTO #TempPostMap VALUES (4, SCOPE_IDENTITY());


-- 4. SEED MATCHMAKING POST RESPONSES (PHẢN HỒI / LỜI NHẮN)
-- Responses for Post 1 (by User 6)
DECLARE @Post1Id BIGINT = (SELECT post_id FROM #TempPostMap WHERE post_index = 1);
IF @Post1Id IS NOT NULL
BEGIN
    INSERT INTO dbo.matchmaking_post_responses (post_id, responder_id, message, status, created_at)
    VALUES (@Post1Id, 7, N'Team mình trình độ trung bình vừa tầm với bên bạn nhé. Có thể đá tối mai lúc 19h00 được không?', 'PENDING', GETDATE());

    INSERT INTO dbo.matchmaking_post_responses (post_id, responder_id, message, status, created_at)
    VALUES (@Post1Id, 8, N'Bên mình đá 7/7 cũng hay chơi ở sân Mỹ Đình. Có gì liên hệ mình qua Zalo nhé.', 'PENDING', GETDATE());
END

-- Responses for Post 2 (by User 7)
DECLARE @Post2Id BIGINT = (SELECT post_id FROM #TempPostMap WHERE post_index = 2);
IF @Post2Id IS NOT NULL
BEGIN
    INSERT INTO dbo.matchmaking_post_responses (post_id, responder_id, message, status, created_at)
    VALUES (@Post2Id, 6, N'Mình vị trí tiền đạo/cánh phải, thể lực khá tốt, muốn xin tham gia cọ xát với đội.', 'PENDING', GETDATE());
END

-- Responses for Post 4 (by User 9 - Closed Post)
DECLARE @Post4Id BIGINT = (SELECT post_id FROM #TempPostMap WHERE post_index = 4);
IF @Post4Id IS NOT NULL
BEGIN
    INSERT INTO dbo.matchmaking_post_responses (post_id, responder_id, message, status, created_at)
    VALUES (@Post4Id, 6, N'Kèo này bên mình đá xong hôm qua rồi nhé, giao lưu rất vui!', 'PENDING', DATEADD(day, -1, GETDATE()));
END

-- Drop temporary mapping table
DROP TABLE #TempPostMap;
GO

PRINT 'Successfully seeded matchmaking (tìm đối) test data!';
