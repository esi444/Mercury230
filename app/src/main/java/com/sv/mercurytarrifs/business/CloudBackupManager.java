package com.sv.mercurytarrifs.business;

import android.content.Context;

import com.sv.mercurytarrifs.data.AddressNamePair;
import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.prefs.AppPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CloudBackupManager {
    public static final int RESULT_OK = 0;
    public static final int RESULT_ERROR = 1;
    public static final int RESULT_NOT_FOUND = 2;
    public static final int RESULT_NO_CONNECTION = -1;
    public static final int RESULT_BUILD_ERROR = -2;

    private static final String CLOUD_API_PATH = "/pokaz/api/cloud_backup.php";
    private static final String CLOUD_PASSWORD = "0000";
    private static final String PAYLOAD_TYPE = "mercury_cloud_backup";

    // ✅ Браузерный User-Agent — помогает пройти файрвол хостинга
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public String lastError = "";

    private final Context context;
    private final HistoryDatabase dbHelper;
    private final AppPreferences prefs;

    public CloudBackupManager(Context context, HistoryDatabase dbHelper, AppPreferences prefs) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.prefs = prefs;
    }

    public static class LoadResult {
        public int code;
        public JSONObject payload;
        public LoadResult(int code, JSONObject payload) {
            this.code = code;
            this.payload = payload;
        }
    }

    public static class CloudInfo {
        public String created = "—";
        public String author = "—";
        public int subscribersCount = 0;
        public int networksCount = 0;
    }

    // ═══════════════════════════════════════
    // ✅ СОЗДАТЬ БЭКАП: всегда POST в виде HTML-формы
    // ═══════════════════════════════════════
    public int saveToCloud() {
        JSONObject payload;
        try {
            payload = buildPayload();
        } catch (Exception e) {
            lastError = "сбор данных: " + e.getMessage();
            return RESULT_BUILD_ERROR;
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(prefs.getServerUrl() + CLOUD_API_PATH +
                    "?action=save&password=" + CLOUD_PASSWORD);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            // ✅ Ключевой момент: отправляем как обычную форму, а не как JSON
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            // ✅ Формируем тело запроса как форму: payload=...
            String body = "payload=" + URLEncoder.encode(payload.toString(), "UTF-8");

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            if (code == 200) {
                lastError = "";
                return RESULT_OK;
            }
            lastError = "HTTP " + code + ": " + readStream(conn.getErrorStream());
            return code;
        } catch (Exception e) {
            lastError = "сеть: " + e.getMessage();
            return RESULT_NO_CONNECTION;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ═══════════════════════════════════════
    // ✅ ЗАГРУЗИТЬ БЭКАП (GET — здесь всегда работает, т.к. только читаем)
    // ═══════════════════════════════════════
    public LoadResult loadFromCloud() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(prefs.getServerUrl() + CLOUD_API_PATH +
                    "?action=get&password=" + CLOUD_PASSWORD);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            if (code == 404) return new LoadResult(RESULT_NOT_FOUND, null);
            if (code != 200) {
                lastError = "HTTP " + code;
                return new LoadResult(RESULT_ERROR, null);
            }

            String body = readStream(conn.getInputStream());
            JSONObject root = new JSONObject(body);
            if (!PAYLOAD_TYPE.equals(root.optString("type"))) {
                lastError = "неверный тип файла на сервере";
                return new LoadResult(RESULT_ERROR, null);
            }
            return new LoadResult(RESULT_OK, root);
        } catch (Exception e) {
            lastError = "сеть: " + e.getMessage();
            return new LoadResult(RESULT_ERROR, null);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public CloudInfo parseInfo(JSONObject payload) {
        CloudInfo info = new CloudInfo();
        try {
            JSONObject meta = payload.optJSONObject("meta");
            if (meta != null) {
                info.created = meta.optString("created", "—");
                info.author = meta.optString("author", "—");
            }
            JSONArray subs = payload.optJSONArray("subscribers");
            info.subscribersCount = (subs != null) ? subs.length() : 0;
            JSONArray networks = payload.optJSONArray("auto_read");
            info.networksCount = (networks != null) ? networks.length() : 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    // ═══════════════════════════════════════
    // ✅ ПРИМЕНИТЬ ЗАГРУЖЕННЫЙ БЭКАП
    // ═══════════════════════════════════════
    public boolean applyPayload(JSONObject payload) {
        try {
            List<HistoryDatabase.SubscriberRow> rows = new ArrayList<>();
            JSONArray subs = payload.optJSONArray("subscribers");
            if (subs != null) {
                for (int i = 0; i < subs.length(); i++) {
                    JSONObject s = subs.getJSONObject(i);
                    rows.add(new HistoryDatabase.SubscriberRow(
                            s.getInt("address"),
                            s.optLong("serial", 0),
                            s.optString("name", "—")));
                }
            }
            dbHelper.replaceSubscribers(rows);

            JSONArray networks = payload.optJSONArray("auto_read");
            if (networks != null) {
                for (int i = 0; i < networks.length(); i++) {
                    JSONObject net = networks.getJSONObject(i);
                    String ssid = net.getString("ssid");
                    List<AddressNamePair> pairs = new ArrayList<>();
                    JSONArray names = net.optJSONArray("names");
                    if (names != null) {
                        for (int j = 0; j < names.length(); j++) {
                            JSONObject n = names.getJSONObject(j);
                            pairs.add(new AddressNamePair(n.getInt("address"), n.optString("name", "—")));
                        }
                    }
                    dbHelper.addAutoReadConfig(ssid, pairs);
                }
            }
            return true;
        } catch (Exception e) {
            lastError = "применение: " + e.getMessage();
            return false;
        }
    }

    // ═══════════════════════════════════════
    // ✅ СБОРКА PAYLOAD
    // ═══════════════════════════════════════
    private JSONObject buildPayload() throws Exception {
        JSONObject root = new JSONObject();
        root.put("type", PAYLOAD_TYPE);

        JSONObject meta = new JSONObject();
        meta.put("created", new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
        meta.put("author", prefs.getDeviceKey());
        root.put("meta", meta);

        JSONArray subs = new JSONArray();
        for (HistoryDatabase.SubscriberRow row : dbHelper.getSubscribersUnion()) {
            JSONObject s = new JSONObject();
            s.put("address", row.address);
            s.put("serial", row.serialNumber);
            s.put("name", row.name);
            subs.put(s);
        }
        root.put("subscribers", subs);

        JSONArray networks = new JSONArray();
        for (String ssid : dbHelper.getAllConfiguredSsids()) {
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

        return root;
    }

    private String readStream(InputStream in) {
        try {
            if (in == null) return "";
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) bos.write(buf, 0, len);
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}