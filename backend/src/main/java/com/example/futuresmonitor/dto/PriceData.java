package com.example.futuresmonitor.dto;

import lombok.Data;

@Data
public class PriceData {
    private String time;
    private double open;
    private double close;
    private double low;
    private double high;
}