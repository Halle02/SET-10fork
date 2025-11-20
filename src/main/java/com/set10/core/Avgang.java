package com.set10.core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Avgang {
    public int id;
    public int ruteID;
    public int stoppestedID;
    public LocalTime tidspunkt;

    public Avgang(int id, int ruteID, int stoppested, LocalTime tidspunkt) {
        this.id = id;
        this.ruteID = ruteID;
        this.stoppestedID = stoppested;
        this.tidspunkt = tidspunkt;
    }

    public Avgang(int ruteID, int stoppested, LocalTime tidspunkt) {
        this.ruteID = ruteID;
        this.stoppestedID = stoppested;
        this.tidspunkt = tidspunkt;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return "(Rute " + ruteID + ") - Avgang: " + tidspunkt.format(formatter);
    }

}