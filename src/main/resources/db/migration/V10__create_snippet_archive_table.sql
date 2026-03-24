-- 스니펫 보관함 테이블: 사용자가 좋아요한 스니펫을 서버에 저장
CREATE TABLE usersnippet_tb (
    us_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    us_user BIGINT NOT NULL,
    us_snippet BIGINT NOT NULL,
    us_createdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_us_user FOREIGN KEY (us_user) REFERENCES user_tb(u_id),
    CONSTRAINT fk_us_snippet FOREIGN KEY (us_snippet) REFERENCES record_tb(r_id),
    CONSTRAINT uk_us_user_snippet UNIQUE (us_user, us_snippet)
);

CREATE INDEX idx_us_user ON usersnippet_tb(us_user);
