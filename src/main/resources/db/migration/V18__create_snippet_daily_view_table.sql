CREATE TABLE snippet_daily_view_tb (
    sdv_id    BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sdv_user  BIGINT NOT NULL,
    sdv_date  DATE   NOT NULL,
    sdv_count INT    NOT NULL DEFAULT 0,
    CONSTRAINT fk_sdv_user   FOREIGN KEY (sdv_user) REFERENCES user_tb (u_id),
    CONSTRAINT uk_sdv_user_date UNIQUE (sdv_user, sdv_date)
);
