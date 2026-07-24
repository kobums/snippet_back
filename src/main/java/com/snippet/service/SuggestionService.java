package com.snippet.service;

import com.snippet.dto.SuggestionAddRequestDto;
import com.snippet.dto.SuggestionAdminDto;
import com.snippet.dto.SuggestionDto;
import com.snippet.entity.Suggestion;
import com.snippet.entity.User;
import com.snippet.repository.SuggestionRepository;
import com.snippet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional
    public SuggestionDto create(Long userId, SuggestionAddRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Suggestion suggestion = Suggestion.builder()
                .user(user)
                .category(dto.getCategory())
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();
        return SuggestionDto.from(suggestionRepository.save(suggestion));
    }

    @Transactional(readOnly = true)
    public List<SuggestionDto> findByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return suggestionRepository.findByUserOrderByCreateDateDesc(user).stream()
                .map(SuggestionDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SuggestionAdminDto> findAllForAdmin(String status) {
        List<Suggestion> suggestions = (status != null && !status.isBlank())
                ? suggestionRepository.findByStatusOrderByCreateDateDesc(status)
                : suggestionRepository.findAllByOrderByCreateDateDesc();
        return suggestions.stream()
                .map(SuggestionAdminDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SuggestionAdminDto answer(Long id, String answer, String status) {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("답변 내용을 입력해주세요.");
        }
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Suggestion not found: " + id));
        suggestion.answer(answer, (status != null && !status.isBlank()) ? status : "COMPLETED");

        User user = suggestion.getUser();
        if (user != null) {
            String title = (suggestion.getTitle() != null && !suggestion.getTitle().isBlank())
                    ? "'" + suggestion.getTitle() + "' 건의에 답변이 도착했어요"
                    : "보내주신 의견에 답변이 도착했어요";
            String body = answer.length() > 100 ? answer.substring(0, 100) + "…" : answer;
            fcmService.sendNotification(user.getFcmToken(), title, body);
        }
        return SuggestionAdminDto.from(suggestion);
    }
}
