package com.snippet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_tb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_id")
    private Long id;

    @Column(name = "u_email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "u_password", nullable = false, length = 255)
    private String password;

    @Column(name = "u_name", nullable = false, length = 50)
    private String name;

    @Column(name = "u_createdate", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
    }

    @Builder
    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }

    public void update(String email, String password, String name) {
        if (email != null) this.email = email;
        if (password != null) this.password = password;
        if (name != null) this.name = name;
    }
}
