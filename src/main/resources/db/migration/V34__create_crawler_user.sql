-- 크롤러(알라딘 문장 수집) 전용 시스템 계정.
-- 예전엔 CrawlerService 가 id=2(운영자 개인 계정)를 작성자로 써서 크롤링 문장이 개인 독서 기록에 섞였다.
-- 비밀번호 칸의 '!login-disabled' 는 BCrypt 형식이 아니라 어떤 입력과도 일치하지 않는다 (로그인 불가).
INSERT INTO user_tb (u_email, u_password, u_name, u_createdate)
SELECT 'crawler@gowoobro.com', '!login-disabled', '알라딘 문장 크롤러', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user_tb WHERE u_email = 'crawler@gowoobro.com');
