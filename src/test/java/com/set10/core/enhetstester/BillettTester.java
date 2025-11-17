package com.set10.core.enhetstester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.set10.core.Billett;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BillettTester {

    @Test
    @DisplayName("Enkel billett skal være gyldig i 90 minutter")
    void enkelTicketDurationIs90Minutes() {
        LocalDateTime start = LocalDateTime.of(2025, 11, 12, 10, 0);
        Billett b = new Billett(Billett.Type.Enkel, start);
        assertEquals(start.plusMinutes(90), b.sluttTid);
    }

    @Test
    @DisplayName("Periodebillett skal være gyldig i 30 dager")
    void periodeTicketDurationIs30Days() {
        LocalDateTime start = LocalDateTime.of(2025, 11, 12, 10, 0);
        Billett b = new Billett(Billett.Type.Periode, start);
        assertEquals(start.plusDays(30), b.sluttTid);
    }
}
