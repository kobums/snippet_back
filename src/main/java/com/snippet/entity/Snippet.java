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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "r_user", nullable = false)
    private User user;

    @Column(name = "r_type", nullable = false, length = 20)
    private String type; // "snippet", "diary", "review"

    @Column(name = "r_content", nullable = false, columnDefinition = "TEXT")
    private String text; // keeping variable name text to minimize breakages, mapping to r_content

    @Column(name = "r_tag", length = 50)
    private String tag;

    @Column(name = "r_relatedpage")
    private Integer relatedPage;

    /**
     * 스와이프 카드 공개 여부. 밑줄(snippet)만 공개할 수 있고 기본은 비공개다 —
     * 예전엔 모든 밑줄이 작성자 동의 없이 다른 사용자의 카드에 노출됐다.
     */
    @Column(name = "r_public", nullable = false)
    private boolean isPublic;

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
    public Snippet(Book book, User user, String type, String text, String tag, Integer relatedPage, boolean isPublic) {
        this.book = book;
        this.user = user;
        this.type = type;
        this.text = text;
        this.tag = tag;
        this.relatedPage = relatedPage;
        // type 이 비면 @PrePersist 가 snippet 으로 채운다
        this.isPublic = isPublic && (type == null || "snippet".equals(type));
    }

    public void update(String type, String text, String tag, Integer relatedPage, Boolean isPublic) {
        if (type != null) this.type = type;
        if (text != null) this.text = text;
        if (tag != null) this.tag = tag;
        if (relatedPage != null) this.relatedPage = relatedPage;
        if (isPublic != null) this.isPublic = isPublic;
        // 공개는 밑줄만 — 일기·리뷰로 바꾸면 카드에서 내린다
        if (!"snippet".equals(this.type)) this.isPublic = false;
    }
}
