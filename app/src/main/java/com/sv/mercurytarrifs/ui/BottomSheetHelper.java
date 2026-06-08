package com.sv.mercurytarrifs.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sv.mercurytarrifs.R;
import com.sv.mercurytarrifs.data.HistoryEntry;
import com.sv.mercurytarrifs.data.LogEntry;

import java.util.Locale;

public class BottomSheetHelper {

    private final Context context;

    public BottomSheetHelper(Context context) {
        this.context = context;
    }

    /**
     * Показать детали истории
     */
    public void showHistoryDetails(HistoryEntry entry) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_history_detail, null);

        TextView bottomDate = sheetView.findViewById(R.id.bottomDate);
        TextView bottomSerial = sheetView.findViewById(R.id.bottomSerial);
        TextView bottomAddr = sheetView.findViewById(R.id.bottomAddr);
        TextView bottomName = sheetView.findViewById(R.id.bottomName);
        TextView bottomT1 = sheetView.findViewById(R.id.bottomT1);
        TextView bottomT2 = sheetView.findViewById(R.id.bottomT2);
        TextView bottomTotal = sheetView.findViewById(R.id.bottomTotal);
        Button bottomCopy = sheetView.findViewById(R.id.bottomCopy);
        Button bottomClose = sheetView.findViewById(R.id.bottomClose);

        bottomDate.setText(entry.datetime);
        bottomSerial.setText(String.valueOf(entry.serial));
        bottomAddr.setText(String.format(Locale.getDefault(), "%03d", entry.address));
        bottomName.setText(entry.name != null ? entry.name : "—");
        bottomT1.setText(String.format(Locale.getDefault(), "%.2f кВт⋅ч", entry.t1));
        bottomT2.setText(String.format(Locale.getDefault(), "%.2f кВт⋅ч", entry.t2));
        bottomTotal.setText(String.format(Locale.getDefault(), "%.2f кВт⋅ч", entry.total));

        bottomCopy.setOnClickListener(v -> {
            String details = String.format(Locale.getDefault(),
                    "📊 Детали записи\n" +
                            "📅 Дата: %s\n" +
                            "🔢 Серийник: %d\n" +
                            "🔌 Адрес: %03d\n" +
                            "☀️ T1: %.2f кВт⋅ч\n" +
                            "🌙 T2: %.2f кВт⋅ч\n" +
                            "💡 Сумма: %.2f кВт⋅ч",
                    entry.datetime, entry.serial, entry.address, entry.t1, entry.t2, entry.total);

            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("history_detail", details);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "✅ Скопировано в буфер", Toast.LENGTH_SHORT).show();
            bottomSheet.dismiss();
        });

        bottomClose.setOnClickListener(v -> bottomSheet.dismiss());
        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    /**
     * Показать детали лога
     */
    public void showLogDetails(LogEntry entry) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_log_detail, null);

        TextView bottomLogTime = sheetView.findViewById(R.id.bottomLogTime);
        TextView bottomLogAddr = sheetView.findViewById(R.id.bottomLogAddr);
        TextView bottomLogSerial = sheetView.findViewById(R.id.bottomLogSerial);
        TextView bottomLogStatus = sheetView.findViewById(R.id.bottomLogStatus);
        TextView bottomLogMessages = sheetView.findViewById(R.id.bottomLogMessages);
        Button bottomLogCopy = sheetView.findViewById(R.id.bottomLogCopy);
        Button bottomLogClose = sheetView.findViewById(R.id.bottomLogClose);

        bottomLogTime.setText(entry.timestamp);
        bottomLogAddr.setText(String.format(Locale.getDefault(), "%03d", entry.address));
        bottomLogSerial.setText(String.valueOf(entry.serial));
        bottomLogStatus.setText(entry.success ? "✅ Успешно" : "❌ Ошибка");
        bottomLogStatus.setTextColor(context.getColor(entry.success ? android.R.color.holo_green_light : android.R.color.holo_red_light));
        bottomLogMessages.setText(entry.messages);

        bottomLogCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("log_detail", entry.messages);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "✅ Лог скопирован в буфер", Toast.LENGTH_SHORT).show();
            bottomSheet.dismiss();
        });

        bottomLogClose.setOnClickListener(v -> bottomSheet.dismiss());
        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }
}