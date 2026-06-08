package com.sv.mercurytarrifs.business;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.network.SyncClient;
import com.sv.mercurytarrifs.ui.LogManager;

import java.util.ArrayList;
import java.util.Map;

public class SyncManager {
    private final Context context;
    private final HistoryDatabase dbHelper;
    private final LogManager logManager;

    public SyncManager(Context context, HistoryDatabase dbHelper, LogManager logManager) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.logManager = logManager;
    }

    // ✅ Безопасный показ Toast из любого потока
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        );
    }

    public void syncWithServer(String serverUrl, String apiPath, String deviceKey) {
        // ✅ Безопасная работа с LogManager
        if (logManager != null) {
            logManager.setCurrentLogMessages(new StringBuilder());
            logManager.logMsg("═══════════════════════════════════════════");
            logManager.logMsg("☁️ СИНХРОНИЗАЦИЯ С СЕРВЕРОМ");
            logManager.logMsg("═══════════════════════════════════════════");
            logManager.logMsg("🌐 URL сервера: " + serverUrl);
            logManager.logMsg("📍 API путь: " + apiPath);
            logManager.logMsg("🔑 Ключ устройства: " + deviceKey);
            logManager.logMsg("═══════════════════════════════════════════");
        }

        if (serverUrl == null || serverUrl.isEmpty() || apiPath == null || apiPath.isEmpty()) {
            showToast("⚠️ Настройте сервер синхронизации");
            if (logManager != null) {
                logManager.logMsg("❌ Настройки не заполнены!");
                logManager.finishLogEntry("Синхронизация", false, 0, 0);
            }
            return;
        }

        if (logManager != null) {
            logManager.logMsg("📊 Чтение НЕотправленных записей из базы...");
        }

        new Thread(() -> {
            try {
                android.database.Cursor cursor = dbHelper.getUnsyncedHistory();
                int count = 0;
                int successCount = 0;
                int failCount = 0;
                ArrayList<Integer> syncedIds = new ArrayList<>();

                if (cursor != null) {
                    int totalRecords = cursor.getCount();

                    if (totalRecords == 0) {
                        if (logManager != null) logManager.logMsg("✅ Нет новых записей для отправки");
                        showToast("✅ Нет новых записей для отправки");
                    } else {
                        if (logManager != null) {
                            logManager.logMsg("📊 Найдено записей: " + totalRecords);
                            logManager.logMsg("═══════════════════════════════════════════");
                        }

                        while (cursor.moveToNext()) {
                            count++;
                            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                            int addr = cursor.getInt(cursor.getColumnIndexOrThrow("address"));
                            String datetime = cursor.getString(cursor.getColumnIndexOrThrow("datetime"));
                            long serial = cursor.getLong(cursor.getColumnIndexOrThrow("serial_number"));
                            double t1 = cursor.getDouble(cursor.getColumnIndexOrThrow("t1"));
                            double t2 = cursor.getDouble(cursor.getColumnIndexOrThrow("t2"));
                            double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));

                            if (logManager != null) {
                                logManager.logMsg("📤 Отправка записи #" + count + "/" + totalRecords);
                                logManager.logMsg("   ID: " + id);
                                logManager.logMsg("   Адрес: " + addr);
                                logManager.logMsg("   Серийник: " + serial);
                                logManager.logMsg("   Дата: " + datetime);
                                logManager.logMsg("   T1: " + t1 + " | T2: " + t2 + " | Σ: " + total);
                            }

                            boolean result = SyncClient.sendRecord(serverUrl, apiPath, deviceKey, addr, serial, datetime, t1, t2, total);

                            if (result) {
                                successCount++;
                                syncedIds.add(id);
                                if (logManager != null) logManager.logMsg("   ✅ Успешно");
                            } else {
                                failCount++;
                                if (logManager != null) logManager.logMsg("   ❌ Ошибка");
                            }

                            if (logManager != null) {
                                logManager.logMsg("───────────────────────────────────────────");
                            }
                        }
                    }
                    cursor.close();
                }

                // ✅ ОТМЕЧАЕМ ОТПРАВЛЕННЫЕ ЗАПИСИ
                if (!syncedIds.isEmpty()) {
                    for (int id : syncedIds) {
                        dbHelper.markAsSynced(id);
                    }
                    if (logManager != null) {
                        logManager.logMsg("✅ Отмечено записей как отправленные: " + syncedIds.size());
                    }
                }

                // ✅ ЗАГРУЗКА ИМЁН АБОНЕНТОВ С СЕРВЕРА
                if (logManager != null) {
                    logManager.logMsg("═══════════════════════════════════════════");
                    logManager.logMsg("👥 Загрузка имён абонентов с сервера...");
                }

                String subscribersApiPath = apiPath.replace("save_reading.php", "get_subscribers.php");
                Map<Long, String> namesMap = SyncClient.getSubscriberNames(serverUrl, subscribersApiPath, deviceKey);

                if (namesMap != null && !namesMap.isEmpty()) {
                    dbHelper.syncSubscriberNames(namesMap);
                    if (logManager != null) {
                        logManager.logMsg("✅ Загружено имён: " + namesMap.size());
                        logManager.logMsg("✅ Удалены имена которых нет на сервере");
                    }
                    showToast("✅ Загружено имён: " + namesMap.size());
                } else {
                    if (logManager != null) logManager.logMsg("⚠️ Имена не загружены");
                }

                final int finalCount = count;
                final int finalSuccess = successCount;
                final int finalFail = failCount;

                if (logManager != null) {
                    logManager.logMsg("═══════════════════════════════════════════");
                    logManager.logMsg("📊 ИТОГИ СИНХРОНИЗАЦИИ:");
                    logManager.logMsg("   Отправлено записей: " + finalCount);
                    logManager.logMsg("   Успешно: " + finalSuccess);
                    logManager.logMsg("   Ошибок: " + finalFail);
                    logManager.logMsg("   Загружено имён: " + (namesMap != null ? namesMap.size() : 0));
                    logManager.logMsg("═══════════════════════════════════════════");
                }

                String message = "✅ Синхронизация завершена!\n";
                if (finalCount > 0) {
                    message += "Отправлено: " + finalSuccess + "/" + finalCount + "\n";
                }
                if (namesMap != null && !namesMap.isEmpty()) {
                    message += "Загружено имён: " + namesMap.size();
                }
                showToast(message);

                if (logManager != null) {
                    logManager.finishLogEntry("Синхронизация", true, 0, 0);
                }

            } catch (Exception e) {
                if (logManager != null) {
                    logManager.logMsg("═══════════════════════════════════════════");
                    logManager.logMsg("❌ ОШИБКА СИНХРОНИЗАЦИИ:");
                    logManager.logMsg("    " + e.getMessage());
                    logManager.logMsg("   Тип: " + e.getClass().getSimpleName());
                    logManager.logMsg("═══════════════════════════════════════════");
                }
                showToast("❌ " + e.getMessage());
                if (logManager != null) {
                    logManager.finishLogEntry("Синхронизация", false, 0, 0);
                }
            }
        }).start();
    }
}