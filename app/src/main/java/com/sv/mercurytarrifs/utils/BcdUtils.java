package com.sv.mercurytarrifs.utils;

public class BcdUtils {

    /**
     * Преобразование десятичного числа в BCD
     */
    public static int decToBcd(int dec) {
        return ((dec / 10) << 4) | (dec % 10);
    }

    /**
     * Преобразование BCD в десятичное число
     */
    public static int bcdToDec(int bcd) {
        return ((bcd >> 4) & 0x0F) * 10 + (bcd & 0x0F);
    }
}