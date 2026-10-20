-- 읽은 페이지(ub_readpage) 변경 이력. 타이머(readingsession_tb)를 쓰지 않는 사용자도
-- 하루에 몇 페이지 읽었는지 집계할 수 있도록, readPage가 바뀔 때마다 한 행씩 쌓는다.
--
-- 서재에서 책을 지워도(userbook_tb 삭제) "그날 읽었다"는 기록은 남겨야 일별 통계/스트릭이
-- 소급해서 바뀌지 않으므로 rl_userbook, rl_book은 SET NULL. 회원 탈퇴 시에는 함께 삭제.
CREATE TABLE readinglog_tb (
  rl_id          BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '로그 고유 ID',
  rl_user        BIGINT      NOT NULL                 COMMENT '사용자 FK',
  rl_userbook    BIGINT      NULL                     COMMENT '서재 아이템 FK (삭제 시 NULL)',
  rl_book        BIGINT      NULL                     COMMENT '책 FK (삭제 시 NULL)',
  rl_frompage    INT         NOT NULL                 COMMENT '변경 전 페이지',
  rl_topage      INT         NOT NULL                 COMMENT '변경 후 페이지',
  rl_delta       INT         NOT NULL                 COMMENT '변경량 (rl_topage - rl_frompage, 음수 가능)',
  rl_source      VARCHAR(20) NOT NULL                 COMMENT 'manual / session / complete',
  rl_logdate     DATE        NOT NULL                 COMMENT '집계 기준 날짜 (KST)',
  rl_createdate  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',

  PRIMARY KEY (rl_id),

  CONSTRAINT fk_rl_user     FOREIGN KEY (rl_user)     REFERENCES user_tb (u_id)      ON DELETE CASCADE,
  CONSTRAINT fk_rl_userbook FOREIGN KEY (rl_userbook) REFERENCES userbook_tb (ub_id) ON DELETE SET NULL,
  CONSTRAINT fk_rl_book     FOREIGN KEY (rl_book)     REFERENCES book_tb (b_id)      ON DELETE SET NULL,

  INDEX idx_rl_user_date (rl_user, rl_logdate),
  INDEX idx_rl_userbook  (rl_userbook),
  INDEX idx_rl_book      (rl_book)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='읽은 페이지 변경 로그';
