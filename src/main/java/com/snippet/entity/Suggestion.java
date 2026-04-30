package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "suggestion_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Suggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "s_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "s_user")
    private User user;

    @Column(name = "s_category", nullable = false, length = 20)
    private String category;

    @Column(name = "s_title", length = 200)
    private String title;

    @Column(name = "s_content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "s_status", nullable = false, length = 20)
    private String status;

    @Column(name = "s_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    @Builder
    public Suggestion(User user, String category, String title, String content) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.content = content;
    }
}
