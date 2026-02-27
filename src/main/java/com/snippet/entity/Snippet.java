package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "record_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Snippet { // Renaming to ReadingRecord might break existing code, keeping Snippet for now
                       // but modifying columns

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "r_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "r_book", nullable = false)
    private Book book;

    @Column(name = "r_type", nullable = false, length = 20)
    private String type; // "snippet", "diary", "review"

    @Column(name = "r_content", nullable = false, columnDefinition = "TEXT")
    private String text; // keeping variable name text to minimize breakages, mapping to r_content

    @Column(name = "r_tag", length = 50)
    private String tag;

    @Column(name = "r_relatedpage")
    private Integer relatedPage;

    @Column(name = "r_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        if (this.type == null) {
            this.type = "snippet";
        }
    }

    @Builder
    public Snippet(Book book, String type, String text, String tag, Integer relatedPage) {
        this.book = book;
        this.type = type;
        this.text = text;
        this.tag = tag;
        this.relatedPage = relatedPage;
    }
}
