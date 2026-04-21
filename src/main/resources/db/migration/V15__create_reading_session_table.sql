CREATE TABLE reading_session_tb (
  rs_id              BIGINT   NOT NULL AUTO_INCREMENT  COMMENT '세션 고유 ID',
  rs_user            BIGINT   NOT NULL                 COMMENT '사용자 FK',
  rs_userbook        BIGINT   NOT NULL                 COMMENT '서재 아이템 FK',
  rs_book            BIGINT   NOT NULL                 COMMENT '책 FK',
  rs_durationseconds INT      NOT NULL DEFAULT 0       COMMENT '독서 시간(초)',
  rs_startpage       INT      NOT NULL DEFAULT 0       COMMENT '시작 페이지',
  rs_endpage         INT      NOT NULL DEFAULT 0       COMMENT '종료 페이지',
  rs_pagesread       INT      NOT NULL DEFAULT 0       COMMENT '읽은 페이지 수',
  rs_secondsperpage  DOUBLE   NOT NULL DEFAULT 0.0     COMMENT '페이지당 소요 시간(초)',
  rs_sessiondate     DATE     NOT NULL                 COMMENT '독서 날짜',
  rs_createdate      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',

  PRIMARY KEY (rs_id),

  CONSTRAINT fk_rs_user     FOREIGN KEY (rs_user)     REFERENCES user_tb (u_id)        ON DELETE CASCADE,
  CONSTRAINT fk_rs_userbook FOREIGN KEY (rs_userbook) REFERENCES userbook_tb (ub_id)   ON DELETE CASCADE,
  CONSTRAINT fk_rs_book     FOREIGN KEY (rs_book)     REFERENCES book_tb (b_id)        ON DELETE CASCADE,

  INDEX idx_rs_user     (rs_user),
  INDEX idx_rs_userbook (rs_userbook),
  INDEX idx_rs_book     (rs_book),
  INDEX idx_rs_date     (rs_sessiondate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='독서 세션 기록';
