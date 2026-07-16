-- ==========================
-- Create feedbacks table
-- ==========================

IF
OBJECT_ID('feedbacks', 'U') IS NULL
BEGIN

CREATE TABLE feedbacks
(
    feedback_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id  BIGINT    NOT NULL UNIQUE,
    user_id     BIGINT    NOT NULL,
    facility_id BIGINT    NOT NULL,
    rating      INT       NOT NULL,
    description NVARCHAR(1000),
    owner_reply NVARCHAR(1000),
    status      NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at  DATETIME2 NULL,
    reply_at    DATETIME2 NULL,

    CONSTRAINT CK_feedback_rating
        CHECK (rating BETWEEN 1 AND 5),

    CONSTRAINT CK_feedback_status
        CHECK (status IN ('ACTIVE', 'HIDDEN')),

    CONSTRAINT FK_feedback_booking
        FOREIGN KEY (booking_id)
            REFERENCES bookings (booking_id),

    CONSTRAINT FK_feedback_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id),

    CONSTRAINT FK_feedback_facility
        FOREIGN KEY (facility_id)
            REFERENCES facilities (facility_id)
);

END;

-- ==========================
-- Sample feedback
-- ==========================

IF
NOT EXISTS (SELECT 1 FROM feedbacks)
BEGIN

INSERT INTO feedbacks
(booking_id,
 user_id,
 facility_id,
 rating,
 description,
 owner_reply,
 status,
 created_at,
 reply_at)
VALUES (3,
        8,
        2,
        5,
        N'Sân đẹp, mặt cỏ chất lượng, nhân viên rất nhiệt tình.',
        N'Cảm ơn bạn đã đánh giá. Hy vọng được phục vụ bạn lần sau!',
        'ACTIVE',
        DATEADD(DAY, -20, GETDATE()),
        DATEADD(DAY, -19, GETDATE())),
       (10,
        7,
        1,
        4,
        N'Sân khá tốt, tuy nhiên bãi giữ xe hơi chật vào giờ cao điểm.',
        N'Cảm ơn góp ý của bạn. Chúng tôi sẽ sắp xếp lại khu vực gửi xe.',
        'ACTIVE',
        DATEADD(DAY, -5, GETDATE()),
        DATEADD(DAY, -4, GETDATE())),
       (11,
        8,
        1,
        5,
        N'Hệ thống đèn sáng, sân sạch sẽ, rất đáng trải nghiệm.',
        NULL,
        'ACTIVE',
        DATEADD(DAY, -3, GETDATE()),
        NULL),
       (15,
        9,
        1,
        2,
        N'Phòng thay đồ chưa sạch và thời gian nhận sân hơi chậm.',
        N'Xin lỗi bạn vì trải nghiệm chưa tốt. Chúng tôi đã nhắc nhở nhân viên và cải thiện quy trình vệ sinh.',
        'ACTIVE',
        DATEADD(DAY, -1, GETDATE()),
        GETDATE());
END;