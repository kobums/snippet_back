package com.snippet.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DateUpdateRequestDto {
    private String startDate; // yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss
    private String endDate; // yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss
}
