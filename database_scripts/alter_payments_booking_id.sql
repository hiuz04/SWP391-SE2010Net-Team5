-- ===============================================================
-- Script: Allow payments.booking_id to be NULL
-- Purpose: Membership payments are not attached to a booking.
-- Safe to run multiple times.
-- ===============================================================

USE FootballBookingSystem;
GO

SET NOCOUNT ON;

IF OBJECT_ID(N'dbo.payments', N'U') IS NULL
BEGIN
    THROW 50000, 'Table dbo.payments does not exist.', 1;
END;

IF COL_LENGTH(N'dbo.payments', N'booking_id') IS NULL
BEGIN
    THROW 50001, 'Column dbo.payments.booking_id does not exist.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.payments')
      AND name = N'booking_id'
      AND is_nullable = 0
)
BEGIN
    ALTER TABLE dbo.payments
    ALTER COLUMN booking_id BIGINT NULL;

    PRINT 'Updated dbo.payments.booking_id to allow NULL.';
END
ELSE
BEGIN
    PRINT 'dbo.payments.booking_id already allows NULL.';
END;
GO
