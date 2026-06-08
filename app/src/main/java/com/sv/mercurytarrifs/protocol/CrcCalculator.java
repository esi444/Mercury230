package com.sv.mercurytarrifs.protocol;

public class CrcCalculator {

    /**
     * Расчёт CRC-16 для команды Меркурий
     */
    public static byte[] calcCrc(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc >>>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>>= 1;
                }
            }
        }
        crc &= 0xFFFF;
        return new byte[] { (byte)(crc & 0xFF), (byte)((crc >> 8) & 0xFF) };
    }
}