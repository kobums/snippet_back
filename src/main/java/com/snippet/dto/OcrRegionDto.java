package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OcrRegionDto {
    private double left;
    private double top;
    private double right;
    private double bottom;
}
