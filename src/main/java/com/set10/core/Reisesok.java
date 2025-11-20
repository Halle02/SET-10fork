package com.set10.core;

import java.time.LocalDateTime;

public class Reisesok {
    public Stoppested fraStoppested;
    public Stoppested tilStoppested;
    public LocalDateTime avreiseTid;
    public LocalDateTime ankomstTid;

    public Reisesok(Stoppested fraStoppested, Stoppested tilStoppested, LocalDateTime avreiseTid, LocalDateTime ankomstTid) {
        this.fraStoppested = fraStoppested;
        this.tilStoppested = tilStoppested;
        this.avreiseTid = avreiseTid;
        this.ankomstTid = ankomstTid;
    }

    public Reisesok(Stoppested fraStoppested, Stoppested tilStoppested) {
        this.fraStoppested = fraStoppested;
        this.tilStoppested = tilStoppested;
    }


    public Stoppested getFraStoppested() {
        return fraStoppested;
    }
    public Stoppested getTilStoppested() {
        return tilStoppested;
    }
    
}
