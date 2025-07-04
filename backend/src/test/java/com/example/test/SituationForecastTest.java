package com.example.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SituationForecastTest {
    // === API 配置 ===
    private static final String BASE_URL = "https://quantapi.51ifind.com/api/v1/";
    private static String accessToken = "6211b1171151205776137e37f6a395aff052dcb2.signs_NzkxNzIxOTA1"; // 请替换为您的有效token

    // === CSV 文件配置 ===
    private static final String CSV_FILE_PATH = "market_data.csv"; // 数据将保存在项目根目录的这个文件中
    private static final String CSV_HEADER = "时间,代码,最新价\n";

    // === HTTP 和 JSON 工具 ===
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // === 交易时间段定义 ===
    private static final LocalTime MORNING_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_END = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 30);
    private static final LocalTime AFTERNOON_END = LocalTime.of(15, 0);
    private static final LocalTime NIGHT_START = LocalTime.of(21, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(23, 0);


    public static void main(String[] args) throws IOException {
        // 程序启动时，检查CSV文件是否存在，如果不存在则创建并写入表头
        initializeCsvFile();
        // 开始实时获取数据
        realTime();
    }

    /**
     * 初始化CSV文件，如果文件不存在，则创建并写入表头。
     */
    private static void initializeCsvFile() {
        File file = new File(CSV_FILE_PATH);
        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append(CSV_HEADER);
                System.out.println("CSV文件已创建，并写入表头。");
            } catch (IOException e) {
                System.err.println("创建或写入CSV表头时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查当前时间是否在指定的交易时段内。
     * @return 如果在交易时段内返回 true，否则返回 false。
     */
    private static boolean isTradingHours() {
        LocalTime now = LocalTime.now();
        // 检查是否在上午时段 (9:00 - 11:30)
        boolean isMorningSession = now.isAfter(MORNING_START) && now.isBefore(MORNING_END);
        // 检查是否在下午时段 (13:30 - 15:00)
        boolean isAfternoonSession = now.isAfter(AFTERNOON_START) && now.isBefore(AFTERNOON_END);
        // 检查是否在夜间时段 (21:00 - 23:00)
        boolean isNightSession = now.isAfter(NIGHT_START) && now.isBefore(NIGHT_END);

        return isMorningSession || isAfternoonSession || isNightSession;
    }

    /**
     * 实时获取数据的主逻辑。
     */
    private static void realTime() {
        String url = BASE_URL + "real_time_quotation";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("codes", "PFZL.CZC");
        requestBody.put("indicators", "latest");

        while (true) {
            try {
                // 检查当前是否为交易时间
                if (!isTradingHours()) {
                    Request request = createRequest(url, requestBody);
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            String errorResponse = response.body() != null ? response.body().string() : "No response body";
                            System.err.println("API请求失败: " + response.code() + " " + response.message());
                            System.err.println("错误响应体: " + errorResponse);
                            // 如果请求失败，可以等待一段时间再重试
                            TimeUnit.SECONDS.sleep(5);
                            continue;
                        }

                        String responseData = response.body().string();
                        System.out.println("成功获取数据: " + responseData);

                        // 解析JSON并写入CSV
                        processAndSaveData(responseData);

                        // 按照原逻辑，每秒查询一次
                        TimeUnit.SECONDS.sleep(1);
                    }
                } else {
                    // 如果不在交易时间，则打印提示并休眠1分钟
                    System.out.println("当前不在交易时段，程序休眠1分钟... 当前时间: " + LocalTime.now());
                    TimeUnit.MINUTES.sleep(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 发生异常后，为避免程序退出，可以选择休眠一段时间后继续
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 解析API返回的JSON数据，并将其中的关键字段追加到CSV文件中。
     * @param jsonData API返回的JSON字符串
     */
    @SuppressWarnings("unchecked")
    private static void processAndSaveData(String jsonData) {
        try {
            Map<String, Object> dataMap = objectMapper.readValue(jsonData, Map.class);

            // 检查errorcode是否为0
            if ((Integer) dataMap.get("errorcode") != 0) {
                System.err.println("API返回错误信息: " + dataMap.get("errmsg"));
                return;
            }

            List<Map<String, Object>> tables = (List<Map<String, Object>>) dataMap.get("tables");
            if (tables != null && !tables.isEmpty()) {
                Map<String, Object> firstTable = tables.get(0);
                String thscode = (String) firstTable.get("thscode");
                List<String> timeList = (List<String>) firstTable.get("time");
                String time = (timeList != null && !timeList.isEmpty()) ? timeList.get(0) : "N/A";

                Map<String, Object> tableData = (Map<String, Object>) firstTable.get("table");
                List<Double> latestList = (List<Double>) tableData.get("latest");
                Double latest = (latestList != null && !latestList.isEmpty()) ? latestList.get(0) : Double.NaN;

                // 准备要写入CSV的行
                String csvRow = String.format("%s,%s,%.2f\n", time, thscode, latest);

                // 使用FileWriter以追加模式写入文件
                try (FileWriter writer = new FileWriter(CSV_FILE_PATH, true)) {
                    writer.append(csvRow);
                    System.out.println("数据已成功保存到 " + CSV_FILE_PATH);
                }
            }
        } catch (IOException e) {
            System.err.println("解析JSON或写入CSV文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建一个POST请求。
     */
    private static Request createRequest(String url, Map<String, Object> requestBody) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        return new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("access_token", accessToken)
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();
    }
}
