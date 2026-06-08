package com.sv.mercurytarrifs.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sv.mercurytarrifs.R;
import com.sv.mercurytarrifs.data.LogEntry;

import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
    private List<LogEntry> logList;
    private final OnLogClickListener listener;

    public interface OnLogClickListener {
        void onLogClick(LogEntry entry);
    }

    public LogAdapter(ArrayList<LogEntry> logList, OnLogClickListener listener) {
        this.logList = logList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LogEntry entry = logList.get(position);
        holder.bind(entry, listener);
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public void updateData(ArrayList<LogEntry> newData) {
        this.logList = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate, tvTime, tvAddress, tvSource;
        private final Button btnDetails;
        private final View colorIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvSource = itemView.findViewById(R.id.tvSource);
            btnDetails = itemView.findViewById(R.id.btnDetails);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
        }

        void bind(LogEntry entry, OnLogClickListener listener) {
            // ✅ Разделяем дату и время
            String[] dateTimeParts = entry.timestamp.split(" ");
            String date = dateTimeParts.length > 0 ? dateTimeParts[0] : entry.timestamp;
            String time = dateTimeParts.length > 1 ? dateTimeParts[1] : "";

            // ✅ КОЛОНКА 1: Дата + Время
            tvDate.setText(date);
            tvTime.setText(time);

            // ✅ КОЛОНКА 2: Адрес
            tvAddress.setText(String.format("%03d", entry.address));

            // ✅ КОЛОНКА 3: Источник
            String source = determineSource(entry.messages);
            tvSource.setText(source);

            // ✅ Цветная полоска
            if (entry.success) {
                colorIndicator.setBackgroundColor(0xFF4CAF50);
            } else {
                colorIndicator.setBackgroundColor(0xFFF44336);
            }

            // ✅ Кнопка Подробнее
            btnDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLogClick(entry);
                }
            });
        }

        private String determineSource(String messages) {
            if (messages == null || messages.isEmpty()) {
                return "";
            }

            String[] lines = messages.split("\n");
            String line1 = lines.length >= 1 ? lines[0].trim() : "";
            String line2 = lines.length >= 2 ? lines[1].trim() : "";

            String checkLine = line1;
            if (line1.contains("═══") || line1.isEmpty()) {
                checkLine = line2;
            }

            if (checkLine.contains("ТЕСТ ПОДКЛЮЧЕНИЯ") ||
                    checkLine.contains("🧪 ТЕСТ") ||
                    checkLine.contains("Тест подключения")) {
                return "🧪 Тест";
            }

            if (checkLine.contains("СИНХРОНИЗАЦИЯ С СЕРВЕРОМ") ||
                    checkLine.contains("☁️ СИНХРОНИЗАЦИЯ")) {
                return "☁️ Синхронизация";
            }

            if (line1.contains("Чтение показаний") ||
                    line1.contains("🔄 Чтение")) {
                return "📊 Показания";
            }

            if (line1.contains("Чтение даты") ||
                    line1.contains("🕐 Чтение")) {
                return "🕐 Чтение даты";
            }

            if (line1.contains("Коррекция времени") ||
                    line1.contains("⏰ Коррекция")) {
                return "⏰ Коррекция даты";
            }

            return "";
        }
    }
}