-- Add category column to book_tb table
ALTER TABLE book_tb ADD COLUMN b_category VARCHAR(50) NULL COMMENT '책 카테고리 (소설, 비문학, 인문, 경제경영, 자기계발 등)';
