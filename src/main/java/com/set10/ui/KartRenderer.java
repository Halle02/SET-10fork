package com.set10.ui;

import com.set10.core.Datadepot;
import com.set10.core.GPSTjeneste;
import com.set10.core.Reiseforslag;
import com.set10.core.Rute;
import com.set10.core.Stoppested;
import imgui.ImGui;
import imgui.type.ImInt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KartRenderer - Ansvarlig for å tegne interaktivt kart med GPS-koordinater
 * 
 * Ingen nedlasting av kart-tiles! Alt tegnes direkte med GPS-koordinater:
 * - Bakgrunn: Enkle farger (vann + land)
 * - Ruter: Fargede linjer mellom stoppesteder
 * - Stoppesteder: Prikker med GPS-posisjon
 * - Busser: Animerte ikoner som beveger seg langs ruter
 * - Pan & Zoom: Mus-interaksjon
 */
public class KartRenderer {
    
    // Datakilde
    private Datadepot datadepot;
    
    // Kart-posisjon og zoom
    private float kartZoom = 1.5f;
    private double kartCenterLat = 59.124;  // Halden sentrum
    private double kartCenterLon = 11.387;
    
    // Pan/drag med mus
    private boolean kartDragging = false;
    private float kartDragStartX = 0;
    private float kartDragStartY = 0;
    private double kartDragStartLat = 0;
    private double kartDragStartLon = 0;
    
    // Buss-animasjon (ID -> posisjon på rute 0.0-N)
    private Map<Integer, Double> bussPosisjoner = new HashMap<>();
    private long lastBusUpdate = System.currentTimeMillis();
    
    // Valgt stoppested (fra main app)
    private ImInt valgtStoppested;
    
    // Reise-visualisering
    private boolean visRutepaKart = false;
    private Reiseforslag valgtReiseforslag = null;
    private int ruteFraStoppestedIndex = -1;
    private int ruteTilStoppestedIndex = -1;
    
    // Bruker GPS-posisjon (oppdateres fra Main_Ui_V2)
    private double brukerLat = 59.124;
    private double brukerLon = 11.387;
    
    // Farger (Østfold Trafikk tema)
    private final float[] COLOR_TEAL = new float[]{0.2f, 0.7f, 0.7f, 1.0f};
    
    public KartRenderer(Datadepot datadepot, ImInt valgtStoppested) {
        this.datadepot = datadepot;
        this.valgtStoppested = valgtStoppested;
    }
    
    /**
     * Tegn hele kartet
     */
    public void tegnKart(float canvasWidth, float canvasHeight) {
        imgui.ImDrawList drawList = ImGui.getWindowDrawList();
        float[] cursorPos = {ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY()};
        
        // 1. Tegn bakgrunn (vann + land)
        tegnBakgrunn(drawList, cursorPos, canvasWidth, canvasHeight);
        
        if (datadepot == null || datadepot.hentStoppesteder() == null) return;
        
        // 2. Håndter mus-interaksjon (zoom & pan)
        handleMusInteraksjon();
        
        // 3. Oppdater buss-animasjoner
        oppdaterBussAnimasjoner();
        
        // 4. Tegn ruter (linjer mellom stopp)
        tegnRuter(drawList, cursorPos, canvasWidth, canvasHeight);
        
        // 5. Tegn stoppesteder (prikker)
        tegnStoppesteder(drawList, cursorPos, canvasWidth, canvasHeight);
        
        // 6. Tegn bruker-posisjon (blå prikk med puls-effekt)
        tegnBrukerPosisjon(drawList, cursorPos, canvasWidth, canvasHeight);
    }
    
    /**
     * Tegn enkel bakgrunn: vann (lys blå) + land (beige)
     */
    private void tegnBakgrunn(imgui.ImDrawList drawList, float[] cursorPos, float canvasWidth, float canvasHeight) {
        // Vann (lys blå)
        int waterColor = ImGui.getColorU32(0.70f, 0.85f, 0.95f, 1.0f);
        drawList.addRectFilled(cursorPos[0], cursorPos[1], 
                               cursorPos[0] + canvasWidth, cursorPos[1] + canvasHeight, waterColor);
        
        // Land (beige) - litt mindre enn hele canvas
        int landColor = ImGui.getColorU32(0.93f, 0.94f, 0.89f, 1.0f);
        float landMargin = 30;
        drawList.addRectFilled(cursorPos[0] + landMargin, cursorPos[1] + landMargin, 
                               cursorPos[0] + canvasWidth - landMargin, 
                               cursorPos[1] + canvasHeight - landMargin, landColor);
    }
    
    /**
     * Håndter mus-interaksjon: zoom med hjul, pan med dra
     */
    private void handleMusInteraksjon() {
        if (!ImGui.isWindowHovered()) return;
        
        // Zoom med musehjul
        float wheel = ImGui.getIO().getMouseWheel();
        if (wheel != 0) {
            kartZoom += wheel * 0.1f;
            kartZoom = Math.max(0.5f, Math.min(3.0f, kartZoom));
        }
        
        // Pan med dra
        if (ImGui.isMouseDown(0)) {
            if (!kartDragging) {
                kartDragging = true;
                kartDragStartX = ImGui.getMousePosX();
                kartDragStartY = ImGui.getMousePosY();
                kartDragStartLat = kartCenterLat;
                kartDragStartLon = kartCenterLon;
            } else {
                float dx = ImGui.getMousePosX() - kartDragStartX;
                float dy = ImGui.getMousePosY() - kartDragStartY;
                
                // Konverter pixel-drag til GPS-koordinater
                kartCenterLon = kartDragStartLon - (dx / (kartZoom * 10000 * Math.cos(Math.toRadians(kartCenterLat))));
                kartCenterLat = kartDragStartLat + (dy / (kartZoom * 10000));
            }
        } else {
            kartDragging = false;
        }
    }
    
    /**
     * Oppdater buss-posisjoner for animasjon
     */
    private void oppdaterBussAnimasjoner() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBusUpdate > 100) {
            for (Rute rute : datadepot.hentRuter()) {
                double pos = bussPosisjoner.getOrDefault(rute.id, 0.0);
                pos += 0.02;
                if (pos > rute.stopp.size() - 1) pos = 0.0;
                bussPosisjoner.put(rute.id, pos);
            }
            lastBusUpdate = currentTime;
        }
    }
    
    /**
     * Tegn alle ruter som fargede linjer mellom stoppesteder
     */
    private void tegnRuter(imgui.ImDrawList drawList, float[] cursorPos, float canvasWidth, float canvasHeight) {
        for (Rute rute : datadepot.hentRuter()) {
            float[] ruteColor = GPSTjeneste.getRouteColor(rute.id);
            boolean isSelectedRoute = visRutepaKart && valgtReiseforslag != null && 
                                     valgtReiseforslag.avganger.get(0).ruteID == rute.id;
            
            float lineWidth = isSelectedRoute ? 8.0f : 4.0f;
            float alpha = isSelectedRoute ? 1.0f : 0.7f;
            int lineColor = ImGui.getColorU32(ruteColor[0], ruteColor[1], ruteColor[2], alpha);
            int shadowColor = ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.2f);
            
            // Tegn linjer mellom påfølgende stopp
            for (int i = 0; i < rute.stopp.size() - 1; i++) {
                Stoppested s1 = rute.stopp.get(i);
                Stoppested s2 = rute.stopp.get(i + 1);
                
                // Hvis valgt rute: tegn bare segment mellom fra/til stopp
                if (isSelectedRoute) {
                    Stoppested fraStopp = datadepot.hentStoppesteder().get(ruteFraStoppestedIndex);
                    Stoppested tilStopp = datadepot.hentStoppesteder().get(ruteTilStoppestedIndex);
                    int fraIndex = rute.stopp.indexOf(fraStopp);
                    int tilIndex = rute.stopp.indexOf(tilStopp);
                    
                    if (i < fraIndex || i >= tilIndex) {
                        continue;  // Hopp over dette segmentet
                    }
                }
                
                // Konverter GPS til skjerm-koordinater
                float[] pos1 = GPSTjeneste.convertGPSToScreen(s1.getLatitude(), s1.getLongitude(),
                                                             kartCenterLat, kartCenterLon, 
                                                             kartZoom, canvasWidth, canvasHeight);
                float[] pos2 = GPSTjeneste.convertGPSToScreen(s2.getLatitude(), s2.getLongitude(),
                                                             kartCenterLat, kartCenterLon, 
                                                             kartZoom, canvasWidth, canvasHeight);
                
                // Tegn skygge først
                drawList.addLine(cursorPos[0] + pos1[0] + 2, cursorPos[1] + pos1[1] + 2,
                               cursorPos[0] + pos2[0] + 2, cursorPos[1] + pos2[1] + 2, 
                               shadowColor, lineWidth + 1);
                
                // Tegn hoved-linje
                drawList.addLine(cursorPos[0] + pos1[0], cursorPos[1] + pos1[1],
                               cursorPos[0] + pos2[0], cursorPos[1] + pos2[1], 
                               lineColor, lineWidth);
                
                // Vis distanse på valgt rute (hvis zoomet inn)
                if (isSelectedRoute && kartZoom > 1.2f) {
                    tegnDistanseLabel(drawList, cursorPos, s1, s2, pos1, pos2);
                }
            }
            
            // Tegn animert buss (hvis zoomet inn)
            if (kartZoom > 1.0f) {
                tegnAnimertBuss(drawList, cursorPos, rute, lineColor, canvasWidth, canvasHeight);
            }
        }
    }
    
    /**
     * Tegn distanse-label mellom to stopp
     */
    private void tegnDistanseLabel(imgui.ImDrawList drawList, float[] cursorPos, 
                                   Stoppested s1, Stoppested s2, float[] pos1, float[] pos2) {
        double distance = GPSTjeneste.avstandMellomStopp(s1, s2);
        String distText = String.format("%.1f km", distance);
        float midX = (pos1[0] + pos2[0]) / 2;
        float midY = (pos1[1] + pos2[1]) / 2;
        float labelW = ImGui.calcTextSize(distText).x + 8;
        float labelH = ImGui.calcTextSize(distText).y + 4;
        
        drawList.addRectFilled(cursorPos[0] + midX - labelW/2, cursorPos[1] + midY - labelH/2,
                              cursorPos[0] + midX + labelW/2, cursorPos[1] + midY + labelH/2,
                              ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.8f), 3.0f);
        drawList.addText(cursorPos[0] + midX - labelW/2 + 4, cursorPos[1] + midY - labelH/2 + 2,
                       ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), distText);
    }
    
    /**
     * Tegn animert buss som beveger seg langs ruten
     */
    private void tegnAnimertBuss(imgui.ImDrawList drawList, float[] cursorPos, Rute rute, 
                                int lineColor, float canvasWidth, float canvasHeight) {
        double busPos = bussPosisjoner.getOrDefault(rute.id, 0.0);
        int segIdx = (int)busPos;
        double progress = busPos - segIdx;
        
        if (segIdx < rute.stopp.size() - 1) {
            Stoppested fra = rute.stopp.get(segIdx);
            Stoppested til = rute.stopp.get(segIdx + 1);
            
            // Interpoler buss-posisjon
            double busLat = fra.getLatitude() + (til.getLatitude() - fra.getLatitude()) * progress;
            double busLon = fra.getLongitude() + (til.getLongitude() - fra.getLongitude()) * progress;
            
            float[] busPos2 = GPSTjeneste.convertGPSToScreen(busLat, busLon,
                                                             kartCenterLat, kartCenterLon,
                                                             kartZoom, canvasWidth, canvasHeight);
            float busX = cursorPos[0] + busPos2[0];
            float busY = cursorPos[1] + busPos2[1];
            float busW = 16 * kartZoom;
            float busH = 24 * kartZoom;
            
            // Skygge
            drawList.addRectFilled(busX - busW/2 + 2, busY - busH/2 + 2,
                                  busX + busW/2 + 2, busY + busH/2 + 2,
                                  ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.3f), 4.0f);
            // Buss-kropp
            drawList.addRectFilled(busX - busW/2, busY - busH/2, busX + busW/2, busY + busH/2,
                                  lineColor, 4.0f);
            drawList.addRect(busX - busW/2, busY - busH/2, busX + busW/2, busY + busH/2,
                           ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 4.0f, 0, 2.0f);
            // Vinduer
            drawList.addRectFilled(busX - busW/2 + 2, busY - busH/2 + 2,
                                  busX + busW/2 - 2, busY - busH/2 + 8,
                                  ImGui.getColorU32(0.9f, 0.95f, 1.0f, 0.9f), 2.0f);
            // Rutenummer
            String rNr = String.valueOf(rute.id);
            float nW = ImGui.calcTextSize(rNr).x;
            drawList.addText(busX - nW/2, busY - 3,
                           ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), rNr);
        }
    }
    
    /**
     * Tegn alle stoppesteder som prikker med hover/select effekter
     */
    private void tegnStoppesteder(imgui.ImDrawList drawList, float[] cursorPos, float canvasWidth, float canvasHeight) {
        List<Stoppested> stoppesteder = datadepot.hentStoppesteder();
        float stopRadius = 8.0f + (kartZoom * 2.0f);
        
        for (int i = 0; i < stoppesteder.size(); i++) {
            Stoppested stopp = stoppesteder.get(i);
            float[] pos = GPSTjeneste.convertGPSToScreen(stopp.getLatitude(), stopp.getLongitude(),
                                                        kartCenterLat, kartCenterLon, 
                                                        kartZoom, canvasWidth, canvasHeight);
            
            float screenX = cursorPos[0] + pos[0];
            float screenY = cursorPos[1] + pos[1];
            
            // Sjekk om mus er over dette stoppet
            float mouseX = ImGui.getMousePosX();
            float mouseY = ImGui.getMousePosY();
            float distToMouse = (float)Math.sqrt((mouseX - screenX) * (mouseX - screenX) + 
                                                (mouseY - screenY) * (mouseY - screenY));
            
            boolean isHovered = distToMouse < stopRadius + 5;
            boolean isSelected = valgtStoppested.get() == i;
            
            // Sjekk om dette er start/slutt for valgt reise
            boolean isStartStop = visRutepaKart && i == ruteFraStoppestedIndex;
            boolean isEndStop = visRutepaKart && i == ruteTilStoppestedIndex;
            
            // Klikk-deteksjon
            if (isHovered && ImGui.isMouseClicked(0)) {
                valgtStoppested.set(i);
                isSelected = true;
            }
            
            // Tegn stoppested-sirkel med spesial-markering for start/slutt
            tegnStoppestedSirkel(drawList, screenX, screenY, stopRadius, stopp, 
                               isStartStop, isEndStop, isSelected, isHovered);
        }
    }
    
    /**
     * Tegn én stoppested-sirkel med styling basert på tilstand
     */
    private void tegnStoppestedSirkel(imgui.ImDrawList drawList, float screenX, float screenY, 
                                     float stopRadius, Stoppested stopp,
                                     boolean isStartStop, boolean isEndStop, 
                                     boolean isSelected, boolean isHovered) {
        // Skygge for alle stopp
        int shadowColor = ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.3f);
        drawList.addCircleFilled(screenX + 2, screenY + 2, stopRadius + 2, shadowColor, 12);
        
        if (isStartStop) {
            // START: Grønn sirkel med label
            tegnStartStopp(drawList, screenX, screenY, stopRadius, stopp.navn);
        } else if (isEndStop) {
            // SLUTT: Rød sirkel med label
            tegnSluttStopp(drawList, screenX, screenY, stopRadius, stopp.navn);
        } else if (isSelected) {
            // Valgt: Teal sirkel med hvit kant
            tegnValgtStopp(drawList, screenX, screenY, stopRadius, stopp.navn);
        } else if (isHovered) {
            // Hovered: Lys teal med navn
            tegnHoveredStopp(drawList, screenX, screenY, stopRadius, stopp.navn);
        } else {
            // Normal: Blå prikk
            tegnNormaltStopp(drawList, screenX, screenY, stopRadius, stopp.navn);
        }
    }
    
    private void tegnStartStopp(imgui.ImDrawList drawList, float x, float y, float r, String navn) {
        int startColor = ImGui.getColorU32(0.2f, 0.8f, 0.3f, 1.0f);
        int outline = ImGui.getColorU32(0.15f, 0.6f, 0.2f, 1.0f);
        drawList.addCircleFilled(x, y, r + 8, startColor, 16);
        drawList.addCircle(x, y, r + 8, outline, 16, 3.0f);
        drawList.addCircle(x, y, r + 10, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 16, 2.0f);
        
        tegnLabel(drawList, x, y, r, "START: " + navn, startColor);
    }
    
    private void tegnSluttStopp(imgui.ImDrawList drawList, float x, float y, float r, String navn) {
        int endColor = ImGui.getColorU32(0.9f, 0.2f, 0.2f, 1.0f);
        int outline = ImGui.getColorU32(0.7f, 0.1f, 0.1f, 1.0f);
        drawList.addCircleFilled(x, y, r + 8, endColor, 16);
        drawList.addCircle(x, y, r + 8, outline, 16, 3.0f);
        drawList.addCircle(x, y, r + 10, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 16, 2.0f);
        
        tegnLabel(drawList, x, y, r, "SLUTT: " + navn, endColor);
    }
    
    private void tegnValgtStopp(imgui.ImDrawList drawList, float x, float y, float r, String navn) {
        int selectedColor = ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        drawList.addCircleFilled(x, y, r + 5, selectedColor, 16);
        drawList.addCircle(x, y, r + 5, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 16, 3.0f);
        drawList.addCircleFilled(x, y, r - 2, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 12);
        
        tegnLabel(drawList, x, y, r, navn, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.98f));
    }
    
    private void tegnHoveredStopp(imgui.ImDrawList drawList, float x, float y, float r, String navn) {
        int hoverColor = ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.8f);
        drawList.addCircleFilled(x, y, r + 2, hoverColor, 12);
        
        float labelX = x + r + 5;
        float labelY = y - 8;
        float labelWidth = ImGui.calcTextSize(navn).x + 10;
        float labelHeight = ImGui.calcTextSize(navn).y + 6;
        
        drawList.addRectFilled(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                              ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.95f));
        drawList.addRect(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                        ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f), 4.0f, 0, 2.0f);
        drawList.addText(labelX + 6, labelY + 4, 
                       ImGui.getColorU32(0.2f, 0.2f, 0.2f, 1.0f), navn);
    }
    
    private void tegnNormaltStopp(imgui.ImDrawList drawList, float x, float y, float r, String navn) {
        // Hvit kant
        drawList.addCircleFilled(x, y, r + 1, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 12);
        // Blå fylling
        drawList.addCircleFilled(x, y, r, ImGui.getColorU32(0.12f, 0.56f, 1.0f, 1.0f), 12);
    }
    
    private void tegnLabel(imgui.ImDrawList drawList, float x, float y, float r, String label, int bgColor) {
        float labelX = x + r + 12;
        float labelY = y - 12;
        float labelWidth = ImGui.calcTextSize(label).x + 12;
        float labelHeight = ImGui.calcTextSize(label).y + 8;
        
        // Skygge
        drawList.addRectFilled(labelX + 2, labelY + 2, labelX + labelWidth + 2, labelY + labelHeight + 2,
                              ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.2f), 4.0f);
        // Bakgrunn
        drawList.addRectFilled(labelX, labelY, labelX + labelWidth, labelY + labelHeight, bgColor, 4.0f);
        // Kant
        drawList.addRect(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                        ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 4.0f, 0, 2.0f);
        // Tekst
        drawList.addText(labelX + 6, labelY + 4, 
                       ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), label);
    }
    
    /**
     * Tegn bruker-posisjon på kartet (blå prikk med puls-effekt)
     */
    private void tegnBrukerPosisjon(imgui.ImDrawList drawList, float[] cursorPos, float canvasWidth, float canvasHeight) {
        float[] pos = GPSTjeneste.convertGPSToScreen(brukerLat, brukerLon,
                                                     kartCenterLat, kartCenterLon, 
                                                     kartZoom, canvasWidth, canvasHeight);
        
        float screenX = cursorPos[0] + pos[0];
        float screenY = cursorPos[1] + pos[1];
        
        // Sjekk om bruker-posisjon er innenfor skjermen
        if (pos[0] < 0 || pos[0] > canvasWidth || pos[1] < 0 || pos[1] > canvasHeight) {
            return; // Ikke vis hvis utenfor
        }
        
        // Puls-effekt (animert radius)
        long time = System.currentTimeMillis();
        float pulse = (float)Math.sin(time / 500.0) * 3.0f + 12.0f;
        
        // Ytre ring (lys blå, transparent med puls)
        int outerColor = ImGui.getColorU32(0.3f, 0.6f, 1.0f, 0.3f);
        drawList.addCircleFilled(screenX, screenY, pulse, outerColor, 20);
        
        // Skygge
        int shadowColor = ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.4f);
        drawList.addCircleFilled(screenX + 2, screenY + 2, 10, shadowColor, 16);
        
        // Hovedsirkel (mørk blå)
        int mainColor = ImGui.getColorU32(0.2f, 0.4f, 0.9f, 1.0f);
        drawList.addCircleFilled(screenX, screenY, 10, mainColor, 16);
        
        // Hvit kant
        drawList.addCircle(screenX, screenY, 10, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 16, 2.5f);
        
        // Indre hvit prikk
        drawList.addCircleFilled(screenX, screenY, 4, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 12);
        
        // "DU ER HER" label
        String label = "Du er her";
        float labelX = screenX + 15;
        float labelY = screenY - 10;
        float labelWidth = ImGui.calcTextSize(label).x + 12;
        float labelHeight = ImGui.calcTextSize(label).y + 8;
        
        // Label skygge
        drawList.addRectFilled(labelX + 2, labelY + 2, labelX + labelWidth + 2, labelY + labelHeight + 2,
                              ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.3f), 4.0f);
        // Label bakgrunn
        drawList.addRectFilled(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                              ImGui.getColorU32(0.2f, 0.4f, 0.9f, 0.95f), 4.0f);
        // Label kant
        drawList.addRect(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                        ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 4.0f, 0, 2.0f);
        // Label tekst
        drawList.addText(labelX + 6, labelY + 4, 
                       ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), label);
    }
    
    // Getters/Setters for ekstern kontroll
    
    public void visReisePaKart(Reiseforslag reise, int fraIndex, int tilIndex) {
        this.visRutepaKart = true;
        this.valgtReiseforslag = reise;
        this.ruteFraStoppestedIndex = fraIndex;
        this.ruteTilStoppestedIndex = tilIndex;
    }
    
    public void skjulReise() {
        this.visRutepaKart = false;
        this.valgtReiseforslag = null;
    }
    
    public void sentrerPa(double lat, double lon) {
        this.kartCenterLat = lat;
        this.kartCenterLon = lon;
    }
    
    public void tilbakestillZoom() {
        this.kartZoom = 1.5f;
        this.kartCenterLat = 59.124;
        this.kartCenterLon = 11.387;
    }
    
    public void oppdaterBrukerPosisjon(double lat, double lon) {
        this.brukerLat = lat;
        this.brukerLon = lon;
    }
    
    public double[] hentBrukerPosisjon() {
        return new double[]{brukerLat, brukerLon};
    }
}
