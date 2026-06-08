package com.sv.mercurytarrifs.protocol;
import com.sv.mercurytarrifs.utils.BcdUtils;
import java.util.Locale;

public class MercuryParser {

    /**
     * Парсинг серийного номера
     * ✅ ПРЯМЫЕ БАЙТЫ — НЕ BCD!
     * ✅ %02d сохраняет ведущие нули (00→"00", 01→"01", 05→"05")
     */
    public static long parseSerialNumber(byte[] resp) {
        if (resp == null || resp.length < 8) return 0;
        try {
            // ✅ ПРЯМЫЕ ЗНАЧЕНИЯ БАЙТОВ — БЕЗ BcdUtils!
            int b0 = resp[1] & 0xFF;
            int b1 = resp[2] & 0xFF;
            int b2 = resp[3] & 0xFF;
            int b3 = resp[4] & 0xFF;

            // ✅ %02d гарантирует 2 цифры с ведущими нулями
            String serial = String.format(Locale.US, "%02d%02d%02d%02d", b0, b1, b2, b3);

            return Long.parseLong(serial);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Парсинг даты выпуска
     */
    public static String parseReleaseDate(byte[] resp) {
        if (resp == null || resp.length < 8) return "Ошибка";
        try {
            int day = resp[5] & 0xFF;
            int month = resp[6] & 0xFF;
            int year = resp[7] & 0xFF;

            if (day < 1 || day > 31) return "Ошибка дня (" + day + ")";
            if (month < 1 || month > 12) return "Ошибка месяца (" + month + ")";

            int fullYear = (year < 100) ? 2000 + year : year;
            return String.format(Locale.getDefault(), "%02d.%02d.%04d", day, month, fullYear);
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
        }
    }

    /**
     * Парсинг тарифа
     */
    public static double parseTariff(byte[] resp) {
        if (resp == null || resp.length < 5) return -1;
        int b0 = resp[1] & 0xFF;
        int b1 = resp[2] & 0xFF;
        int b2 = resp[3] & 0xFF;
        int b3 = resp[4] & 0xFF;
        int s0 = b1;
        int s1 = b0;
        int s2 = b3;
        int s3 = b2;
        long val = ((long)s0 << 24) | ((long)s1 << 16) | ((long)s2 << 8) | (long)s3;
        return val * 0.001;
    }

    /**
     * Парсинг даты и времени
     * ✅ ИСПРАВЛЕНО: Правильные индексы для Mercury 230
     * ✅ ВАРИАНТ B: Показываем день недели (Чт, Пт, Сб и т.д.)
     *
     * Формат ответа: [Адрес] [Сек] [Мин] [Час] [ДеньНед] [День] [Месяц] [Год] [?] [CRC]
     *                 [0]    [1]   [2]   [3]   [4]        [5]    [6]     [7]    [8]  [9-10]
     */
    public static String parseDateTime(byte[] resp) {
        if (resp == null || resp.length < 9) return "🕐 Ошибка";
        try {
            // ✅ ПРАВИЛЬНЫЕ ИНДЕКСЫ (по данным логов):
            int second = BcdUtils.bcdToDec(resp[1] & 0xFF);   // ✅ Байт 1 = секунды
            int minute = BcdUtils.bcdToDec(resp[2] & 0xFF);   // ✅ Байт 2 = минуты
            int hour = BcdUtils.bcdToDec(resp[3] & 0xFF);     // ✅ Байт 3 = часы
            int dayOfWeek = resp[4] & 0xFF;                   // ✅ Байт 4 = день недели (01-07)
            int day = BcdUtils.bcdToDec(resp[5] & 0xFF);      // ✅ Байт 5 = день
            int month = BcdUtils.bcdToDec(resp[6] & 0xFF);    // ✅ Байт 6 = месяц
            int year = BcdUtils.bcdToDec(resp[7] & 0xFF);     // ✅ Байт 7 = год
            int fullYear = (year < 50) ? 2000 + year : 1900 + year;

            // ✅ ВАРИАНТ B: Добавляем день недели в вывод
            String dayOfWeekStr = getDayOfWeekName(dayOfWeek);

            return String.format(Locale.getDefault(), "🕐 %02d.%02d.%04d %02d:%02d:%02d (%s)",
                    day, month, fullYear, hour, minute, second, dayOfWeekStr);
        } catch (Exception e) {
            return "🕐 Ошибка парсинга: " + e.getMessage();
        }
    }

    /**
     * Получение названия дня недели
     * ✅ 01=Пн, 02=Вт, 03=Ср, 04=Чт, 05=Пт, 06=Сб, 07=Вс
     */
    private static String getDayOfWeekName(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1: return "Пн";
            case 2: return "Вт";
            case 3: return "Ср";
            case 4: return "Чт";
            case 5: return "Пт";
            case 6: return "Сб";
            case 7: return "Вс";
            default: return "";
        }
    }

    /**
     * Проверка успешности ответа
     */
    public static boolean isSuccess(byte[] resp) {
        return resp != null && resp.length >= 2 && resp[1] == 0x00;
    }
}