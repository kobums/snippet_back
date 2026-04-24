package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "readingsession_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rs_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rs_user", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rs_userbook", nullable = false)
    private UserBook userBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rs_book", nullable = false)
    private Book book;

    @Column(name = "rs_durationseconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "rs_startpage", nullable = false)
    private Integer startPage;

    @Column(name = "rs_endpage", nullable = false)
    private Integer endPage;

    @Column(name = "rs_pagesread", nullable = false)
    private Integer pagesRead;

    @Column(name = "rs_secondsperpage", nullable = false)
    private Double secondsPerPage;

    @Column(name = "rs_sessiondate", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "rs_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.pagesRead = Math.max(0, this.endPage - this.startPage);
        this.secondsPerPage = this.pagesRead > 0
                ? (double) this.durationSeconds / this.pagesRead
                : 0.0;
    }

    @Builder
    public ReadingSession(User user, UserBook userBook, Book book,
                          Integer durationSeconds, Integer startPage,
                          Integer endPage, LocalDate sessionDate) {
        this.user = user;
        this.userBook = userBook;
        this.book = book;
        this.durationSeconds = durationSeconds;
        this.startPage = startPage;
        this.endPage = endPage;
        this.sessionDate = sessionDate;
    }
}
