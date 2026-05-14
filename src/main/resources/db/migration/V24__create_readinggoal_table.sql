CREATE TABLE readinggoal_tb (
    rg_id        BIGINT NOT NULL AUTO_INCREMENT,
    rg_userid    BIGINT NOT NULL,
    rg_year      INT    NOT NULL,
    rg_targetbooks INT  NOT NULL DEFAULT 12,
    rg_createdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (rg_id),
    UNIQUE KEY uq_readinggoal_user_year (rg_userid, rg_year),
    CONSTRAINT fk_readinggoal_user FOREIGN KEY (rg_userid) REFERENCES user_tb (u_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
