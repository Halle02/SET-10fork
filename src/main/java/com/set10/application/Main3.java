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
import imgui.flag.ImGuiInputTextFlags;

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
public class Main3 extends Application {

    
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
    public ImString avreiseTidInput = new ImString(LocalTime.now().toString(), 128);
    public ImString avreiseDatoInput = new ImString(LocalDate.now().toString(), 128);
    public List<Reiseforslag> funnetReiser = new ArrayList<>();
    public String feilmeldingSok = "";
    private boolean visAlleReiser = false;
    
    // Login & Registration
    private boolean visVelkomst = true;
    private boolean visLogin = false;
    private boolean erRegistreringsmodus = false;
    private ImString loginNavn = new ImString(128);
    private ImString loginPassord = new ImString(128);
    private String loginFeil = "";
    private ImString registrerFodselsdato = new ImString(LocalDate.now().minusYears(25).toString(), 128);
    private String[] brukerGruppeNavn = {"Auto (basert på alder)", "Barn (0-15)", "Ungdom (16-18)", "Voksen (19-66)", "Student", "Honnør (67+)"};
    
    // Billetter
    private ImInt valgtBillettType = new ImInt(0);
    private String[] billettTypeNavn = {"Enkel (90 min)", "Periode (30 dager)"};
    private ImInt kjopBrukerGruppe = new ImInt(0);
    
    // Debug
    private boolean visDebugMeny = false;
    
    // Colors
    private final float[] COLOR_PRIMARY = new float[]{0.2f, 0.6f, 1.0f, 1.0f};
    private final float[] COLOR_SUCCESS = new float[]{0.2f, 0.8f, 0.3f, 1.0f};
    private final float[] COLOR_WARNING = new float[]{1.0f, 0.7f, 0.0f, 1.0f};
    private final float[] COLOR_ERROR = new float[]{1.0f, 0.2f, 0.2f, 1.0f};

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Østfold trafikk premium");
        config.setWidth(1400);
        config.setHeight(900);
    }

    @Override
    public void process() {
        
        if (visVelkomst) {
            tegnVelkomst();
            return;
        }
        
       
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

        
        tegnBottomNav();
        
        // Debug-meny (kun synlig hvis aktivert fra Bruker-siden)
        if (visDebugMeny) {
            tegnDebugMeny();
        }
    }

    // ==================== VELKOMST ====================
    
    private void tegnVelkomst() {
        float windowWidth = 600;
        float windowHeight = 500;
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.setNextWindowPos((ImGui.getIO().getDisplaySizeX() - windowWidth) / 2, 
                                (ImGui.getIO().getDisplaySizeY() - windowHeight) / 2);
        
        ImGui.begin("Velkommen", imgui.flag.ImGuiWindowFlags.NoResize | imgui.flag.ImGuiWindowFlags.NoMove | imgui.flag.ImGuiWindowFlags.NoTitleBar);

        ImGui.dummy(0, 40);
        
        // Logo/Title
        ImGui.dummy((windowWidth - 300) / 2, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], COLOR_PRIMARY[3]);
        ImGui.setWindowFontScale(2.0f);
        ImGui.text("🚌 Østfold Trafikk");
        ImGui.setWindowFontScale(1.0f);
        ImGui.popStyleColor();
        
        ImGui.dummy(0, 20);
        
        // Subtitle
        ImGui.dummy((windowWidth - 350) / 2, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.7f, 0.7f, 0.7f, 1.0f);
        ImGui.text("Planlegg din neste reise med oss");
        ImGui.popStyleColor();
        
        ImGui.dummy(0, 60);
        
        // Center buttons
        float buttonWidth = 300;
        float buttonHeight = 50;
        ImGui.dummy((windowWidth - buttonWidth) / 2, 0);
        ImGui.sameLine();
        
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], 0.8f);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], 1.0f);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonActive, COLOR_PRIMARY[0] * 0.8f, COLOR_PRIMARY[1] * 0.8f, COLOR_PRIMARY[2] * 0.8f, 1.0f);
        if (ImGui.button("Logg inn", buttonWidth, buttonHeight)) {
            visVelkomst = false;
            visLogin = true;
            erRegistreringsmodus = false;
        }
        ImGui.popStyleColor(3);
        
        ImGui.dummy(0, 20);
        
        ImGui.dummy((windowWidth - buttonWidth) / 2, 0);
        ImGui.sameLine();
        
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, COLOR_SUCCESS[0], COLOR_SUCCESS[1], COLOR_SUCCESS[2], 0.8f);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, COLOR_SUCCESS[0], COLOR_SUCCESS[1], COLOR_SUCCESS[2], 1.0f);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonActive, COLOR_SUCCESS[0] * 0.8f, COLOR_SUCCESS[1] * 0.8f, COLOR_SUCCESS[2] * 0.8f, 1.0f);
        if (ImGui.button("Opprett ny konto", buttonWidth, buttonHeight)) {
            visVelkomst = false;
            visLogin = true;
            erRegistreringsmodus = true;
        }
        ImGui.popStyleColor(3);
        
        ImGui.dummy(0, 40);
        
        // Footer
        ImGui.dummy((windowWidth - 250) / 2, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
        ImGui.text("Din partner for kollektivreiser");
        ImGui.popStyleColor();

        ImGui.end();
    }

    // ==================== LOGIN ====================
    
    private void tegnLogin() {
        float windowWidth = 500;
        float windowHeight = 600;
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.setNextWindowPos((ImGui.getIO().getDisplaySizeX() - windowWidth) / 2, 
                                (ImGui.getIO().getDisplaySizeY() - windowHeight) / 2);
        
        String windowTitle = erRegistreringsmodus ? "Opprett ny konto" : "Logg inn";
        ImGui.begin(windowTitle, imgui.flag.ImGuiWindowFlags.NoResize | imgui.flag.ImGuiWindowFlags.NoMove);

        ImGui.dummy(0, 20);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], COLOR_PRIMARY[3]);
        ImGui.text("Østfold Trafikk");
        ImGui.popStyleColor();
        ImGui.separator();
        ImGui.dummy(0, 10);
        
        // Back button
        if (ImGui.button("← Tilbake", 100, 30)) {
            visLogin = false;
            visVelkomst = true;
            loginFeil = "";
            loginNavn.set("");
            loginPassord.set("");
        }
        ImGui.dummy(0, 10);
        ImGui.separator();
        ImGui.dummy(0, 10);

        ImGui.text("Brukernavn:");
        ImGui.setNextItemWidth(windowWidth - 40);
        ImGui.inputText("##loginNavn", loginNavn);
        ImGui.dummy(0, 5);

        ImGui.text("Passord:");
        ImGui.setNextItemWidth(windowWidth - 40);
        ImGui.inputText("##loginPassord", loginPassord, ImGuiInputTextFlags.Password);
        ImGui.dummy(0, 10);

        if (!loginFeil.isEmpty()) {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_ERROR[0], COLOR_ERROR[1], COLOR_ERROR[2], COLOR_ERROR[3]);
            ImGui.textWrapped(loginFeil);
            ImGui.popStyleColor();
            ImGui.dummy(0, 5);
        }

        if (!erRegistreringsmodus && ImGui.button("Logg inn", 220, 35)) {
            loginFeil = "";
            if (loginNavn.get().trim().isEmpty()) {
                loginFeil = "Skriv inn brukernavn.";
            } else {
                Bruker funnet = finnBrukerByName(loginNavn.get().trim());
                if (funnet == null) {
                    loginFeil = "Fant ikke bruker. Opprett konto eller prøv et annet navn.";
                } else {
                    String angittPass = loginPassord.get();
                    if (!funnet.validerPassord(angittPass)) {
                        loginFeil = "Feil brukernavn eller passord.";
                    } else {
                        valgtBrukerId = funnet.id;
                        valgtBrukerNavn = funnet.navn;
                        visLogin = false;
                        visVelkomst = false;
                        aktivSide.set(0);
                    }
                }
            }
        }
        
        if (erRegistreringsmodus) {
            ImGui.separator();
            ImGui.dummy(0, 10);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], COLOR_PRIMARY[3]);
            ImGui.text("Opprett ny konto:");
            ImGui.popStyleColor();
            ImGui.dummy(0, 5);
        
            ImGui.text("Fødselsdato (YYYY-MM-DD):");
            ImGui.setNextItemWidth(windowWidth - 40);
            ImGui.inputText("##fodselsdato", registrerFodselsdato);
            ImGui.dummy(0, 10);

            if (ImGui.button("Opprett konto", 220, 35)) {
            loginFeil = "";
            String navn = loginNavn.get().trim();
            if (navn.isEmpty()) {
                loginFeil = "Skriv ønsket brukernavn.";
            } else if (finnBrukerByName(navn) != null) {
                loginFeil = "Brukernavn finnes allerede.";
            } else {
                try {
                    LocalDate fodselsDato = LocalDate.parse(registrerFodselsdato.get());
                    String pass = loginPassord.get();
                    Bruker ny = new Bruker(navn, fodselsDato, (pass == null || pass.isEmpty()) ? "1234" : pass);
                    
                    // Brukergruppe settes automatisk basert på alder
                    ny.brukerGruppe = Bruker.BrukerGruppe.auto;
                    
                    datadepot.opprettBruker(ny);
                    try { 
                        datadepot.lagreTilDisk(); 
                    } catch (Exception e) { 
                        System.err.println("Kunne ikke lagre ny bruker: " + e); 
                    }
                    valgtBrukerId = ny.id;
                    valgtBrukerNavn = ny.navn;
                    visLogin = false;
                    visVelkomst = false;
                    aktivSide.set(0);
                } catch (Exception e) {
                    loginFeil = "Ugyldig fødselsdato format. Bruk YYYY-MM-DD (f.eks. 2000-01-15)";
                }
            }
            }

            ImGui.dummy(0, 10);
            ImGui.separator();
            ImGui.dummy(0, 5);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.6f, 0.6f, 0.6f, 1.0f);
            ImGui.textWrapped("Tips: Standard passord er '1234' hvis du ikke angir noe.");
            ImGui.popStyleColor();
        }

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
        float windowWidth = 900;
        ImGui.setNextWindowSize(windowWidth, 700);
        ImGui.setNextWindowPos(100, 100);
        
        ImGui.begin("Hjem", imgui.flag.ImGuiWindowFlags.NoResize);
        
        ImGui.dummy(0, 10);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], COLOR_PRIMARY[3]);
        String velkomstTekst = "Velkommen, " + (valgtBrukerNavn != null ? valgtBrukerNavn : "gjest") + "!";
        ImGui.text(velkomstTekst);
        ImGui.popStyleColor();
        ImGui.separator();
        ImGui.dummy(0, 15);
        
        // Next journey section
        ImGui.beginChild("NesteReise", windowWidth - 20, 250, true);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], COLOR_PRIMARY[3]);
        ImGui.text("Din neste reise:");
        ImGui.popStyleColor();
        ImGui.separator();
        ImGui.dummy(0, 10);
        
        if (!funnetReiser.isEmpty()) {
            Reiseforslag neste = funnetReiser.get(0);
            ImGui.textWrapped(neste.toString());
            ImGui.dummy(0, 10);
            
            if (ImGui.button("Se detaljer i Planlegg", 200, 35)) {
                // Forhåndsutfyll Planlegg med valgt reise og naviger dit
                try {
                    Reisesok sok = neste.sok;
                    java.util.List<Stoppested> alle = datadepot.hentStoppesteder();
                    int idxFra = alle.indexOf(sok.getFraStoppested());
                    int idxTil = alle.indexOf(sok.getTilStoppested());
                    if (idxFra >= 0) valgtFraStopp.set(idxFra);
                    if (idxTil >= 0) valgtTilStopp.set(idxTil);
                    if (neste.avganger != null && !neste.avganger.isEmpty()) {
                        avreiseTidInput.set(neste.avganger.get(0).tidspunkt.toString());
                    }
                    if (sok.avreiseTid != null) avreiseDatoInput.set(sok.avreiseTid.toLocalDate().toString());
                } catch (Exception e) {
                    // fallback: ingen forhåndsutfylling
                }
                aktivSide.set(1); // Planlegg-siden
            }
        } else {
            ImGui.text("Ingen reise planlagt.");
            ImGui.dummy(0, 5);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.7f, 0.7f, 0.7f, 1.0f);
            ImGui.text("Bruk 'Planlegg' for å søke etter reiser.");
            ImGui.popStyleColor();
        }
        ImGui.endChild();
        
        ImGui.dummy(0, 15);
        
        // Active tickets section
        ImGui.beginChild("AktiveBilletter", windowWidth - 20, 220, true);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_SUCCESS[0], COLOR_SUCCESS[1], COLOR_SUCCESS[2], COLOR_SUCCESS[3]);
        ImGui.text("Dine aktive billetter:");
        ImGui.popStyleColor();
        ImGui.separator();
        ImGui.dummy(0, 10);
        
        if (valgtBrukerId != null) {
            Bruker bruker = datadepot.hentBruker(valgtBrukerId);
            if (bruker.aktiveBilletter != null && !bruker.aktiveBilletter.isEmpty()) {
                for (Billett billett : bruker.aktiveBilletter) {
                    ImGui.bulletText(billett.type + " - Gyldig til: " + billett.sluttTid.toLocalTime());
                }
            } else {
                ImGui.text("Du har ingen aktive billetter.");
                ImGui.dummy(0, 5);
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.7f, 0.7f, 0.7f, 1.0f);
                ImGui.text("Gå til 'Billetter' for å kjøpe.");
                ImGui.popStyleColor();
            }
        } else {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_WARNING[0], COLOR_WARNING[1], COLOR_WARNING[2], COLOR_WARNING[3]);
            ImGui.text("Logg inn for å se dine billetter.");
            ImGui.popStyleColor();
        }
        ImGui.endChild();
        
        ImGui.end();
    }    // ==================== PLANLEGG ====================
    
    private void tegnPlanlegg() {
        float windowWidth = 1000;
        ImGui.setNextWindowSize(windowWidth, 800);
        ImGui.setNextWindowPos(200, 50);
        
        ImGui.begin("Planlegg reise", imgui.flag.ImGuiWindowFlags.NoResize);

        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], COLOR_PRIMARY[3]);
        ImGui.text("Søk etter reise");
        ImGui.popStyleColor();
        ImGui.separator();
        ImGui.dummy(0, 10);
        
        if (stoppestedNavn != null && stoppestedNavn.length > 0) {
            ImGui.text("Fra stoppested:");
            ImGui.setNextItemWidth(400);
            ImGui.combo("##frastopp", valgtFraStopp, stoppestedNavn);
            ImGui.dummy(0, 5);
            
            ImGui.text("Til stoppested:");
            ImGui.setNextItemWidth(400);
            ImGui.combo("##tilstopp", valgtTilStopp, stoppestedNavn);
        } else {
            ImGui.text("Laster stoppesteder...");
        }

        ImGui.dummy(0, 10);
        ImGui.separator();
        ImGui.dummy(0, 10);
        
        //Nå tid
        ImGui.text("Rask tidsvelger:");
        if (ImGui.button("Nå", 100, 30)) {
            LocalTime now = LocalTime.now();
            avreiseTidInput.set(now.toString());
            avreiseDatoInput.set(LocalDate.now().toString());
        }
        
        ImGui.dummy(0, 10);
        
        // Manual time/date input
       // ImGui.button(label:"Filtere:");
        ImGui.text("Avreisetid (HH:MM):");
        ImGui.setNextItemWidth(200);
        ImGui.inputText("##avreisetid", avreiseTidInput);
        ImGui.sameLine();
        ImGui.dummy(20, 0);
        ImGui.sameLine();
        ImGui.text("Dato (YYYY-MM-DD):");
        ImGui.setNextItemWidth(200);
        ImGui.inputText("##avreisedato", avreiseDatoInput);
        
        ImGui.dummy(0, 15);
        
        if (ImGui.button("Finn din neste reise", 250, 40)) {
            sokEtterReiser();
        }

        ImGui.dummy(0, 10);
        ImGui.separator();
        ImGui.dummy(0, 10);

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
            
            // Valider at fra og til stopp ikke er like
            if (valgtFraStopp.get() == valgtTilStopp.get()) {
                feilmeldingSok = "Feil: Du kan ikke velge samme stoppested som start og slutt!";
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
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_ERROR[0], COLOR_ERROR[1], COLOR_ERROR[2], COLOR_ERROR[3]);
            ImGui.textWrapped(feilmeldingSok);
            ImGui.popStyleColor();
        } else if (funnetReiser.isEmpty()) {
            ImGui.text("Ingen reiseforslag funnet.");
        } else {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_SUCCESS[0], COLOR_SUCCESS[1], COLOR_SUCCESS[2], COLOR_SUCCESS[3]);
            ImGui.text("Fant " + funnetReiser.size() + " reiseforslag:");
            ImGui.popStyleColor();
            ImGui.dummy(0, 5);

            int antallÅVise = visAlleReiser ? funnetReiser.size() : Math.min(3, funnetReiser.size());

            for (int i = 0; i < antallÅVise; i++) {
                Reiseforslag rf = funnetReiser.get(i);
                Avgang forsteAvgang = rf.avganger.get(0);
                Rute rute = datadepot.hentRute(forsteAvgang.ruteID);
                
                Stoppested fraStopp = datadepot.hentStoppesteder().get(valgtFraStopp.get());
                Stoppested tilStopp = datadepot.hentStoppesteder().get(valgtTilStopp.get());

                int fraIndex = rute.stopp.indexOf(fraStopp);
                int tilIndex = rute.stopp.indexOf(tilStopp);
                
                // Calculate journey info
                LocalTime startTid = forsteAvgang.tidspunkt;
                LocalTime sluttTid = (tilIndex != -1) ? navigasjonstjeneste.beregnEstimertAnkomst(startTid, fraIndex, tilIndex) : startTid;
                long minutter = java.time.Duration.between(startTid, sluttTid).toMinutes();
                
                // Header without departure time
                String header = "Rute " + forsteAvgang.ruteID + " → " + fraStopp.navn + " til " + tilStopp.navn + 
                               " (" + minutter + " min" + (rf.avganger.size() > 1 ? ", med bytte)" : ")");
                
                if (ImGui.collapsingHeader(header + "##rute" + i)) {
                    ImGui.indent(20);
                    
                    // Show timeline
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], COLOR_PRIMARY[3]);
                    ImGui.text("Reiserute:");
                    ImGui.popStyleColor();
                    ImGui.dummy(0, 5);
                    
                    if (fraIndex != -1 && tilIndex != -1) {
                        for (int j = fraIndex; j <= tilIndex; j++) {
                            LocalTime tidForStopp = navigasjonstjeneste.beregnEstimertAnkomst(forsteAvgang.tidspunkt, fraIndex, j);
                            String stopIcon = (j == fraIndex) ? "🚏 [START]" : (j == tilIndex) ? "🏁 [SLUTT]" : "   •";
                            ImGui.text(stopIcon + " " + tidForStopp.toString() + " - " + rute.stopp.get(j).navn);
                        }
                        
                        // Show transfer info
                        if (rf.avganger.size() > 1) {
                            ImGui.dummy(0, 5);
                            ImGui.separator();
                            ImGui.dummy(0, 5);
                            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_WARNING[0], COLOR_WARNING[1], COLOR_WARNING[2], COLOR_WARNING[3]);
                            ImGui.text("⚠ Bytte kreves:");
                            ImGui.popStyleColor();
                            for (int k = 1; k < rf.avganger.size(); k++) {
                                Avgang ekstraAvgang = rf.avganger.get(k);
                                ImGui.text("  → Bytt til rute " + ekstraAvgang.ruteID + " kl. " + ekstraAvgang.tidspunkt);
                            }
                        }
                        
                        // Show all matching departures for this route
                        ImGui.dummy(0, 10);
                        ImGui.separator();
                        ImGui.dummy(0, 5);
                        ImGui.text("Andre avganger på denne ruten:");
                        ImGui.dummy(0, 3);
                        
                        // Get all departures for this stop on this route
                        java.util.List<Avgang> alleAvganger = datadepot.hentAvganger();
                        java.util.List<String> andreAvganger = new java.util.ArrayList<>();
                        
                        for (Avgang avg : alleAvganger) {
                            if (avg.ruteID == forsteAvgang.ruteID && avg.stoppestedID == fraStopp.id) {
                                if (!avg.tidspunkt.equals(forsteAvgang.tidspunkt)) {
                                    andreAvganger.add(avg.tidspunkt.toString());
                                }
                            }
                        }
                        
                        // Sort and display
                        java.util.Collections.sort(andreAvganger);
                        int visCount = 0;
                        for (String avgTid : andreAvganger) {
                            if (visCount < 10) { // Show max 10 more departures
                                ImGui.text("  • " + avgTid);
                                visCount++;
                            }
                        }
                        
                        if (andreAvganger.size() > 10) {
                            ImGui.text("  ... og " + (andreAvganger.size() - 10) + " flere");
                        }
                        
                        if (andreAvganger.isEmpty()) {
                            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.6f, 0.6f, 0.6f, 1.0f);
                            ImGui.text("  Ingen andre avganger funnet");
                            ImGui.popStyleColor();
                        }
                    }
                    
                    ImGui.unindent(20);
                }
                
                ImGui.dummy(0, 5);
            }

            // Show more/less button
            if (funnetReiser.size() > 3) {
                ImGui.dummy(0, 10);
                if (visAlleReiser) {
                    if (ImGui.button("Vis mindre", 150, 30)) visAlleReiser = false;
                } else {
                    if (ImGui.button("Vis alle " + funnetReiser.size() + " forslag", 200, 30)) visAlleReiser = true;
                }
            }
        }
    }

    // ==================== BILLETTER ====================
    
    private void tegnBilletter() {
        float windowWidth = 800;
        ImGui.setNextWindowSize(windowWidth, 700);
        ImGui.setNextWindowPos(50, 50);
        
        ImGui.begin("Billetter", imgui.flag.ImGuiWindowFlags.NoResize);
        
        if (valgtBrukerId == null) {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_ERROR[0], COLOR_ERROR[1], COLOR_ERROR[2], COLOR_ERROR[3]);
            ImGui.text("Ingen bruker valgt.");
            ImGui.popStyleColor();
            ImGui.end();
            return;
        }

        Bruker bruker = datadepot.hentBruker(valgtBrukerId);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], COLOR_PRIMARY[3]);
        ImGui.text("Bruker: " + bruker.navn + " (" + bruker.brukerGruppe + ")");
        ImGui.popStyleColor();
        ImGui.separator();
        ImGui.dummy(0, 10);

        // Aktive billetter section
        ImGui.beginChild("AktiveBilletter", windowWidth - 20, 200, true);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_SUCCESS[0], COLOR_SUCCESS[1], COLOR_SUCCESS[2], COLOR_SUCCESS[3]);
        ImGui.text("Aktive billetter:");
        ImGui.popStyleColor();
        ImGui.separator();
        
        if (bruker.aktiveBilletter == null || bruker.aktiveBilletter.isEmpty()) {
            ImGui.text("Ingen aktive billetter.");
        } else {
            for (Billett bil : bruker.aktiveBilletter) {
                ImGui.bulletText(bil.type + " - Kjøpt: " + bil.startTid.toLocalTime());
                ImGui.sameLine();
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_SUCCESS[0], COLOR_SUCCESS[1], COLOR_SUCCESS[2], COLOR_SUCCESS[3]);
                ImGui.text("(Gyldig til: " + bil.sluttTid.toLocalTime() + ")");
                ImGui.popStyleColor();
            }
        }
        ImGui.endChild();
        
        ImGui.dummy(0, 10);
        ImGui.separator();
        ImGui.dummy(0, 10);

        // Kjøp billett section
        ImGui.text("Kjøp ny billett:");
        ImGui.dummy(0, 5);
        
        ImGui.text("Billetttype:");
        ImGui.setNextItemWidth(300);
        ImGui.combo("##billetttype", valgtBillettType, billettTypeNavn);
        ImGui.dummy(0, 5);
        
        ImGui.text("Brukergruppe for pris:");
        ImGui.setNextItemWidth(300);
        ImGui.combo("##kjopBrukergruppe", kjopBrukerGruppe, brukerGruppeNavn);
        ImGui.dummy(0, 10);
        
        // Price display
        String priceInfo = "";
        if (valgtBillettType.get() == 0) { // Enkel
            priceInfo = "Pris: 40 kr (gyldig 90 minutter)";
        } else { // Periode
            if (kjopBrukerGruppe.get() == 0) priceInfo = "Pris: Gratis (barn/auto)";
            else if (kjopBrukerGruppe.get() == 1) priceInfo = "Pris: Gratis (barn)";
            else if (kjopBrukerGruppe.get() == 2) priceInfo = "Pris: 400 kr (ungdom, 30 dager)";
            else if (kjopBrukerGruppe.get() == 3) priceInfo = "Pris: 800 kr (voksen, 30 dager)";
            else if (kjopBrukerGruppe.get() == 4) priceInfo = "Pris: 500 kr (student, 30 dager)";
            else if (kjopBrukerGruppe.get() == 5) priceInfo = "Pris: 500 kr (honnør, 30 dager)";
        }
        
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_WARNING[0], COLOR_WARNING[1], COLOR_WARNING[2], COLOR_WARNING[3]);
        ImGui.text(priceInfo);
        ImGui.popStyleColor();
        ImGui.dummy(0, 10);
        
        if (ImGui.button("Kjøp billett", 200, 40)) {
            try {
                Billett.Type type = (valgtBillettType.get() == 0) ? Billett.Type.Enkel : Billett.Type.Periode;
                Billett ny = new Billett(type, LocalDateTime.now());
                datadepot.opprettBillett(ny);
                bruker.aktiveBilletter.add(ny);
                datadepot.lagreTilDisk();
                
                String typeTekst = (type == Billett.Type.Enkel) ? "Enkel-billett (90 min)" : "Periodebillett (30 dager)";
                feilmeldingSok = "✓ Kjøpte " + typeTekst + " for " + bruker.navn + "!";
            } catch (Exception e) {
                feilmeldingSok = "Feil ved kjøp av billett: " + e.getMessage();
                System.err.println("Feil ved kjøp: " + e);
            }
        }
        
        if (!feilmeldingSok.isEmpty()) {
            ImGui.dummy(0, 5);
            if (feilmeldingSok.startsWith("✓")) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_SUCCESS[0], COLOR_SUCCESS[1], COLOR_SUCCESS[2], COLOR_SUCCESS[3]);
            } else {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_ERROR[0], COLOR_ERROR[1], COLOR_ERROR[2], COLOR_ERROR[3]);
            }
            ImGui.textWrapped(feilmeldingSok);
            ImGui.popStyleColor();
        }
        
        ImGui.dummy(0, 10);
        ImGui.separator();
        ImGui.dummy(0, 10);
        
        // Tidligere billetter
        if (bruker.gamleBilletter != null && !bruker.gamleBilletter.isEmpty()) {
            if (ImGui.collapsingHeader("Tidligere billetter (" + bruker.gamleBilletter.size() + ")")) {
                ImGui.beginChild("GamleBilletter", windowWidth - 20, 150, true);
                for (Billett bil : bruker.gamleBilletter) {
                    ImGui.bulletText(bil.toString());
                }
                ImGui.endChild();
            }
        }

        ImGui.end();
    }

    // ==================== BRUKER ====================
    
    private void tegnBruker() {
        float windowWidth = 700;
        ImGui.setNextWindowSize(windowWidth, 650);
        ImGui.setNextWindowPos(300, 100);
        
        ImGui.begin("Bruker", imgui.flag.ImGuiWindowFlags.NoResize);
        
        if (valgtBrukerId == null) {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_WARNING[0], COLOR_WARNING[1], COLOR_WARNING[2], COLOR_WARNING[3]);
            ImGui.text("Ingen bruker logget inn.");
            ImGui.popStyleColor();
        } else {
            Bruker bruker = datadepot.hentBruker(valgtBrukerId);
            
            ImGui.dummy(0, 10);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2], COLOR_PRIMARY[3]);
            ImGui.text("Brukerinformasjon");
            ImGui.popStyleColor();
            ImGui.separator();
            ImGui.dummy(0, 10);
            
            // User info section
            ImGui.beginChild("BrukerInfo", windowWidth - 20, 200, true);
            ImGui.text("Navn: " + bruker.navn);
            ImGui.text("ID: " + bruker.id);
            ImGui.text("Fødselsdato: " + bruker.fodselsDato);
            ImGui.text("Brukergruppe: " + bruker.brukerGruppe);
            ImGui.dummy(0, 20);
            
            int antallAktiveBilletter = bruker.aktiveBilletter != null ? bruker.aktiveBilletter.size() : 0;
            int antallGamleBilletter = bruker.gamleBilletter != null ? bruker.gamleBilletter.size() : 0;
            
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_SUCCESS[0], COLOR_SUCCESS[1], COLOR_SUCCESS[2], COLOR_SUCCESS[3]);
            ImGui.text("Aktive billetter: " + antallAktiveBilletter);
            ImGui.popStyleColor();
            
            ImGui.text("Tidligere reiser: " + antallGamleBilletter);
            ImGui.endChild();
            
            ImGui.dummy(0, 15);
            ImGui.separator();
            ImGui.dummy(0, 15);
            
            // Settings section
            ImGui.text("Innstillinger:");
            ImGui.dummy(0, 5);
            
            if (ImGui.checkbox("Vis debug-meny", visDebugMeny)) {
                visDebugMeny = !visDebugMeny;
            }
            
            ImGui.dummy(0, 20);
            ImGui.separator();
            ImGui.dummy(0, 20);
            
            // Logout button
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, COLOR_ERROR[0], COLOR_ERROR[1], COLOR_ERROR[2], 0.8f);
            if (ImGui.button("Logg ut", 150, 40)) {
                valgtBrukerId = null;
                valgtBrukerNavn = null;
                visLogin = false;
                visVelkomst = true;
                visDebugMeny = false;
            }
            ImGui.popStyleColor();
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

  
  
    protected void postRun() {
        try {
            datadepot.lagreTilDisk();
            System.out.println("Data lagret automatisk ved lukking");
        } catch (Exception e) {
            System.err.println("Feil ved automatisk lagring: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(new Main3());
    }
}