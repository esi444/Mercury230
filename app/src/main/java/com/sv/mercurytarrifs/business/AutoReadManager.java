package com.sv.mercurytarrifs.business;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.sv.mercurytarrifs.MainActivity;
import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.prefs.AppPreferences;
import com.sv.mercurytarrifs.ui.LogManager;

import java.util.List;

public class AutoReadManager {
    private final Context context;
    private final MainActivity activity;
    private final HistoryDatabase dbHelper;
    private final LogManager logManager;
    private final AppPreferences prefs;
    private final ReadingManager readingManager;
    private final Handler mainHandler;

    private boolean isReading = false;
    private String currentSsid = null;
    private List<String> namesToRead;
    private int currentIndex = 0;
    private final StringBuilder successNames = new StringBuilder();
    private final StringBuilder failNames = new StringBuilder();

    public AutoReadManager(Context context, MainActivity activity, HistoryDatabase dbHelper,
                           LogManager logManager, AppPreferences prefs, ReadingManager readingManager) {
        this.context = context;
        this.activity = activity;
        this.dbHelper = dbHelper;
        this.logManager = logManager;
        this.prefs = prefs;
        this.readingManager = readingManager;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void checkAndStartAutoRead() {
        if (!prefs.isAutoReadEnabled()) return;
        String ssid = getCurrentWifiSsid();
        if (ssid == null || ssid.isEmpty()) return;
        if (isReading && currentSsid != null && currentSsid.equals(ssid)) return;
        if (isReading && (currentSsid == null || !currentSsid.equals(ssid))) stopReading();

        List<String> names = dbHelper.getNamesForSsid(ssid);
        if (names == null || names.isEmpty()) return;

        startReading(ssid, names);
    }

    private void startReading(String ssid, List<String> names) {
        isReading = true;
        currentSsid = ssid;
        namesToRead = names;
        currentIndex = 0;
        successNames.setLength(0);
        failNames.setLength(0);

        logManager.logMsg("═══════════════════════════════════════════");
        logManager.logMsg("🤖 АВТОСЧИТЫВАНИЕ: Подключено к \"" + ssid + "\"");
        logManager.logMsg("📋 Адреса для чтения: " + String.join(", ", names));
        logManager.logMsg("⏱️ Интервал: " + prefs.getAutoReadInterval() + " мс");
        logManager.logMsg("═══════════════════════════════════════════");

        readNextName();
    }

    private void readNextName() {
        if (currentIndex >= namesToRead.size()) {
            finishReading();
            return;
        }

        final String name = namesToRead.get(currentIndex);
        final int address = extractAddressFromName(name);

        logManager.logMsg("🔄 Авто-чтение #" + (currentIndex + 1) + "/" + namesToRead.size() + ": " + name + " (адрес: " + address + ")");

        readingManager.readEnergy(prefs.getIp(), prefs.getPort(), address, new Runnable() {
            @Override
            public void run() {
                // ✅ ВАЖНО: Получаем показания СРАЗУ после чтения, пока они не перезаписались
                final double t1 = readingManager.getCurrentT1();
                final double t2 = readingManager.getCurrentT2();
                final double total = readingManager.getCurrentTotal();
                final long serial = readingManager.getCurrentSerial();

                if (serial > 0) {
                    successNames.append(successNames.length() > 0 ? " | " : "").append(name);

                    // ✅ Подробный лог с показаниями ИМЕННО ЭТОГО адреса
                    logManager.logMsg("✅ Прочитано: " + name);
                    logManager.logMsg("   📊 T1: " + String.format("%.2f", t1) + " кВт⋅ч");
                    logManager.logMsg("   📊 T2: " + String.format("%.2f", t2) + " кВт⋅ч");
                    logManager.logMsg("   📊 Σ: " + String.format("%.2f", total) + " кВт⋅ч");
                    logManager.logMsg("   🔢 Серийник: " + serial);
                } else {
                    failNames.append(failNames.length() > 0 ? " | " : "").append(name);
                    logManager.logMsg("❌ Не прочитано: " + name);
                    mainHandler.post(() -> Toast.makeText(context, "⚠️ Не прочитаны показания " + name, Toast.LENGTH_SHORT).show());
                }

                currentIndex++;
                int interval = prefs.getAutoReadInterval();
                mainHandler.postDelayed(() -> readNextName(), interval);
            }
        });
    }

    private void finishReading() {
        isReading = false;

        // ✅ Автоматически обновляем историю после автосчитывания
        activity.loadHistoryWithFilter();

        mainHandler.post(() -> {
            StringBuilder toastMsg = new StringBuilder();
            if (successNames.length() > 0) toastMsg.append("✅ Прочитаны адреса: ").append(successNames);
            if (failNames.length() > 0) {
                if (toastMsg.length() > 0) toastMsg.append("\n");
                toastMsg.append("❌ Непрочитаны адреса: ").append(failNames);
            }
            if (toastMsg.length() > 0) Toast.makeText(context, toastMsg.toString(), Toast.LENGTH_LONG).show();
        });

        logManager.logMsg("═══════════════════════════════════════════");
        logManager.logMsg("✅ Автосчитывание завершено");
        if (successNames.length() > 0) logManager.logMsg("   Успешно: " + successNames);
        if (failNames.length() > 0) logManager.logMsg("   Ошибки: " + failNames);
        logManager.logMsg("═══════════════════════════════════════════");
        logManager.finishLogEntry("Автосчитывание", true, 0, 0);
    }

    public void stopReading() {
        isReading = false;
        currentSsid = null;
        namesToRead = null;
        logManager.logMsg("⏹️ Автосчитывание остановлено");
    }

    private String getCurrentWifiSsid() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String ssid = wifiInfo.getSSID();
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    if (ssid != null && !ssid.isEmpty() && !ssid.equals("<unknown ssid>")) return ssid;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private int extractAddressFromName(String name) {
        try {
            if (name.contains("/")) {
                String[] parts = name.split("/");
                return Integer.parseInt(parts[parts.length - 1].trim());
            }
            return Integer.parseInt(name.trim());
        } catch (Exception e) { return 1; }
    }

    public boolean isReading() { return isReading; }
    public String getCurrentSsid() { return currentSsid; }
}