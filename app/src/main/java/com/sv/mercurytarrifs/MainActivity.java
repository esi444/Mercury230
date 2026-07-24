package com.sv.mercurytarrifs;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Outline;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.sv.mercurytarrifs.business.AutoReadManager;
import com.sv.mercurytarrifs.business.ReadingManager;
import com.sv.mercurytarrifs.business.ServerTestManager;
import com.sv.mercurytarrifs.business.SyncManager;
import com.sv.mercurytarrifs.business.TestReadingManager;
import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.data.HistoryEntry;
import com.sv.mercurytarrifs.prefs.AppPreferences;
import com.sv.mercurytarrifs.ui.AddressBottomSheet;
import com.sv.mercurytarrifs.ui.AutoReadBottomSheet;
import com.sv.mercurytarrifs.ui.BottomSheetHelper;
import com.sv.mercurytarrifs.ui.HistoryAdapter;
import com.sv.mercurytarrifs.ui.LogManager;
import com.sv.mercurytarrifs.ui.TabManager;
import com.sv.mercurytarrifs.workers.AutoSyncWorker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements AddressBottomSheet.OnAddressSelectedListener {

    private static final String UNLOCK_PASSWORD = "0000";
    private static final int TAPS_TO_UNLOCK = 10;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1001;
    private static final int TAPS_TO_IP_SETTINGS = 3;

    private EditText etIp, etPort, etAddr;
    private EditText etFilterAddr, etFilterDate, etFilterSerial;
    private EditText etServerUrl, etApiPath, etTestUrl, etDeviceKey;
    private EditText etTestDateTime, etTestT1, etTestT2, etTestTotal, etTestSerial, etTestAddr;
    private Button btnRead, btnDateTime;
    private Button btnUnlockSettings, btnTestConnection, btnSyncNow;
    private Button btnTabReadings, btnTabHistory, btnTabService;
    private Button btnLogCopy, btnLogClear, btnHistoryCopy, btnHistoryClear, btnFilterApply;
    private Button btnAddTestReading;
    private TextView tvTotal, tvT1, tvT2, tvDateTime;
    private TextView tvStatus, tvPhone, tvInfoPhone, tvSiteLink, tvFooter;
    private TextView tvTestResponse;
    private TextView historyToggle, logToggle, syncToggle, testReadingToggle;
    private TextView btnAddressList;
    private TextView tvAddressLabel;
    private TextView tvWifiSsid;
    private TextView btnWifiInfo;
    private TextView btnWifiScan;
    private TextView btnAddressInfo;
    private TextView networkToggleService;
    private LinearLayout networkHeaderService, networkContentService;
    private boolean networkServiceExpanded = false;

    // ✅ НОВЫЕ: Для автосчитывания
    private LinearLayout btnAutoRead;
    private Switch switchAutoRead;
    private AutoReadManager autoReadManager;

    // ✅ НОВОЕ: Кнопка быстрой синхронизации
    private ImageButton btnQuickSync;

    // ✅ НОВОЕ: Кнопка настроек авто-синхронизации
    private ImageButton btnAutoSyncSettings;
    private Switch switchAutoSync;

    private ImageView ivLogo;
    private LinearLayout layoutDateTime, layoutResults, layoutInfo;
    private LinearLayout tabReadings, tabService, tabHistory;
    private LinearLayout layoutWebsite;
    private LinearLayout historyContent, logContent, syncContent, testReadingContent;
    private LinearLayout historyHeader, logHeader, syncHeader, testReadingHeader;
    private RecyclerView rvHistory, rvLog;

    private AppPreferences prefs;
    private HistoryDatabase dbHelper;
    private BottomSheetHelper bottomSheetHelper;
    private LogManager logManager;
    private TabManager tabManager;
    private ReadingManager readingManager;
    private SyncManager syncManager;
    private ServerTestManager serverTestManager;
    private TestReadingManager testReadingManager;

    // ✅ НОВОЕ: WorkManager для фоновой синхронизации
    private WorkManager workManager;

    private ArrayList<HistoryEntry> historyList;
    private HistoryAdapter historyAdapter;
    private BroadcastReceiver wifiReceiver;

    private int tapCounter = 0;
    private int addressTapCounter = 0;
    private boolean historyExpanded = false;
    private boolean logExpanded = false;
    private boolean syncExpanded = false;
    private boolean testReadingExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new AppPreferences(this);
        dbHelper = new HistoryDatabase(this);
        bottomSheetHelper = new BottomSheetHelper(this);

        bindViews();

        logManager = new LogManager(this, rvLog, bottomSheetHelper);

        tabManager = new TabManager(this, tabReadings, tabHistory, tabService,
                btnTabReadings, btnTabHistory, btnTabService);

        readingManager = new ReadingManager(this, dbHelper, logManager,
                tvT1, tvT2, tvTotal, tvDateTime,
                layoutResults, layoutInfo, layoutDateTime);

        syncManager = new SyncManager(this, dbHelper, logManager);
        serverTestManager = new ServerTestManager(this, logManager, tvTestResponse);

        testReadingManager = new TestReadingManager(this, dbHelper, prefs);
        testReadingManager.setFields(etTestDateTime, etTestT1, etTestT2, etTestTotal, etTestSerial, etTestAddr);

        // ✅ НОВОЕ: Инициализация AutoReadManager
        autoReadManager = new AutoReadManager(this, this, dbHelper, logManager, prefs, readingManager);

        // ✅ НОВОЕ: Инициализация WorkManager
        workManager = WorkManager.getInstance(this);

        setupRecyclerViews();
        setupLogo();
        setupLinks();
        setupButtonListeners();
        setupAccordion();
        loadState();
        testReadingManager.loadSavedValues();

        requestWifiPermissions();
        initWifiReceiver();
    }

    private void initWifiReceiver() {
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWifiStatus();
                if (autoReadManager != null && prefs.isAutoReadEnabled()) {
                    // ✅ Задержка 3 секунды для стабилизации Wi-Fi соединения
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            autoReadManager.checkAndStartAutoRead(), 3000);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(wifiReceiver, filter);
        }
        updateWifiStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (wifiReceiver != null) {
            unregisterReceiver(wifiReceiver);
        }
    }

    private void requestWifiPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_LOCATION_PERMISSION);
            } else {
                updateWifiStatus();
            }
        } else {
            updateWifiStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                updateWifiStatus();
            } else {
                if (tvWifiSsid != null) {
                    tvWifiSsid.setText("—");
                    tvWifiSsid.setTextColor(Color.parseColor("#888888"));
                }
            }
        }
    }

    private String getCurrentWifiSsid() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String ssid = wifiInfo.getSSID();
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    if (ssid != null && !ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                        return ssid;
                    }
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private void updateWifiStatus() {
        if (tvWifiSsid == null) return;
        String ssid = getCurrentWifiSsid();
        if (ssid != null && !ssid.isEmpty()) {
            tvWifiSsid.setText(ssid);
            tvWifiSsid.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvWifiSsid.setText("—");
            tvWifiSsid.setTextColor(Color.parseColor("#888888"));
        }
    }

    // ✅ Обновлённое: Окно информации о WiFi
    private void showWifiInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wifi_info, null);
        builder.setView(dialogView);

        Button btnClose = dialogView.findViewById(R.id.btnWifiInfoClose);
        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    // ✅ Окно информации об адресе
    private void showAddressInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_address_info, null);
        builder.setView(dialogView);

        Button btnClose = dialogView.findViewById(R.id.btnAddressInfoClose);
        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void openWifiSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                startActivity(panelIntent);
            } else {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(this, "⚠️ Не удалось открыть настройки Wi-Fi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateStatus() {
        int address = parseIntSafe(etAddr.getText().toString(), 1);
        String prefix = "Текущий сетевой адрес для снятия показаний: ";
        String addressStr = String.valueOf(address);
        SpannableString spannable = new SpannableString(prefix + addressStr);
        spannable.setSpan(
                new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                prefix.length(),
                prefix.length() + addressStr.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        tvStatus.setText(spannable);
    }

    private void bindViews() {
        etIp = findViewById(R.id.etIp);
        etPort = findViewById(R.id.etPort);
        etAddr = findViewById(R.id.etAddr);
        etFilterAddr = findViewById(R.id.etFilterAddr);
        etFilterDate = findViewById(R.id.etFilterDate);
        etFilterSerial = findViewById(R.id.etFilterSerial);
        etServerUrl = findViewById(R.id.etServerUrl);
        etApiPath = findViewById(R.id.etApiPath);
        etTestUrl = findViewById(R.id.etTestUrl);
        etDeviceKey = findViewById(R.id.etDeviceKey);
        etTestDateTime = findViewById(R.id.etTestDateTime);
        etTestT1 = findViewById(R.id.etTestT1);
        etTestT2 = findViewById(R.id.etTestT2);
        etTestTotal = findViewById(R.id.etTestTotal);
        etTestSerial = findViewById(R.id.etTestSerial);
        etTestAddr = findViewById(R.id.etTestAddr);

        btnRead = findViewById(R.id.btnRead);
        btnDateTime = findViewById(R.id.btnDateTime);
        btnUnlockSettings = findViewById(R.id.btnUnlockSettings);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnSyncNow = findViewById(R.id.btnSyncNow);
        btnTabReadings = findViewById(R.id.btnTabReadings);
        btnTabHistory = findViewById(R.id.btnTabHistory);
        btnTabService = findViewById(R.id.btnTabService);
        btnLogCopy = findViewById(R.id.btnLogCopy);
        btnLogClear = findViewById(R.id.btnLogClear);
        btnHistoryCopy = findViewById(R.id.btnHistoryCopy);
        btnHistoryClear = findViewById(R.id.btnHistoryClear);
        btnFilterApply = findViewById(R.id.btnFilterApply);
        btnAddTestReading = findViewById(R.id.btnAddTestReading);

        tvTotal = findViewById(R.id.tvTotal);
        tvT1 = findViewById(R.id.tvT1);
        tvT2 = findViewById(R.id.tvT2);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvStatus = findViewById(R.id.tvStatus);
        tvPhone = findViewById(R.id.tvPhone);
        tvInfoPhone = findViewById(R.id.tvInfoPhone);
        tvSiteLink = findViewById(R.id.tvSiteLink);
        tvFooter = findViewById(R.id.tvFooter);
        tvTestResponse = findViewById(R.id.tvTestResponse);

        historyToggle = findViewById(R.id.historyToggle);
        logToggle = findViewById(R.id.logToggle);
        syncToggle = findViewById(R.id.syncToggle);
        testReadingToggle = findViewById(R.id.testReadingToggle);

        btnAddressList = findViewById(R.id.btnAddressList);
        tvAddressLabel = findViewById(R.id.tvAddressLabel);
        tvWifiSsid = findViewById(R.id.tvWifiSsid);
        btnWifiInfo = findViewById(R.id.btnWifiInfo);
        btnWifiScan = findViewById(R.id.btnWifiScan);
        btnAddressInfo = findViewById(R.id.btnAddressInfo);

        networkHeaderService = findViewById(R.id.networkHeaderService);
        networkContentService = findViewById(R.id.networkContentService);
        networkToggleService = findViewById(R.id.networkToggleService);

        btnAutoRead = findViewById(R.id.btnAutoRead);
        switchAutoRead = findViewById(R.id.switchAutoRead);

        btnQuickSync = findViewById(R.id.btnQuickSync);
        btnAutoSyncSettings = findViewById(R.id.btnAutoSyncSettings);
        switchAutoSync = findViewById(R.id.switchAutoSync);

        ivLogo = findViewById(R.id.ivLogo);

        layoutDateTime = findViewById(R.id.layoutDateTime);
        layoutResults = findViewById(R.id.layoutResults);
        layoutInfo = findViewById(R.id.layoutInfo);

        tabReadings = findViewById(R.id.tabReadings);
        tabService = findViewById(R.id.tabService);
        tabHistory = findViewById(R.id.tabHistory);

        layoutWebsite = findViewById(R.id.layoutWebsite);

        historyContent = findViewById(R.id.historyContent);
        logContent = findViewById(R.id.logContent);
        syncContent = findViewById(R.id.syncContent);
        testReadingContent = findViewById(R.id.testReadingContent);

        historyHeader = findViewById(R.id.historyHeader);
        logHeader = findViewById(R.id.logHeader);
        syncHeader = findViewById(R.id.syncHeader);
        testReadingHeader = findViewById(R.id.testReadingHeader);

        rvHistory = findViewById(R.id.rvHistory);
        rvLog = findViewById(R.id.rvLog);
    }

    private void setupRecyclerViews() {
        historyList = new ArrayList<>();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyList, this::showHistoryDetails);
        rvHistory.setAdapter(historyAdapter);
    }

    private void setupLogo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ivLogo.setClipToOutline(true);
            ivLogo.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int size = Math.min(view.getWidth(), view.getHeight());
                    outline.setOval(0, 0, size, size);
                }
            });
        }
    }

    private void setupLinks() {
        tvInfoPhone.setMovementMethod(LinkMovementMethod.getInstance());
        tvInfoPhone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+79022973008"));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "📞 Номер: +7 (902) 297-30-08", Toast.LENGTH_LONG).show();
            }
        });

        tvPhone.setMovementMethod(LinkMovementMethod.getInstance());
        tvPhone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+78482357524"));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "📞 Номер: +7 (8482) 35-75-24", Toast.LENGTH_LONG).show();
            }
        });

        // ✅ Сайт
        layoutWebsite.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sntprimorskoe.ru/"));
            startActivity(intent);
        });
    }

    private void setupAccordion() {
        if (networkHeaderService != null) {
            networkHeaderService.setOnClickListener(v -> toggleNetworkService());
        }
        historyHeader.setOnClickListener(v -> toggleHistory());
        logHeader.setOnClickListener(v -> toggleLog());
        syncHeader.setOnClickListener(v -> toggleSync());
        testReadingHeader.setOnClickListener(v -> toggleTestReading());
        btnAddTestReading.setOnClickListener(v -> addTestReading());
    }

    private void toggleNetworkService() {
        networkServiceExpanded = !networkServiceExpanded;
        networkContentService.setVisibility(networkServiceExpanded ? View.VISIBLE : View.GONE);
        networkToggleService.setText(networkServiceExpanded ? "🔼" : "🔽");
        prefs.setNetworkServiceExpanded(networkServiceExpanded);
    }

    private void toggleHistory() {
        historyExpanded = !historyExpanded;
        updateHistoryUI();
        prefs.setHistoryExpanded(historyExpanded);
    }

    private void toggleLog() {
        logExpanded = !logExpanded;
        updateLogUI();
        prefs.setLogExpanded(logExpanded);
    }

    private void toggleSync() {
        syncExpanded = !syncExpanded;
        updateSyncUI();
        prefs.setSyncExpanded(syncExpanded);
    }

    private void toggleTestReading() {
        testReadingExpanded = !testReadingExpanded;
        updateTestReadingUI();
        prefs.setTestReadingExpanded(testReadingExpanded);
    }

    private void updateHistoryUI() {
        historyContent.setVisibility(historyExpanded ? View.VISIBLE : View.GONE);
        historyToggle.setText(historyExpanded ? "🔼" : "🔽");
    }

    private void updateLogUI() {
        logContent.setVisibility(logExpanded ? View.VISIBLE : View.GONE);
        logToggle.setText(logExpanded ? "🔼" : "🔽");
    }

    private void updateSyncUI() {
        syncContent.setVisibility(syncExpanded ? View.VISIBLE : View.GONE);
        syncToggle.setText(syncExpanded ? "🔼" : "🔽");
    }

    private void updateTestReadingUI() {
        testReadingContent.setVisibility(testReadingExpanded ? View.VISIBLE : View.GONE);
        testReadingToggle.setText(testReadingExpanded ? "🔼" : "🔽");
    }

    private void loadState() {
        etIp.setText(prefs.getIp());
        etPort.setText(String.valueOf(prefs.getPort()));
        etAddr.setText(String.valueOf(prefs.getAddress()));
        etServerUrl.setText(prefs.getServerUrl());
        etApiPath.setText(prefs.getApiPath());
        etTestUrl.setText(prefs.getTestUrl());
        etDeviceKey.setText(prefs.getDeviceKey());

        boolean isTabsVisible = prefs.isTabsUnlocked();
        tapCounter = prefs.getTapCounter();

        historyExpanded = prefs.isHistoryExpanded();
        logExpanded = prefs.isLogExpanded();
        syncExpanded = prefs.isSyncExpanded();
        testReadingExpanded = prefs.isTestReadingExpanded();
        networkServiceExpanded = prefs.isNetworkServiceExpanded();

        updateHistoryUI();
        updateLogUI();
        updateSyncUI();
        updateTestReadingUI();

        if (networkContentService != null && networkToggleService != null) {
            networkContentService.setVisibility(networkServiceExpanded ? View.VISIBLE : View.GONE);
            networkToggleService.setText(networkServiceExpanded ? "🔼" : "🔽");
        }

        if (switchAutoRead != null) {
            switchAutoRead.setChecked(prefs.isAutoReadEnabled());
        }

        // ✅ Загрузка состояния тумблера автосинхронизации
        if (switchAutoSync != null) {
            switchAutoSync.setChecked(prefs.isAutoSyncEnabled());
        }

        tabManager.setTabsVisible(isTabsVisible);
        if (isTabsVisible) {
            btnAddressList.setVisibility(View.VISIBLE);
            btnDateTime.setVisibility(View.VISIBLE);
        } else {
            btnAddressList.setVisibility(View.GONE);
            btnDateTime.setVisibility(View.GONE);
        }

        updateStatus();
    }

    private void setupButtonListeners() {
        tvFooter.setOnClickListener(v -> handleFooterTap());
        btnAddressList.setOnClickListener(v -> showAddressList());

        // ✅ Обработчик 3-х кликов на "Адрес:"
        if (tvAddressLabel != null) {
            tvAddressLabel.setOnClickListener(v -> handleAddressTap());
        }

        if (btnWifiInfo != null) {
            btnWifiInfo.setOnClickListener(v -> showWifiInfoDialog());
        }

        // ✅ НОВОЕ: Обработчик кнопки Инфо рядом с Адресом
        if (btnAddressInfo != null) {
            btnAddressInfo.setOnClickListener(v -> showAddressInfoDialog());
        }

        if (btnWifiScan != null) {
            btnWifiScan.setOnClickListener(v -> openWifiSettings());
        }

        // ✅ Обработчики для автосчитывания
        if (btnAutoRead != null) {
            btnAutoRead.setOnClickListener(v -> showAutoReadBottomSheet());
        }

        if (switchAutoRead != null) {
            switchAutoRead.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.setAutoReadEnabled(isChecked);
                if (!isChecked && autoReadManager != null) {
                    autoReadManager.stopReading();
                }
                if (tabManager.isTabsVisible()) {
                    Toast.makeText(this, isChecked ? "✅ Автосчитывание включено" : "️ Автосчитывание выключено", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // ✅ Кнопка быстрой синхронизации
        if (btnQuickSync != null) {
            btnQuickSync.setOnClickListener(v -> syncWithServer());
        }

        // ✅ Кнопка настроек авто-синхронизации
        if (btnAutoSyncSettings != null) {
            btnAutoSyncSettings.setOnClickListener(v -> showAutoSyncSettingsDialog());
        }

        // ✅ Тумблер автосинхронизации
        if (switchAutoSync != null) {
            switchAutoSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.setAutoSyncEnabled(isChecked);
                if (isChecked) {
                    scheduleAutoSync(prefs.getAutoSyncHour(), prefs.getAutoSyncMinute(), prefs.getAutoSyncPeriod());
                    Toast.makeText(this, "✅ Автосинхронизация включена", Toast.LENGTH_SHORT).show();
                } else {
                    cancelAutoSync();
                    Toast.makeText(this, "️ Автосинхронизация выключена", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnRead.setOnClickListener(v -> readEnergy());
        btnDateTime.setOnClickListener(v -> readDateTime());
        btnUnlockSettings.setOnClickListener(v -> saveIpPort());
        btnTestConnection.setOnClickListener(v -> testServerConnection());
        btnSyncNow.setOnClickListener(v -> syncWithServer());
        btnHistoryCopy.setOnClickListener(v -> copyHistoryToClipboard());
        btnHistoryClear.setOnClickListener(v -> clearHistory());
        btnFilterApply.setOnClickListener(v -> loadHistoryWithFilter());
        btnLogCopy.setOnClickListener(v -> logManager.copyLogToClipboard());
        btnLogClear.setOnClickListener(v -> logManager.clearLog());

        btnTabReadings.setOnClickListener(v -> tabManager.switchTab(0));
        btnTabService.setOnClickListener(v -> tabManager.switchTab(2));
        btnTabHistory.setOnClickListener(v -> {
            tabManager.switchTab(1);
            loadHistoryWithFilter();
        });

        etTestT1.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) testReadingManager.saveValues(); });
        etTestT2.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) testReadingManager.saveValues(); });
        etTestTotal.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) testReadingManager.saveValues(); });
        etTestSerial.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) testReadingManager.saveValues(); });
        etTestAddr.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) testReadingManager.saveValues(); });

        etServerUrl.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) saveSyncSettings(); });
        etApiPath.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) saveSyncSettings(); });
        etTestUrl.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) saveSyncSettings(); });
        etDeviceKey.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) saveSyncSettings(); });
    }

    // ✅ Обработка 3-х кликов на "Адрес:"
    private void handleAddressTap() {
        addressTapCounter++;
        int remaining = TAPS_TO_IP_SETTINGS - addressTapCounter;
        if (remaining > 0) {
            Toast.makeText(this, "🔧 До открытия осталось " + remaining + " клик(ов)", Toast.LENGTH_SHORT).show();
        } else {
            showQuickIpPortDialog();
            addressTapCounter = 0;
        }
    }

    // ✅ Быстрый диалог настроек IP/порта
    private void showQuickIpPortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quick_ip_port, null);
        builder.setView(dialogView);

        EditText etQuickIp = dialogView.findViewById(R.id.etQuickIp);
        EditText etQuickPort = dialogView.findViewById(R.id.etQuickPort);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelQuick);
        Button btnSave = dialogView.findViewById(R.id.btnSaveQuick);

        etQuickIp.setText(prefs.getIp());
        etQuickPort.setText(String.valueOf(prefs.getPort()));

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newIp = etQuickIp.getText().toString().trim();
            String newPortStr = etQuickPort.getText().toString().trim();
            if (!newIp.isEmpty() && !newPortStr.isEmpty()) {
                try {
                    int newPort = Integer.parseInt(newPortStr);
                    prefs.setIp(newIp);
                    prefs.setPort(newPort);
                    Toast.makeText(this, "✅ IP и порт сохранены: " + newIp + ":" + newPort, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "❌ Ошибка: неверный формат порта", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "⚠️ Заполните все поля", Toast.LENGTH_SHORT).show();
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    // ✅ Диалог настроек авто-синхронизации
    private void showAutoSyncSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_auto_sync_settings, null);
        builder.setView(dialogView);

        EditText etHour = dialogView.findViewById(R.id.etSyncHour);
        EditText etMinute = dialogView.findViewById(R.id.etSyncMinute);
        RadioGroup rgPeriod = dialogView.findViewById(R.id.rgSyncPeriod);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelAutoSync);
        Button btnSave = dialogView.findViewById(R.id.btnSaveAutoSync);

        etHour.setText(String.valueOf(prefs.getAutoSyncHour()));
        etMinute.setText(String.valueOf(prefs.getAutoSyncMinute()));

        int savedPeriod = prefs.getAutoSyncPeriod();
        if (savedPeriod == 12) rgPeriod.check(R.id.rbTwiceDaily);
        else if (savedPeriod == 6) rgPeriod.check(R.id.rbEvery6Hours);
        else rgPeriod.check(R.id.rbDaily);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            try {
                int hour = Integer.parseInt(etHour.getText().toString().trim());
                int minute = Integer.parseInt(etMinute.getText().toString().trim());
                if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                    Toast.makeText(this, "⚠️ Неверное время", Toast.LENGTH_SHORT).show();
                    return;
                }
                int selectedId = rgPeriod.getCheckedRadioButtonId();
                int intervalHours = 24;
                if (selectedId == R.id.rbTwiceDaily) intervalHours = 12;
                else if (selectedId == R.id.rbEvery6Hours) intervalHours = 6;

                prefs.setAutoSyncHour(hour);
                prefs.setAutoSyncMinute(minute);
                prefs.setAutoSyncPeriod(intervalHours);

                scheduleAutoSync(hour, minute, intervalHours);
                Toast.makeText(this, "✅ Авто-синхронизация настроена", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "⚠️ Введите корректное время", Toast.LENGTH_SHORT).show();
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    // ✅ Планирование авто-синхронизации
    private void scheduleAutoSync(int hour, int minute, int intervalHours) {
        workManager.cancelUniqueWork("AutoSyncWork");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWork = new PeriodicWorkRequest.Builder(
                AutoSyncWorker.class,
                intervalHours, TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .setInitialDelay(calculateInitialDelay(hour, minute), TimeUnit.MILLISECONDS)
                .addTag("AutoSyncWork")
                .build();

        workManager.enqueueUniquePeriodicWork(
                "AutoSyncWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                syncWork
        );

        logManager.logMsg("✅ Авто-синхронизация запланирована: каждые " + intervalHours + " ч в " +
                String.format("%02d:%02d", hour, minute));
    }

    // ✅ Расчет задержки до первого запуска
    private long calculateInitialDelay(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();
        nextRun.set(Calendar.HOUR_OF_DAY, hour);
        nextRun.set(Calendar.MINUTE, minute);
        nextRun.set(Calendar.SECOND, 0);
        nextRun.set(Calendar.MILLISECOND, 0);
        if (nextRun.before(now)) {
            nextRun.add(Calendar.DAY_OF_YEAR, 1);
        }
        return nextRun.getTimeInMillis() - now.getTimeInMillis();
    }

    // ✅ Отмена автосинхронизации
    private void cancelAutoSync() {
        if (workManager != null) {
            workManager.cancelUniqueWork("AutoSyncWork");
            logManager.logMsg("⏹️ Автосинхронизация отменена");
        }
    }

    // ✅ Открытие BottomSheet с настройками автосчитывания
    private void showAutoReadBottomSheet() {
        AutoReadBottomSheet bottomSheet = new AutoReadBottomSheet();
        bottomSheet.setOnNetworkSelectedListener((ssid, names) -> {
            Toast.makeText(this, " Выбрано: " + ssid + " (" + names.size() + " имён)", Toast.LENGTH_SHORT).show();
        });
        bottomSheet.show(getSupportFragmentManager(), "AutoReadSettings");
    }

    private void handleFooterTap() {
        if (tabManager.isTabsVisible()) return;
        tapCounter++;
        prefs.setTapCounter(tapCounter);
        if (tapCounter >= TAPS_TO_UNLOCK) {
            showPasswordDialog();
        }
    }

    private void showPasswordDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Введите пароль");
        input.setTextSize(16);
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("🔒 Введите пароль")
                .setMessage("Для доступа к сервисным функциям введите пароль:")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String password = input.getText().toString();
                    if (password.equals(UNLOCK_PASSWORD)) {
                        prefs.setTabsUnlocked(true);
                        tabManager.setTabsVisible(true);
                        btnAddressList.setVisibility(View.VISIBLE);
                        btnDateTime.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "✅ Вкладки разблокированы!", Toast.LENGTH_LONG).show();
                    } else {
                        tapCounter = 0;
                        prefs.setTapCounter(0);
                        Toast.makeText(this, "❌ Неверный пароль.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveIpPort() {
        prefs.setIp(etIp.getText().toString());
        try {
            prefs.setPort(Integer.parseInt(etPort.getText().toString()));
        } catch (Exception e) {
            prefs.setPort(8899);
        }
        Toast.makeText(this, "IP и порт успешно сохранены", Toast.LENGTH_SHORT).show();
        btnUnlockSettings.setText("💾 Успешно");
        btnUnlockSettings.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_light));
    }

    private void saveSyncSettings() {
        prefs.setServerUrl(etServerUrl.getText().toString().trim());
        prefs.setApiPath(etApiPath.getText().toString().trim());
        prefs.setTestUrl(etTestUrl.getText().toString().trim());
        prefs.setDeviceKey(etDeviceKey.getText().toString().trim());
        Toast.makeText(this, "✅ Настройки синхронизации сохранены", Toast.LENGTH_SHORT).show();
    }

    private void testServerConnection() {
        String serverUrl = etServerUrl.getText().toString().trim();
        String testUrl = etTestUrl.getText().toString().trim();
        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "⚠️ Заполните URL сервера", Toast.LENGTH_SHORT).show();
            return;
        }
        serverTestManager.testServerConnection(serverUrl, testUrl);
    }

    private void syncWithServer() {
        String serverUrl = prefs.getServerUrl();
        String apiPath = prefs.getApiPath();
        String deviceKey = prefs.getDeviceKey();
        if (serverUrl.isEmpty() || apiPath.isEmpty()) {
            Toast.makeText(this, "⚠️ Настройте сервер синхронизации", Toast.LENGTH_SHORT).show();
            return;
        }
        syncManager.syncWithServer(serverUrl, apiPath, deviceKey);
    }

    private void readEnergy() {
        prefs.setAddress(parseIntSafe(etAddr.getText().toString(), 1));
        updateStatus();
        readingManager.readEnergy(prefs.getIp(), prefs.getPort(), prefs.getAddress(), null);
    }

    private void readDateTime() {
        int addr = parseIntSafe(etAddr.getText().toString(), 1);
        readingManager.readDateTime(prefs.getIp(), prefs.getPort(), addr, null);
    }

    public void triggerAutoRead(int address) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            etAddr.setText(String.valueOf(address));
            prefs.setAddress(address);
            updateStatus();
            readEnergy();
            Toast.makeText(this, "🔄 Чтение показаний для адреса " + address, Toast.LENGTH_SHORT).show();
        }, 300);
    }

    @Override
    public void onAddressSelected(int address) {
        etAddr.setText(String.valueOf(address));
        prefs.setAddress(address);
        updateStatus();
    }

    private void addTestReading() {
        boolean success = testReadingManager.addTestReading();
        if (success) {
            loadHistoryWithFilter();
            if (tabHistory.getVisibility() != View.VISIBLE) {
                Toast.makeText(this, "📜 Показание добавлено!", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void loadHistoryWithFilter() {
        String filterAddr = etFilterAddr.getText().toString().trim();
        String filterDate = etFilterDate.getText().toString().trim();
        String filterSerial = etFilterSerial.getText().toString().trim();

        historyList.clear();

        android.database.Cursor cursor = dbHelper.getHistory(
                filterAddr.isEmpty() ? null : filterAddr,
                filterDate.isEmpty() ? null : filterDate,
                filterSerial.isEmpty() ? null : filterSerial
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int addr = cursor.getInt(cursor.getColumnIndexOrThrow("address"));
                String datetime = cursor.getString(cursor.getColumnIndexOrThrow("datetime"));
                long serial = cursor.getLong(cursor.getColumnIndexOrThrow("serial_number"));
                double t1 = cursor.getDouble(cursor.getColumnIndexOrThrow("t1"));
                double t2 = cursor.getDouble(cursor.getColumnIndexOrThrow("t2"));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("custom_name"));
                historyList.add(new HistoryEntry(addr, serial, datetime, t1, t2, total, name));
            }
            cursor.close();
        }

        historyAdapter.notifyDataSetChanged();
    }

    private void clearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("🗑️ Очистить историю")
                .setMessage("Вы уверены?")
                .setPositiveButton("✅ Да", (dialog, which) -> {
                    dbHelper.clearHistory();
                    historyList.clear();
                    historyAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "✅ История очищена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("❌ Нет", null)
                .show();
    }

    private void copyHistoryToClipboard() {
        StringBuilder sb = new StringBuilder();
        for (HistoryEntry entry : historyList) {
            sb.append(String.format(java.util.Locale.getDefault(),
                    "📊 %s | 📍 %03d | 🔢 %d | ☀️ %.2f | 🌙 %.2f |  %.2f кВт⋅ч\n",
                    entry.datetime, entry.address, entry.serial, entry.t1, entry.t2, entry.total));
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("mercury_history", sb.toString()));
        Toast.makeText(this, "✅ История скопирована", Toast.LENGTH_SHORT).show();
    }

    private void showHistoryDetails(HistoryEntry entry) {
        bottomSheetHelper.showHistoryDetails(entry);
    }

    private void showAddressList() {
        AddressBottomSheet bottomSheet = new AddressBottomSheet();
        bottomSheet.setOnAddressSelectedListener(address -> {
            onAddressSelected(address);
            triggerAutoRead(address);
        });
        bottomSheet.show(getSupportFragmentManager(), "AddressList");
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}