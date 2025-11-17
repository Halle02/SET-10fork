package com.set10.core;
import com.set10.core.Avgang;

import java.time.LocalTime;
import java.util.ArrayList;

public class Stoppested {
    public int id;
    public String navn;
    public ArrayList<Avgang> avganger = new ArrayList<>(); 
    public int sone;
    public double latitude;
    public double longitude;

    public Stoppested(String navn) {
        this.navn = navn;
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    public Stoppested(int id, String navn) {
        this.id = id;
        this.navn = navn;
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    public Stoppested(String navn, int sone) {
        this.navn = navn;
        this.sone = sone;
        this.latitude = 0.0;
        this.longitude = 0.0;
    }
    
    public Stoppested(int id, String navn, double latitude, double longitude) {
        this.id = id;
        this.navn = navn;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void leggTilAvgang(Avgang avgang) {
        if (avgang == null) return;
        // Unngå dobbel-registrering av samme avgang på samme stopp
        for (Avgang a : avganger) {
            if (a != null && a.ruteID == avgang.ruteID && a.stoppestedID == avgang.stoppestedID && a.tidspunkt != null && a.tidspunkt.equals(avgang.tidspunkt)) {
                return;
            }
        }
        avganger.add(avgang);
    }

    public void visAvganger() {
        System.out.println("Avganger fra " + navn + ":");
        for (Avgang a : avganger) {
            System.out.println("  " + a);
        }
    }

    public int getSone() {
        return sone;
    }

    @Override
    public String toString() {
        return "StoppID: " + id + " Stoppested: " + navn + ".";
    }



    public ArrayList<Avgang> hentAvganger() {
        return avganger;
    }
    
}
