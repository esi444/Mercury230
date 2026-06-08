// pokaz/ui/LogManager.java - Управление логами

package com.sv.mercurytarrifs.ui;

import android.content.Context;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sv.mercurytarrifs.data.LogEntry;
import com.sv.mercurytarrifs.ui.LogAdapter;
import com.sv.mercurytarrifs.ui.BottomSheetHelper;
import com.sv.mercurytarrifs.utils.DateTimeUtils;

import java.util.ArrayList;

public class LogManager {

    private final Context context;
    private final RecyclerView rvLog;
    private final LogAdapter logAdapter;
    private final ArrayList<LogEntry> logList;
    private final BottomSheetHelper bottomSheetHelper;  // ✅ ДОБАВЛЕНО
    private StringBuilder currentLogMessages;

    public LogManager(Context context, RecyclerView rvLog, BottomSheetHelper bottomSheetHelper) {
        this.context = context;
        this.rvLog = rvLog;
        this.logList = new ArrayList<>();
        this.bottomSheetHelper = bottomSheetHelper;  // ✅ СОХРАНЯЕМ

        rvLog.setLayoutManager(new LinearLayoutManager(context));
        this.logAdapter = new LogAdapter(logList, this::showLogDetails);
        rvLog.setAdapter(logAdapter);

        currentLogMessages = new StringBuilder();
    }

    public void logMsg(String msg) {
        if (currentLogMessages != null) {
            currentLogMessages.append(msg).append("\n");
        }
    }

    public void finishLogEntry(String operationName, boolean success, int address, long serial) {
        if (currentLogMessages == null || currentLogMessages.length() == 0) {
            return;
        }

        String fullLog = currentLogMessages.toString();
        addLogEntry(address, serial, fullLog, success, -1, -1, -1);

        currentLogMessages = new StringBuilder();
    }

    public void addLogEntry(int address, long serial, String messages, boolean success,
                            double t1, double t2, double total) {
        String timestamp = DateTimeUtils.formatDateTimeWithSeconds(new java.util.Date());
        LogEntry entry = new LogEntry(timestamp, address, serial, messages, success, t1, t2, total);
        logList.add(0, entry);

        if (rvLog != null) {
            rvLog.post(() -> {
                logAdapter.notifyDataSetChanged();
                rvLog.scrollToPosition(0);
            });
        }
    }

    public void clearLog() {
        logList.clear();
        logAdapter.notifyDataSetChanged();
        Toast.makeText(context, "✅ Лог очищен", Toast.LENGTH_SHORT).show();
    }

    public void copyLogToClipboard() {
        StringBuilder sb = new StringBuilder();
        for (LogEntry entry : logList) {
            sb.append(entry.messages).append("\n\n");
        }

        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("mercury_log", sb.toString()));
        Toast.makeText(context, "✅ Лог скопирован", Toast.LENGTH_SHORT).show();
    }

    public StringBuilder getCurrentLogMessages() {
        return currentLogMessages;
    }

    public void setCurrentLogMessages(StringBuilder messages) {
        this.currentLogMessages = messages;
    }

    public ArrayList<LogEntry> getLogList() {
        return logList;
    }

    // ✅ ИСПРАВЛЕНО: Открываем BottomSheet вместо Toast
    private void showLogDetails(LogEntry entry) {
        bottomSheetHelper.showLogDetails(entry);
    }
}