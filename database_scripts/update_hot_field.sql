-- Chạy script này để thêm cột is_hot vào bảng fields

ALTER TABLE fields ADD is_hot BIT NOT NULL DEFAULT 0;
