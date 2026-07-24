-- 건의사항 관리자 답변 필드 추가
ALTER TABLE suggestion_tb ADD COLUMN s_answer TEXT NULL COMMENT '관리자 답변';
ALTER TABLE suggestion_tb ADD COLUMN s_answerdate DATETIME NULL COMMENT '답변 일시';
