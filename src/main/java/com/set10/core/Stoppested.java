package com.set10.core;
import com.set10.core.Avgang;

import java.time.LocalTime;
import java.util.ArrayList;

public class Stoppested {
    public int id;
    public String navn;
    public ArrayList<Avgang> avganger = new ArrayList<>(); 
    public int sone;

    public Stoppested(String navn) {
        this.navn = navn;
    }

    public Stoppested(int id, String navn) {
        this.id = id;
        this.navn = navn;
    }

    public Stoppested(String navn, int sone) {
        this.navn = navn;
        this.sone = sone;
    }

    public void leggTilAvgang(Avgang avgang) {
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
