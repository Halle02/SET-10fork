package com.set10.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class NavigasjonsTjenesteTester{

    //tull bar test jeg driver med
    @Test
    @DisplayName("FinnReiser() finner én direkte reise etter ønsket tid når flere avganger finnes")
    void testFinnReiser_MultipleAvganger() {

        Datadepot depot = new Datadepot(null);
        Navigasjonstjeneste tjeneste = new Navigasjonstjeneste();
        tjeneste.dataDepot = depot;

        Stoppested stoppA = new Stoppested(0, "Halden");
        Stoppested stoppB = new Stoppested(1, "Sarpsborg");
        Stoppested stoppC = new Stoppested(2, "Fredrikstad");

        depot.hentStoppesteder().add(stoppA);
        depot.hentStoppesteder().add(stoppB);
        depot.hentStoppesteder().add(stoppC);

        Rute rute1 = new Rute(10);
        rute1.leggTilStopp(stoppA);
        rute1.leggTilStopp(stoppB);
        rute1.leggTilStopp(stoppC);
        depot.hentRuter().add(rute1);

        //flere avganger på samme rute
        stoppA.leggTilAvgang(new Avgang(1, rute1.id, stoppA.id, LocalTime.of(12, 0)));
        stoppA.leggTilAvgang(new Avgang(2, rute1.id, stoppA.id, LocalTime.of(12, 15)));
        stoppA.leggTilAvgang(new Avgang(3, rute1.id, stoppA.id, LocalTime.of(12, 30)));

        LocalDateTime sokTid = LocalDateTime.of(2025, 1, 1, 11, 0);
        Reisesok sok = new Reisesok(stoppA, stoppC, sokTid, null);

        List<Reiseforslag> forslag = tjeneste.FinnReiser(sok);

        //kun returnere neste reise
        assertEquals(1, forslag.size(), "FinnReiser returnerer kun den neste reisen");

        Reiseforslag valgt = forslag.get(0);

        assertEquals(stoppA, valgt.sok.getFraStoppested(), "Start skal være Halden");
        assertEquals(stoppC, valgt.sok.getTilStoppested(), "Slutt skal være Fredrikstad");
        assertTrue(valgt.avganger.get(0).tidspunkt.isAfter(LocalTime.of(11, 0)),
                "Avgangen må være etter ønsket tid");
    }

    @Test
    @DisplayName("FinnReiser() returnerer tom liste når ingen reise passer etter ønsket tid")
    void testFinnReiser_IngenReiseFunnet() {

        Datadepot depot = new Datadepot(null);
        Navigasjonstjeneste tjeneste = new Navigasjonstjeneste();
        tjeneste.dataDepot = depot;

        Stoppested stoppA = new Stoppested(0, "Halden");
        Stoppested stoppB = new Stoppested(1, "Sarpsborg");

        depot.hentStoppesteder().add(stoppA);
        depot.hentStoppesteder().add(stoppB);

        Rute rute = new Rute(20);
        rute.leggTilStopp(stoppA);
        rute.leggTilStopp(stoppB);
        depot.hentRuter().add(rute);

        //teste Avgang FØR ønsket tid
        stoppA.leggTilAvgang(new Avgang(1, rute.id, stoppA.id, LocalTime.of(8, 0)));

        LocalDateTime sokTid = LocalDateTime.of(2025, 1, 1, 9, 0);
        Reisesok sok = new Reisesok(stoppA, stoppB, sokTid, null);

        List<Reiseforslag> forslag = tjeneste.FinnReiser(sok);
        assertTrue(forslag.isEmpty(), "Det skal ikke finnes noen gyldig reise etter kl. 09:00");
    }

}


 



//public class NavigasjonsTjenesteTester {

    // @Test
    // @DisplayName("can create and link navigation data")
    // void LagOgLenkeNavigasjonsData() {
    //     Navigasjonstjeneste navcontainer = new Navigasjonstjeneste();
    //     navcontainer.stoppesteder.add(new Stoppested(0, "a"));
    //     navcontainer.stoppesteder.add(new Stoppested(1, "b"));
    //     navcontainer.stoppesteder.add(new Stoppested(2, "c"));
    //     navcontainer.stoppesteder.add(new Stoppested(3, "d"));
    //     navcontainer.stoppesteder.add(new Stoppested(4, "e"));

    //     Rute rute = new Rute(0);
    //     navcontainer.ruter.add(rute);

    //     rute.leggTilStopp(navcontainer.stoppesteder.get(0));
    //     rute.leggTilStopp(navcontainer.stoppesteder.get(1));
    //     rute.leggTilStopp(navcontainer.stoppesteder.get(2));
    //     rute.leggTilStopp(navcontainer.stoppesteder.get(3));
    //     rute.leggTilStopp(navcontainer.stoppesteder.get(4));

    //     assertEquals(rute.stopp.get(0), navcontainer.stoppesteder.get(0));
    //     assertEquals(rute.stopp.get(2), navcontainer.stoppesteder.get(2));

    //     assertEquals(rute.stopp.size(), 5);
    //     assertEquals(navcontainer.stoppesteder.size(), 5);
    // }
//

