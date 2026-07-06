package com.sv.mercurytarrifs.data;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "history.db";
    private static final int DATABASE_VERSION = 4; // ✅ Увеличена версия для новых таблиц

    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ADDRESS = "address";
    private static final String COLUMN_SERIAL_NUMBER = "serial_number";
    private static final String COLUMN_DATETIME = "datetime";
    private static final String COLUMN_T1 = "t1";
    private static final String COLUMN_T2 = "t2";
    private static final String COLUMN_TOTAL = "total";
    private static final String COLUMN_SYNCED = "synced";
    private static final String COLUMN_CUSTOM_NAME = "custom_name";

    // ✅ НОВЫЕ: Таблицы для автосчитывания
    private static final String TABLE_AUTO_READ_CONFIG = "auto_read_config";
    private static final String TABLE_AUTO_READ_NAMES = "auto_read_names";

    public HistoryDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ADDRESS + " INTEGER NOT NULL, " +
                COLUMN_SERIAL_NUMBER + " INTEGER NOT NULL, " +
                COLUMN_DATETIME + " TEXT NOT NULL, " +
                COLUMN_T1 + " REAL NOT NULL, " +
                COLUMN_T2 + " REAL NOT NULL, " +
                COLUMN_TOTAL + " REAL NOT NULL, " +
                COLUMN_SYNCED + " INTEGER DEFAULT 0, " +
                COLUMN_CUSTOM_NAME + " TEXT DEFAULT '—' " +
                ")");

        // ✅ НОВЫЕ: Таблицы для автосчитывания
        db.execSQL("CREATE TABLE " + TABLE_AUTO_READ_CONFIG + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "wifi_ssid TEXT NOT NULL UNIQUE, " +
                "is_enabled INTEGER DEFAULT 1, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE " + TABLE_AUTO_READ_NAMES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "config_id INTEGER, " +
                "custom_name TEXT NOT NULL, " +
                "read_order INTEGER, " +
                "FOREIGN KEY (config_id) REFERENCES " + TABLE_AUTO_READ_CONFIG + "(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_HISTORY + " ADD COLUMN " + COLUMN_SYNCED + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_HISTORY + " ADD COLUMN " + COLUMN_CUSTOM_NAME + " TEXT DEFAULT '—'");
        }
        // ✅ НОВЫЕ: Таблицы для автосчитывания (версия 4)
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE " + TABLE_AUTO_READ_CONFIG + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "wifi_ssid TEXT NOT NULL UNIQUE, " +
                    "is_enabled INTEGER DEFAULT 1, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            db.execSQL("CREATE TABLE " + TABLE_AUTO_READ_NAMES + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "config_id INTEGER, " +
                    "custom_name TEXT NOT NULL, " +
                    "read_order INTEGER, " +
                    "FOREIGN KEY (config_id) REFERENCES " + TABLE_AUTO_READ_CONFIG + "(id))");
        }
    }

    public void addHistory(int address, long serialNumber, String datetime, double t1, double t2, double total) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ADDRESS, address);
        values.put(COLUMN_SERIAL_NUMBER, serialNumber);
        values.put(COLUMN_DATETIME, datetime);
        values.put(COLUMN_T1, t1);
        values.put(COLUMN_T2, t2);
        values.put(COLUMN_TOTAL, total);
        values.put(COLUMN_SYNCED, 0);
        values.put(COLUMN_CUSTOM_NAME, "—");
        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }

    public void syncSubscriberNames(Map<Long, String> namesMap) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues resetValues = new ContentValues();
            resetValues.put(COLUMN_CUSTOM_NAME, "—");
            db.update(TABLE_HISTORY, resetValues, null, null);

            for (Map.Entry<Long, String> entry : namesMap.entrySet()) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_CUSTOM_NAME, entry.getValue());
                db.update(TABLE_HISTORY, values, COLUMN_SERIAL_NUMBER + " = ?", new String[]{String.valueOf(entry.getKey())});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public Cursor getHistory(String filterAddr, String filterDate, String filterSerial) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = null;
        String[] selectionArgs = null;

        if (filterAddr != null && !filterAddr.isEmpty() && filterDate != null && !filterDate.isEmpty() && filterSerial != null && !filterSerial.isEmpty()) {
            selection = COLUMN_ADDRESS + "=? AND " + COLUMN_DATETIME + " LIKE ? AND " + COLUMN_SERIAL_NUMBER + "=?";
            selectionArgs = new String[]{filterAddr, "%" + filterDate + "%", filterSerial};
        } else if (filterAddr != null && !filterAddr.isEmpty() && filterDate != null && !filterDate.isEmpty()) {
            selection = COLUMN_ADDRESS + "=? AND " + COLUMN_DATETIME + " LIKE ?";
            selectionArgs = new String[]{filterAddr, "%" + filterDate + "%"};
        } else if (filterAddr != null && !filterAddr.isEmpty() && filterSerial != null && !filterSerial.isEmpty()) {
            selection = COLUMN_ADDRESS + "=? AND " + COLUMN_SERIAL_NUMBER + "=?";
            selectionArgs = new String[]{filterAddr, filterSerial};
        } else if (filterDate != null && !filterDate.isEmpty() && filterSerial != null && !filterSerial.isEmpty()) {
            selection = COLUMN_DATETIME + " LIKE ? AND " + COLUMN_SERIAL_NUMBER + "=?";
            selectionArgs = new String[]{"%" + filterDate + "%", filterSerial};
        } else if (filterAddr != null && !filterAddr.isEmpty()) {
            selection = COLUMN_ADDRESS + "=?";
            selectionArgs = new String[]{filterAddr};
        } else if (filterDate != null && !filterDate.isEmpty()) {
            selection = COLUMN_DATETIME + " LIKE ?";
            selectionArgs = new String[]{"%" + filterDate + "%"};
        } else if (filterSerial != null && !filterSerial.isEmpty()) {
            selection = COLUMN_SERIAL_NUMBER + "=?";
            selectionArgs = new String[]{filterSerial};
        }

        return db.query(TABLE_HISTORY, null, selection, selectionArgs, null, null, COLUMN_ID + " DESC");
    }

    public Cursor getUnsyncedHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_HISTORY, null, COLUMN_SYNCED + " = ?", new String[]{"0"}, null, null, COLUMN_ID + " ASC");
    }

    public void markAsSynced(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SYNCED, 1);
        db.update(TABLE_HISTORY, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
    }

    public int getUnsyncedCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, new String[]{"COUNT(*)"}, COLUMN_SYNCED + " = ?", new String[]{"0"}, null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public List<com.sv.mercurytarrifs.ui.AddressAdapter.AddressItem> getAddressBook() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<com.sv.mercurytarrifs.ui.AddressAdapter.AddressItem> list = new ArrayList<>();
        String query = "SELECT address, serial_number, custom_name FROM history GROUP BY address ORDER BY MAX(id) DESC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int address = cursor.getInt(cursor.getColumnIndexOrThrow("address"));
                long serial = cursor.getLong(cursor.getColumnIndexOrThrow("serial_number"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("custom_name"));
                list.add(new com.sv.mercurytarrifs.ui.AddressAdapter.AddressItem(address, serial, name));
            }
            cursor.close();
        }
        db.close();
        return list;
    }

    // ✅ НОВЫЕ: Методы для автосчитывания
    public void addAutoReadConfig(String ssid, List<String> names) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(TABLE_AUTO_READ_CONFIG, "wifi_ssid = ?", new String[]{ssid});
            ContentValues configValues = new ContentValues();
            configValues.put("wifi_ssid", ssid);
            configValues.put("is_enabled", 1);
            long configId = db.insert(TABLE_AUTO_READ_CONFIG, null, configValues);
            for (int i = 0; i < names.size(); i++) {
                ContentValues nameValues = new ContentValues();
                nameValues.put("config_id", configId);
                nameValues.put("custom_name", names.get(i));
                nameValues.put("read_order", i);
                db.insert(TABLE_AUTO_READ_NAMES, null, nameValues);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public List<String> getNamesForSsid(String ssid) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> names = new ArrayList<>();
        String query = "SELECT n.custom_name FROM " + TABLE_AUTO_READ_NAMES + " n " +
                "JOIN " + TABLE_AUTO_READ_CONFIG + " c ON n.config_id = c.id " +
                "WHERE c.wifi_ssid = ? ORDER BY n.read_order";
        Cursor cursor = db.rawQuery(query, new String[]{ssid});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0));
            }
            cursor.close();
        }
        db.close();
        return names;
    }

    public List<String> getAllConfiguredSsids() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> ssids = new ArrayList<>();
        Cursor cursor = db.query(TABLE_AUTO_READ_CONFIG, new String[]{"wifi_ssid"}, null, null, null, null, "wifi_ssid ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ssids.add(cursor.getString(0));
            }
            cursor.close();
        }
        db.close();
        return ssids;
    }

    public void deleteAutoReadConfig(String ssid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_AUTO_READ_CONFIG, "wifi_ssid = ?", new String[]{ssid});
        db.close();
    }
}