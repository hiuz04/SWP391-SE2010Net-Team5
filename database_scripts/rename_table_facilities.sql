-- Rename facilities -> football_complexes
IF OBJECT_ID('dbo.facilities', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.football_complexes', 'U') IS NULL
BEGIN
EXEC sp_rename 'dbo.facilities', 'football_complexes';
END
GO

-- Rename facility_images -> football_complex_images
IF OBJECT_ID('dbo.facility_images', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.football_complex_images', 'U') IS NULL
BEGIN
EXEC sp_rename 'dbo.facility_images', 'football_complex_images';
END
GO

IF COL_LENGTH('dbo.football_complexes', 'facility_id') IS NOT NULL
BEGIN
EXEC sp_rename
        'dbo.football_complexes.facility_id',
        'complex_id',
        'COLUMN';
END
GO

IF COL_LENGTH('dbo.football_complexes', 'facility_name') IS NOT NULL
BEGIN
EXEC sp_rename
        'dbo.football_complexes.facility_name',
        'complex_name',
        'COLUMN';
END
GO

IF COL_LENGTH('dbo.football_complex_images', 'facility_id') IS NOT NULL
BEGIN
EXEC sp_rename
        'dbo.football_complex_images.facility_id',
        'complex_id',
        'COLUMN';
END
GO

-- price_rules
IF COL_LENGTH('dbo.price_rules', 'facility_id') IS NOT NULL
BEGIN
EXEC sp_rename
        'dbo.price_rules.facility_id',
        'complex_id',
        'COLUMN';
END
GO

-- bookings
IF COL_LENGTH('dbo.bookings', 'facility_id') IS NOT NULL
BEGIN
EXEC sp_rename
        'dbo.bookings.facility_id',
        'complex_id',
        'COLUMN';
END
GO

-- feedbacks
IF COL_LENGTH('dbo.feedbacks', 'facility_id') IS NOT NULL
BEGIN
EXEC sp_rename
        'dbo.feedbacks.facility_id',
        'complex_id',
        'COLUMN';
END
GO

-- fields
IF COL_LENGTH('dbo.fields', 'facility_id') IS NOT NULL
BEGIN
EXEC sp_rename
        'dbo.fields.facility_id',
        'complex_id',
        'COLUMN';
END
GO

-- matchmaking_posts
IF COL_LENGTH('dbo.matchmaking_posts', 'facility_id') IS NOT NULL
BEGIN
EXEC sp_rename
        'dbo.matchmaking_posts.facility_id',
        'complex_id',
        'COLUMN';
END
GO

-- work_shifts
IF COL_LENGTH('dbo.work_shifts', 'facility_id') IS NOT NULL
BEGIN
EXEC sp_rename
        'dbo.work_shifts.facility_id',
        'complex_id',
        'COLUMN';
END
GO

EXEC sp_rename 'FK_BOOKING_FACILITY', 'FK_BOOKING_COMPLEX', 'OBJECT';
GO

EXEC sp_rename 'FK_FACILITY_IMAGE', 'FK_COMPLEX_IMAGE', 'OBJECT';
GO

EXEC sp_rename 'FK_FIELD_FACILITY', 'FK_FIELD_COMPLEX', 'OBJECT';
GO

EXEC sp_rename 'FK_feedback_facility', 'FK_FEEDBACK_COMPLEX', 'OBJECT';
GO

EXEC sp_rename 'FK_SHIFT_FACILITY', 'FK_SHIFT_COMPLEX', 'OBJECT';
GO

EXEC sp_rename 'FK_MATCH_POST_FACILITY', 'FK_MATCH_POST_COMPLEX', 'OBJECT';
GO

EXEC sp_rename 'FK_PRICE_RULE_FACILITY', 'FK_PRICE_RULE_COMPLEX', 'OBJECT';
GO