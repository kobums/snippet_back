-- u_refresh_token → u_refreshtoken (prefix 이후 특수문자 사용 금지 규칙)
ALTER TABLE user_tb CHANGE COLUMN u_refresh_token u_refreshtoken TEXT NULL COMMENT '리프레시 토큰';
