package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "readinglog_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingLog {

    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_SESSION = "session";
    public static final String SOURCE_COMPLETE = "complete";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rl_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rl_user", nullable = false)
    private User user;

    // 서재/책이 삭제되면 DB에서 SET NULL — 일별 집계는 그대로 유지된다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rl_userbook")
    private UserBook userBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rl_book")
    private Book book;

    @Column(name = "rl_frompage", nullable = false)
    private Integer fromPage;

    @Column(name = "rl_topage", nullable = false)
    private Integer toPage;

    @Column(name = "rl_delta", nullable = false)
    private Integer delta;

    @Column(name = "rl_source", nullable = false, length = 20)
    private String source;

    @Column(name = "rl_logdate", nullable = false)
    private LocalDate logDate;

    @Column(name = "rl_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.delta = this.toPage - this.fromPage;
    }

    @Builder
    public ReadingLog(User user, UserBook userBook, Book book, Integer fromPage,
                      Integer toPage, String source, LocalDate logDate) {
        this.user = user;
        this.userBook = userBook;
        this.book = book;
        this.fromPage = fromPage;
        this.toPage = toPage;
        this.source = source;
        this.logDate = logDate;
    }
}
