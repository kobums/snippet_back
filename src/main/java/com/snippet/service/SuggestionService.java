package com.snippet.service;

import com.snippet.dto.SuggestionAddRequestDto;
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
}
