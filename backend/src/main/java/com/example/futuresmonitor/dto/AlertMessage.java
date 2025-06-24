package com.example.futuresmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertMessage {
    private String type; // "ALERT_HIGH", "ALERT_LOW"
    private String symbol;
    private double currentPrice;
    private double threshold;
    private String time;
    private String message;
}