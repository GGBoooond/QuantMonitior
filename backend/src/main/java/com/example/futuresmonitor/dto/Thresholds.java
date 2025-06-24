package com.example.futuresmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Thresholds {
    private Double high;
    private Double low;
}