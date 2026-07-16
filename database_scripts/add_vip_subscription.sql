-- Script to add VIP subscription columns to users table
IF COL_LENGTH('users', 'is_vip') IS NULL
BEGIN
    ALTER TABLE users
    ADD is_vip BIT NOT NULL DEFAULT 0;
END

IF COL_LENGTH('users', 'vip_valid_until') IS NULL
BEGIN
    ALTER TABLE users
    ADD vip_valid_until DATETIME NULL;
END
