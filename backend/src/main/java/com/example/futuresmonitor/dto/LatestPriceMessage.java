package com.example.futuresmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatestPriceMessage {
    private String type = "LATEST_PRICE";
    private String symbol;
    private String time;
    private double price;
}
