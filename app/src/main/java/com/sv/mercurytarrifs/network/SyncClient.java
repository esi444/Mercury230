package com.sv.mercurytarrifs.network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class SyncClient {
    public static boolean sendRecord(String serverUrl, String apiPath, String deviceKey,
                                     int address, long serial, String datetime,
                                     double t1, double t2, double total) {
        try {
            String fullUrl = serverUrl + apiPath;
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            String postData = "device_key=" + URLEncoder.encode(deviceKey, "UTF-8") +
                    "&address=" + URLEncoder.encode(String.valueOf(address), "UTF-8") +
                    "&serial_number=" + URLEncoder.encode(String.valueOf(serial), "UTF-8") +
                    "&datetime=" + URLEncoder.encode(datetime, "UTF-8") +
                    "&t1=" + URLEncoder.encode(String.valueOf(t1), "UTF-8") +
                    "&t2=" + URLEncoder.encode(String.valueOf(t2), "UTF-8") +
                    "&total=" + URLEncoder.encode(String.valueOf(total), "UTF-8");

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            BufferedReader reader;
            if (responseCode >= 400) {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            String responseStr = response.toString();
            return responseStr.contains("\"success\":true") || responseStr.contains("\"success\": true");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ НОВОЕ: Загрузка имён абонентов с сервера
    public static Map<Long, String> getSubscriberNames(String serverUrl, String apiPath, String deviceKey) {
        try {
            String fullUrl = serverUrl + apiPath + "?device_key=" + URLEncoder.encode(deviceKey, "UTF-8");
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();

            BufferedReader reader;
            if (responseCode >= 400) {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            String responseStr = response.toString();

            // ✅ Лог для отладки
            android.util.Log.d("SyncClient", "Ответ сервера: " + responseStr);

            // ✅ Парсим JSON
            JSONObject json = new JSONObject(responseStr);
            if (json.optBoolean("success", false)) {
                JSONArray data = json.optJSONArray("data");
                Map<Long, String> namesMap = new HashMap<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject subscriber = data.getJSONObject(i);
                        // ✅ Получаем серийный номер (может быть String или Long)
                        String serialStr = subscriber.optString("serial_number", "0");
                        long serial = 0;
                        try {
                            serial = Long.parseLong(serialStr);
                        } catch (Exception e) {
                            serial = 0;
                        }
                        String name = subscriber.optString("custom_name", "—");
                        if (serial > 0) {
                            namesMap.put(serial, (name != null && !name.isEmpty()) ? name : "—");
                        }
                    }
                }
                android.util.Log.d("SyncClient", "Загружено имён: " + namesMap.size());
                return namesMap;
            } else {
                android.util.Log.d("SyncClient", "success = false");
            }

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.d("SyncClient", "Ошибка загрузки имён: " + e.getMessage());
        }
        return null;
    }
}