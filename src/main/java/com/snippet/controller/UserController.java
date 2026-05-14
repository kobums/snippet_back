package com.snippet.controller;

import com.snippet.entity.User;
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

    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody Map<String, String> body) {
        User user = userService.create(body.get("email"), body.get("password"), body.get("name"));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User updated = userService.update(id, body.get("email"), body.get("password"), body.get("name"));
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<User> patch(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User updated = userService.update(id, body.get("email"), body.get("password"), body.get("name"));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
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
