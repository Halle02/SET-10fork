package com.set10.core;
import java.util.ArrayList;
import java.util.List;

public class Reiseforslag {
    public List<Avgang> avganger;
    public Reisesok sok;

    public Reiseforslag(Avgang forsteAvgang, Reisesok sok) {
        this.avganger = new ArrayList<>();
        this.avganger.add(forsteAvgang);
        this.sok = sok;
    }

    public void leggTilAvgang(Avgang avgang) {
        this.avganger.add(avgang);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Reiseforslag:\n");
        for (Avgang avgang : avganger) {
            sb.append(avgang.toString()).append("\n");
        }
        return sb.toString();
    }


    
}
