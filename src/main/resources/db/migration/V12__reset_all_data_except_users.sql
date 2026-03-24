-- V12: User 데이터를 제외한 모든 데이터 초기화
-- 외래 키 제약 조건 순서에 따라 삭제

-- 1. SnippetArchive 삭제 (snippet 참조)
DELETE FROM usersnippet_tb;

-- 2. Record(Snippet/Diary/Review) 삭제 (book, user 참조)
DELETE FROM record_tb;

-- 3. UserBook 삭제 (book, user 참조)
DELETE FROM userbook_tb;

-- 4. Book 삭제 (마지막 - 다른 테이블에서 참조되지 않음)
DELETE FROM book_tb;
