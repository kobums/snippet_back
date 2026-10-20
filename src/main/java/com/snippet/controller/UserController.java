package com.snippet.controller;

import com.snippet.entity.User;
import com.snippet.security.AdminGuard;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AdminGuard adminGuard;

    // 아래 사용자 CRUD는 관리자 전용이다. (일반 사용자는 /api/auth/* 로만 자기 계정을 다룬다)

    @GetMapping
    public ResponseEntity<List<User>> getAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        adminGuard.check(userDetails);
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        adminGuard.check(userDetails);
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<User> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> body) {
        adminGuard.check(userDetails);
        User user = userService.create(body.get("email"), body.get("password"), body.get("name"));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        adminGuard.check(userDetails);
        User updated = userService.update(id, body.get("email"), body.get("password"), body.get("name"));
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<User> patch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        adminGuard.check(userDetails);
        User updated = userService.update(id, body.get("email"), body.get("password"), body.get("name"));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        adminGuard.check(userDetails);
        userService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fcmtoken")
    public ResponseEntity<Void> updateFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> body) {
        userService.updateFcmToken(userDetails.getUser().getId(), body.get("fcmToken"));
        return ResponseEntity.ok().build();
    }
}
