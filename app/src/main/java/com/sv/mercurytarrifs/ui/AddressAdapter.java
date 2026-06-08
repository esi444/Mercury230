package com.sv.mercurytarrifs.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sv.mercurytarrifs.R;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {

    private List<AddressItem> items;
    private OnItemClickListener listener;

    public AddressAdapter(List<AddressItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AddressItem item = items.get(position);
        holder.tvName.setText(item.name != null ? item.name : "—");
        holder.tvAddress.setText(String.valueOf(item.address));
        holder.tvSerial.setText(String.valueOf(item.serial));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvSerial;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvSerial = itemView.findViewById(R.id.tvSerial);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(AddressItem item);
    }

    public static class AddressItem {
        public int address;
        public long serial;
        public String name;

        public AddressItem(int address, long serial, String name) {
            this.address = address;
            this.serial = serial;
            this.name = name;
        }
    }
}