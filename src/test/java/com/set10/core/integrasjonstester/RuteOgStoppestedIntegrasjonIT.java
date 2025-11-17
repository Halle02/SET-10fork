package com.set10.core.integrasjonstester;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.set10.core.Avgang;
import com.set10.core.Rute;
import com.set10.core.Stoppested;

import java.time.LocalTime;

/**
 * Integrasjonstest: Tester hvordan Rute og Stoppested fungerer sammen.
 
 */
public class RuteOgStoppestedIntegrasjonIT {

    @Test
    @DisplayName("Rute kan legge til stoppesteder og beregne rute")
    void ruteKanLeggeTilStoppesteder() {
        // Opprett stoppesteder
        Stoppested oslo = new Stoppested("Oslo");
        Stoppested drammen = new Stoppested("Drammen");
        Stoppested tønsberg = new Stoppested("Tønsberg");

        // Opprett rute og legg til stopp
        Rute r = new Rute(1);
        r.leggTilStopp(oslo);
        r.leggTilStopp(drammen);
        r.leggTilStopp(tønsberg);

        // Assert
        assertEquals(3, r.stopp.size());
        assertEquals("Oslo", r.stopp.get(0).navn);
        assertEquals("Drammen", r.stopp.get(1).navn);
        assertEquals("Tønsberg", r.stopp.get(2).navn);
    }

    @Test
    @DisplayName("Stoppested kan legge til avganger")
    void stoppestedKanLeggeTilAvganger() {
        Stoppested halden = new Stoppested("Halden");
        
        Avgang a1 = new Avgang(33, halden.id, LocalTime.of(8, 30));
        Avgang a2 = new Avgang(34, halden.id, LocalTime.of(9, 15));

        halden.leggTilAvgang(a1);
        halden.leggTilAvgang(a2);

        assertEquals(2, halden.avganger.size());
        assertEquals(LocalTime.of(8, 30), halden.avganger.get(0).tidspunkt);
        assertEquals(LocalTime.of(9, 15), halden.avganger.get(1).tidspunkt);
    }

    @Test
    @DisplayName("Stoppested unngår duplikate avganger")
    void stoppestedUnngaarDuplikater() {
        Stoppested stopp = new Stoppested(1, "Test");
        
        Avgang a = new Avgang(10, 1, LocalTime.of(10, 0));
        stopp.leggTilAvgang(a);
        stopp.leggTilAvgang(a); // samme avgang

        assertEquals(1, stopp.avganger.size(), "Samme avgang skal ikke legges til to ganger");
    }

    @Test
    @DisplayName("Rute med stoppesteder som har avganger")
    void ruteIntegrertMedAvganger() {
        Stoppested s1 = new Stoppested(0, "Start");
        Stoppested s2 = new Stoppested(1, "Midt");
        Stoppested s3 = new Stoppested(2, "Slutt");

        s1.leggTilAvgang(new Avgang(5, 0, LocalTime.of(7, 0)));
        s2.leggTilAvgang(new Avgang(5, 1, LocalTime.of(7, 30)));
        s3.leggTilAvgang(new Avgang(5, 2, LocalTime.of(8, 0)));

        Rute rute = new Rute(5);
        rute.leggTilStopp(s1);
        rute.leggTilStopp(s2);
        rute.leggTilStopp(s3);

        assertEquals(3, rute.stopp.size());
        assertEquals(1, rute.stopp.get(0).avganger.size());
        assertEquals(1, rute.stopp.get(1).avganger.size());
        assertEquals(1, rute.stopp.get(2).avganger.size());
    }
}
