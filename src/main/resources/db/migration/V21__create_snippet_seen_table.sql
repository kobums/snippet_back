CREATE TABLE snippet_seen_tb (
    ss_id       BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ss_user     BIGINT   NOT NULL COMMENT '사용자 FK',
    ss_snippet  BIGINT   NOT NULL COMMENT '스니펫 FK',
    ss_seendate DATE     NOT NULL COMMENT '마지막으로 본 날짜',
    CONSTRAINT fk_ss_user    FOREIGN KEY (ss_user)    REFERENCES user_tb(u_id)    ON DELETE CASCADE,
    CONSTRAINT fk_ss_snippet FOREIGN KEY (ss_snippet) REFERENCES record_tb(r_id)  ON DELETE CASCADE,
    CONSTRAINT uk_ss_user_snippet UNIQUE (ss_user, ss_snippet)
);

CREATE INDEX idx_ss_user_seendate ON snippet_seen_tb (ss_user, ss_seendate);
