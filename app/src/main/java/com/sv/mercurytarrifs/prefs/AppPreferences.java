package com.sv.mercurytarrifs.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREFS_NAME = "mercury_prefs";
    private static final String KEY_IP = "ip";
    private static final String KEY_PORT = "port";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_TIME_PASSWORD = "time_password";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_API_PATH = "api_path";
    private static final String KEY_TEST_URL = "test_url";
    private static final String KEY_DEVICE_KEY = "device_key";
    private static final String KEY_TABS_UNLOCKED = "tabs_unlocked";
    private static final String KEY_TAP_COUNTER = "tap_counter";
    private static final String KEY_HISTORY_EXPANDED = "history_expanded";
    private static final String KEY_LOG_EXPANDED = "log_expanded";
    private static final String KEY_SYNC_EXPANDED = "sync_expanded";
    private static final String KEY_TEST_READING_EXPANDED = "test_reading_expanded";
    private static final String KEY_NETWORK_EXPANDED = "network_expanded";
    private static final String KEY_NETWORK_SERVICE_EXPANDED = "network_service_expanded";
    private static final String KEY_TEST_T1 = "test_t1";
    private static final String KEY_TEST_T2 = "test_t2";
    private static final String KEY_TEST_TOTAL = "test_total";
    private static final String KEY_TEST_SERIAL = "test_serial";
    private static final String KEY_TEST_ADDR = "test_addr";
    private static final String KEY_LAST_SELECTED_ADDRESS = "last_selected_address";

    // ✅ НОВЫЕ: Для функции Автосчитывания
    private static final String KEY_AUTO_READ_ENABLED = "auto_read_enabled";
    private static final String KEY_AUTO_READ_INTERVAL = "auto_read_interval";

    // ✅ НОВЫЕ: Для авто-синхронизации
    private static final String KEY_AUTO_SYNC_HOUR = "auto_sync_hour";
    private static final String KEY_AUTO_SYNC_MINUTE = "auto_sync_minute";
    private static final String KEY_AUTO_SYNC_PERIOD = "auto_sync_period";
    private static final String KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled";

    private static final String DEFAULT_IP = "10.10.100.254";
    private static final int DEFAULT_PORT = 8899;
    private static final int DEFAULT_ADDRESS = 1;
    private static final String DEFAULT_TIME_PASSWORD = "111111";
    private static final String DEFAULT_SERVER_URL = "http://a96497eh.beget.tech";
    private static final String DEFAULT_API_PATH = "/pokaz/api/save_reading.php";
    private static final String DEFAULT_TEST_URL = "/pokaz/api/test.php";
    private static final String DEFAULT_DEVICE_KEY = "mobile";
    private static final int DEFAULT_AUTO_READ_INTERVAL = 200;

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getIp() { return prefs.getString(KEY_IP, DEFAULT_IP); }
    public void setIp(String ip) { prefs.edit().putString(KEY_IP, ip).apply(); }

    public int getPort() { return prefs.getInt(KEY_PORT, DEFAULT_PORT); }
    public void setPort(int port) { prefs.edit().putInt(KEY_PORT, port).apply(); }

    public int getAddress() { return prefs.getInt(KEY_ADDRESS, DEFAULT_ADDRESS); }
    public void setAddress(int address) { prefs.edit().putInt(KEY_ADDRESS, address).apply(); }

    public String getTimePassword() { return prefs.getString(KEY_TIME_PASSWORD, DEFAULT_TIME_PASSWORD); }
    public void setTimePassword(String password) { prefs.edit().putString(KEY_TIME_PASSWORD, password).apply(); }

    public String getServerUrl() { return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL); }
    public void setServerUrl(String url) { prefs.edit().putString(KEY_SERVER_URL, url).apply(); }

    public String getApiPath() { return prefs.getString(KEY_API_PATH, DEFAULT_API_PATH); }
    public void setApiPath(String path) { prefs.edit().putString(KEY_API_PATH, path).apply(); }

    public String getTestUrl() { return prefs.getString(KEY_TEST_URL, DEFAULT_TEST_URL); }
    public void setTestUrl(String url) { prefs.edit().putString(KEY_TEST_URL, url).apply(); }

    public String getDeviceKey() { return prefs.getString(KEY_DEVICE_KEY, DEFAULT_DEVICE_KEY); }
    public void setDeviceKey(String key) { prefs.edit().putString(KEY_DEVICE_KEY, key).apply(); }

    public boolean isTabsUnlocked() { return prefs.getBoolean(KEY_TABS_UNLOCKED, false); }
    public void setTabsUnlocked(boolean unlocked) { prefs.edit().putBoolean(KEY_TABS_UNLOCKED, unlocked).apply(); }

    public int getTapCounter() { return prefs.getInt(KEY_TAP_COUNTER, 0); }
    public void setTapCounter(int count) { prefs.edit().putInt(KEY_TAP_COUNTER, count).apply(); }

    public boolean isHistoryExpanded() { return prefs.getBoolean(KEY_HISTORY_EXPANDED, false); }
    public void setHistoryExpanded(boolean expanded) { prefs.edit().putBoolean(KEY_HISTORY_EXPANDED, expanded).apply(); }

    public boolean isLogExpanded() { return prefs.getBoolean(KEY_LOG_EXPANDED, false); }
    public void setLogExpanded(boolean expanded) { prefs.edit().putBoolean(KEY_LOG_EXPANDED, expanded).apply(); }

    public boolean isSyncExpanded() { return prefs.getBoolean(KEY_SYNC_EXPANDED, false); }
    public void setSyncExpanded(boolean expanded) { prefs.edit().putBoolean(KEY_SYNC_EXPANDED, expanded).apply(); }

    public boolean isTestReadingExpanded() { return prefs.getBoolean(KEY_TEST_READING_EXPANDED, false); }
    public void setTestReadingExpanded(boolean expanded) { prefs.edit().putBoolean(KEY_TEST_READING_EXPANDED, expanded).apply(); }

    public boolean isNetworkExpanded() { return prefs.getBoolean(KEY_NETWORK_EXPANDED, false); }
    public void setNetworkExpanded(boolean expanded) { prefs.edit().putBoolean(KEY_NETWORK_EXPANDED, expanded).apply(); }

    public boolean isNetworkServiceExpanded() { return prefs.getBoolean(KEY_NETWORK_SERVICE_EXPANDED, false); }
    public void setNetworkServiceExpanded(boolean expanded) { prefs.edit().putBoolean(KEY_NETWORK_SERVICE_EXPANDED, expanded).apply(); }

    public String getTestT1() { return prefs.getString(KEY_TEST_T1, "1.00"); }
    public void setTestT1(String t1) { prefs.edit().putString(KEY_TEST_T1, t1).apply(); }

    public String getTestT2() { return prefs.getString(KEY_TEST_T2, "2.00"); }
    public void setTestT2(String t2) { prefs.edit().putString(KEY_TEST_T2, t2).apply(); }

    public String getTestTotal() { return prefs.getString(KEY_TEST_TOTAL, "3.00"); }
    public void setTestTotal(String total) { prefs.edit().putString(KEY_TEST_TOTAL, total).apply(); }

    public String getTestSerial() { return prefs.getString(KEY_TEST_SERIAL, "48253981"); }
    public void setTestSerial(String serial) { prefs.edit().putString(KEY_TEST_SERIAL, serial).apply(); }

    public String getTestAddr() { return prefs.getString(KEY_TEST_ADDR, "81"); }
    public void setTestAddr(String addr) { prefs.edit().putString(KEY_TEST_ADDR, addr).apply(); }

    public void setLastSelectedAddress(int address) {
        prefs.edit().putInt(KEY_LAST_SELECTED_ADDRESS, address).apply();
    }

    public int getLastSelectedAddress() {
        return prefs.getInt(KEY_LAST_SELECTED_ADDRESS, -1);
    }

    // ✅ НОВЫЕ: Для функции Автосчитывания
    public boolean isAutoReadEnabled() { return prefs.getBoolean(KEY_AUTO_READ_ENABLED, false); }
    public void setAutoReadEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_AUTO_READ_ENABLED, enabled).apply(); }

    public int getAutoReadInterval() { return prefs.getInt(KEY_AUTO_READ_INTERVAL, DEFAULT_AUTO_READ_INTERVAL); }
    public void setAutoReadInterval(int interval) { prefs.edit().putInt(KEY_AUTO_READ_INTERVAL, interval).apply(); }

    // ✅ НОВЫЕ: Методы для авто-синхронизации
    public int getAutoSyncHour() { return prefs.getInt(KEY_AUTO_SYNC_HOUR, 16); }
    public void setAutoSyncHour(int hour) { prefs.edit().putInt(KEY_AUTO_SYNC_HOUR, hour).apply(); }

    public int getAutoSyncMinute() { return prefs.getInt(KEY_AUTO_SYNC_MINUTE, 0); }
    public void setAutoSyncMinute(int minute) { prefs.edit().putInt(KEY_AUTO_SYNC_MINUTE, minute).apply(); }

    public int getAutoSyncPeriod() { return prefs.getInt(KEY_AUTO_SYNC_PERIOD, 24); }
    public void setAutoSyncPeriod(int hours) { prefs.edit().putInt(KEY_AUTO_SYNC_PERIOD, hours).apply(); }

    public boolean isAutoSyncEnabled() { return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, false); }
    public void setAutoSyncEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply(); }
}