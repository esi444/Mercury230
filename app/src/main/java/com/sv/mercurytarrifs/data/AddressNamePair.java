package com.sv.mercurytarrifs.data;

/**
 * Пара "адрес + имя" для автосчитывания
 */
public class AddressNamePair {
    public final int address;
    public final String name;

    public AddressNamePair(int address, String name) {
        this.address = address;
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " (адрес: " + address + ")";
    }
}