package com.sv.mercurytarrifs.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sv.mercurytarrifs.R;
import com.sv.mercurytarrifs.data.HistoryEntry;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<HistoryEntry> historyList;
    private final OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryClick(HistoryEntry entry);
    }

    public HistoryAdapter(ArrayList<HistoryEntry> historyList, OnHistoryClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEntry entry = historyList.get(position);
        holder.bind(entry, listener);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void updateData(ArrayList<HistoryEntry> newData) {
        this.historyList = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate, tvTime, tvAddress, tvSerial, tvName; // ✅ УДАЛЕНО: tvDescription
        private final Button btnDetails;
        private final View colorIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvSerial = itemView.findViewById(R.id.tvSerial);
            tvName = itemView.findViewById(R.id.tvName);
            // ✅ УДАЛЕНО: tvDescription = itemView.findViewById(R.id.tvDescription);
            btnDetails = itemView.findViewById(R.id.btnDetails);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
        }

        void bind(HistoryEntry entry, OnHistoryClickListener listener) {
            // ✅ Разделяем дату и время
            String[] dateTimeParts = entry.datetime.split(" ");
            String date = dateTimeParts.length > 0 ? dateTimeParts[0] : entry.datetime;
            String time = dateTimeParts.length > 1 ? dateTimeParts[1] : "";

            // ✅ КОЛОНКА 1: Дата + Время
            tvDate.setText(date);
            tvTime.setText(time);

            // ✅ КОЛОНКА 2: Адрес + Серийник
            tvAddress.setText(String.format("%03d", entry.address));
            tvSerial.setText(String.valueOf(entry.serial));

            // ✅ КОЛОНКА 3: Имя абонента
            tvName.setText("👤 " + entry.name);

            // ✅ Цветная полоска
            if (entry.t1 > 0 && entry.t2 > 0 && entry.total > 0) {
                colorIndicator.setBackgroundColor(0xFF4CAF50);
            } else {
                colorIndicator.setBackgroundColor(0xFFF44336);
            }

            // ✅ Кнопка Подробнее
            btnDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryClick(entry);
                }
            });
        }
    }
}