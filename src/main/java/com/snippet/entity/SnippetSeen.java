package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "snippet_seen_tb", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ss_user_snippet", columnNames = {"ss_user", "ss_snippet"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SnippetSeen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ss_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ss_user", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ss_snippet", nullable = false)
    private Snippet snippet;

    @Column(name = "ss_seendate", nullable = false)
    private LocalDate seenDate;

    @Builder
    public SnippetSeen(User user, Snippet snippet, LocalDate seenDate) {
        this.user = user;
        this.snippet = snippet;
        this.seenDate = seenDate;
    }

    public void updateSeenDate(LocalDate seenDate) {
        this.seenDate = seenDate;
    }
}
