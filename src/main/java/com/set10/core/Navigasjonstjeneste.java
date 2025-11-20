package com.set10.core;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collection;


/*
 * Denne klassen har en serie med metoder ment for å kunne utføre grunnleggende handlinger 
 * som har med navigasjon å gjøre. 
 */
public class Navigasjonstjeneste {

    public Datadepot dataDepot;

    public Navigasjonstjeneste(){
    }

    // Vil eventuelt ha slike metoder
    // public Reise FinnReise(Stoppested A, Stoppested B){}
    
    // public reise FinnNærmesteStoppested(Posisjon posisjon){}

    public List<Reiseforslag> FinnReiser(Reisesok sok) {
        LocalTime onsketAvreiseTid = sok.avreiseTid.toLocalTime(); 
        Stoppested fraStopp = sok.getFraStoppested();
        Stoppested tilStopp = sok.getTilStoppested();
        List<Reiseforslag> alleGyldigeReiser = new ArrayList<>();
        System.out.println("Søker reise fra " + fraStopp.navn + " til " + tilStopp.navn + " etter " + onsketAvreiseTid);

        for (Avgang avgang : fraStopp.hentAvganger()) {
            if (avgang.tidspunkt.isBefore(onsketAvreiseTid)) {
                continue;
            }

            Rute ruten = dataDepot.hentRute(avgang.ruteID);
            if (ruten == null || ruten.stopp.isEmpty()) {
                continue;
            }

            int fraIndex = ruten.stopp.indexOf(fraStopp);
            int tilIndex = ruten.stopp.indexOf(tilStopp);
            if (fraIndex != -1 && tilIndex != -1 && fraIndex != tilIndex) {
                Reiseforslag nyttForslag = new Reiseforslag(avgang, sok);
                LocalTime ankomstTid = beregnAnkomstTid(avgang, fraStopp, tilStopp);
                nyttForslag.settAnkomstTid(ankomstTid);
                if (sok.minankomst != null && ankomstTid.isBefore(sok.minankomst)) continue;
                if (sok.maxankomst != null && ankomstTid.isAfter(sok.maxankomst)) continue;
                    
                
                alleGyldigeReiser.add(nyttForslag);
            }
        }

        if (alleGyldigeReiser.isEmpty()) {
            alleGyldigeReiser = finnReiserMedBytte(sok, onsketAvreiseTid);
        }

        if (alleGyldigeReiser.isEmpty()) {
            System.out.println("Fant ingen reiser som passer for søket ditt.");
            return alleGyldigeReiser;
        }

       // alleGyldigeReiser.sort(Comparator.comparing(rf -> rf.avganger.get(0).tidspunkt));
       alleGyldigeReiser.sort(Comparator.comparing(Reiseforslag:: ReisetidMinutter));
        System.out.println("Fant " + alleGyldigeReiser.size() + " reiseforslag totalt.");
        return alleGyldigeReiser;
    }

    private List<Reiseforslag> finnReiserMedBytte(Reisesok sok, LocalTime onsketAvreiseTid) {
        List<Reiseforslag> reiserMedBytte = new ArrayList<>();
        Stoppested fraStopp = sok.getFraStoppested();
        Stoppested tilStopp = sok.getTilStoppested();

        for (Avgang førsteAvgang : fraStopp.hentAvganger()) {
            if (førsteAvgang.tidspunkt.isBefore(onsketAvreiseTid)) {
                continue;
            }

            Rute førsteRute = dataDepot.hentRute(førsteAvgang.ruteID);
            if (førsteRute == null) continue;

            int fraIndex = førsteRute.stopp.indexOf(fraStopp);
            if (fraIndex == -1) continue;

            for (int i = 0; i < førsteRute.stopp.size(); i++) {
                if (i == fraIndex) continue;
                Stoppested bytteStopp = førsteRute.stopp.get(i);
                LocalTime estimertAnkomstBytte;
                
                if (i > fraIndex) {
                    estimertAnkomstBytte = beregnEstimertAnkomst(førsteAvgang.tidspunkt, fraIndex, i);
                } else {
                    estimertAnkomstBytte = beregnEstimertAnkomst(førsteAvgang.tidspunkt, fraIndex, førsteRute.stopp.size());
                    int extraMinutter = (i) * 2;
                    estimertAnkomstBytte = estimertAnkomstBytte.plusMinutes(extraMinutter);
                }
                
                for (Avgang andreAvgang : bytteStopp.hentAvganger()) {
                    if (andreAvgang.tidspunkt.isBefore(estimertAnkomstBytte)) {
                        continue;
                    }

                    Rute andreRute = dataDepot.hentRute(andreAvgang.ruteID);
                    if (andreRute == null) continue;

                    int bytteIndex = andreRute.stopp.indexOf(bytteStopp);
                    int tilIndex = andreRute.stopp.indexOf(tilStopp);
                    
                    //sjekk at de begge finnes og at det er ikke samme stopp
                    if (bytteIndex != -1 && tilIndex != -1 && bytteIndex != tilIndex) {
                        Reiseforslag reiseMedBytte = new Reiseforslag(førsteAvgang, sok);
                        reiseMedBytte.leggTilAvgang(andreAvgang);
                        reiseMedBytte.leggTilBytteStopp(bytteStopp);
                        LocalTime ankomstTid = beregnAnkomstTid(andreAvgang, bytteStopp, tilStopp);
                        reiseMedBytte.settAnkomstTid(ankomstTid);
                        reiserMedBytte.add(reiseMedBytte);
                    }
                }
            }
        }
        
        return reiserMedBytte;
    }

    // gir bare et estimat, gammel metode
    public LocalTime beregnEstimertAnkomst(LocalTime avgangsTid, int fraIndex, int tilIndex) {
        int antallStopp = tilIndex - fraIndex;
        int minutter = antallStopp * 2; //2 min mellom hvert stopp
        return avgangsTid.plusMinutes(minutter);
    }

    // ny metode, beregner ankomsttid basert på avgang, fra-stoppested og til-stoppested
    public LocalTime beregnAnkomstTid(Avgang avgang, Stoppested fraStopp, Stoppested tilStopp) {
        Rute rute = dataDepot.hentRute(avgang.ruteID);
        if (rute == null) return null;
        int fraIndex = rute.stopp.indexOf(fraStopp);
        int tilIndex = rute.stopp.indexOf(tilStopp);
        if (fraIndex == -1 || tilIndex == -1 || fraIndex == tilIndex) return null;
        int antallStopp;
        if (tilIndex > fraIndex) {
            antallStopp = tilIndex - fraIndex;
        } else {
            antallStopp = (rute.stopp.size() - fraIndex) + tilIndex;
        }
        int minutter = antallStopp * 2; //2 min mellom hvert stopp - midlertidig fiks på tidsestimat
        return avgang.tidspunkt.plusMinutes(minutter);
    }
}