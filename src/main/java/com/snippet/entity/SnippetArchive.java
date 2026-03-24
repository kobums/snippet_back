package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "usersnippet_tb", uniqueConstraints = {
        @UniqueConstraint(name = "uk_us_user_snippet", columnNames = { "us_user", "us_snippet" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SnippetArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "us_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "us_user", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "us_snippet", nullable = false)
    private Snippet snippet;

    @Column(name = "us_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
    }

    @Builder
    public SnippetArchive(User user, Snippet snippet) {
        this.user = user;
        this.snippet = snippet;
    }
}
