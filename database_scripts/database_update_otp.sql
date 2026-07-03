-- Chạy đoạn script này trên SQL Server để bổ sung các bảng cần thiết cho chức năng OTP Reset Password và Quản lý ca làm việc (Work Shifts)

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[password_reset_tokens]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[password_reset_tokens] (
        [token_id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [user_id] BIGINT NOT NULL,
        [token] VARCHAR(255) NULL,
        [otp_code] VARCHAR(10) NULL,
        [expires_at] DATETIME NOT NULL,
        [used] BIT DEFAULT 0,
        [created_at] DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_password_reset_users FOREIGN KEY ([user_id]) REFERENCES [users]([user_id])
    );
END

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[activity_logs]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[activity_logs] (
        [log_id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [user_id] BIGINT NOT NULL,
        [action] VARCHAR(100) NOT NULL,
        [description] NVARCHAR(MAX) NULL,
        [created_at] DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_activity_logs_users FOREIGN KEY ([user_id]) REFERENCES [users]([user_id])
    );
END

-- Bảng ca làm việc (Work Shifts)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[work_shifts]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[work_shifts] (
        [shift_id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [facility_id] BIGINT NOT NULL,
        [shift_name] NVARCHAR(100) NOT NULL,
        [shift_date] DATE NOT NULL,
        [start_time] TIME NOT NULL,
        [end_time] TIME NOT NULL,
        [created_at] DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_work_shifts_facilities FOREIGN KEY ([facility_id]) REFERENCES [dbo].[facilities]([facility_id])
    );
END

-- Bảng phân công ca làm việc cho nhân viên (Shift Assignments)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[shift_assignments]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[shift_assignments] (
        [shift_id] BIGINT NOT NULL,
        [staff_id] BIGINT NOT NULL,
        [status] VARCHAR(50) DEFAULT 'ASSIGNED',
        PRIMARY KEY ([shift_id], [staff_id]),
        CONSTRAINT FK_shift_assignments_work_shifts FOREIGN KEY ([shift_id]) REFERENCES [dbo].[work_shifts]([shift_id]),
        CONSTRAINT FK_shift_assignments_users FOREIGN KEY ([staff_id]) REFERENCES [dbo].[users]([user_id])
    );
END
