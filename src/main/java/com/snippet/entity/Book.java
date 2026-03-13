package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "b_id")
    private Long id;

    @Column(name = "b_isbn", nullable = false, length = 13)
    private String isbn;

    @Column(name = "b_title", nullable = false, length = 200)
    private String title;

    @Column(name = "b_author", nullable = false, length = 200)
    private String author;

    @Column(name = "b_coverurl", nullable = false, length = 500)
    private String coverUrl;

    @Column(name = "b_affiliateurl", nullable = false, length = 500)
    private String affiliateUrl;

    @Column(name = "b_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "b_publisher", length = 100)
    private String publisher;

    @Column(name = "b_totalpage")
    private Integer totalPage;

    @Column(name = "b_publicationdate")
    private java.time.LocalDate publicationDate;

    @Column(name = "b_category", length = 50)
    private String category;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
    }

    @Builder
    public Book(String isbn, String title, String author, String coverUrl, String affiliateUrl, String publisher,
            Integer totalPage, java.time.LocalDate publicationDate, String category) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.coverUrl = coverUrl;
        this.affiliateUrl = affiliateUrl;
        this.publisher = publisher;
        this.totalPage = totalPage;
        this.publicationDate = publicationDate;
        this.category = category;
    }

    public void update(String isbn, String title, String author, String coverUrl, String affiliateUrl,
            String publisher, Integer totalPage, java.time.LocalDate publicationDate, String category) {
        if (isbn != null) this.isbn = isbn;
        if (title != null) this.title = title;
        if (author != null) this.author = author;
        if (coverUrl != null) this.coverUrl = coverUrl;
        if (affiliateUrl != null) this.affiliateUrl = affiliateUrl;
        if (publisher != null) this.publisher = publisher;
        if (totalPage != null) this.totalPage = totalPage;
        if (publicationDate != null) this.publicationDate = publicationDate;
        if (category != null) this.category = category;
    }
}
