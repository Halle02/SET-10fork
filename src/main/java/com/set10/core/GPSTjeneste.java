package com.set10.core;
public abstract class GPSTjeneste {

    /**
     * Beregner avstanden mellom to stoppesteder ved bruk av Haversine-formelen
     * @return avstanden i kilometer
     */
    public static double avstandMellomStopp(Stoppested nåværendeStopp, Stoppested nesteStopp) {
        if (nåværendeStopp == null || nesteStopp == null) return 0.0;
        
        double lat1 = nåværendeStopp.getLatitude();
        double lon1 = nåværendeStopp.getLongitude();
        double lat2 = nesteStopp.getLatitude();
        double lon2 = nesteStopp.getLongitude();
        
        return beregnAvstand(lat1, lon1, lat2, lon2);
    }
    
    /**
     * Haversine-formel for å beregne avstand mellom to GPS-koordinater
     * @return avstanden i kilometer
     */
    public static double beregnAvstand(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Konverterer GPS-koordinater til skjerm-koordinater for kart-visning
     * @return 
     */
    public static float[] convertGPSToScreen(double lat, double lon, double centerLat, double centerLon, 
                                            float zoom, float screenWidth, float screenHeight) {
        // Enkel projeksjonsformel - virker greit for små områder som Østfold
        double latDiff = (lat - centerLat) * zoom * 10000;
        double lonDiff = (lon - centerLon) * zoom * 10000 * Math.cos(Math.toRadians(centerLat));
        
        float x = (float)(screenWidth / 2 + lonDiff);
        float y = (float)(screenHeight / 2 - latDiff); // 
        
        return new float[]{x, y};
    }
    
    /**
     * Returnerer farge for en gitt rute-ID
     * @return float array med [r, g, b] verdier (0.0-1.0)
     */
    public static float[] getRouteColor(int ruteID) {
        // Forhåndsdefinerte farger for de tre rutene (33, 34, 35)
        float[][] colors = {
            {0.2f, 0.5f, 1.0f},   // Rute 33: Blå
            {0.2f, 0.8f, 0.3f},   // Rute 34: Grønn
            {1.0f, 0.6f, 0.0f},   // Rute 35: Oransje
        };
        
        // Bruk modulo for å resirkulere farger hvis det er flere ruter
        int colorIndex = ruteID % colors.length;
        return colors[colorIndex];
    }
}