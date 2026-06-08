package com.sv.mercurytarrifs.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sv.mercurytarrifs.R;
import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.prefs.AppPreferences;
import com.sv.mercurytarrifs.ui.AddressAdapter.AddressItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoReadBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView recyclerView;
    private ImageButton btnSort;
    private ImageButton btnSettings;
    private ImageButton btnAdd;

    private AutoReadNetworkAdapter adapter;
    private List<AutoReadNetworkItem> networkList;
    private HistoryDatabase dbHelper;
    private AppPreferences prefs;
    private boolean isAscending = true;

    private OnNetworkSelectedListener listener;

    public interface OnNetworkSelectedListener {
        void onNetworkSelected(String ssid, List<String> names);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_auto_read, container, false);

        recyclerView = view.findViewById(R.id.rvNetworks);
        btnSort = view.findViewById(R.id.btnSort);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnAdd = view.findViewById(R.id.btnAdd);

        dbHelper = new HistoryDatabase(requireContext());
        prefs = new AppPreferences(requireContext());
        networkList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL));

        adapter = new AutoReadNetworkAdapter(networkList, this::onNetworkClick, this::onDeleteClick);
        recyclerView.setAdapter(adapter);

        loadNetworks();

        btnSort.setOnClickListener(v -> {
            isAscending = !isAscending;
            sortList();
            adapter.notifyDataSetChanged();
        });

        btnSettings.setOnClickListener(v -> showIntervalSettings());

        btnAdd.setOnClickListener(v -> showAddDialog(null));

        return view;
    }

    public void setOnNetworkSelectedListener(OnNetworkSelectedListener listener) {
        this.listener = listener;
    }

    private void loadNetworks() {
        List<String> ssids = dbHelper.getAllConfiguredSsids();
        networkList.clear();
        for (String ssid : ssids) {
            List<String> names = dbHelper.getNamesForSsid(ssid);
            networkList.add(new AutoReadNetworkItem(ssid, names));
        }
        sortList();
    }

    private void sortList() {
        Collections.sort(networkList, new Comparator<AutoReadNetworkItem>() {
            @Override
            public int compare(AutoReadNetworkItem o1, AutoReadNetworkItem o2) {
                String s1 = addLeadingZeros(o1.ssid);
                String s2 = addLeadingZeros(o2.ssid);
                int result = s1.compareTo(s2);
                return isAscending ? result : -result;
            }
        });
    }

    private String addLeadingZeros(String name) {
        Pattern pattern = Pattern.compile("(\\d+)|(\\D+)");
        Matcher matcher = pattern.matcher(name);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String group = matcher.group();
            if (group.matches("\\d+")) {
                int num = Integer.parseInt(group);
                result.append(num < 10 ? String.format("%02d", num) : group);
            } else {
                result.append(group);
            }
        }
        return result.toString();
    }

    private void onNetworkClick(AutoReadNetworkItem item) {
        showAddDialog(item.ssid);
    }

    private void onDeleteClick(AutoReadNetworkItem item) {
        if (!isAdded()) return;
        try {
            dbHelper.deleteAutoReadConfig(item.ssid);
            networkList.remove(item);
            adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), "❌ Удалено: " + item.ssid, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "❌ Ошибка удаления: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ✅ НОВОЕ: Диалог настроек интервала (красивый, закруглённый)
    private void showIntervalSettings() {
        if (!isAdded() || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_interval_settings, null);
        builder.setView(dialogView);

        EditText etInterval = dialogView.findViewById(R.id.etInterval);
        Button btnPreset100 = dialogView.findViewById(R.id.btnPreset100);
        Button btnPreset200 = dialogView.findViewById(R.id.btnPreset200);
        Button btnPreset500 = dialogView.findViewById(R.id.btnPreset500);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelInterval);
        Button btnSave = dialogView.findViewById(R.id.btnSaveInterval);

        // ✅ Загружаем текущее значение
        int currentInterval = prefs.getAutoReadInterval();
        etInterval.setText(String.valueOf(currentInterval));

        // ✅ Предустановки
        btnPreset100.setOnClickListener(v -> etInterval.setText("100"));
        btnPreset200.setOnClickListener(v -> etInterval.setText("200"));
        btnPreset500.setOnClickListener(v -> etInterval.setText("500"));

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            try {
                int newInterval = Integer.parseInt(etInterval.getText().toString().trim());
                if (newInterval > 0) {
                    prefs.setAutoReadInterval(newInterval);
                    Toast.makeText(requireContext(), "✅ Интервал установлен: " + newInterval + " мс", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(requireContext(), "⚠️ Значение должно быть > 0", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "⚠️ Введите число", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Делаем фон прозрачным для показа закруглений
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    // ✅ ДИЗАЛОГ ДОБАВЛЕНИЯ/РЕДАКТИРОВАНИЯ
    private void showAddDialog(String existingSsid) {
        if (!isAdded() || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_auto_read_config, null);
        builder.setView(dialogView);

        EditText etNewSsid = dialogView.findViewById(R.id.etNewSsid);
        Button btnSelectNames = dialogView.findViewById(R.id.btnSelectNames);
        LinearLayout llSelectedNamesList = dialogView.findViewById(R.id.llSelectedNamesList);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelConfig);
        Button btnSave = dialogView.findViewById(R.id.btnSaveConfig);

        boolean isEdit = existingSsid != null;
        List<String> selectedNames = new ArrayList<>();

        if (isEdit) {
            etNewSsid.setText(existingSsid);
            etNewSsid.setEnabled(false);
            etNewSsid.setTextColor(0xFF888888);
            selectedNames.addAll(dbHelper.getNamesForSsid(existingSsid));
        } else {
            etNewSsid.setText("");
            etNewSsid.setEnabled(true);
            etNewSsid.setTextColor(0xFFFFFFFF);
        }

        // ✅ Первая отрисовка списка
        updateNamesListUI(llSelectedNamesList, selectedNames);

        btnSelectNames.setOnClickListener(v -> {
            if (!isAdded()) return;
            AddressBottomSheet addressSheet = new AddressBottomSheet();
            addressSheet.setOnAddressSelectedListener(address -> {
                if (!isAdded()) return;
                List<AddressItem> items = dbHelper.getAddressBook();
                for (AddressItem item : items) {
                    if (item.address == address && item.name != null && !item.name.equals("—")) {
                        if (!selectedNames.contains(item.name)) {
                            selectedNames.add(item.name);
                            updateNamesListUI(llSelectedNamesList, selectedNames);
                        }
                        break;
                    }
                }
            });
            addressSheet.show(getChildFragmentManager(), "SelectNames");
        });

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            if (!isAdded()) return;

            String ssid = etNewSsid.getText().toString().trim();

            if (ssid.isEmpty()) {
                Toast.makeText(requireContext(), "⚠️ Введите название сети", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedNames.isEmpty()) {
                Toast.makeText(requireContext(), "⚠️ Выберите хотя бы одно имя", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                dbHelper.addAutoReadConfig(ssid, selectedNames);
                loadNetworks();
                adapter.notifyDataSetChanged();
                Toast.makeText(requireContext(), isEdit ? "✅ Обновлено: " + ssid : "✅ Добавлено: " + ssid, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "❌ Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    // ✅ ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ОТРИСОВКИ СПИСКА ИМЁН (исправляет ошибку компилятора)
    private void updateNamesListUI(LinearLayout container, List<String> names) {
        container.removeAllViews();
        if (names.isEmpty()) {
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText("— пусто —");
            tvEmpty.setTextColor(0xFF666666);
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(0, 20, 0, 20);
            container.addView(tvEmpty);
        } else {
            for (String name : names) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(10, 10, 10, 10);

                TextView tvName = new TextView(requireContext());
                tvName.setText(name);
                tvName.setTextColor(0xFF4CAF50);
                tvName.setTextSize(14);
                LinearLayout.LayoutParams paramsName = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                tvName.setLayoutParams(paramsName);

                ImageButton btnRemove = new ImageButton(requireContext());
                btnRemove.setImageResource(android.R.drawable.ic_menu_delete);
                btnRemove.setBackgroundColor(Color.TRANSPARENT);
                btnRemove.setColorFilter(0xFFF44336);
                btnRemove.setPadding(10, 10, 10, 10);

                btnRemove.setOnClickListener(v -> {
                    names.remove(name);
                    updateNamesListUI(container, names);
                });

                row.addView(tvName);
                row.addView(btnRemove);
                container.addView(row);
            }
        }
    }

    public static class AutoReadNetworkItem {
        public String ssid;
        public List<String> names;

        public AutoReadNetworkItem(String ssid, List<String> names) {
            this.ssid = ssid;
            this.names = names != null ? names : new ArrayList<>();
        }
    }

    public static class AutoReadNetworkAdapter extends RecyclerView.Adapter<AutoReadNetworkAdapter.ViewHolder> {
        private List<AutoReadNetworkItem> items;
        private OnItemClickListener onItemClickListener;
        private OnDeleteClickListener onDeleteClickListener;

        public interface OnItemClickListener { void onItemClick(AutoReadNetworkItem item); }
        public interface OnDeleteClickListener { void onDeleteClick(AutoReadNetworkItem item); }

        public AutoReadNetworkAdapter(List<AutoReadNetworkItem> items,
                                      OnItemClickListener onItemClickListener,
                                      OnDeleteClickListener onDeleteClickListener) {
            this.items = items;
            this.onItemClickListener = onItemClickListener;
            this.onDeleteClickListener = onDeleteClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_auto_read_network, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AutoReadNetworkItem item = items.get(position);
            holder.tvSsid.setText(item.ssid);
            holder.tvNames.setText(item.names.isEmpty() ? "—" : String.join(", ", item.names));
            holder.tvCount.setText("К сети прикреплено: " + item.names.size() + " адр.");

            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) onItemClickListener.onItemClick(item);
            });

            holder.btnDelete.setOnClickListener(v -> {
                if (onDeleteClickListener != null) onDeleteClickListener.onDeleteClick(item);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSsid, tvNames, tvCount;
            Button btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvSsid = itemView.findViewById(R.id.tvSsid);
                tvNames = itemView.findViewById(R.id.tvNames);
                tvCount = itemView.findViewById(R.id.tvCount);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}