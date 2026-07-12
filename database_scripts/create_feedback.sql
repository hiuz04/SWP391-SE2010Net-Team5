-- Chạy file này để thêm hai bảng feedback và feedback_img

IF OBJECT_ID('feedbacks', 'U') IS NULL
BEGIN
CREATE TABLE feedbacks (
   feedback_id BIGINT IDENTITY(1,1) PRIMARY KEY,
   user_id BIGINT NOT NULL,
   facility_id BIGINT NOT NULL,
   rating INT NOT NULL,
   description NVARCHAR(1000) NULL,
   owner_reply NVARCHAR(1000) NULL,
   status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
   created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
   updated_at DATETIME2 NULL,
   reply_at DATETIME2 NULL,

   CONSTRAINT CK_feedback_rating
       CHECK (rating BETWEEN 1 AND 5),

   CONSTRAINT CK_feedback_status
       CHECK (status IN ('ACTIVE', 'HIDDEN')),

   CONSTRAINT FK_feedback_user
       FOREIGN KEY (user_id)
       REFERENCES users(user_id),

   CONSTRAINT FK_feedback_facility
       FOREIGN KEY (facility_id)
       REFERENCES facilities(facility_id)
);
END;

IF OBJECT_ID('feedback_images', 'U') IS NULL
BEGIN
CREATE TABLE feedback_images (
     image_id BIGINT IDENTITY(1,1) PRIMARY KEY,
     feedback_id BIGINT NOT NULL,
     image_url NVARCHAR(500) NOT NULL,
     public_id NVARCHAR(255) NOT NULL,
     created_at DATETIME2 NOT NULL DEFAULT GETDATE(),

     CONSTRAINT FK_feedback_image_feedback
         FOREIGN KEY (feedback_id)
         REFERENCES feedbacks(feedback_id)
         ON DELETE CASCADE
);
END;

IF NOT EXISTS (SELECT 1 FROM feedbacks)
BEGIN
INSERT INTO feedbacks
(
    user_id,
    facility_id,
    rating,
    description,
    owner_reply,
    status,
    created_at,
    reply_at
)
VALUES
(
    8,
    2,
    5,
    N'Sân đẹp, mặt cỏ chất lượng, nhân viên rất nhiệt tình.',
    N'Cảm ơn bạn đã đánh giá. Hy vọng được phục vụ bạn lần sau!',
    'ACTIVE',
    DATEADD(DAY, -20, GETDATE()),
    DATEADD(DAY, -19, GETDATE())
),
(
    7,
    1,
    4,
    N'Sân khá tốt, tuy nhiên bãi giữ xe hơi chật vào giờ cao điểm.',
    N'Cảm ơn góp ý của bạn. Chúng tôi sẽ sắp xếp lại khu vực gửi xe.',
    'ACTIVE',
    DATEADD(DAY, -5, GETDATE()),
    DATEADD(DAY, -4, GETDATE())
),
(
    8,
    1,
    5,
    N'Hệ thống đèn sáng, sân sạch sẽ, rất đáng trải nghiệm.',
    NULL,
    'ACTIVE',
    DATEADD(DAY, -3, GETDATE()),
    NULL
),
(
    9,
    2,
    2,
    N'Phòng thay đồ chưa sạch và thời gian nhận sân hơi chậm.',
    N'Xin lỗi bạn vì trải nghiệm chưa tốt. Chúng tôi đã nhắc nhở nhân viên và cải thiện quy trình vệ sinh.',
    'ACTIVE',
    DATEADD(DAY, -1, GETDATE()),
    GETDATE()
);
END;