package com.set10.core.enhetstester;

import static org.junit.jupiter.api.Assertions.*;

import com.set10.core.Validering;
import com.set10.core.Billett;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ValidieringTester {

    @Test
    @DisplayName("erBillettGyldigTid bør returnere true når nåværende tid er innenfor gyldighetsperioden")
    void billettGyldigInnerUtvaliditetsperiode() {
        LocalDateTime startTid = LocalDateTime.now().minusHours(1);
        LocalDateTime sluttTid = LocalDateTime.now().plusHours(2);
        Billett billett = new Billett(Billett.Type.Enkel, startTid);
        billett.sluttTid = sluttTid;
        assertTrue(Validering.erBillettGyldigTid(billett), 
                   "Billett bør være gyldig innenfor gyldighetsperioden");
    }

    @Test
    @DisplayName("erBillettGyldigTid bør returnere false når nåværende tid er før starttidspunktet")
    void billettIkkeGyldigForStarttid() {
        LocalDateTime startTid = LocalDateTime.now().plusHours(1);
        Billett billett = new Billett(Billett.Type.Periode, startTid);
        assertFalse(Validering.erBillettGyldigTid(billett), 
                    "Billett bør ikke være gyldig før starttidspunktet");
    }

    @Test
    @DisplayName("erBillettGyldigTid bør returnere false når nåværende tid er etter slutttidspunktet")
    void billettIkkeGyldigEtterSlutttid() {
        LocalDateTime startTid = LocalDateTime.now().minusHours(3);
        LocalDateTime sluttTid = LocalDateTime.now().minusHours(1);
        Billett billett = new Billett(Billett.Type.Enkel, startTid);
        billett.sluttTid = sluttTid;
        assertFalse(Validering.erBillettGyldigTid(billett), 
                    "Billett er ikke gyldig når nåværende tid er etter slutttidspunktet");
    }

}
