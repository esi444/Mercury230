package com.sv.mercurytarrifs.business;

import android.content.Context;
import android.net.Uri;

import com.sv.mercurytarrifs.data.AddressNamePair;
import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.prefs.AppPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BackupManager {
    public static final int IMPORT_OK = 0;
    public static final int IMPORT_INVALID_FILE = 1;
    public static final int IMPORT_ERROR = 2;

    // ✅ Имя файла БД должно совпадать с HistoryDatabase.DATABASE_NAME
    private static final String DB_FILE_NAME = "history.db";
    // ✅ Магический заголовок SQLite (16 байт)
    private static final byte[] SQLITE_MAGIC = "SQLite format 3\u0000".getBytes();
    // ✅ Маркер файла настроек
    private static final String SETTINGS_TYPE = "mercury_settings";

    private final Context context;
    private final HistoryDatabase dbHelper;
    private final AppPreferences prefs;

    public BackupManager(Context context, HistoryDatabase dbHelper, AppPreferences prefs) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.prefs = prefs;
    }

    // ═══════════════════════════════════════
    // ✅ БАЗА ДАННЫХ (полная копия с историей)
    // ═══════════════════════════════════════

    public boolean exportDatabase(Uri destUri) {
        File dbFile = context.getDatabasePath(DB_FILE_NAME);
        if (dbFile == null || !dbFile.exists()) return false;
        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = context.getContentResolver().openOutputStream(destUri, "wt")) {
            if (out == null) return false;
            copyStream(in, out);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int importDatabase(Uri srcUri) {
        if (!isSqliteFile(srcUri)) return IMPORT_INVALID_FILE;
        File dbFile = context.getDatabasePath(DB_FILE_NAME);
        if (dbFile == null) return IMPORT_ERROR;
        try {
            // ✅ ОБЯЗАТЕЛЬНО закрываем соединение перед заменой файла
            dbHelper.close();

            try (InputStream in = context.getContentResolver().openInputStream(srcUri);
                 OutputStream out = new FileOutputStream(dbFile)) {
                copyStream(in, out);
            }

            // ✅ Удаляем возможные WAL/SHM файлы старого соединения
            File wal = new File(dbFile.getPath() + "-wal");
            File shm = new File(dbFile.getPath() + "-shm");
            if (wal.exists()) wal.delete();
            if (shm.exists()) shm.delete();

            return IMPORT_OK;
        } catch (Exception e) {
            e.printStackTrace();
            return IMPORT_ERROR;
        }
    }

    // ═══════════════════════════════════════
    // ✅ НАСТРОЙКИ (без истории): JSON-файл
    // ═══════════════════════════════════════

    public boolean exportSettings(Uri destUri) {
        try {
            JSONObject root = new JSONObject();
            root.put("type", SETTINGS_TYPE);
            root.put("version", 1);
            root.put("created", new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));

            // ✅ Все настройки приложения
            JSONObject s = new JSONObject();
            s.put("ip", prefs.getIp());
            s.put("port", prefs.getPort());
            s.put("address", prefs.getAddress());
            s.put("serverUrl", prefs.getServerUrl());
            s.put("apiPath", prefs.getApiPath());
            s.put("testUrl", prefs.getTestUrl());
            s.put("deviceKey", prefs.getDeviceKey());
            s.put("testT1", prefs.getTestT1());
            s.put("testT2", prefs.getTestT2());
            s.put("testTotal", prefs.getTestTotal());
            s.put("testSerial", prefs.getTestSerial());
            s.put("testAddr", prefs.getTestAddr());
            s.put("autoReadEnabled", prefs.isAutoReadEnabled());
            s.put("autoReadInterval", prefs.getAutoReadInterval());
            s.put("autoSyncEnabled", prefs.isAutoSyncEnabled());
            s.put("autoSyncHour", prefs.getAutoSyncHour());
            s.put("autoSyncMinute", prefs.getAutoSyncMinute());
            s.put("autoSyncPeriod", prefs.getAutoSyncPeriod());
            s.put("tabsUnlocked", prefs.isTabsUnlocked());
            root.put("settings", s);

            // ✅ Сети автосчитывания со списками адресов
            JSONArray networks = new JSONArray();
            List<String> ssids = dbHelper.getAllConfiguredSsids();
            for (String ssid : ssids) {
                JSONObject net = new JSONObject();
                net.put("ssid", ssid);
                JSONArray names = new JSONArray();
                for (AddressNamePair pair : dbHelper.getAddressNamesForSsid(ssid)) {
                    JSONObject n = new JSONObject();
                    n.put("address", pair.address);
                    n.put("name", pair.name);
                    names.put(n);
                }
                net.put("names", names);
                networks.put(net);
            }
            root.put("auto_read", networks);

            OutputStream out = context.getContentResolver().openOutputStream(destUri, "wt");
            if (out == null) return false;
            out.write(root.toString(2).getBytes("UTF-8"));
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int importSettings(Uri srcUri) {
        try {
            InputStream in = context.getContentResolver().openInputStream(srcUri);
            if (in == null) return IMPORT_ERROR;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) bos.write(buf, 0, len);
            in.close();

            JSONObject root = new JSONObject(new String(bos.toByteArray(), "UTF-8"));
            if (!SETTINGS_TYPE.equals(root.optString("type"))) return IMPORT_INVALID_FILE;

            // ✅ Восстанавливаем настройки
            JSONObject s = root.optJSONObject("settings");
            if (s != null) {
                if (s.has("ip")) prefs.setIp(s.getString("ip"));
                if (s.has("port")) prefs.setPort(s.getInt("port"));
                if (s.has("address")) prefs.setAddress(s.getInt("address"));
                if (s.has("serverUrl")) prefs.setServerUrl(s.getString("serverUrl"));
                if (s.has("apiPath")) prefs.setApiPath(s.getString("apiPath"));
                if (s.has("testUrl")) prefs.setTestUrl(s.getString("testUrl"));
                if (s.has("deviceKey")) prefs.setDeviceKey(s.getString("deviceKey"));
                if (s.has("testT1")) prefs.setTestT1(s.getString("testT1"));
                if (s.has("testT2")) prefs.setTestT2(s.getString("testT2"));
                if (s.has("testTotal")) prefs.setTestTotal(s.getString("testTotal"));
                if (s.has("testSerial")) prefs.setTestSerial(s.getString("testSerial"));
                if (s.has("testAddr")) prefs.setTestAddr(s.getString("testAddr"));
                if (s.has("autoReadEnabled")) prefs.setAutoReadEnabled(s.getBoolean("autoReadEnabled"));
                if (s.has("autoReadInterval")) prefs.setAutoReadInterval(s.getInt("autoReadInterval"));
                if (s.has("autoSyncEnabled")) prefs.setAutoSyncEnabled(s.getBoolean("autoSyncEnabled"));
                if (s.has("autoSyncHour")) prefs.setAutoSyncHour(s.getInt("autoSyncHour"));
                if (s.has("autoSyncMinute")) prefs.setAutoSyncMinute(s.getInt("autoSyncMinute"));
                if (s.has("autoSyncPeriod")) prefs.setAutoSyncPeriod(s.getInt("autoSyncPeriod"));
                if (s.has("tabsUnlocked")) prefs.setTabsUnlocked(s.getBoolean("tabsUnlocked"));
            }

            // ✅ Восстанавливаем сети автосчитывания (перезаписываются по SSID)
            JSONArray networks = root.optJSONArray("auto_read");
            if (networks != null) {
                for (int i = 0; i < networks.length(); i++) {
                    JSONObject net = networks.getJSONObject(i);
                    String ssid = net.getString("ssid");
                    JSONArray names = net.optJSONArray("names");
                    List<AddressNamePair> pairs = new ArrayList<>();
                    if (names != null) {
                        for (int j = 0; j < names.length(); j++) {
                            JSONObject n = names.getJSONObject(j);
                            pairs.add(new AddressNamePair(n.getInt("address"), n.getString("name")));
                        }
                    }
                    dbHelper.addAutoReadConfig(ssid, pairs);
                }
            }
            return IMPORT_OK;
        } catch (Exception e) {
            e.printStackTrace();
            return IMPORT_ERROR;
        }
    }

    // ═══════════════════════════════════════
    // ✅ СЛУЖЕБНЫЕ МЕТОДЫ
    // ═══════════════════════════════════════

    // ✅ Проверка: действительно ли файл является базой SQLite
    private boolean isSqliteFile(Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return false;
            byte[] header = new byte[16];
            int read = 0;
            while (read < 16) {
                int r = in.read(header, read, 16 - read);
                if (r < 0) break;
                read += r;
            }
            if (read < 16) return false;
            for (int i = 0; i < 16; i++) {
                if (header[i] != SQLITE_MAGIC[i]) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        out.flush();
    }
}