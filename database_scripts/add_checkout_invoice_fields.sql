IF COL_LENGTH('dbo.invoices', 'overtime_minutes') IS NULL
BEGIN
    ALTER TABLE dbo.invoices
    ADD overtime_minutes INT NOT NULL
        CONSTRAINT DF_invoices_overtime_minutes DEFAULT 0;
END
GO

IF COL_LENGTH('dbo.invoices', 'overtime_fee') IS NULL
BEGIN
    ALTER TABLE dbo.invoices
    ADD overtime_fee DECIMAL(18,2) NOT NULL
        CONSTRAINT DF_invoices_overtime_fee DEFAULT 0;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM dbo.system_settings
    WHERE setting_key = 'CHECKOUT_OVERTIME_FEE_PER_MINUTE'
)
BEGIN
    INSERT INTO dbo.system_settings (setting_key, setting_value, description, updated_at)
    VALUES ('CHECKOUT_OVERTIME_FEE_PER_MINUTE', '5000', N'Checkout overtime fee per minute', GETDATE());
END
GO
