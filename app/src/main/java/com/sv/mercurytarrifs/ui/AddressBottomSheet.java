package com.sv.mercurytarrifs.ui;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sv.mercurytarrifs.R;
import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.prefs.AppPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressBottomSheet extends BottomSheetDialogFragment {
    private RecyclerView recyclerView;
    private ImageButton btnSort;
    private AddressAdapter adapter;
    private List<AddressAdapter.AddressItem> addressList;
    private HistoryDatabase dbHelper;
    private AppPreferences prefs;
    private boolean isAscending = true;

    public interface OnAddressSelectedListener {
        void onAddressSelected(int address);
    }

    private OnAddressSelectedListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_addresses, container, false);

        recyclerView = view.findViewById(R.id.rvAddresses);
        btnSort = view.findViewById(R.id.btnSort);

        dbHelper = new HistoryDatabase(requireContext());
        prefs = new AppPreferences(requireContext());
        addressList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL));

        adapter = new AddressAdapter(addressList, item -> {
            prefs.setLastSelectedAddress(item.address);
            if (listener != null) listener.onAddressSelected(item.address);
            dismiss();
        });

        recyclerView.setAdapter(adapter);
        loadAddresses();

        btnSort.setOnClickListener(v -> {
            isAscending = !isAscending;
            sortList();
            adapter.notifyDataSetChanged();
        });

        return view;
    }

    public void setOnAddressSelectedListener(OnAddressSelectedListener listener) {
        this.listener = listener;
    }

    private void loadAddresses() {
        List<AddressAdapter.AddressItem> dbData = dbHelper.getAddressBook();
        addressList.clear();
        addressList.addAll(dbData);
        sortList();
        scrollToLastSelectedAddress();
    }

    private void scrollToLastSelectedAddress() {
        int lastAddress = prefs.getLastSelectedAddress();
        if (lastAddress != -1) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                for (int i = 0; i < addressList.size(); i++) {
                    if (addressList.get(i).address == lastAddress) {
                        recyclerView.scrollToPosition(i);
                        break;
                    }
                }
            }, 200);
        }
    }

    private void sortList() {
        Collections.sort(addressList, new Comparator<AddressAdapter.AddressItem>() {
            @Override
            public int compare(AddressAdapter.AddressItem o1, AddressAdapter.AddressItem o2) {
                String n1 = o1.name != null && !o1.name.isEmpty() ? o1.name : "—";
                String n2 = o2.name != null && !o2.name.isEmpty() ? o2.name : "—";
                String s1 = addLeadingZeros(n1);
                String s2 = addLeadingZeros(n2);
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
}