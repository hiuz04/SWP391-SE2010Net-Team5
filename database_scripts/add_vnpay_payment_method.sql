IF EXISTS (
    SELECT 1
    FROM payment_methods
    WHERE method_code = 'VNPAY'
)
BEGIN
    UPDATE payment_methods
    SET method_name = 'VNPay Sandbox',
        status = 'ACTIVE'
    WHERE method_code = 'VNPAY';
END
ELSE
BEGIN
    INSERT INTO payment_methods (method_code, method_name, status)
    VALUES ('VNPAY', 'VNPay Sandbox', 'ACTIVE');
END;
