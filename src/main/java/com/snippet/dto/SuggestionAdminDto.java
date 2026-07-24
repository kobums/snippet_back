package com.snippet.dto;

import com.snippet.entity.Suggestion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SuggestionAdminDto {
    private Long id;
    private String userEmail;
    private String userName;
    private boolean pushAvailable;
    private String category;
    private String title;
    private String content;
    private String status;
    private String answer;
    private LocalDateTime answerDate;
    private LocalDateTime createDate;

    public static SuggestionAdminDto from(Suggestion s) {
        return SuggestionAdminDto.builder()
                .id(s.getId())
                .userEmail(s.getUser() != null ? s.getUser().getEmail() : null)
                .userName(s.getUser() != null ? s.getUser().getName() : null)
                .pushAvailable(s.getUser() != null
                        && s.getUser().getFcmToken() != null
                        && !s.getUser().getFcmToken().isBlank())
                .category(s.getCategory())
                .title(s.getTitle())
                .content(s.getContent())
                .status(s.getStatus())
                .answer(s.getAnswer())
                .answerDate(s.getAnswerDate())
                .createDate(s.getCreateDate())
                .build();
    }
}
