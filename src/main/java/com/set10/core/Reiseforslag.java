package com.set10.core;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Reiseforslag {
    public List<Avgang> avganger;
    public Reisesok sok;
    public List<Stoppested> bytteStopp;
    public List<Rute> ruter;
    public LocalTime ankomstTid;

    public Reiseforslag(Avgang forsteAvgang, Reisesok sok) {
        this.avganger = new ArrayList<>();
        this.avganger.add(forsteAvgang);
        this.sok = sok;
        this.bytteStopp = new ArrayList<>();
        this.ruter = new ArrayList<>();
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
    
    public void leggTilRute(Rute rute) {
        if (rute != null && !ruter.contains(rute)) {
            ruter.add(rute);
        }
    }
    
    public List<Rute> hentRuter() {
        return ruter;
    }
    
    public List<Stoppested> hentAlleStoppPaaReisen() {
        List<Stoppested> alleStoppPaaReisen = new ArrayList<>();
        
        if (avganger.isEmpty() || ruter.isEmpty()) {
            return alleStoppPaaReisen;
        }
        
        // For hver avgang/rute i reisen
        for (int i = 0; i < avganger.size() && i < ruter.size(); i++) {
            Avgang avgang = avganger.get(i);
            Rute rute = ruter.get(i);
            Stoppested startStopp = null;
            
            // Finn startstedet fra avgangens stoppestedID
            for (Stoppested s : rute.stopp) {
                if (s != null && s.id == avgang.stoppestedID) {
                    startStopp = s;
                    break;
                }
            }
            
            if (startStopp == null) continue;
            
            // Finn neste stopp (byttestedet eller sluttdestinasjonen)
            Stoppested sluttStopp = null;
            if (i < bytteStopp.size()) {
                sluttStopp = bytteStopp.get(i);
            } else if (i == avganger.size() - 1) {
                // Siste avgang - bruk destinasjonen fra søket
                sluttStopp = sok.tilStoppested;
            }
            
            // Hent alle stopp mellom start og slutt på denne ruten
            List<Stoppested> stoppestedPaaRute = rute.stopp;
            int startIndex = stoppestedPaaRute.indexOf(startStopp);
            int sluttIndex = sluttStopp != null ? stoppestedPaaRute.indexOf(sluttStopp) : stoppestedPaaRute.size() - 1;
            
            if (startIndex != -1 && sluttIndex != -1 && startIndex < sluttIndex) {
                for (int j = startIndex; j <= sluttIndex; j++) {
                    Stoppested stopp = stoppestedPaaRute.get(j);
                    // Unngå duplikater (bytter vises bare én gang)
                    if (!alleStoppPaaReisen.contains(stopp)) {
                        alleStoppPaaReisen.add(stopp);
                    }
                }
            }
        }
        
        return alleStoppPaaReisen;
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
