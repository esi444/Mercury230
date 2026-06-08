package com.sv.mercurytarrifs.data;

public class LogEntry {
    public String timestamp;
    public int address;
    public long serial;
    public String messages;
    public boolean success;
    public double t1, t2, total;

    public LogEntry(String timestamp, int address, long serial, String messages, boolean success, double t1, double t2, double total) {
        this.timestamp = timestamp;
        this.address = address;
        this.serial = serial;
        this.messages = messages;
        this.success = success;
        this.t1 = t1;
        this.t2 = t2;
        this.total = total;
    }
}