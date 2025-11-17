package com.set10.core.enhetstester;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.set10.core.Billett;
import com.set10.core.Bruker;

public class BrukerPassordOgGruppeTest {

    @Test
    @DisplayName("Aldersbasert gruppe: barn/ungdom/voksen/honnor")
    void aldersbasertGruppe() {
        Bruker barn = new Bruker("Barn", LocalDate.now().minusYears(10));
        Bruker ungdom = new Bruker("Ungdom", LocalDate.now().minusYears(15));
        Bruker voksen = new Bruker("Voksen", LocalDate.now().minusYears(30));
        Bruker senior = new Bruker("Senior", LocalDate.now().minusYears(70));

        assertEquals(Bruker.BrukerGruppe.barn, barn.beregnAldersbasertGruppe());
        assertEquals(Bruker.BrukerGruppe.ungdom, ungdom.beregnAldersbasertGruppe());
        assertEquals(Bruker.BrukerGruppe.voksen, voksen.beregnAldersbasertGruppe());
        assertEquals(Bruker.BrukerGruppe.honnor, senior.beregnAldersbasertGruppe());

        // Auto-oppdatering
        voksen.oppdaterBrukerGruppeAuto();
        assertEquals(Bruker.BrukerGruppe.voksen, voksen.brukerGruppe);
    }

    @Test
    @DisplayName("Passord hashing og validering")
    void passordHashValidering() {
        Bruker bruker = new Bruker("Test", LocalDate.now().minusYears(25), "1234");
        assertNotNull(bruker.passordHash, "Hash skal settes ved konstruksjon med passord");
        assertTrue(bruker.validerPassord("1234"), "Korrekt passord skal godkjennes");
        assertFalse(bruker.validerPassord("feil"), "Feil passord skal avvises");

        String hash1 = Bruker.hashPassord("hemmelig");
        String hash2 = Bruker.hashPassord("hemmelig");
        assertNotNull(hash1);
        assertEquals(hash1, hash2, "Hashing skal være deterministisk for samme input");
    }

    @Test
    @DisplayName("Kjøp billett og flytt utgåtte billetter")
    void kjopOgFlyttBilletter() {
        Bruker bruker = new Bruker("Kjøper", LocalDate.now().minusYears(30));

        // Kjøp gyldig billett
        LocalDateTime startGyldig = LocalDateTime.now().minusMinutes(10);
        bruker.kjopBillett(Billett.Type.Enkel, startGyldig);
        assertEquals(1, bruker.aktiveBilletter.size());

        // Kjøp utgått billett (start for langt tilbake)
        LocalDateTime startUtdatert = LocalDateTime.now().minusDays(2);
        bruker.kjopBillett(Billett.Type.Enkel, startUtdatert);
        assertEquals(2, bruker.aktiveBilletter.size());

        // Oppdater statuser: den ene skal flyttes til gamleBilletter
        bruker.opdaterBillettStatus();
        assertEquals(1, bruker.aktiveBilletter.size(), "Kun gyldig billett skal være aktiv");
        assertEquals(1, bruker.gamleBilletter.size(), "Utgått billett skal flyttes til gamleBilletter");
    }
}
