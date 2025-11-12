package com.set10.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BrukerTester {

    @Test
    @DisplayName("kjopBillett should add an active ticket to the buyer")
    void kjopBillettAddsActiveTicket() {
        Bruker b = new Bruker("Alice", LocalDate.of(1990, 1, 1));
        assertEquals(0, b.aktiveBilletter.size());

        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 12, 0);
        b.kjopBillett(Billett.Type.Enkel, start);

        assertEquals(1, b.aktiveBilletter.size());
        Billett bil = b.aktiveBilletter.get(0);
        assertEquals(Billett.Type.Enkel, bil.type);
        assertEquals(start, bil.startTid);
    }

    @Test
    @DisplayName("kjopBillettTilAnnenBruker should add a ticket to the receiver")
    void kjopBillettTilAnnenBrukerAddsToReceiver() {
        Bruker alice = new Bruker("Alice", LocalDate.of(1990, 1, 1));
        Bruker bob = new Bruker("Bob", LocalDate.of(1990, 1, 1));

        LocalDateTime start = LocalDateTime.of(2025, 1, 2, 8, 0);
        alice.kjopBillettTilAnnenBruker(Billett.Type.Periode, start, bob);

        assertEquals(0, alice.aktiveBilletter.size(), "buyer should not have the ticket in their active list");
        assertEquals(1, bob.aktiveBilletter.size(), "receiver should have one active ticket");
        assertEquals(Billett.Type.Periode, bob.aktiveBilletter.get(0).type);
        assertEquals(start, bob.aktiveBilletter.get(0).startTid);
    }
}
