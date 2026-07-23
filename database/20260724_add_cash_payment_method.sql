IF OBJECT_ID('payment_methods', 'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM payment_methods
        WHERE UPPER(method_code) = 'CASH'
    )
    BEGIN
        INSERT INTO payment_methods (method_code, method_name, status)
        VALUES ('CASH', N'Tiền mặt', 'ACTIVE');
    END
END;
