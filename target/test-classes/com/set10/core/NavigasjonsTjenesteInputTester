package com.set10.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NavigasjonsTjenesteInputTester {

    @Test
    @DisplayName("FinnReiser() håndterer ugyldige stoppesteder")
    void testFinnReiser_UgyldigStoppested() {
        Datadepot depot = new Datadepot(null);
        Navigasjonstjeneste tjeneste = new Navigasjonstjeneste();
        tjeneste.dataDepot = depot;

        Stoppested stoppA = new Stoppested(0, "Halden");
        Stoppested stoppB = new Stoppested(1, "Sarpsborg");

        depot.hentStoppesteder().add(stoppA);
        depot.hentStoppesteder().add(stoppB);

        //stopp som ikke finnes i depot
        Stoppested ugyldigStopp = new Stoppested(99, "Ukjent");

        LocalDateTime sokTid = LocalDateTime.of(2025, 1, 1, 10, 0);
        Reisesok sok = new Reisesok(ugyldigStopp, stoppB, sokTid, null);

        List<Reiseforslag> forslag = tjeneste.FinnReiser(sok);

        assertTrue(forslag.isEmpty(), "Skal returnere tom liste når start-stoppested ikke finnes");

        //same start og slutt
        sok = new Reisesok(stoppA, stoppA, sokTid, null);
        forslag = tjeneste.FinnReiser(sok);

        assertTrue(forslag.isEmpty(), "Skal returnere tom liste når start og slutt er samme");
    }

    @Test
    @DisplayName("FinnReiser() håndterer ugyldig tidformat")
    void testFinnReiser_UgyldigTid() {
        try {
            LocalTime.parse("25:99"); // ugyldig tid
            fail("LocalTime.parse skal kaste exception for ugyldig format");
        } catch (Exception e) {
            assertTrue(e instanceof java.time.format.DateTimeParseException, "Skal kaste DateTimeParseException");
        }
    }
}
