package com.example.futuresmonitor.dto;

import lombok.Data;

@Data
public class WebSocketRequest {
    private String type; // e.g., "subscribe"
    private String symbol;
}