package com.sv.mercurytarrifs.business;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.sv.mercurytarrifs.MainActivity;
import com.sv.mercurytarrifs.data.AddressNamePair;
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
    private List<AddressNamePair> itemsToRead; // ✅ Теперь хранит пары (адрес, имя)
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

        // ✅ НОВЫЙ МЕТОД: получаем пары (адрес, имя)
        List<AddressNamePair> pairs = dbHelper.getAddressNamesForSsid(ssid);
        if (pairs == null || pairs.isEmpty()) return;

        startReading(ssid, pairs);
    }

    private void startReading(String ssid, List<AddressNamePair> pairs) {
        isReading = true;
        currentSsid = ssid;
        itemsToRead = pairs;
        currentIndex = 0;
        successNames.setLength(0);
        failNames.setLength(0);

        logManager.logMsg("═══════════════════════════════════════════");
        logManager.logMsg("🤖 АВТОСЧИТЫВАНИЕ: Подключено к \"" + ssid + "\"");

        // ✅ Логируем адреса для чтения
        StringBuilder namesLog = new StringBuilder();
        for (AddressNamePair pair : pairs) {
            if (namesLog.length() > 0) namesLog.append(", ");
            namesLog.append(pair.name).append(" (адр. ").append(pair.address).append(")");
        }
        logManager.logMsg("📋 Счётчики для чтения: " + namesLog.toString());
        logManager.logMsg("⏱️ Интервал: " + prefs.getAutoReadInterval() + " мс");
        logManager.logMsg("═══════════════════════════════════════════");

        readNextItem();
    }

    private void readNextItem() {
        if (currentIndex >= itemsToRead.size()) {
            finishReading();
            return;
        }

        final AddressNamePair item = itemsToRead.get(currentIndex);
        final int address = item.address; // ✅ Адрес берём напрямую из БД!
        final String name = item.name;

        logManager.logMsg("🔄 Авто-чтение #" + (currentIndex + 1) + "/" + itemsToRead.size() +
                ": " + name + " (адрес: " + address + ")");

        readingManager.readEnergy(prefs.getIp(), prefs.getPort(), address, new Runnable() {
            @Override
            public void run() {
                if (readingManager.getCurrentSerial() > 0) {
                    successNames.append(successNames.length() > 0 ? " | " : "").append(name);
                    logManager.logMsg("✅ Прочитано: " + name);
                    logManager.logMsg("   🔢 Серийник: " + readingManager.getCurrentSerial());
                    logManager.logMsg("   📊 T1: " + String.format("%.2f", readingManager.getCurrentT1()) + " кВт⋅ч");
                    logManager.logMsg("   📊 T2: " + String.format("%.2f", readingManager.getCurrentT2()) + " кВт⋅ч");
                    logManager.logMsg("   📊 Σ: " + String.format("%.2f", readingManager.getCurrentTotal()) + " кВт⋅ч");
                } else {
                    failNames.append(failNames.length() > 0 ? " | " : "").append(name);
                    logManager.logMsg("❌ Не прочитано: " + name);
                    mainHandler.post(() -> Toast.makeText(context, "⚠️ Не прочитаны показания " + name, Toast.LENGTH_SHORT).show());
                }

                currentIndex++;
                int interval = prefs.getAutoReadInterval();
                mainHandler.postDelayed(() -> readNextItem(), interval);
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
        itemsToRead = null;
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

    public boolean isReading() { return isReading; }
    public String getCurrentSsid() { return currentSsid; }
}