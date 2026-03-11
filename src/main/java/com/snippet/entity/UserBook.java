package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "userbook_tb", uniqueConstraints = {
        @UniqueConstraint(name = "uk_userbook_user_book", columnNames = { "ub_user", "ub_book" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ub_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ub_user", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ub_book", nullable = false)
    private Book book;

    @Column(name = "ub_type", nullable = false, length = 20)
    private String type;

    @Column(name = "ub_status", nullable = false, length = 20)
    private String status;

    @Column(name = "ub_readpage", nullable = false)
    private Integer readPage;

    @Column(name = "ub_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "ub_updatedate", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "ub_startdate", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "ub_enddate", nullable = false)
    private LocalDateTime endDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        if (this.type == null)
            this.type = "wish";
        // wish 타입은 status를 none으로, 그 외엔 waiting 기본값
        if (this.status == null) {
            this.status = "wish".equals(this.type) ? "none" : "waiting";
        }
        if (this.readPage == null)
            this.readPage = 0;
        if (this.startDate == null)
            this.startDate = LocalDateTime.now();
        if (this.endDate == null)
            this.endDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }

    @Builder
    public UserBook(User user, Book book, String type, String status, Integer readPage, LocalDateTime startDate,
            LocalDateTime endDate) {
        this.user = user;
        this.book = book;
        this.type = type;
        this.status = status;
        this.readPage = readPage;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateType(String type) {
        this.type = type;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void updateReadPage(Integer readPage) {
        this.readPage = readPage;
    }

    public void updateStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void updateEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
