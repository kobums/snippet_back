package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StreakDto {
    private int currentStreak;
    private int maxStreak;
    private String lastReadDate;
}
