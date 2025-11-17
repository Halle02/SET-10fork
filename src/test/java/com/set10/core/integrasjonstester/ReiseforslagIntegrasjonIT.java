package com.set10.core.integrasjonstester;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.set10.core.Avgang;
import com.set10.core.Reiseforslag;
import com.set10.core.Reisesok;
import com.set10.core.Stoppested;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Integrasjonstest: Tester hvordan Reiseforslag fungerer med Reisesok, Avgang og Stoppested.
 */
public class ReiseforslagIntegrasjonIT {

    @Test
    @DisplayName("Reiseforslag kan opprettes med første avgang")
    void reiseforslagOpprettelse() {
        Stoppested fra = new Stoppested(0, "Oslo");
        Stoppested til = new Stoppested(1, "Bergen");
        
        Reisesok sok = new Reisesok(fra, til);
        Avgang forsteAvgang = new Avgang(1, 0, LocalTime.of(8, 0));
        
        Reiseforslag forslag = new Reiseforslag(forsteAvgang, sok);

        assertNotNull(forslag);
        assertEquals(1, forslag.avganger.size());
        assertEquals(forsteAvgang, forslag.avganger.get(0));
        assertEquals(sok, forslag.sok);
    }

    @Test
    @DisplayName("Reiseforslag med direktereise (én avgang)")
    void direktereise() {
        Stoppested fra = new Stoppested(0, "Halden");
        Stoppested til = new Stoppested(1, "Sarpsborg");
        
        Reisesok sok = new Reisesok(fra, til);
        Avgang avgang = new Avgang(33, 0, LocalTime.of(10, 30));
        
        Reiseforslag forslag = new Reiseforslag(avgang, sok);
        forslag.settAnkomstTid(LocalTime.of(11, 0));

        assertEquals(1, forslag.avganger.size());
        assertEquals(0, forslag.bytteStopp.size());
        assertEquals(LocalTime.of(11, 0), forslag.ankomstTid);
        
        String beskrivelse = forslag.toString();
        assertTrue(beskrivelse.contains("Direkte reise"));
        assertTrue(beskrivelse.contains("Halden"));
        assertTrue(beskrivelse.contains("Sarpsborg"));
    }

    @Test
    @DisplayName("Reiseforslag med ett bytte")
    void reiseMedBytte() {
        Stoppested fra = new Stoppested(0, "Oslo");
        Stoppested bytte = new Stoppested(1, "Drammen");
        Stoppested til = new Stoppested(2, "Bergen");
        
        Reisesok sok = new Reisesok(fra, til);
        Avgang forsteAvgang = new Avgang(1, 0, LocalTime.of(8, 0));
        Avgang andreAvgang = new Avgang(2, 1, LocalTime.of(9, 0));
        
        Reiseforslag forslag = new Reiseforslag(forsteAvgang, sok);
        forslag.leggTilAvgang(andreAvgang);
        forslag.leggTilBytteStopp(bytte);
        forslag.settAnkomstTid(LocalTime.of(14, 0));

        assertEquals(2, forslag.avganger.size());
        assertEquals(1, forslag.bytteStopp.size());
        assertEquals("Drammen", forslag.bytteStopp.get(0).navn);
        assertEquals(LocalTime.of(14, 0), forslag.ankomstTid);
    }

    @Test
    @DisplayName("Reiseforslag kan legge til flere avganger og byttestopp")
    void flereAvgangerOgBytte() {
        Stoppested fra = new Stoppested("Start");
        Stoppested til = new Stoppested("Slutt");
        Stoppested bytte1 = new Stoppested("Bytte1");
        Stoppested bytte2 = new Stoppested("Bytte2");
        
        Reisesok sok = new Reisesok(fra, til);
        Avgang a1 = new Avgang(1, 0, LocalTime.of(8, 0));
        Avgang a2 = new Avgang(2, 1, LocalTime.of(9, 0));
        Avgang a3 = new Avgang(3, 2, LocalTime.of(10, 0));
        
        Reiseforslag forslag = new Reiseforslag(a1, sok);
        forslag.leggTilAvgang(a2);
        forslag.leggTilAvgang(a3);
        forslag.leggTilBytteStopp(bytte1);
        forslag.leggTilBytteStopp(bytte2);
        forslag.settAnkomstTid(LocalTime.of(11, 30));

        assertEquals(3, forslag.avganger.size());
        assertEquals(2, forslag.bytteStopp.size());
        assertEquals("Bytte1", forslag.bytteStopp.get(0).navn);
        assertEquals("Bytte2", forslag.bytteStopp.get(1).navn);
    }

    @Test
    @DisplayName("Reisesok holder på fra- og til-stoppested korrekt")
    void reisesokStoppesteder() {
        Stoppested fra = new Stoppested(10, "Fredrikstad");
        Stoppested til = new Stoppested(20, "Oslo");
        LocalDateTime avreise = LocalDateTime.of(2025, 1, 15, 8, 0);
        LocalDateTime ankomst = LocalDateTime.of(2025, 1, 15, 9, 30);
        
        Reisesok sok = new Reisesok(fra, til, avreise, ankomst);

        assertEquals(fra, sok.getFraStoppested());
        assertEquals(til, sok.getTilStoppested());
        assertEquals("Fredrikstad", sok.fraStoppested.navn);
        assertEquals("Oslo", sok.tilStoppested.navn);
        assertEquals(avreise, sok.avreiseTid);
        assertEquals(ankomst, sok.ankomstTid);
    }
}
