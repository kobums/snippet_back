package com.snippet.controller;

import com.snippet.dto.SuggestionAdminDto;
import com.snippet.security.AdminGuard;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/suggestions")
@RequiredArgsConstructor
public class AdminSuggestionController {

    private final SuggestionService suggestionService;
    private final AdminGuard adminGuard;

    @GetMapping
    public ResponseEntity<List<SuggestionAdminDto>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String status) {
        adminGuard.check(userDetails);
        return ResponseEntity.ok(suggestionService.findAllForAdmin(status));
    }

    @PatchMapping("/{id}/answer")
    public ResponseEntity<SuggestionAdminDto> answer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        adminGuard.check(userDetails);
        return ResponseEntity.ok(suggestionService.answer(id, body.get("answer"), body.get("status")));
    }
}
