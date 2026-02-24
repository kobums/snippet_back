-- book_tb: 책 정보
CREATE TABLE book_tb (
  b_id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '책 고유 ID',
  b_isbn         VARCHAR(13)   NOT NULL                COMMENT '국제 표준 도서 번호',
  b_title        VARCHAR(200)  NOT NULL                COMMENT '책 제목',
  b_author       VARCHAR(200)  NOT NULL                COMMENT '저자',
  b_coverurl     VARCHAR(500)  NOT NULL                COMMENT '표지 이미지 URL',
  b_affiliateurl VARCHAR(500)  NOT NULL                COMMENT '제휴 마케팅 링크',
  b_createdate   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',

  PRIMARY KEY (b_id),
  UNIQUE KEY uk_book_isbn (b_isbn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='책 정보';

-- snippet_tb: 책 속 인상 깊은 문장/문단
CREATE TABLE snippet_tb (
  s_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '문장 고유 ID',
  s_book       BIGINT       NOT NULL                COMMENT '책 FK (book_tb.b_id)',
  s_text       TEXT         NOT NULL                COMMENT '문장 또는 문단 텍스트',
  s_tag        VARCHAR(50)  NOT NULL                COMMENT '카테고리 태그',
  s_createdate DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',

  PRIMARY KEY (s_id),
  CONSTRAINT fk_snippet_book FOREIGN KEY (s_book) REFERENCES book_tb (b_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='책 속 인상 깊은 문장/문단';

CREATE INDEX idx_snippet_book ON snippet_tb (s_book);
CREATE INDEX idx_snippet_tag  ON snippet_tb (s_tag);
