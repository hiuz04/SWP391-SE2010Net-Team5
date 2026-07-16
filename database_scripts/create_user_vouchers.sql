CREATE TABLE user_vouchers
(
    user_voucher_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         BIGINT    NOT NULL,
    voucher_id      int    NOT NULL,
    status          NVARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    received_at     DATETIME2 NOT NULL DEFAULT GETDATE(),
    expired_at      DATETIME2 NULL,
    used_at         DATETIME2 NULL,

    booking_id      BIGINT NULL,

    CONSTRAINT FK_USER_VOUCHER_USER
        FOREIGN KEY (user_id)
            REFERENCES users (user_id),

    CONSTRAINT FK_USER_VOUCHER_VOUCHER
        FOREIGN KEY (voucher_id)
            REFERENCES vouchers (id),

    CONSTRAINT FK_USER_VOUCHER_BOOKING
        FOREIGN KEY (booking_id)
            REFERENCES bookings (booking_id),

    CONSTRAINT CK_USER_VOUCHER_STATUS
        CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED'))
);