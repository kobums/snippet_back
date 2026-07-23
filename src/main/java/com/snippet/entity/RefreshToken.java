package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기기(세션)별 리프레시 토큰. 유저 1명이 여러 행을 가질 수 있어
 * 다른 기기에서 로그인해도 기존 기기의 세션이 유지된다.
 * 토큰은 불투명 랜덤 문자열이며 만료는 DB(rt_expiredate)로만 관리한다.
 */
@Entity
@Table(name = "refreshtoken_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rt_user_id", nullable = false)
    private User user;

    @Column(name = "rt_token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "rt_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "rt_expiredate", nullable = false)
    private LocalDateTime expireDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
    }

    @Builder
    public RefreshToken(User user, String token, LocalDateTime expireDate) {
        this.user = user;
        this.token = token;
        this.expireDate = expireDate;
    }

    public boolean isExpired() {
        return expireDate.isBefore(LocalDateTime.now());
    }

    /** 사용될 때마다 만료를 미뤄 활동 중인 기기는 로그인이 유지되게 한다(sliding expiry). */
    public void extendExpiry(LocalDateTime newExpireDate) {
        this.expireDate = newExpireDate;
    }
}
