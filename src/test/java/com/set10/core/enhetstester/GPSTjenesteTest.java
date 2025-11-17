package com.set10.core.enhetstester;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.set10.core.GPSTjeneste;
import com.set10.core.Stoppested;

public class GPSTjenesteTest {

    @Test
    @DisplayName("beregnAvstand() returnerer 0 for samme koordinater")
    void avstandMellomSammePunkt() {
        double avstand = GPSTjeneste.beregnAvstand(59.124, 11.387, 59.124, 11.387);
        assertEquals(0.0, avstand, 0.001);
    }

    @Test
    @DisplayName("beregnAvstand() beregner riktig avstand mellom Oslo og Bergen (ca 300 km)")
    void avstandOsloTilBergen() {
        double osloLat = 59.9139, osloLon = 10.7522;
        double bergenLat = 60.3913, bergenLon = 5.3221;
        
        double avstand = GPSTjeneste.beregnAvstand(osloLat, osloLon, bergenLat, bergenLon);
        
        // Oslo-Bergen er ca 300-310 km i luftlinje
        assertTrue(avstand > 290 && avstand < 320, "Avstand skulle være rundt 300 km, men var: " + avstand);
    }

    @Test
    @DisplayName("avstandMellomStopp() beregner avstand mellom to stoppesteder")
    void avstandMellomStoppesteder() {
        Stoppested haldenStasjon = new Stoppested(0, "Halden stasjon", 59.1240, 11.3871);
        Stoppested rikshospitalet = new Stoppested(1, "Rikshospitalet", 59.1290, 11.3950);
        
        double avstand = GPSTjeneste.avstandMellomStopp(haldenStasjon, rikshospitalet);
        
        // Kort avstand innenfor Halden, bør være under 1 km
        assertTrue(avstand > 0 && avstand < 1.0, "Avstand skulle være under 1 km, men var: " + avstand);
    }

    @Test
    @DisplayName("avstandMellomStopp() returnerer 0 for null-verdier")
    void avstandMedNullStopp() {
        Stoppested stopp = new Stoppested(0, "Test", 59.124, 11.387);
        
        assertEquals(0.0, GPSTjeneste.avstandMellomStopp(null, stopp));
        assertEquals(0.0, GPSTjeneste.avstandMellomStopp(stopp, null));
        assertEquals(0.0, GPSTjeneste.avstandMellomStopp(null, null));
    }

    @Test
    @DisplayName("convertGPSToScreen() konverterer GPS til skjermkoordinater")
    void gpsKonverteringTilSkjerm() {
        double centerLat = 59.124, centerLon = 11.387;
        float zoom = 1.0f;
        float screenWidth = 800f, screenHeight = 600f;
        
        
        float[] center = GPSTjeneste.convertGPSToScreen(centerLat, centerLon, centerLat, centerLon, zoom, screenWidth, screenHeight);
        assertEquals(screenWidth / 2, center[0], 1.0, "X skulle være midt på skjermen");
        assertEquals(screenHeight / 2, center[1], 1.0, "Y skulle være midt på skjermen");
        
        
        float[] north = GPSTjeneste.convertGPSToScreen(centerLat + 0.01, centerLon, centerLat, centerLon, zoom, screenWidth, screenHeight);
        assertTrue(north[1] < screenHeight / 2, "Nord skulle ha lavere Y-verdi");
        
        
        float[] east = GPSTjeneste.convertGPSToScreen(centerLat, centerLon + 0.01, centerLat, centerLon, zoom, screenWidth, screenHeight);
        assertTrue(east[0] > screenWidth / 2, "Øst skulle ha høyere X-verdi");
    }

    @Test
    @DisplayName("getRouteColor() returnerer forskjellige farger for forskjellige ruter")
    void ruteFargerErUlike() {
        float[] color0 = GPSTjeneste.getRouteColor(0);
        float[] color1 = GPSTjeneste.getRouteColor(1);
        float[] color2 = GPSTjeneste.getRouteColor(2);
        
        // Sjekk at fargene er forskjellige
        assertFalse(color0[0] == color1[0] && color0[1] == color1[1] && color0[2] == color1[2]);
        assertFalse(color1[0] == color2[0] && color1[1] == color2[1] && color1[2] == color2[2]);
        
        // Sjekk at verdiene er innenfor gyldig RGB-område (0.0-1.0)
        for (float c : color0) {
            assertTrue(c >= 0.0f && c <= 1.0f, "RGB-verdi må være mellom 0 og 1");
        }
    }

    @Test
    @DisplayName("getRouteColor() resirkulerer farger med modulo")
    void ruteFargerResirkulerer() {
        float[] color0 = GPSTjeneste.getRouteColor(0);
        float[] color8 = GPSTjeneste.getRouteColor(8); // 8 % 8 = 0
        
        // Farge 0 og farge 8 skal være identiske (modulo 8 farger)
        assertArrayEquals(color0, color8, "Farger skal resirkulere med modulo");
    }
}
