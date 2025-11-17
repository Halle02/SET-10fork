package com.set10.core.integrasjonstester;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.set10.core.Billett;
import com.set10.core.Bruker;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Integrasjonstest: Tester hvordan Bruker og Billett fungerer sammen.
 */
public class BrukerOgBillettIntegrasjonIT {

    @Test
    @DisplayName("Bruker kan kjøpe billett og den legges til aktiveBilletter")
    void brukerKjoperBillett() {
        Bruker bruker = new Bruker("Ola", LocalDate.of(1990, 5, 15));
        LocalDateTime now = LocalDateTime.now();

        assertEquals(0, bruker.aktiveBilletter.size());

        bruker.kjopBillett(Billett.Type.Enkel, now);

        assertEquals(1, bruker.aktiveBilletter.size());
        assertEquals(Billett.Type.Enkel, bruker.aktiveBilletter.get(0).type);
    }

    @Test
    @DisplayName("Bruker kan kjøpe billett til annen bruker")
    void brukerKjoperBillettTilAnnen() {
        Bruker ola = new Bruker("Ola", LocalDate.of(1990, 1, 1));
        Bruker kari = new Bruker("Kari", LocalDate.of(1995, 3, 10));
        LocalDateTime start = LocalDateTime.now();

        assertEquals(0, kari.aktiveBilletter.size());

        ola.kjopBillettTilAnnenBruker(Billett.Type.Periode, start, kari);

        assertEquals(0, ola.aktiveBilletter.size(), "Ola skal ikke ha billett");
        assertEquals(1, kari.aktiveBilletter.size(), "Kari skal ha mottatt billetten");
        assertEquals(Billett.Type.Periode, kari.aktiveBilletter.get(0).type);
    }

    @Test
    @DisplayName("Utgåtte billetter flyttes til gamleBilletter ved opdatering")
    void utgaatteBilletterFlyttes() {
        Bruker bruker = new Bruker("Test", LocalDate.of(1985, 6, 20));
        
        // Legg til en utgått billett (startet for 2 dager siden)
        LocalDateTime gammel = LocalDateTime.now().minusDays(2);
        bruker.kjopBillett(Billett.Type.Enkel, gammel);

        // Legg til en gyldig billett
        LocalDateTime gyldig = LocalDateTime.now().minusMinutes(10);
        bruker.kjopBillett(Billett.Type.Enkel, gyldig);

        assertEquals(2, bruker.aktiveBilletter.size());
        assertEquals(0, bruker.gamleBilletter.size());

        // Oppdater status
        bruker.opdaterBillettStatus();

        assertEquals(1, bruker.aktiveBilletter.size(), "Kun gyldig billett skal være aktiv");
        assertEquals(1, bruker.gamleBilletter.size(), "Utgått billett skal være flyttet");
    }

    @Test
    @DisplayName("Bruker med aldersbasert gruppe og billettkjøp")
    void brukerGruppeOgBillett() {
        Bruker barn = new Bruker("Barn", LocalDate.now().minusYears(10));
        barn.oppdaterBrukerGruppeAuto();

        assertEquals(Bruker.BrukerGruppe.barn, barn.brukerGruppe);

        barn.kjopBillett(Billett.Type.Enkel, LocalDateTime.now());

        assertEquals(1, barn.aktiveBilletter.size());
        assertEquals(Bruker.BrukerGruppe.barn, barn.brukerGruppe);
    }

    @Test
    @DisplayName("Flere brukere med billetter samspiller korrekt")
    void flereBrukereOgBilletter() {
        Bruker b1 = new Bruker("Bruker1", LocalDate.of(1980, 1, 1));
        Bruker b2 = new Bruker("Bruker2", LocalDate.of(1990, 2, 2));
        
        b1.kjopBillett(Billett.Type.Enkel, LocalDateTime.now());
        b1.kjopBillett(Billett.Type.Periode, LocalDateTime.now());
        b2.kjopBillett(Billett.Type.Enkel, LocalDateTime.now());

        assertEquals(2, b1.aktiveBilletter.size());
        assertEquals(1, b2.aktiveBilletter.size());
        assertNotSame(b1.aktiveBilletter.get(0), b2.aktiveBilletter.get(0));
    }
}
