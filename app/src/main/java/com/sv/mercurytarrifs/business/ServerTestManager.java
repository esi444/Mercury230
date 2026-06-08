// pokaz/business/ServerTestManager.java - Тест подключения к серверу

package com.sv.mercurytarrifs.business;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import com.sv.mercurytarrifs.ui.LogManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerTestManager {

    private final Context context;
    private final LogManager logManager;
    private final TextView tvTestResponse;
    private final Handler mainHandler;  // ✅ Handler для UI потока

    public ServerTestManager(Context context, LogManager logManager, TextView tvTestResponse) {
        this.context = context;
        this.logManager = logManager;
        this.tvTestResponse = tvTestResponse;
        this.mainHandler = new Handler(Looper.getMainLooper());  // ✅ Инициализация
    }

    public void testServerConnection(String serverUrl, String testUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            showToast("⚠️ Заполните URL сервера");
            return;
        }

        logManager.setCurrentLogMessages(new StringBuilder());

        logManager.logMsg("═══════════════════════════════════════════");
        logManager.logMsg("🧪 ТЕСТ ПОДКЛЮЧЕНИЯ К СЕРВЕРУ");
        logManager.logMsg("═══════════════════════════════════════════");
        logManager.logMsg("🌐 URL сервера: " + serverUrl);
        logManager.logMsg("🧪 Тест URL: " + testUrl);
        logManager.logMsg("🔗 Полный URL: " + serverUrl + testUrl);
        logManager.logMsg("═══════════════════════════════════════════");

        showTestResponse("🔄 Подключение...");

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String fullUrl = serverUrl + testUrl;
                URL url = new URL(fullUrl);

                logManager.logMsg("📡 Создание соединения...");
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Connection", "close");

                logManager.logMsg("📋 User-Agent: Mozilla/5.0");
                logManager.logMsg("📋 Accept: application/json");

                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(true);

                logManager.logMsg("⏱️ Таймаут: 10 секунд");
                logManager.logMsg("🔄 Редиректы: включены");

                int responseCode = conn.getResponseCode();
                String responseMessage = conn.getResponseMessage();

                logManager.logMsg("═══════════════════════════════════════════");
                logManager.logMsg("📊 ОТВЕТ СЕРВЕРА:");
                logManager.logMsg("   HTTP Код: " + responseCode);
                logManager.logMsg("   HTTP Сообщение: " + responseMessage);
                logManager.logMsg("═══════════════════════════════════════════");

                InputStream is;
                if (responseCode >= 400) {
                    is = conn.getErrorStream();
                    logManager.logMsg("⚠️ Ошибка сервера (читаем error stream)");
                } else {
                    is = conn.getInputStream();
                    logManager.logMsg("✅ Успешный ответ (читаем input stream)");
                }

                if (is != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String responseStr = response.toString();

                    logManager.logMsg("═══════════════════════════════════════════");
                    logManager.logMsg("📄 ТЕЛО ОТВЕТА:");
                    logManager.logMsg(responseStr);
                    logManager.logMsg("═══════════════════════════════════════════");

                    try {
                        JSONObject json = new JSONObject(responseStr);
                        String message = json.optString("message", "OK");
                        String phpVersion = json.optString("php_version", "?");
                        String sqlite = json.optString("sqlite", "?");

                        logManager.logMsg("📋 Сообщение: " + message);
                        logManager.logMsg("🐘 PHP версия: " + phpVersion);
                        logManager.logMsg("💾 SQLite: " + sqlite);

                        showTestResponse("✅ " + message + "\nPHP: " + phpVersion + " | SQLite: " + sqlite);

                        // ✅ TOAST ЧЕРЕЗ Handler (надёжно!)
                        showToast("✅ Сервер подключён!");

                        logManager.finishLogEntry("Тест подключения", true, 0, 0);

                    } catch (Exception e) {
                        logManager.logMsg("❌ Ошибка парсинга JSON: " + e.getMessage());
                        showTestResponse("⚠️ Ответ получен, но JSON не распарсен");
                        logManager.finishLogEntry("Тест подключения", false, 0, 0);
                    }
                } else {
                    logManager.logMsg("❌ Пустой ответ от сервера");
                    showTestResponse("❌ Пустой ответ");
                    logManager.finishLogEntry("Тест подключения", false, 0, 0);
                }

                conn.disconnect();
                logManager.logMsg("🔌 Соединение закрыто");

            } catch (Exception e) {
                logManager.logMsg("═══════════════════════════════════════════");
                logManager.logMsg("❌ ОШИБКА ПОДКЛЮЧЕНИЯ:");
                logManager.logMsg("   Тип: " + e.getClass().getSimpleName());
                logManager.logMsg("   Сообщение: " + e.getMessage());
                logManager.logMsg("═══════════════════════════════════════════");

                showTestResponse("❌ " + e.getMessage());
                logManager.finishLogEntry("Тест подключения", false, 0, 0);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    // ✅ ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ TOAST НА UI ПОТОКЕ
    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    private void showTestResponse(String response) {
        if (tvTestResponse != null) {
            mainHandler.post(() -> {
                tvTestResponse.setText("Ответ сервера: " + response);
                tvTestResponse.setVisibility(TextView.VISIBLE);
            });
        }
    }
}