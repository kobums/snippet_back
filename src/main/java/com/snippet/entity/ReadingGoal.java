package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "readinggoal_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rg_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rg_userid", nullable = false)
    private User user;

    @Column(name = "rg_year", nullable = false)
    private int year;

    @Column(name = "rg_targetbooks", nullable = false)
    private int targetBooks;

    @Column(name = "rg_createdate", nullable = false)
    private LocalDateTime createDate;

    @Builder
    public ReadingGoal(User user, int year, int targetBooks) {
        this.user = user;
        this.year = year;
        this.targetBooks = targetBooks;
        this.createDate = LocalDateTime.now();
    }

    public void updateTarget(int targetBooks) {
        this.targetBooks = targetBooks;
    }
}
