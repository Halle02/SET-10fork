package com.set10.core.integrasjonstester;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.set10.core.Avgang;
import com.set10.core.Datadepot;
import com.set10.core.Reiseforslag;
import com.set10.core.Reisesok;
import com.set10.core.Rute;
import com.set10.core.Stoppested;
import com.set10.database.DatabaseText;

import java.time.LocalTime;

/**
 * Integrasjonstest: Tester Reisesok og Reiseforslag sammen med ekte data fra Datadepot
 */
public class ReisesokMedDatadepotIT {

    private Datadepot datadepot;

    @BeforeEach
    void setUp() {
        DatabaseText db = new DatabaseText("data\\testdata.txt");
        datadepot = new Datadepot(db);
        datadepot.opprettDummydata(); // Bruker ekte Halden-data
    }

    @Test
    @DisplayName("Reisesok kan opprettes med stoppesteder fra Datadepot")
    void opprettReisesokMedEkteData() {
        Stoppested haldenTerminal = datadepot.hentStoppested(0); // "Halden bussterminal"
        Stoppested haldenStasjon = datadepot.hentStoppested(13); // "Halden stasjon"
        
        Reisesok sok = new Reisesok(haldenTerminal, haldenStasjon);
        
        assertNotNull(sok);
        assertEquals("Halden bussterminal", sok.getFraStoppested().navn);
        assertEquals("Halden stasjon", sok.getTilStoppested().navn);
    }

    @Test
    @DisplayName("Reiseforslag med ekte avganger fra Datadepot")
    void reiseforslagMedEkteAvganger() {
        Stoppested fra = datadepot.hentStoppested(0); // Halden bussterminal
        Stoppested til = datadepot.hentStoppested(9); // Skofabrikken
        
        Reisesok sok = new Reisesok(fra, til);
        
        // Finn en avgang som går fra terminalen
        Avgang avgang = fra.avganger.get(0); // Første avgang fra terminalen
        
        Reiseforslag forslag = new Reiseforslag(avgang, sok);
        forslag.settAnkomstTid(LocalTime.of(8, 20));
        
        assertNotNull(forslag);
        assertEquals(1, forslag.avganger.size());
        assertEquals(fra.id, avgang.stoppestedID);
    }

    @Test
    @DisplayName("Rute 33 går gjennom flere stoppesteder fra Datadepot")
    void rute33HarFlereStoppFraDatadepot() {
        Rute r33 = datadepot.hentRute(33);
        
        assertNotNull(r33);
        assertTrue(r33.stopp.size() >= 3, "Rute 33 skal ha flere stoppesteder");
        
        // Sjekk at Halden bussterminal er på rute 33
        boolean harBussterminal = r33.stopp.stream()
            .anyMatch(s -> s.navn.equals("Halden bussterminal"));
        assertTrue(harBussterminal, "Rute 33 skal ha Halden bussterminal");
    }

    @Test
    @DisplayName("Stoppested har avganger som refererer til riktig rute")
    void stoppestedAvgangerReferererTilRiktigRute() {
        Stoppested haldenTerminal = datadepot.hentStoppested(0);
        
        assertFalse(haldenTerminal.avganger.isEmpty(), "Halden bussterminal skal ha avganger");
        
        // Sjekk at avganger har gyldig ruteID
        for (Avgang avg : haldenTerminal.avganger) {
            Rute rute = datadepot.hentRute(avg.ruteID);
            assertNotNull(rute, "Avgang skal referere til en gyldig rute");
            assertTrue(avg.ruteID == 33 || avg.ruteID == 34 || avg.ruteID == 35, 
                      "Avgang skal være på rute 33, 34 eller 35");
        }
    }

    @Test
    @DisplayName("Reiseforslag kan legge til rute fra Datadepot")
    void reiseforslagMedRuteFraDatadepot() {
        Stoppested fra = datadepot.hentStoppested(0);
        Stoppested til = datadepot.hentStoppested(3);
        Rute r34 = datadepot.hentRute(34);
        
        Reisesok sok = new Reisesok(fra, til);
        Avgang avgang = new Avgang(34, 0, LocalTime.of(8, 0));
        
        Reiseforslag forslag = new Reiseforslag(avgang, sok);
        forslag.leggTilRute(r34);
        
        assertEquals(1, forslag.hentRuter().size());
        assertEquals(34, forslag.hentRuter().get(0).id);
    }
}
