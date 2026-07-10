IF OBJECT_ID('dbo.vouchers', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.vouchers (
        id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(50) NOT NULL UNIQUE,
        name NVARCHAR(255) NOT NULL,
        discount_type NVARCHAR(20) NOT NULL,
        discount_value DECIMAL(18,2) NOT NULL,
        min_order DECIMAL(18,2) NOT NULL
            CONSTRAINT DF_vouchers_min_order DEFAULT 0,
        quantity INT NOT NULL,
        used INT NOT NULL
            CONSTRAINT DF_vouchers_used DEFAULT 0,
        start_date DATETIME NOT NULL,
        end_date DATETIME NOT NULL,
        status NVARCHAR(20) NOT NULL,
        created_at DATETIME NOT NULL
            CONSTRAINT DF_vouchers_created_at DEFAULT GETDATE(),
        updated_at DATETIME NULL,
        CONSTRAINT CK_vouchers_discount_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
        CONSTRAINT CK_vouchers_status CHECK (status IN ('ACTIVE', 'DISABLED')),
        CONSTRAINT CK_vouchers_quantity CHECK (quantity > 0),
        CONSTRAINT CK_vouchers_used CHECK (used >= 0 AND used <= quantity),
        CONSTRAINT CK_vouchers_discount_value CHECK (discount_value > 0),
        CONSTRAINT CK_vouchers_min_order CHECK (min_order >= 0),
        CONSTRAINT CK_vouchers_date_range CHECK (start_date <= end_date)
    );
END
GO

IF COL_LENGTH('dbo.bookings', 'voucher_id') IS NULL
BEGIN
    ALTER TABLE dbo.bookings
    ADD voucher_id INT NULL;
END
GO

IF COL_LENGTH('dbo.bookings', 'discount_amount') IS NULL
BEGIN
    ALTER TABLE dbo.bookings
    ADD discount_amount DECIMAL(18,2) NOT NULL
        CONSTRAINT DF_bookings_discount_amount DEFAULT 0;
END
GO

IF COL_LENGTH('dbo.bookings', 'final_amount') IS NULL
BEGIN
    ALTER TABLE dbo.bookings
    ADD final_amount DECIMAL(18,2) NULL;
END
GO

UPDATE dbo.bookings
SET final_amount = total_amount
WHERE final_amount IS NULL
  AND total_amount IS NOT NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_bookings_vouchers'
      AND parent_object_id = OBJECT_ID('dbo.bookings')
)
BEGIN
    ALTER TABLE dbo.bookings
    ADD CONSTRAINT FK_bookings_vouchers
        FOREIGN KEY (voucher_id) REFERENCES dbo.vouchers(id);
END
GO

IF OBJECT_ID('dbo.voucher_usages', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.voucher_usages (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        voucher_id INT NOT NULL,
        customer_id BIGINT NOT NULL,
        booking_id BIGINT NULL,
        payment_id BIGINT NULL,
        used_at DATETIME NOT NULL
            CONSTRAINT DF_voucher_usages_used_at DEFAULT GETDATE(),
        CONSTRAINT FK_voucher_usages_vouchers
            FOREIGN KEY (voucher_id) REFERENCES dbo.vouchers(id),
        CONSTRAINT FK_voucher_usages_users
            FOREIGN KEY (customer_id) REFERENCES dbo.users(user_id),
        CONSTRAINT FK_voucher_usages_bookings
            FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id),
        CONSTRAINT FK_voucher_usages_payments
            FOREIGN KEY (payment_id) REFERENCES dbo.payments(payment_id)
    );
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = 'UQ_voucher_usages_voucher_customer'
      AND parent_object_id = OBJECT_ID('dbo.voucher_usages')
)
BEGIN
    ALTER TABLE dbo.voucher_usages
    ADD CONSTRAINT UQ_voucher_usages_voucher_customer
        UNIQUE (voucher_id, customer_id);
END
GO

;WITH first_usage AS (
    SELECT b.voucher_id,
           b.customer_id,
           MIN(b.booking_id) AS booking_id
    FROM dbo.bookings b
    WHERE b.voucher_id IS NOT NULL
      AND EXISTS (
          SELECT 1
          FROM dbo.payments p
          WHERE p.booking_id = b.booking_id
            AND p.customer_id = b.customer_id
            AND p.payment_type = 'DEPOSIT'
            AND p.status = 'SUCCESS'
      )
    GROUP BY b.voucher_id,
             b.customer_id
)
INSERT INTO dbo.voucher_usages (
    voucher_id,
    customer_id,
    booking_id,
    payment_id,
    used_at
)
SELECT fu.voucher_id,
       fu.customer_id,
       fu.booking_id,
       paid.payment_id,
       COALESCE(paid.paid_at, GETDATE())
FROM first_usage fu
OUTER APPLY (
    SELECT TOP 1 p.payment_id,
           p.paid_at
    FROM dbo.payments p
    WHERE p.booking_id = fu.booking_id
      AND p.customer_id = fu.customer_id
      AND p.payment_type = 'DEPOSIT'
      AND p.status = 'SUCCESS'
    ORDER BY p.paid_at DESC,
             p.payment_id DESC
) paid
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.voucher_usages vu
    WHERE vu.voucher_id = fu.voucher_id
      AND vu.customer_id = fu.customer_id
);
GO
