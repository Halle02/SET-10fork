package com.set10.core.enhetstester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.set10.core.Avgang;
import com.set10.core.Datadepot;
import com.set10.core.Stoppested;
import com.set10.database.DatabaseText;

public class StoppestedOgAvgang {
    @Test
    @DisplayName("Opprett Stoppested")
    void testOpprettStoppested() {
        Datadepot depot = new Datadepot(new DatabaseText("ignore.txt"));

        int id1 = depot.opprettStoppested(new Stoppested("A"));
        int id2 = depot.opprettStoppested(new Stoppested("B"));

        assertNotEquals(id1, id2);
        assertEquals(2, depot.stoppestedCache.size());
    }
       
    
    @Test
    @DisplayName("Legg til avganger")
    void testLeggTilAvgang() {
        Stoppested stopp = new Stoppested(0, "Halden terminal");

        Avgang avgang = new Avgang(1, 33, 0, LocalTime.of(12, 30));
        stopp.leggTilAvgang(avgang);

        assertEquals(1, stopp.hentAvganger().size());
        assertEquals(avgang, stopp.hentAvganger().get(0));
    }
}
