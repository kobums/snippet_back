CREATE TABLE suggestion_tb (
    s_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    s_user       BIGINT,
    s_category   VARCHAR(20)  NOT NULL,
    s_title      VARCHAR(200),
    s_content    TEXT         NOT NULL,
    s_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    s_createdate DATETIME     NOT NULL,
    CONSTRAINT fk_suggestion_user
        FOREIGN KEY (s_user) REFERENCES user_tb (u_id) ON DELETE SET NULL
);
