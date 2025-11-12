package com.set10.core;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Reiseforslag {
    public List<Avgang> avganger;
    public Reisesok sok;
    public List<Stoppested> bytteStopp;
    public LocalTime ankomstTid;

    public Reiseforslag(Avgang forsteAvgang, Reisesok sok) {
        this.avganger = new ArrayList<>();
        this.avganger.add(forsteAvgang);
        this.sok = sok;
        this.bytteStopp = new ArrayList<>();
    }


    public void leggTilAvgang(Avgang avgang) {
        this.avganger.add(avgang);
    }
    public void leggTilBytteStopp(Stoppested stopp) {
        this.bytteStopp.add(stopp);
    }
    public void settAnkomstTid(LocalTime ankomstTid) {
        this.ankomstTid = ankomstTid;
    }

     
    @Override
    public String toString() {
        if (avganger.size() == 1) {
            return "Direkte reise:\n" + avganger.get(0) + 
                    "\nFra:   *  " + sok.fraStoppested.navn + 
                    "\n       |"+
                    "\n       |"+
                    "\n       V"+
                    "\nTil:   *  " + sok.tilStoppested.navn + 
                    "\nAnkomst:    " + ankomstTid;
        } else {
            String reiseInfo = "";
            reiseInfo += "1: " + avganger.get(0) + "  * " + sok.fraStoppested.navn;
            reiseInfo += "\n                              |";
            reiseInfo += "\n                              |";
            reiseInfo += "\n                              V";
            
            for (int i = 0; i < bytteStopp.size(); i++) {
                reiseInfo += "\n" + (i + 2) + ": Gå av på: " + "                 * " + bytteStopp.get(i).navn;
                reiseInfo += "\n                              |";
                reiseInfo += "\n" + (i + 3) + ": " + avganger.get(i + 1) + "  * " + bytteStopp.get(i).navn;
                reiseInfo += "\n                              |";
                reiseInfo += "\n                              |";
                reiseInfo += "\n                              V";
                reiseInfo += "\n" + (i + 4) + ": Destinasjon:        " + ankomstTid + "  * " +sok.tilStoppested.navn;
            }
            
            return reiseInfo;
        }
    }
}
