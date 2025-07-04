package com.example.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class QuantApiClient {
    private static final String BASE_URL = "https://quantapi.51ifind.com/api/v1/";
    private static String accessToken;
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        // First get the access token
//        getAccessToken("此处填写refresh_token");
        accessToken = "7e309032eab153adc8eb21651dafd42e12ca78f7.signs_Nzg4Nzk0MDgx";
        // Then you can call any of the API functions
        try {
            realTime();
            highFrequency();
            // historyQuotes();
            // basicData();
            // dateSerial();
            // dataPool();
            // highFrequency();
            // edb();
            // snapShot();
            // reportQuery();
            // wcQuery();
            // dateOffset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getAccessToken(String refreshToken) {
        String url = BASE_URL + "get_access_token";

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("refresh_token", refreshToken);

        Request request = new Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .post(RequestBody.create("", MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JsonNode rootNode = objectMapper.readTree(response.body().string());
            accessToken = rootNode.path("data").path("access_token").asText();
            System.out.println("Access Token: " + accessToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void realTime() throws IOException {
        String url = BASE_URL + "real_time_quotation";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("codes", "PFZL.CZC");
        requestBody.put("indicators", "latest");

        while (true) {
            Request request = createRequest(url, requestBody);

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                String responseData = response.body().string();
                System.out.println(responseData);

                // Process the response data as needed
                // You can use Jackson to parse the JSON into Java objects

//                TimeUnit.SECONDS.sleep(3);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void historyQuotes() throws IOException {
        String url = BASE_URL + "cmd_history_quotation";

        Map<String, Object> functionPara = new HashMap<>();
        functionPara.put("Fill", "Blank");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("codes", "000001.SZ,600000.SH");
        requestBody.put("indicators", "open,high,low,close");
        requestBody.put("startdate", "2021-07-05");
        requestBody.put("enddate", "2022-07-05");
        requestBody.put("functionpara", functionPara);

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static void basicData() throws IOException {
        String url = BASE_URL + "basic_data_service";

        List<Map<String, Object>> indiPara = new ArrayList<>();

        Map<String, Object> indi1 = new HashMap<>();
        indi1.put("indicator", "ths_regular_report_actual_dd_stock");
        indi1.put("indiparams", Arrays.asList("104"));

        Map<String, Object> indi2 = new HashMap<>();
        indi2.put("indicator", "ths_total_shares_stock");
        indi2.put("indiparams", Arrays.asList("20220705"));

        indiPara.add(indi1);
        indiPara.add(indi2);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("codes", "300033.SZ,600000.SH");
        requestBody.put("indipara", indiPara);

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static void dateSerial() throws IOException {
        String url = BASE_URL + "date_sequence";

        Map<String, Object> functionPara = new HashMap<>();
        functionPara.put("Fill", "Blank");

        List<Map<String, Object>> indiPara = new ArrayList<>();

        Map<String, Object> indi1 = new HashMap<>();
        indi1.put("indicator", "ths_close_price_stock");
        indi1.put("indiparams", Arrays.asList("", "100", ""));

        Map<String, Object> indi2 = new HashMap<>();
        indi2.put("indicator", "ths_total_shares_stock");
        indi2.put("indiparams", Arrays.asList(""));

        indiPara.add(indi1);
        indiPara.add(indi2);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("codes", "000001.SZ,600000.SH");
        requestBody.put("startdate", "20220605");
        requestBody.put("enddate", "20220705");
        requestBody.put("functionpara", functionPara);
        requestBody.put("indipara", indiPara);

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static void dataPool() throws IOException {
        String url = BASE_URL + "data_pool";

        Map<String, Object> functionPara = new HashMap<>();
        functionPara.put("date", "20220706");
        functionPara.put("blockname", "001005010");
        functionPara.put("iv_type", "allcontract");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("reportname", "p03425");
        requestBody.put("functionpara", functionPara);
        requestBody.put("outputpara", "p03291_f001,p03291_f002,p03291_f003,p03291_f004");

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static void highFrequency() throws IOException {
        String url = BASE_URL + "high_frequency";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("codes", "000001.SZ");
        requestBody.put("indicators", "open,high,low,close,volume,amount,changeRatio");
        requestBody.put("starttime", "2025-06-23 14:15:00");
        requestBody.put("endtime", "2025-06-24 00:00:00");

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static void edb() throws IOException {
        String url = BASE_URL + "edb_service";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("indicators", "G009035746");
        requestBody.put("startdate", "2022-04-01");
        requestBody.put("enddate", "2022-05-01");

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static void snapShot() throws IOException {
        String url = BASE_URL + "snap_shot";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("codes", "000001.SZ");
        requestBody.put("indicators", "open,high,low,latest,bid1,ask1,bidSize1,askSize1");
        requestBody.put("starttime", "2022-07-06 09:15:00");
        requestBody.put("endtime", "2022-07-06 15:15:00");

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static void reportQuery() throws IOException {
        String url = BASE_URL + "report_query";

        Map<String, Object> functionPara = new HashMap<>();
        functionPara.put("reportType", "901");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("codes", "000001.SZ,600000.SH");
        requestBody.put("functionpara", functionPara);
        requestBody.put("beginrDate", "2021-01-01");
        requestBody.put("endrDate", "2022-07-06");
        requestBody.put("outputpara", "reportDate:Y,thscode:Y,secName:Y,ctime:Y,reportTitle:Y,pdfURL:Y,seq:Y");

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static void wcQuery() throws IOException {
        String url = BASE_URL + "smart_stock_picking";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("searchstring", "涨跌幅");
        requestBody.put("searchtype", "stock");

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static void dateOffset() throws IOException {
        String url = BASE_URL + "get_trade_dates";

        Map<String, Object> functionPara = new HashMap<>();
        functionPara.put("dateType", "0");
        functionPara.put("period", "D");
        functionPara.put("offset", "-10");
        functionPara.put("dateFormat", "0");
        functionPara.put("output", "sequencedate");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("marketcode", "212001");
        requestBody.put("functionpara", functionPara);
        requestBody.put("startdate", "2022-07-05");

        Request request = createRequest(url, requestBody);
        executeRequest(request);
    }

    private static Request createRequest(String url, Map<String, Object> requestBody) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        return new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("access_token", accessToken)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
    }

    private static void executeRequest(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseData = response.body().string();
            System.out.println(responseData);
        }
    }
}
