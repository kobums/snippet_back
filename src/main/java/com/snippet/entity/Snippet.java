package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "snippet_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Snippet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "s_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "s_book", nullable = false)
    private Book book;

    @Column(name = "s_text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "s_tag", nullable = false, length = 50)
    private String tag;

    @Column(name = "s_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
    }

    @Builder
    public Snippet(Book book, String text, String tag) {
        this.book = book;
        this.text = text;
        this.tag = tag;
    }
}
