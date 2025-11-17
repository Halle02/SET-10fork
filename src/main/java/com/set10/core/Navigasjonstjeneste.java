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
    // Midlertidige lister for integrasjonstester og enkel bruk
    public java.util.List<Stoppested> stoppesteder = new java.util.ArrayList<>();
    public java.util.List<Rute> ruter = new java.util.ArrayList<>();

    public Navigasjonstjeneste(){
    }

    /**
     * Hent listen med stoppesteder mellom to stopp (inklusive begge). Søker gjennom tilgjengelige ruter
     * og returnerer segmentet hvis begge stopp finnes på samme rute (i begge retninger).
     */
    public java.util.List<Stoppested> hentStoppMellom(Stoppested a, Stoppested b) {
        java.util.List<Rute> sokRuter;
        if (dataDepot != null && dataDepot.hentRuter() != null) {
            sokRuter = dataDepot.hentRuter();
        } else {
            sokRuter = this.ruter;
        }

        for (Rute r : sokRuter) {
            int idxA = r.stopp.indexOf(a);
            int idxB = r.stopp.indexOf(b);
            if (idxA != -1 && idxB != -1) {
                if (idxA <= idxB) {
                    return new java.util.ArrayList<>(r.stopp.subList(idxA, idxB + 1));
                } else {
                    // Returner segment i motsatt rekkefølge hvis b ligger før a
                    java.util.List<Stoppested> segment = new java.util.ArrayList<>(r.stopp.subList(idxB, idxA + 1));
                    java.util.Collections.reverse(segment);
                    return segment;
                }
            }
        }
        return new java.util.ArrayList<>();
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
            // Støtt begge retninger: både når tilIndex > fraIndex og når tilIndex < fraIndex
            //Fra A til B og B til A
            //All andre jobbe steder løser seg selv automatisk
            if (fraIndex != -1 && tilIndex != -1 && tilIndex != fraIndex) {
                Reiseforslag nyttForslag = new Reiseforslag(avgang, sok);
                nyttForslag.leggTilRute(ruten);
                LocalTime ankomstTid = beregnAnkomstTid(avgang, fraStopp, tilStopp);
                nyttForslag.settAnkomstTid(ankomstTid);
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

        alleGyldigeReiser.sort(Comparator.comparing(rf -> rf.avganger.get(0).tidspunkt));
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

            //finner bare stopp etter startstoppestedet for mulig bytte
            for (int i = fraIndex + 1; i < førsteRute.stopp.size(); i++) {
                Stoppested bytteStopp = førsteRute.stopp.get(i);
                LocalTime estimertAnkomstBytte = beregnEstimertAnkomst(førsteAvgang.tidspunkt, fraIndex, i);
                
                // Krev minimum 5 minutter byttetid
                LocalTime minimumAvgangsTid = estimertAnkomstBytte.plusMinutes(5);
                
                for (Avgang andreAvgang : bytteStopp.hentAvganger()) {
                    if (andreAvgang.tidspunkt.isBefore(minimumAvgangsTid)) {
                        continue;
                    }

                    Rute andreRute = dataDepot.hentRute(andreAvgang.ruteID);
                    if (andreRute == null) continue;

                    int bytteIndex = andreRute.stopp.indexOf(bytteStopp);
                    int tilIndex = andreRute.stopp.indexOf(tilStopp);
                    
                    //sjekk rekkefølge
                    if (bytteIndex != -1 && tilIndex != -1 && tilIndex > bytteIndex) {
                        Reiseforslag reiseMedBytte = new Reiseforslag(førsteAvgang, sok);
                        reiseMedBytte.leggTilRute(førsteRute);
                        reiseMedBytte.leggTilAvgang(andreAvgang);
                        reiseMedBytte.leggTilRute(andreRute);
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
        int antallStopp = Math.abs(tilIndex - fraIndex);
        int minutter = antallStopp * 5; //5 min mellom hvert stopp
        return avgangsTid.plusMinutes(minutter);
    }

    // ny metode, beregner ankomsttid basert på avgang, fra-stoppested og til-stoppested
    public LocalTime beregnAnkomstTid(Avgang avgang, Stoppested fraStopp, Stoppested tilStopp) {
        Rute rute = dataDepot.hentRute(avgang.ruteID);
        if (rute == null) return null;
        int fraIndex = rute.stopp.indexOf(fraStopp);
        int tilIndex = rute.stopp.indexOf(tilStopp);
        if (fraIndex == -1 || tilIndex == -1 || tilIndex == fraIndex) return null;
        return beregnEstimertAnkomst(avgang.tidspunkt, fraIndex, tilIndex);
    }

    /**
     * Hjelpemetode for tester: registrerer en avgang på riktig stoppested
     */
    public void opprettAvgang(Avgang avgang) {
        if (avgang == null) return;
        for (Stoppested s : stoppesteder) {
            if (s != null && s.id == avgang.stoppestedID) {
                s.leggTilAvgang(avgang);
                return;
            }
        }
        // Hvis stoppested ikke funnet, legg ikke til — dette er akseptabelt for testformål
    }
}