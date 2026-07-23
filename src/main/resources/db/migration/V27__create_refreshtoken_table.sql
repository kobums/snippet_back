-- 기기별 리프레시 토큰 테이블: 유저당 1개 토큰(user_tb.u_refreshtoken) 구조에서는
-- 다른 기기 로그인/토큰 회전 시 기존 기기 세션이 전부 무효화되어 로그인이 풀렸다.
-- 기기(세션)마다 독립된 토큰 행을 두어 서로 영향을 주지 않게 한다.
CREATE TABLE refreshtoken_tb (
    rt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rt_user_id BIGINT NOT NULL,
    rt_token VARCHAR(512) NOT NULL,
    rt_createdate DATETIME NOT NULL,
    rt_expiredate DATETIME NOT NULL,
    CONSTRAINT uk_refreshtoken_token UNIQUE (rt_token),
    CONSTRAINT fk_refreshtoken_user FOREIGN KEY (rt_user_id) REFERENCES user_tb (u_id) ON DELETE CASCADE
);

CREATE INDEX idx_refreshtoken_user ON refreshtoken_tb (rt_user_id);

-- 기존 세션 유지: user_tb에 저장돼 있던 마지막 리프레시 토큰을 이관.
-- 실제 JWT 만료 시각은 SQL에서 알 수 없으므로 최대치(30일)로 근사한다.
INSERT INTO refreshtoken_tb (rt_user_id, rt_token, rt_createdate, rt_expiredate)
SELECT u_id, u_refreshtoken, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY)
FROM user_tb
WHERE u_refreshtoken IS NOT NULL;

ALTER TABLE user_tb DROP COLUMN u_refreshtoken;
