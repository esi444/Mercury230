// pokaz/business/ReadingManager.java - Управление чтением показаний
package com.sv.mercurytarrifs.business;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import com.sv.mercurytarrifs.MainActivity;
import com.sv.mercurytarrifs.data.HistoryDatabase;
import com.sv.mercurytarrifs.network.MercurySocket;
import com.sv.mercurytarrifs.protocol.MercuryCommands;
import com.sv.mercurytarrifs.protocol.MercuryParser;
import com.sv.mercurytarrifs.ui.LogManager;
import com.sv.mercurytarrifs.utils.DateTimeUtils;
public class ReadingManager {
    private final MainActivity activity;
    private final HistoryDatabase dbHelper;
    private final LogManager logManager;
    // ✅ УДАЛЕНО: tvCheck
    private final TextView tvT1, tvT2, tvTotal, tvDateTime;
    private final LinearLayout layoutResults, layoutInfo, layoutDateTime;
    private double currentT1 = 0, currentT2 = 0, currentTotal = 0;
    private long currentSerial = 0;

    public ReadingManager(MainActivity activity, HistoryDatabase dbHelper, LogManager logManager,
                          TextView tvT1, TextView tvT2, TextView tvTotal,
                          TextView tvDateTime, LinearLayout layoutResults, LinearLayout layoutInfo,
                          LinearLayout layoutDateTime) {
        this.activity = activity;
        this.dbHelper = dbHelper;
        this.logManager = logManager;
        this.tvT1 = tvT1;
        this.tvT2 = tvT2;
        this.tvTotal = tvTotal;
        // ✅ УДАЛЕНО: this.tvCheck = tvCheck;
        this.tvDateTime = tvDateTime;
        this.layoutResults = layoutResults;
        this.layoutInfo = layoutInfo;
        this.layoutDateTime = layoutDateTime;
    }

    public void readEnergy(String ip, int port , int addr, Runnable onComplete) {
        logManager.setCurrentLogMessages(new StringBuilder());
        logManager.logMsg("🔄 Чтение показаний (Адрес: " + addr + ")...");

        hideResults();

        new Thread(() -> {
            MercurySocket socket = new MercurySocket();
            try {
                socket.connect(ip, port, 3000);
                logManager.logMsg("   ✅ TCP соединение установлено");

                byte[] respKeep = socket.sendAndReceive(MercuryCommands.buildKeepalive(addr), 3000);
                if (respKeep == null) throw new Exception("Нет ответа на Keepalive");
                logManager.logMsg("   1️⃣ Keepalive: " + bytesToHex(respKeep));
                Thread.sleep(200);

                byte[] respLogin = socket.sendAndReceive(MercuryCommands.buildLogin(addr), 3000);
                if (respLogin == null) throw new Exception("Нет ответа на Login");
                logManager.logMsg("   2️⃣ Login: " + bytesToHex(respLogin));
                Thread.sleep(200);

                byte[] respT1 = socket.sendAndReceive(MercuryCommands.buildReadT1(addr), 3000);
                double t1 = MercuryParser.parseTariff(respT1);
                currentT1 = t1;
                logManager.logMsg("   3️⃣ T1: " + DateTimeUtils.formatValue(t1));
                Thread.sleep(200);

                byte[] respT2 = socket.sendAndReceive(MercuryCommands.buildReadT2(addr), 3000);
                double t2 = MercuryParser.parseTariff(respT2);
                currentT2 = t2;
                logManager.logMsg("   4️⃣ T2: " + DateTimeUtils.formatValue(t2));
                Thread.sleep(200);

                byte[] respTotal = socket.sendAndReceive(MercuryCommands.buildReadTotal(addr), 3000);
                double total = MercuryParser.parseTariff(respTotal);
                currentTotal = total;
                logManager.logMsg("   5️⃣ Сумма: " + DateTimeUtils.formatValue(total));
                Thread.sleep(200);

                // ✅ ЧТЕНИЕ СЕРИЙНОГО НОМЕРА И ДАТЫ ВЫПУСКА
                byte[] respSerial = socket.sendAndReceive(MercuryCommands.buildReadSerial(addr), 3000);
                logManager.logMsg("   6️⃣ Серийник HEX: " + bytesToHex(respSerial));

                // ✅ Серийник — прямые байты (БЕЗ BCD)
                long serial = MercuryParser.parseSerialNumber(respSerial);
                currentSerial = serial;
                logManager.logMsg("   6️⃣ Серийник: " + serial);

                // ✅ ДОБАВЛЕНО: Логирование даты выпуска
                String releaseDate = MercuryParser.parseReleaseDate(respSerial);
                logManager.logMsg("   6️⃣ Дата выпуска: " + releaseDate);

                Thread.sleep(200);

                activity.runOnUiThread(() -> updateResults(t1, t2, total, serial));

                if (t1 >= 0 && t2 >= 0 && total >= 0) {
                    saveToHistory(addr, serial, t1, t2, total);
                }

                logManager.logMsg("✅ Чтение энергии завершено!");
                logManager.finishLogEntry("Чтение показаний", true, addr, currentSerial);

                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "✅ Показания прочитаны!", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                logManager.logMsg("❌ Ошибка: " + e.getMessage());
                logManager.logMsg("   Тип: " + e.getClass().getSimpleName());
                logManager.finishLogEntry("Чтение показаний", false, addr, 0);
            } finally {
                socket.close();
            }

            if (onComplete != null) {
                activity.runOnUiThread(onComplete);
            }
        }).start();
    }

    public void readDateTime(String ip, int port, int addr, Runnable onComplete) {
        logManager.setCurrentLogMessages(new StringBuilder());
        logManager.logMsg("🕐 Чтение даты и времени (Адрес: " + addr + ")...");

        new Thread(() -> {
            MercurySocket socket = new MercurySocket();
            try {
                socket.connect(ip, port, 3000);
                logManager.logMsg("   ✅ TCP соединение установлено");

                socket.sendAndReceive(MercuryCommands.buildKeepalive(addr), 3000);
                socket.sendAndReceive(MercuryCommands.buildLogin(addr), 3000);

                byte[] respDateTime = socket.sendAndReceive(MercuryCommands.buildReadDateTime(addr), 2000);
                logManager.logMsg("   - Дата/время HEX: " + bytesToHex(respDateTime));
                String dateTime = MercuryParser.parseDateTime(respDateTime);
                logManager.logMsg("   - Дата/время: " + dateTime);

                activity.runOnUiThread(() -> {
                    tvDateTime.setText(dateTime);
                    tvDateTime.setVisibility(android.view.View.VISIBLE);

                    if (layoutDateTime != null) {
                        layoutDateTime.setVisibility(android.view.View.VISIBLE);
                    }

                    if (layoutResults != null) {
                        layoutResults.setVisibility(android.view.View.VISIBLE);
                    }

                    if (layoutInfo != null) {
                        layoutInfo.setVisibility(android.view.View.VISIBLE);
                    }
                });

                logManager.logMsg("✅ Чтение даты/времени завершено!");
                logManager.finishLogEntry("Чтение даты/времени", true, addr, 0);

                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "✅ Дата и время получены!", Toast.LENGTH_SHORT).show()
                );

                if (onComplete != null) {
                    activity.runOnUiThread(onComplete);
                }

            } catch (Exception e)  {
                logManager.logMsg("❌ Ошибка: " + e.getMessage());
                logManager.logMsg("   Тип: " + e.getClass().getSimpleName());
                logManager.finishLogEntry("Чтение даты/времени", false, addr, 0);
            } finally {
                socket.close();
            }
        }).start();
    }

    private void hideResults() {
        activity.runOnUiThread(() -> {
            tvT1.setText("0.00");
            tvT2.setText("0.00");
            tvTotal.setText("0.00");
            // ✅ УДАЛЕНО: tvCheck.setText(" ");
            if (layoutResults != null) layoutResults.setVisibility(android.view.View.GONE);
            if (layoutInfo != null) layoutInfo.setVisibility(android.view.View.GONE);
            if (layoutDateTime != null) layoutDateTime.setVisibility(android.view.View.GONE);
        });
    }

    private void updateResults(double t1, double t2, double total, long serial) {
        activity.runOnUiThread(() -> {
            tvT1.setText(DateTimeUtils.formatValue(t1));
            tvT2.setText(DateTimeUtils.formatValue(t2));
            tvTotal.setText(DateTimeUtils.formatValue(total));

            // ✅ УДАЛЕНО: tvCheck.setText(" ");

            if (layoutResults != null) layoutResults.setVisibility(android.view.View.VISIBLE);
            if (layoutInfo != null) layoutInfo.setVisibility(android.view.View.VISIBLE);
        });
    }

    private void saveToHistory(int addr, long serial, double t1, double t2, double total) {
        String datetime = DateTimeUtils.formatDateTimeWithSeconds(new java.util.Date());
        dbHelper.addHistory(addr, serial, datetime, t1, t2, total);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format(java.util.Locale.US, "%02X", b & 0xFF));
        return sb.toString();
    }

    public double getCurrentT1() { return currentT1; }
    public double getCurrentT2() { return currentT2; }
    public double getCurrentTotal() { return currentTotal; }
    public long getCurrentSerial() { return currentSerial; }
}