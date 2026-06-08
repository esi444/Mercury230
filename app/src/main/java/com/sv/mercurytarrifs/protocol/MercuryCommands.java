package com.sv.mercurytarrifs.protocol;
import com.sv.mercurytarrifs.utils.BcdUtils;
import java.util.Calendar;

public class MercuryCommands {

    /**
     * Команда Keepalive
     */
    public static byte[] buildKeepalive(int addr) {
        return buildCommand(addr, new byte[]{0x00});
    }

    /**
     * Команда Login (пароль 1 уровня)
     */
    public static byte[] buildLogin(int addr) {
        return buildCommand(addr, new byte[]{0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01});
    }

    /**
     * Команда Login (пароль 2 уровня)
     * ✅ 0x01 0x02 + 6 байт пароля
     */
    public static byte[] buildLogin(int addr, byte[] password) {
        byte[] loginData = new byte[8];
        loginData[0] = 0x01;
        loginData[1] = 0x02;
        int passLen = Math.min(password.length, 6);
        System.arraycopy(password, 0, loginData, 2, passLen);
        return buildCommand(addr, loginData);
    }

    /**
     * Чтение тарифа 1
     */
    public static byte[] buildReadT1(int addr) {
        return buildCommand(addr, new byte[]{0x05, 0x00, 0x01});
    }

    /**
     * Чтение тарифа 2
     */
    public static byte[] buildReadT2(int addr) {
        return buildCommand(addr, new byte[]{0x05, 0x00, 0x02});
    }

    /**
     * Чтение суммы
     */
    public static byte[] buildReadTotal(int addr) {
        return buildCommand(addr, new byte[]{0x05, 0x00, 0x00});
    }

    /**
     * Чтение серийного номера и даты выпуска
     */
    public static byte[] buildReadSerial(int addr) {
        return buildCommand(addr, new byte[]{0x08, 0x00});
    }

    /**
     * Чтение даты и времени
     */
    public static byte[] buildReadDateTime(int addr) {
        return buildCommand(addr, new byte[]{0x04, 0x00});
    }

    /**
     * Запись даты и времени
     */
    public static byte[] buildWriteDateTime(int addr, Calendar calendar) {
        int second = calendar.get(Calendar.SECOND);
        int minute = calendar.get(Calendar.MINUTE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR) % 100;

        byte[] timeData = new byte[]{
                0x09, 0x00,
                (byte) BcdUtils.decToBcd(second),
                (byte) BcdUtils.decToBcd(minute),
                (byte) BcdUtils.decToBcd(hour),
                (byte) dayOfWeek,
                (byte) BcdUtils.decToBcd(day),
                (byte) BcdUtils.decToBcd(month),
                (byte) BcdUtils.decToBcd(year)
        };

        return buildCommand(addr, timeData);
    }

    /**
     * Построение полной команды с CRC
     */
    private static byte[] buildCommand(int addr, byte[] body) {
        byte[] data = new byte[body.length + 1];
        data[0] = (byte) addr;
        System.arraycopy(body, 0, data, 1, body.length);
        byte[] crc = CrcCalculator.calcCrc(data);
        byte[] result = new byte[data.length + 2];
        System.arraycopy(data, 0, result, 0, data.length);
        result[data.length] = crc[0];
        result[data.length + 1] = crc[1];
        return result;
    }
}