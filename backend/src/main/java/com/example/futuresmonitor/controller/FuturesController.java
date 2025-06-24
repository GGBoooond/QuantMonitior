package com.example.futuresmonitor.controller;

import com.example.futuresmonitor.dto.PriceData;
import com.example.futuresmonitor.dto.Thresholds;
import com.example.futuresmonitor.service.FuturesMonitorService;
import com.example.futuresmonitor.service.QuantApiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/futures")
public class FuturesController {

    @Autowired
    private QuantApiService quantApiService;

    @Autowired
    private FuturesMonitorService monitorService;

    @GetMapping("/{symbol}/history")
    public List<PriceData> getHistoryForChart(
            @PathVariable String symbol,
            @RequestParam String startDate,
            @RequestParam String endDate) throws IOException {

        JsonNode historyData = quantApiService.getHistoryQuotes(symbol, startDate, endDate, "open,high,low,close");
        List<PriceData> priceDataList = new ArrayList<>();

        // 根据新文档修正解析路径
        if (historyData.path("tables").isArray() && historyData.path("tables").size() > 0) {
            JsonNode firstTable = historyData.path("tables").get(0);
            JsonNode dates = firstTable.path("time");
            JsonNode tableContent = firstTable.path("table");

            JsonNode openPrices = tableContent.path("open");
            JsonNode highPrices = tableContent.path("high");
            JsonNode lowPrices = tableContent.path("low");
            JsonNode closePrices = tableContent.path("close");

            if (dates.isArray()) {
                for (int i = 0; i < dates.size(); i++) {
                    if (openPrices.has(i) && !openPrices.get(i).isNull()) {
                        PriceData pd = new PriceData();
                        pd.setTime(dates.get(i).asText());
                        pd.setOpen(openPrices.get(i).asDouble());
                        pd.setClose(closePrices.get(i).asDouble());
                        pd.setLow(lowPrices.get(i).asDouble());
                        pd.setHigh(highPrices.get(i).asDouble());
                        priceDataList.add(pd);
                    }
                }
            }
        }
        return priceDataList;
    }

    // 新增获取阈值的接口
    @GetMapping("/{symbol}/thresholds")
    public Thresholds getThresholds(@PathVariable String symbol) {
        return new Thresholds(
                monitorService.getHighThreshold(symbol),
                monitorService.getLowThreshold(symbol)
        );
    }
}
