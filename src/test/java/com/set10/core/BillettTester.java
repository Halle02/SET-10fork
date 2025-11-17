package com.set10.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BillettTester {

    @Test
    @DisplayName("Enkel ticket should expire after 90 minutes")
    void enkelTicketDurationIs90Minutes() {
        LocalDateTime start = LocalDateTime.of(2025, 11, 12, 10, 0);
        Billett b = new Billett(Billett.Type.Enkel, start);
        assertEquals(start.plusMinutes(90), b.sluttTid);
    }

    @Test
    @DisplayName("Periode ticket should expire after 30 days")
    void periodeTicketDurationIs30Days() {
        LocalDateTime start = LocalDateTime.of(2025, 11, 12, 10, 0);
        Billett b = new Billett(Billett.Type.Periode, start);
        assertEquals(start.plusDays(30), b.sluttTid);
    }
}
