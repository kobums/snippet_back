package com.snippet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "snippet_daily_view_tb", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sdv_user_date", columnNames = {"sdv_user", "sdv_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SnippetDailyView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sdv_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sdv_user", nullable = false)
    private User user;

    @Column(name = "sdv_date", nullable = false)
    private LocalDate date;

    @Column(name = "sdv_count", nullable = false)
    private int count;

    @Builder
    public SnippetDailyView(User user, LocalDate date, int count) {
        this.user = user;
        this.date = date;
        this.count = count;
    }

    public void addCount(int amount) {
        this.count += amount;
    }
}
