package com.sv.mercurytarrifs.business;

import android.content.Context;
import android.widget.EditText;
import android.widget.Toast;

import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.prefs.AppPreferences;
import com.sv.mercurytarrifs.utils.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TestReadingManager {
    private final Context context;
    private final HistoryDatabase dbHelper;
    private final AppPreferences prefs;

    private EditText etDateTime, etT1, etT2, etTotal, etSerial, etAddr;

    public TestReadingManager(Context context, HistoryDatabase dbHelper, AppPreferences prefs) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.prefs = prefs;
    }

    public void setFields(EditText etDateTime, EditText etT1, EditText etT2,
                          EditText etTotal, EditText etSerial, EditText etAddr) {
        this.etDateTime = etDateTime;
        this.etT1 = etT1;
        this.etT2 = etT2;
        this.etTotal = etTotal;
        this.etSerial = etSerial;
        this.etAddr = etAddr;
    }

    // ✅ ИСПРАВЛЕНО: ДОБАВЛЕНЫ СЕКУНДЫ
    public void updateDateTime() {
        if (etDateTime != null) {
            // ✅ БЫЛО: "dd.MM.yyyy HH:mm"
            // ✅ СТАЛО: "dd.MM.yyyy HH:mm:ss"
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
            String currentDateTime = sdf.format(new Date());
            etDateTime.setText(currentDateTime);
        }
    }

    public void loadSavedValues() {
        if (etT1 != null) etT1.setText(prefs.getTestT1());
        if (etT2 != null) etT2.setText(prefs.getTestT2());
        if (etTotal != null) etTotal.setText(prefs.getTestTotal());
        if (etSerial != null) etSerial.setText(prefs.getTestSerial());
        if (etAddr != null) etAddr.setText(prefs.getTestAddr());
        updateDateTime();
    }

    public void saveValues() {
        if (etT1 != null) prefs.setTestT1(etT1.getText().toString().trim());
        if (etT2 != null) prefs.setTestT2(etT2.getText().toString().trim());
        if (etTotal != null) prefs.setTestTotal(etTotal.getText().toString().trim());
        if (etSerial != null) prefs.setTestSerial(etSerial.getText().toString().trim());
        if (etAddr != null) prefs.setTestAddr(etAddr.getText().toString().trim());
    }

    public boolean addTestReading() {
        try {
            String dateTime = etDateTime.getText().toString().trim();
            double t1 = Double.parseDouble(etT1.getText().toString().trim());
            double t2 = Double.parseDouble(etT2.getText().toString().trim());
            double total = Double.parseDouble(etTotal.getText().toString().trim());
            long serial = Long.parseLong(etSerial.getText().toString().trim());
            int addr = Integer.parseInt(etAddr.getText().toString().trim());

            saveValues();
            dbHelper.addHistory(addr, serial, dateTime, t1, t2, total);
            updateDateTime();

            Toast.makeText(context, "✅ Тестовое показание добавлено!", Toast.LENGTH_LONG).show();
            return true;

        } catch (NumberFormatException e) {
            Toast.makeText(context, "❌ Ошибка формата: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        } catch (Exception e) {
            Toast.makeText(context, "❌ Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }
}