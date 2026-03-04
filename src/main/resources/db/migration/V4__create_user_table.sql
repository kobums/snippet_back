-- V4__create_user_table.sql

CREATE TABLE user_tb (
  u_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 고유 ID',
  u_email      VARCHAR(100) NOT NULL UNIQUE         COMMENT '이메일 (로그인 아이디)',
  u_password   VARCHAR(255) NOT NULL                COMMENT '비밀번호 (해시됨)',
  u_name       VARCHAR(50)  NOT NULL                COMMENT '사용자 닉네임/이름',
  u_createdate DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',

  PRIMARY KEY (u_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='사용자 계정 정보';
