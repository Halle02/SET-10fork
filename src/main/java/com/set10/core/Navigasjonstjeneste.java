package com.set10.core;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collection;
import java.util.Comparator;

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

            // riktig rekkefølge-sjekk
            int fraIndex = ruten.stopp.indexOf(fraStopp);
            int tilIndex = ruten.stopp.indexOf(tilStopp);
            if (fraIndex != -1 && tilIndex != -1 && tilIndex > fraIndex) {
                Reiseforslag nyttForslag = new Reiseforslag(avgang, sok); 
                alleGyldigeReiser.add(nyttForslag);
            }
        }
        
        if (alleGyldigeReiser.isEmpty()) {
            System.out.println("Fant ingen reiser som passer for søket ditt.");
            return alleGyldigeReiser;
        }

        alleGyldigeReiser.sort(Comparator.comparing(rf -> rf.avganger.get(0).tidspunkt));
        List<Reiseforslag> nesteReise = new ArrayList<>();
        nesteReise.add(alleGyldigeReiser.get(0));
        System.out.println("Fant " + alleGyldigeReiser.size() + " totalt. Returnerer den neste.");
        return nesteReise;
    }

}
