-- Chạy file này để thêm các attribute mới cho users và bảng membership_tier và point_history

IF COL_LENGTH('users', 'membership_tier_id') IS NULL
BEGIN
ALTER TABLE users
    ADD
        membership_tier_id INT NULL,
        available_reward_points INT NOT NULL DEFAULT 0,
        total_spending DECIMAL(18,2) NOT NULL DEFAULT 0,
        membership_since DATETIME NULL;
END

IF OBJECT_ID('membership_tiers', 'U') IS NULL
BEGIN
CREATE TABLE membership_tiers (
  tier_id INT IDENTITY(1,1) PRIMARY KEY,
  tier_name NVARCHAR(50) NOT NULL UNIQUE,
  min_spending DECIMAL(18,2) NOT NULL,
  point_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.00,
  description NVARCHAR(255)
);
END

IF NOT EXISTS (SELECT 1 FROM membership_tiers)
BEGIN
INSERT INTO membership_tiers
(
    tier_name,
    min_spending,
    point_multiplier,
    description
)
VALUES
    ('Member', 0, 1.00, N'Hội viên cấp thường'),
    ('Silver', 2750000, 1.20, N'Hội viên cấp bạc'),
    ('Gold', 15000000, 1.50, N'Hội viên cấp vàng'),
    ('Platinum', 30000000, 2.00, N'Hội viên cấp bạch kim');
END

IF NOT EXISTS (
    SELECT *
    FROM sys.foreign_keys
    WHERE name = 'FK_USERS_MEMBERSHIPTIER'
)
BEGIN
ALTER TABLE users
    ADD CONSTRAINT FK_Users_MembershipTier
        FOREIGN KEY (membership_tier_id)
            REFERENCES membership_tiers(tier_id);
END

UPDATE users
SET
    membership_tier_id = ISNULL(membership_tier_id, 1),
    membership_since = ISNULL(membership_since, GETDATE());

UPDATE u
SET
    u.total_spending = ISNULL(b.total_spending, 0),
    u.available_reward_points = CAST(ISNULL(b.total_spending, 0) / 1000 AS INT)
    FROM users u
LEFT JOIN (
    SELECT
        customer_id,
        SUM(total_amount) AS total_spending
    FROM bookings
    WHERE status = 'Completed'
    GROUP BY customer_id
) b
ON u.user_id = b.customer_id;

UPDATE u
SET membership_tier_id =
(
    SELECT TOP 1 tier_id
    FROM membership_tiers mt
    WHERE mt.min_spending <= u.total_spending
    ORDER BY mt.min_spending DESC
)
FROM users u;

IF OBJECT_ID('point_history', 'U') IS NULL
BEGIN
CREATE TABLE point_history (
   history_id BIGINT IDENTITY(1,1) PRIMARY KEY,
   user_id BIGINT NOT NULL,
   booking_id BIGINT NULL,
   points INT NOT NULL,
   transaction_type NVARCHAR(20) NOT NULL,
   description NVARCHAR(255),
   created_at DATETIME NOT NULL DEFAULT GETDATE(),

   CONSTRAINT FK_PointHistory_User
       FOREIGN KEY (user_id)
           REFERENCES users(user_id)
);
END

IF NOT EXISTS (SELECT 1 FROM point_history)
BEGIN
INSERT INTO point_history
(
    user_id,
    booking_id,
    points,
    transaction_type,
    description,
    created_at
)
SELECT
    b.customer_id,
    b.booking_id,
    CAST(b.total_amount / 1000 AS INT),
    'EARN',
    CONCAT('Earned reward points from completed booking #', b.booking_id),
    ISNULL(b.updated_at, GETDATE())
FROM bookings b
WHERE b.status = 'Completed';
END