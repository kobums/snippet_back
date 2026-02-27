package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "userbook_tb", uniqueConstraints = {
        @UniqueConstraint(name = "uk_userbook_user_book", columnNames = { "ub_userid", "ub_book" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ub_id")
    private Long id;

    @Column(name = "ub_userid", nullable = false, length = 100)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ub_book", nullable = false)
    private Book book;

    @Column(name = "ub_status", nullable = false, length = 20)
    private String status;

    @Column(name = "ub_readpage", nullable = false)
    private Integer readPage;

    @Column(name = "ub_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "ub_updatedate", nullable = false)
    private LocalDateTime updateDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        if (this.status == null)
            this.status = "wish";
        if (this.readPage == null)
            this.readPage = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }

    @Builder
    public UserBook(String userId, Book book, String status, Integer readPage) {
        this.userId = userId;
        this.book = book;
        this.status = status;
        this.readPage = readPage;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void updateReadPage(Integer readPage) {
        this.readPage = readPage;
    }
}
