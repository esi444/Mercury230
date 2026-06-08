package com.sv.mercurytarrifs.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    /**
     * Форматирование даты и времени
     */
    public static String formatDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Форматирование даты и времени с секундами
     */
    public static String formatDateTimeWithSeconds(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Форматирование значения тарифа
     */
    public static String formatValue(double value) {
        return (value >= 0) ? String.format(Locale.US, "%.2f", value) : "---";
    }
}