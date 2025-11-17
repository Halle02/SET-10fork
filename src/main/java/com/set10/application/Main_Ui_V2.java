package com.set10.application;

import com.set10.core.Datadepot;
import com.set10.database.DatabaseText;

import com.set10.core.Navigasjonstjeneste;
import com.set10.core.Stoppested;
import com.set10.core.Rute;
import com.set10.core.Avgang;
import com.set10.core.Billett;
import com.set10.core.Bruker;
import com.set10.core.Reiseforslag;
import com.set10.core.Reisesok;

import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.type.ImString;
import imgui.type.ImInt;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/**
 * Forbedret Main_Ui_V2:
 * - Bedre struktur med tydelig navigasjon
 * - Login/opprett-bruker funksjonalitet
 * - Fire hovedsider: Hjem, Planlegg, Billetter, Bruker
 * - Debug-meny tilgjengelig fra Bruker-siden
 * - Alle funksjoner fra gamle Main beholdt
 * - OPPDATERT: Støtte for både tid og dato-input (ikke lenger hardkodet til dagens dato)
 */
public class Main_Ui_V2 extends Application {

    // Core services
    Navigasjonstjeneste navigasjonstjeneste;
    Datadepot datadepot;
    
    // Bruker-relatert
    private Integer valgtBrukerId = null;
    private String valgtBrukerNavn = null;
    
    // Navigasjon (0=Hjem, 1=Planlegg, 2=Billetter, 3=Bruker)
    public ImInt aktivSide = new ImInt(0);
    
    // Reisesøk
    public String[] stoppestedNavn;
    public ImInt valgtFraStopp = new ImInt(0);
    public ImInt valgtTilStopp = new ImInt(0);
    public ImString avreiseTidInput = new ImString("12:30", 128);
    public ImString avreiseDatoInput = new ImString(LocalDate.now().toString(), 128);
    public List<Reiseforslag> funnetReiser = new ArrayList<>();
    public String feilmeldingSok = "";
    private boolean visAlleReiser = false;
    
    // Login
    private boolean visLogin = true;
    private ImString loginNavn = new ImString(128);
    private String loginFeil = "";
    
    // Debug
    private boolean visDebugMeny = false;

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Østfold trafikk premium");
    }

    @Override
    public void process() {
        // Hvis ikke logget inn, vis login-skjerm
        if (visLogin) {
            tegnLogin();
            return;
        }

        // Vis aktiv side
        switch (aktivSide.get()) {
            case 0 -> tegnHjem();
            case 1 -> tegnPlanlegg();
            case 2 -> tegnBilletter();
            case 3 -> tegnBruker();
        }

        // Bunn-navigasjon (alltid synlig)
        tegnBottomNav();
        
        // Debug-meny (kun synlig hvis aktivert fra Bruker-siden)
        if (visDebugMeny) {
            tegnDebugMeny();
        }
    }

    // ==================== LOGIN ====================
    
    private void tegnLogin() {
        ImGui.begin("Logg inn / Opprett bruker");

        ImGui.text("Østfold Trafikk - Logg inn");
        ImGui.separator();

        ImGui.text("Brukernavn:");
        ImGui.inputText("##loginNavn", loginNavn);

        if (!loginFeil.isEmpty()) {
            ImGui.textColored(1f, 0f, 0f, 1f, loginFeil);
        }

        if (ImGui.button("Logg inn")) {
            loginFeil = "";
            if (loginNavn.get().trim().isEmpty()) {
                loginFeil = "Skriv inn brukernavn.";
            } else {
                Bruker funnet = finnBrukerByName(loginNavn.get().trim());
                if (funnet == null) {
                    loginFeil = "Fant ikke bruker. Opprett konto eller prøv et annet navn.";
                } else {
                    valgtBrukerId = funnet.id;
                    valgtBrukerNavn = funnet.navn;
                    visLogin = false;
                    aktivSide.set(0);
                }
            }
        }

        ImGui.sameLine();
        if (ImGui.button("Opprett konto")) {
            loginFeil = "";
            String navn = loginNavn.get().trim();
            if (navn.isEmpty()) {
                loginFeil = "Skriv ønsket brukernavn.";
            } else if (finnBrukerByName(navn) != null) {
                loginFeil = "Brukernavn finnes allerede.";
            } else {
                Bruker ny = new Bruker(navn, LocalDate.of(2000, 1, 1));
                datadepot.opprettBruker(ny);
                try { 
                    datadepot.lagreTilDisk(); 
                } catch (Exception e) { 
                    System.err.println("Kunne ikke lagre ny bruker: " + e); 
                }
                valgtBrukerId = ny.id;
                valgtBrukerNavn = ny.navn;
                visLogin = false;
                aktivSide.set(0);
            }
        }

        ImGui.separator();
        ImGui.textWrapped("Tips: Dette er en enkel demo-login. Skriv inn et brukernavn for å logge inn eller opprette ny konto.");

        ImGui.end();
    }

    private Bruker finnBrukerByName(String navn) {
        if (datadepot == null || datadepot.hentBrukere() == null) return null;
        for (Bruker b : datadepot.hentBrukere()) {
            if (b != null && b.navn != null && b.navn.equalsIgnoreCase(navn)) {
                return b;
            }
        }
        return null;
    }

    // ==================== HJEM ====================
    
    private void tegnHjem() {
        ImGui.begin("Hjem");
        
        ImGui.text("Velkommen, " + (valgtBrukerNavn != null ? valgtBrukerNavn : "gjest") + "!");
        ImGui.separator();
        
        // Vis neste reise hvis tilgjengelig
        ImGui.text("Din neste reise:");
        if (!funnetReiser.isEmpty()) {
            Reiseforslag neste = funnetReiser.get(0);
            ImGui.textWrapped(neste.toString());
            
            if (ImGui.button("Se detaljer")) {
                aktivSide.set(1); // Planlegg-siden
            }
        } else {
            ImGui.text("Ingen reise planlagt.");
            ImGui.text("Bruk 'Planlegg' for å søke etter reiser.");
        }
        
        ImGui.separator();
        
        // Vis aktive billetter
        if (valgtBrukerId != null) {
            Bruker bruker = datadepot.hentBruker(valgtBrukerId);
            if (bruker.aktiveBilletter != null && !bruker.aktiveBilletter.isEmpty()) {
                ImGui.text("Dine aktive billetter:");
                for (Billett billett : bruker.aktiveBilletter) {
                    ImGui.text("• " + billett.type + " - Gyldig til: " + billett.sluttTid.toLocalTime());
                }
            } else {
                ImGui.text("Du har ingen aktive billetter.");
            }
        }
        
        ImGui.end();
    }

    // ==================== PLANLEGG ====================
    
    private void tegnPlanlegg() {
        ImGui.begin("Planlegg reise");

        
        if (stoppestedNavn != null && stoppestedNavn.length > 0) {
            ImGui.combo("Fra stoppested", valgtFraStopp, stoppestedNavn);
            ImGui.combo("Til stoppested", valgtTilStopp, stoppestedNavn);
        } else {
            ImGui.text("Laster stoppesteder...");
        }

        ImGui.separator();
        
        // TID OG DATO INPUT (oppdatert!) samme som main
        ImGui.text("Avreisetid (format HH:MM):");
        ImGui.inputText("##avreisetid", avreiseTidInput);
        ImGui.sameLine();
        ImGui.text("Dato (YYYY-MM-DD):");
        ImGui.inputText("##avreisedato", avreiseDatoInput);
        ImGui.separator();

        
        if (ImGui.button("Finn din neste reise")) {
            sokEtterReiser();
        }

        ImGui.separator();

        
        visReiseforslag();

        ImGui.end();
    }
    
    private void sokEtterReiser() {
        funnetReiser.clear();
        feilmeldingSok = "";
        visAlleReiser = false;
        
        try {
            
            if (valgtFraStopp.get() < 0 || valgtFraStopp.get() >= datadepot.hentStoppesteder().size() ||
                valgtTilStopp.get() < 0 || valgtTilStopp.get() >= datadepot.hentStoppesteder().size()) {
                feilmeldingSok = "Ugyldig valg av stoppested.";
                return;
            }
            
            Stoppested fra = datadepot.hentStoppesteder().get(valgtFraStopp.get());
            Stoppested til = datadepot.hentStoppesteder().get(valgtTilStopp.get());

            if (fra == til) {
                feilmeldingSok = "Start- og stoppested kan ikke være det samme.";
                return;
            }
            
            //nå data
            LocalTime onsketTid = LocalTime.parse(avreiseTidInput.get());
            LocalDate onsketDato = LocalDate.parse(avreiseDatoInput.get());
            LocalDateTime avreiseDatoTid = onsketTid.atDate(onsketDato);
            Reisesok sok = new Reisesok(fra, til, avreiseDatoTid, null);
            funnetReiser = navigasjonstjeneste.FinnReiser(sok);
            
            if (funnetReiser.isEmpty()) {
                feilmeldingSok = "Ingen reiseforslag funnet for valgt tidspunkt.";
            }
            
        } catch (Exception e) {
            feilmeldingSok = "Ugyldig tid eller dato format. Bruk HH:MM og YYYY-MM-DD (f.eks. 12:30 og 2025-11-15)";
            System.err.println("Feil ved parsing av tid/dato eller søk: " + e.getMessage());
        }
    }
    
    private void visReiseforslag() {
        if (!feilmeldingSok.isEmpty()) {
            ImGui.textColored(1f, 0f, 0f, 1f, feilmeldingSok);
        } else if (funnetReiser.isEmpty()) {
            ImGui.text("Ingen reiseforslag funnet.");
        } else {
            ImGui.text("Fant " + funnetReiser.size() + " reiseforslag:");
            ImGui.separator();

            int antallÅVise = visAlleReiser ? funnetReiser.size() : Math.min(3, funnetReiser.size());

            for (int i = 0; i < antallÅVise; i++) {
                Reiseforslag rf = funnetReiser.get(i);
                Avgang avgang = rf.avganger.get(0);

                Rute rute = datadepot.hentRute(avgang.ruteID);
                Stoppested fraStopp = datadepot.hentStoppesteder().get(valgtFraStopp.get());
                Stoppested tilStopp = datadepot.hentStoppesteder().get(valgtTilStopp.get());

                int fraIndex = rute.stopp.indexOf(fraStopp);
                int tilIndex = rute.stopp.indexOf(tilStopp);

                if (ImGui.collapsingHeader("Rute " + avgang.ruteID + " - Avgang " + avgang.tidspunkt)) {
                    if (fraIndex != -1 && tilIndex != -1) {
                        for (int j = fraIndex; j <= tilIndex; j++) {
                            ImGui.text(avgang.tidspunkt + " - " + rute.stopp.get(j).navn);
                        }
                        
                        // Hvis bytte er nødvendig
                        if (rf.avganger.size() > 1) {
                            ImGui.separator();
                            ImGui.text("Bytte:");
                            for (int k = 1; k < rf.avganger.size(); k++) {
                                Avgang ekstra = rf.avganger.get(k);
                                ImGui.text("→ Bytt til rute " + ekstra.ruteID + " kl. " + ekstra.tidspunkt);
                            }
                        }
                    }
                }
                ImGui.separator();
            }

            // Vis mer/mindre knapp
            if (funnetReiser.size() > 3) {
                if (visAlleReiser) {
                    if (ImGui.button("Vis mindre")) visAlleReiser = false;
                } else {
                    if (ImGui.button("Vis mer")) visAlleReiser = true;
                }
            }
        }
    }

    // ==================== BILLETTER ====================
    
    private void tegnBilletter() {
        ImGui.begin("Billetter");
        
        if (valgtBrukerId == null) {
            ImGui.text("Ingen bruker valgt.");
            ImGui.end();
            return;
        }

        Bruker bruker = datadepot.hentBruker(valgtBrukerId);
        ImGui.text("Bruker: " + bruker.navn);
        ImGui.separator();

        // Aktive billetter
        ImGui.text("Aktive billetter:");
        if (bruker.aktiveBilletter == null || bruker.aktiveBilletter.isEmpty()) {
            ImGui.text("Ingen aktive billetter.");
        } else {
            for (Billett bil : bruker.aktiveBilletter) {
                ImGui.text("• " + bil.type + " - Kjøpt: " + bil.startTid.toLocalTime());
                ImGui.sameLine();
                ImGui.textColored(0f, 1f, 0f, 1f, "(Gyldig til: " + bil.sluttTid.toLocalTime() + ")");
            }
        }
        
        ImGui.separator();

        // Kjøp billett
        if (ImGui.button("Kjøp Enkel-billett (90 min)")) {
            try {
                Billett ny = new Billett(Billett.Type.Enkel, LocalDateTime.now());
                datadepot.opprettBillett(ny);
                bruker.aktiveBilletter.add(ny);
                datadepot.lagreTilDisk();
                feilmeldingSok = "Kjøpte billett for " + bruker.navn + "! Gyldig i 90 minutter.";
            } catch (Exception e) {
                feilmeldingSok = "Feil ved kjøp av billett: " + e.getMessage();
                System.err.println("Feil ved kjøp: " + e);
            }
        }
        
        if (!feilmeldingSok.isEmpty()) {
            ImGui.separator();
            ImGui.textWrapped(feilmeldingSok);
        }
        
        ImGui.separator();
        
        // Tidligere billetter
        if (bruker.gamleBilletter != null && !bruker.gamleBilletter.isEmpty()) {
            if (ImGui.collapsingHeader("Tidligere billetter")) {
                for (Billett bil : bruker.gamleBilletter) {
                    ImGui.text("• " + bil.toString());
                }
            }
        }

        ImGui.end();
    }

    // ==================== BRUKER ====================
    
    private void tegnBruker() {
        ImGui.begin("Bruker");
        
        if (valgtBrukerId == null) {
            ImGui.text("Ingen bruker logget inn.");
        } else {
            Bruker bruker = datadepot.hentBruker(valgtBrukerId);
            ImGui.text("Navn: " + bruker.navn);
            ImGui.text("ID: " + bruker.id);
            ImGui.text("Fødselsdato: " + bruker.fodselsDato);
            
            ImGui.separator();
            
            
            int antallAktiveBilletter = bruker.aktiveBilletter != null ? bruker.aktiveBilletter.size() : 0;
            int antallGamleBilletter = bruker.gamleBilletter != null ? bruker.gamleBilletter.size() : 0;
            
            ImGui.text("Aktive billetter: " + antallAktiveBilletter);
            ImGui.text("Tidligere reiser: " + antallGamleBilletter);
            
            ImGui.separator();
            
          
            if (ImGui.checkbox("Vis debug-meny", visDebugMeny)) {
                visDebugMeny = !visDebugMeny;
            }
            
            ImGui.separator();
            
            if (ImGui.button("Logg ut")) {
                valgtBrukerId = null;
                valgtBrukerNavn = null;
                visLogin = true;
                visDebugMeny = false;
            }
        }
        
        ImGui.end();
    }

    // ==================== DEBUG MENY ====================
    
    private void tegnDebugMeny() {
        ImGui.begin("Debug meny");

        if (valgtBrukerId == null) {
            ImGui.text("Logg inn for å se debug-info.");
            ImGui.end();
            return;
        }

        Bruker valgtBruker = datadepot.hentBruker(valgtBrukerId);
        ImGui.text("Debug-info for: " + valgtBruker.navn);
        ImGui.separator();

        // Ruter
        if (ImGui.collapsingHeader("Ruter")) {
            ImGui.separator();
            for (Rute rute : datadepot.hentRuter()) {
                if (ImGui.treeNode("Rute " + rute.id + ": " + rute.toString())) {
                    for (Stoppested stopp : rute.stopp) {
                        ImGui.text("• " + stopp.toString());
                    }
                    ImGui.treePop();
                }
                ImGui.separator();
            }
        }

        // Stoppesteder med avganger
        if (ImGui.collapsingHeader("Stoppesteder")) {
            ImGui.separator();
            for (Stoppested stoppested : datadepot.hentStoppesteder()) {
                if (ImGui.treeNode(stoppested.toString())) {
                    for (Rute rute : datadepot.hentRuter()) {
                        if (rute.stopp.contains(stoppested)) {
                            String ruteNavn = "Rute " + rute.id + ": " + 
                                            rute.stopp.get(0).navn + " → " + 
                                            rute.stopp.get(rute.stopp.size() - 1).navn;
                            
                            if (ImGui.treeNode(ruteNavn)) {
                                for (Avgang a : stoppested.hentAvganger()) {
                                    if (a.ruteID == rute.id) {
                                        ImGui.text("  " + a.toString());
                                    }
                                }
                                ImGui.treePop();
                            }
                        }
                    }
                    ImGui.treePop();
                }
                ImGui.separator();
            }
        }

        // Alle brukere
        if (ImGui.collapsingHeader("Alle brukere")) {
            ImGui.separator();
            for (Bruker b : datadepot.hentBrukere()) {
                if (ImGui.treeNode(b.navn + " (ID: " + b.id + ")")) {
                    ImGui.text("Fødselsdato: " + b.fodselsDato);
                    ImGui.text("Aktive billetter: " + (b.aktiveBilletter != null ? b.aktiveBilletter.size() : 0));
                    ImGui.text("Gamle billetter: " + (b.gamleBilletter != null ? b.gamleBilletter.size() : 0));
                    ImGui.treePop();
                }
            }
        }

        ImGui.end();
    }

    // ==================== BUNN-NAVIGASJON ====================
    
    private void tegnBottomNav() {
        ImGui.begin("##bunnmeny", imgui.flag.ImGuiWindowFlags.NoTitleBar | imgui.flag.ImGuiWindowFlags.NoResize);
        
        if (ImGui.button("Hjem")) aktivSide.set(0);
        ImGui.sameLine();
        if (ImGui.button("Planlegg")) aktivSide.set(1);
        ImGui.sameLine();
        if (ImGui.button("Billetter")) aktivSide.set(2);
        ImGui.sameLine();
        if (ImGui.button("Bruker")) aktivSide.set(3);
        
        ImGui.end();
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void preRun() {
        
        datadepot = new Datadepot(new DatabaseText());
        
        
        boolean lastetFraDisk = false;
        try {
            datadepot.lasteFraDisk();
            lastetFraDisk = true;
            System.out.println("Data lastet inn fra fil.");
        } catch (Exception e) {
            System.err.println("[INFO] Kunne ikke laste inn data. Oppretter dummy-data. -> " + e.getMessage());
        }

        
        if (!lastetFraDisk) {
            datadepot.opprettDummydata();
            try {
                datadepot.lagreTilDisk();
            } catch (Exception e) {
                System.err.println("[ERROR] Kan ikke lagre dummy-data til fil -> " + e);
            }
        }
        
       
        navigasjonstjeneste = new Navigasjonstjeneste();
        navigasjonstjeneste.dataDepot = datadepot;

        
        if (datadepot.hentStoppesteder() != null) {
            stoppestedNavn = datadepot.hentStoppesteder().stream()
                                .map(s -> s.navn)
                                .toArray(String[]::new);
        } else {
            stoppestedNavn = new String[0];
        }

        
        if (datadepot.hentBrukere() == null || datadepot.hentBrukere().isEmpty()) {
            Bruker demo = new Bruker("demo", LocalDate.of(1990, 1, 1));
            datadepot.opprettBruker(demo);
            try {
                datadepot.lagreTilDisk();
            } catch (Exception e) {
                System.err.println("Feil ved lagring av demo-bruker: " + e);
            }
            System.out.println("Opprettet demo-bruker.");
        }

        
        visLogin = true;
    }

  
    @Override
    protected void postRun() {
        try {
            datadepot.lagreTilDisk();
            System.out.println("Data lagret automatisk ved lukking");
        } catch (Exception e) {
            System.err.println("Feil ved automatisk lagring: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(new Main_Ui_V2());
    }
}