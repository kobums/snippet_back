-- 밑줄(스니펫) 기록의 스와이프 카드 공개 여부.
-- 예전엔 type='snippet' 기록이 작성자 동의 없이 전부 다른 사용자의 카드에 노출됐다.
-- 기존 사용자 기록은 비공개로 두고(동의 없이 공개되지 않게), 크롤러가 모은 문장만 공개한다.
-- Hibernate 의 MariaDB boolean 매핑이 BIT 라서 BIT(1) 로 만든다 (ddl-auto: validate 통과).
ALTER TABLE record_tb ADD COLUMN r_public BIT(1) NOT NULL DEFAULT b'0';

UPDATE record_tb SET r_public = b'1'
WHERE r_type = 'snippet'
  AND r_user IN (SELECT u_id FROM user_tb WHERE u_email = 'crawler@gowoobro.com');
