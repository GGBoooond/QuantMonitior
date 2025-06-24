package com.example.futuresmonitor.service;

import com.example.futuresmonitor.dto.AlertMessage;
import com.example.futuresmonitor.dto.LatestPriceMessage;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FuturesMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(FuturesMonitorService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Autowired
    private QuantApiService quantApiService;

    @Autowired
    private AlertWebSocketHandler webSocketHandler;

    private final AtomicReference<String> currentMonitoringSymbol = new AtomicReference<>("RU00.SHF");
    private final ConcurrentHashMap<String, Double> highPriceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> lowPriceCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        update7DayPriceRange(currentMonitoringSymbol.get());
    }

    public void switchMonitoringSymbol(String newSymbol) {
        if (newSymbol != null && !newSymbol.trim().isEmpty() && !newSymbol.equals(currentMonitoringSymbol.get())) {
            logger.info("Switching monitoring symbol to: {}", newSymbol);
            currentMonitoringSymbol.set(newSymbol);
            highPriceCache.remove(newSymbol);
            lowPriceCache.remove(newSymbol);
            update7DayPriceRange(newSymbol);
        }
    }

    // 公开获取阈值的方法
    public Double getHighThreshold(String symbol) {
        return highPriceCache.get(symbol);
    }
    public Double getLowThreshold(String symbol) {
        return lowPriceCache.get(symbol);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledUpdate7DayPriceRange() {
        logger.info("Performing scheduled update for 7-day price range.");
        update7DayPriceRange(currentMonitoringSymbol.get());
    }

    @Scheduled(fixedRate = 5000)
    public void monitorRealTimePrice() {
        String symbol = currentMonitoringSymbol.get();
        if (symbol == null) return;

        Double high7d = highPriceCache.get(symbol);
        Double low7d = lowPriceCache.get(symbol);

        if (high7d == null || low7d == null) {
            update7DayPriceRange(symbol); // 尝试重新获取
            high7d = highPriceCache.get(symbol);
            low7d = lowPriceCache.get(symbol);
            if (high7d == null || low7d == null) {
                logger.error("Failed to fetch 7-day price range for {}. Skipping check.", symbol);
                return;
            }
        }

        try {
            JsonNode realTimeData = quantApiService.getRealTimeQuotation(symbol);
            // 根据新文档修正解析路径
            JsonNode tableNode = realTimeData.path("tables").path(0).path("table");
            if (!tableNode.isMissingNode() && tableNode.path("latest").isArray() && tableNode.path("latest").size() > 0) {
                double currentPrice = tableNode.path("latest").get(0).asDouble();
                String currentTime = realTimeData.path("tables").path(0).path("time").get(0).asText();

                logger.info("Symbol: {}, Current Price: {}, Time: {}", symbol, currentPrice, currentTime);

                // 广播最新价格给前端K线图
                webSocketHandler.broadcast(new LatestPriceMessage("LATEST_PRICE", symbol, currentTime, currentPrice));

                // 检查警报
                if (currentPrice > high7d) {
                    String message = String.format("【警报】%s 当前价格 %.2f 突破7日最高点 %.2f！", symbol, currentPrice, high7d);
                    logger.warn(message);
                    webSocketHandler.broadcast(new AlertMessage("ALERT_HIGH", symbol, currentPrice, high7d, currentTime, message));
                }
                if (currentPrice < low7d) {
                    String message = String.format("【警报】%s 当前价格 %.2f 跌破7日最低点 %.2f！", symbol, currentPrice, low7d);
                    logger.warn(message);
                    webSocketHandler.broadcast(new AlertMessage("ALERT_LOW", symbol, currentPrice, low7d, currentTime, message));
                }
            }
        } catch (IOException e) {
            logger.error("Error fetching real-time price for " + symbol, e);
        }
    }

    private void update7DayPriceRange(String symbol) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);
            JsonNode historyData = quantApiService.getHistoryQuotes(symbol, startDate.format(DATE_FORMATTER), endDate.format(DATE_FORMATTER), "high,low");

            // 根据新文档修正解析路径
            JsonNode tableData = historyData.path("tables").path(0).path("table");
            if (tableData.isMissingNode()) {
                logger.warn("No 'table' data found for {} in history response.", symbol);
                return;
            }

            double maxHigh = Double.MIN_VALUE;
            double minLow = Double.MAX_VALUE;

            if (tableData.path("high").isArray()) {
                for (JsonNode high : tableData.path("high")) {
                    if (high.isNumber()) maxHigh = Math.max(maxHigh, high.asDouble());
                }
            }
            if (tableData.path("low").isArray()) {
                for (JsonNode low : tableData.path("low")) {
                    if (low.isNumber()) minLow = Math.min(minLow, low.asDouble());
                }
            }

            if (maxHigh != Double.MIN_VALUE && minLow != Double.MAX_VALUE) {
                highPriceCache.put(symbol, maxHigh);
                lowPriceCache.put(symbol, minLow);
                logger.info("Updated 7-day price range for {}: High={}, Low={}", symbol, maxHigh, minLow);
            } else {
                logger.warn("Could not calculate 7-day price range for {}. History data might be empty.", symbol);
            }
        } catch (IOException e) {
            logger.error("Error updating 7-day price range for " + symbol, e);
        }
    }
}