-- Chạy đoạn script này trên SQL Server để bổ sung 2 bảng cần thiết cho chức năng OTP Reset Password

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
