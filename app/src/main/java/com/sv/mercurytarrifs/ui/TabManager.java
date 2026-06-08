// pokaz/ui/TabManager.java - Управление вкладками

package com.sv.mercurytarrifs.ui;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.sv.mercurytarrifs.MainActivity;

public class TabManager {

    private final MainActivity activity;
    private final LinearLayout tabReadings;
    private final LinearLayout tabHistory;
    private final LinearLayout tabService;
    private final Button btnTabReadings;
    private final Button btnTabHistory;
    private final Button btnTabService;

    private boolean isTabsVisible = false;

    public TabManager(MainActivity activity,
                      LinearLayout tabReadings, LinearLayout tabHistory, LinearLayout tabService,
                      Button btnTabReadings, Button btnTabHistory, Button btnTabService) {
        this.activity = activity;
        this.tabReadings = tabReadings;
        this.tabHistory = tabHistory;
        this.tabService = tabService;
        this.btnTabReadings = btnTabReadings;
        this.btnTabHistory = btnTabHistory;
        this.btnTabService = btnTabService;
    }

    public void switchTab(int tab) {
        if (!isTabsVisible) return;

        activity.runOnUiThread(() -> {
            if (tab == 0) {
                tabReadings.setVisibility(View.VISIBLE);
                tabHistory.setVisibility(View.GONE);
                tabService.setVisibility(View.GONE);
                setColor(btnTabReadings, android.R.color.holo_green_light);
                setColor(btnTabHistory, android.R.color.darker_gray);
                setColor(btnTabService, android.R.color.darker_gray);
            } else if (tab == 1) {
                tabReadings.setVisibility(View.GONE);
                tabHistory.setVisibility(View.VISIBLE);
                tabService.setVisibility(View.GONE);
                setColor(btnTabReadings, android.R.color.darker_gray);
                setColor(btnTabHistory, android.R.color.holo_green_light);
                setColor(btnTabService, android.R.color.darker_gray);
            } else {
                tabReadings.setVisibility(View.GONE);
                tabHistory.setVisibility(View.GONE);
                tabService.setVisibility(View.VISIBLE);
                setColor(btnTabReadings, android.R.color.darker_gray);
                setColor(btnTabHistory, android.R.color.darker_gray);
                setColor(btnTabService, android.R.color.holo_green_light);
            }
        });
    }

    public void setTabsVisible(boolean visible) {
        isTabsVisible = visible;

        int visibility = visible ? View.VISIBLE : View.GONE;
        if (btnTabReadings != null) btnTabReadings.setVisibility(visibility);
        if (btnTabHistory != null) btnTabHistory.setVisibility(visibility);
        if (btnTabService != null) btnTabService.setVisibility(visibility);
    }

    public boolean isTabsVisible() {
        return isTabsVisible;
    }

    private void setColor(Button btn, int color) {
        if (btn != null) btn.setBackgroundColor(activity.getColor(color));
    }
}