package com.sv.mercurytarrifs.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sv.mercurytarrifs.business.SyncManager;
import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.prefs.AppPreferences;

public class AutoSyncWorker extends Worker {

    public AutoSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        Log.d("AutoSyncWorker", "🔧 Конструктор воркера");
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("AutoSyncWorker", "🚀 doWork() запущен");

        try {
            Context context = getApplicationContext();
            Log.d("AutoSyncWorker", "📦 Контекст: " + (context != null ? "OK" : "NULL"));

            AppPreferences prefs = new AppPreferences(context);
            Log.d("AutoSyncWorker", "⚙️ Загрузка настроек...");

            String serverUrl = prefs.getServerUrl();
            String apiPath = prefs.getApiPath();
            String deviceKey = prefs.getDeviceKey();

            Log.d("AutoSyncWorker", "🌐 URL: " + serverUrl);
            Log.d("AutoSyncWorker", "📍 API: " + apiPath);
            Log.d("AutoSyncWorker", "🔑 Key: " + deviceKey);

            if (serverUrl == null || serverUrl.isEmpty() || apiPath == null || apiPath.isEmpty()) {
                Log.e("AutoSyncWorker", "❌ Настройки пустые!");
                return Result.failure();
            }

            HistoryDatabase dbHelper = new HistoryDatabase(context);
            Log.d("AutoSyncWorker", "🗄️ База данных: OK");

            SyncManager syncManager = new SyncManager(context, dbHelper, null);
            Log.d("AutoSyncWorker", "🔄 SyncManager создан");

            Log.d("AutoSyncWorker", "📡 Начинаем отправку...");
            syncManager.syncWithServer(serverUrl, apiPath, deviceKey);
            Log.d("AutoSyncWorker", "✅ Отправка завершена");

            return Result.success();

        } catch (Exception e) {
            Log.e("AutoSyncWorker", "❌ КРАШ: " + e.getMessage(), e);
            Log.e("AutoSyncWorker", "❌ StackTrace: " + android.util.Log.getStackTraceString(e));
            return Result.retry();
        }
    }
}