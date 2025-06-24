package com.example.futuresmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class QuantApiService {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${quant.api.base-url}")
    private String baseUrl;

    @Value("${quant.api.access-token}")
    private String accessToken;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取实时行情 (根据新文档修正解析逻辑)
     */
    public JsonNode getRealTimeQuotation(String code) throws IOException {
        String url = baseUrl + "real_time_quotation";
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("codes", code);
        requestBodyMap.put("indicators", "latest");

        return executePostRequest(url, requestBodyMap);
    }

    /**
     * 获取历史行情 (根据新文档修正解析逻辑，并接收日期参数)
     */
    public JsonNode getHistoryQuotes(String code, String startDate, String endDate, String indicators) throws IOException {
        String url = baseUrl + "cmd_history_quotation";

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("codes", code);
        requestBodyMap.put("indicators", indicators);
        requestBodyMap.put("startdate", startDate);
        requestBodyMap.put("enddate", endDate);

        Map<String, String> functionPara = new HashMap<>();
        functionPara.put("Fill", "Blank");
        requestBodyMap.put("functionpara", functionPara);

        return executePostRequest(url, requestBodyMap);
    }

    /**
     * 获取高频分钟线数据 (用于K线图实时更新)
     */
    public JsonNode getHighFrequencyQuotes(String code, LocalDateTime startDateTime, LocalDateTime endDateTime) throws IOException {
        String url = baseUrl + "high_frequency";

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("codes", code);
        requestBodyMap.put("indicators", "open,high,low,close");
        requestBodyMap.put("starttime", startDateTime.format(DATETIME_FORMATTER));
        requestBodyMap.put("endtime", endDateTime.format(DATETIME_FORMATTER));

        return executePostRequest(url, requestBodyMap);
    }


    private JsonNode executePostRequest(String url, Map<String, Object> bodyMap) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(bodyMap);
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("access_token", this.accessToken)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + " with body: " + response.body().string());
            }
            if (response.body() == null) {
                throw new IOException("Response body is null");
            }
            String responseData = response.body().string();
            JsonNode rootNode = objectMapper.readTree(responseData);

            // 统一错误检查
            if (rootNode.path("errorcode").asInt() != 0) {
                throw new IOException("API Error: " + rootNode.path("errmsg").asText());
            }

            return rootNode;
        }
    }
}