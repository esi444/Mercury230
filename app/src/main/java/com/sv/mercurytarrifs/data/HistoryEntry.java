package com.sv.mercurytarrifs.data;

public class HistoryEntry {
    public long id = -1;  
    public int address;
    public long serial;
    public String datetime;
    public double t1, t2, total;
    public String name;  

    public HistoryEntry(int address, long serial, String datetime, double t1, double t2, double total) {
        this.address = address;
        this.serial = serial;
        this.datetime = datetime;
        this.t1 = t1;
        this.t2 = t2;
        this.total = total;
        this.name = "—";  
    }

    public HistoryEntry(int address, long serial, String datetime, double t1, double t2, double total, String name) {
        this.address = address;
        this.serial = serial;
        this.datetime = datetime;
        this.t1 = t1;
        this.t2 = t2;
        this.total = total;
        this.name = (name != null && !name.isEmpty()) ? name : "—";
    }

    
    public HistoryEntry(long id, int address, long serial, String datetime, double t1, double t2, double total, String name) {
        this.id = id;
        this.address = address;
        this.serial = serial;
        this.datetime = datetime;
        this.t1 = t1;
        this.t2 = t2;
        this.total = total;
        this.name = (name != null && !name.isEmpty()) ? name : "—";
    }
}