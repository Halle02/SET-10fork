package com.set10.core.enhetstester;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.set10.core.Billett;
import com.set10.core.Bruker;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BrukerTester {

    Bruker bruker;
    Bruker annenBruker;

    @BeforeEach
    void setUp() {
        bruker = new Bruker(1, "Ola Nordmann", LocalDate.of(2000, 1, 1));
        annenBruker = new Bruker(2, "Kari Nordmann", LocalDate.of(2010, 5, 10));
    }

    @Test
    @DisplayName("Opprettelse av bruker setter riktig verdier")
    void testBrukerOpprettelse() {
        assertEquals(1, bruker.id);
        assertEquals("Ola Nordmann", bruker.navn);
        assertEquals(LocalDate.of(2000, 1, 1), bruker.fodselsDato);
        assertEquals(Bruker.BrukerGruppe.auto, bruker.brukerGruppe);
        assertTrue(bruker.aktiveBilletter.isEmpty());
        assertTrue(bruker.gamleBiletter.isEmpty());
    }

    @Test
    @DisplayName("finnAlder() returnerer korrekt alder basert på fødselsdato")
    void testFinnAlder() {
        int forventetAlder = LocalDate.now().getYear() - 2000;
        assertEquals(forventetAlder, bruker.finnAlder());
    }

    @Test
    @DisplayName("kjopBillett() legger til billett i aktiveBilletter")
    void testKjopBillett() {
        LocalDateTime startTid = LocalDateTime.now();
        bruker.kjopBillett(Billett.Type.Enkel, startTid);

        assertEquals(1, bruker.aktiveBilletter.size());
        assertEquals(Billett.Type.Enkel, bruker.aktiveBilletter.get(0).type);
    }

    @Test
    @DisplayName("kjopBillettTilAnnenBruker() legger til billett hos mottaker")
    void testKjopBillettTilAnnenBruker() {
        LocalDateTime startTid = LocalDateTime.now();
        bruker.kjopBillettTilAnnenBruker(Billett.Type.Periode, startTid, annenBruker);

        assertTrue(bruker.aktiveBilletter.isEmpty());
        assertEquals(1, annenBruker.aktiveBilletter.size());
        assertEquals(Billett.Type.Periode, annenBruker.aktiveBilletter.get(0).type);
    }

}
