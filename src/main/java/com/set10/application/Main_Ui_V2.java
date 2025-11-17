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
import com.set10.core.GPSTjeneste;
import com.set10.ui.KartRenderer;

import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.type.ImString;
import imgui.type.ImInt;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/**
 * Østfold Trafikk - Hovedapplikasjon
 * 
 * KART-SYSTEM:
 * - Bruker GPSTjeneste for å konvertere GPS (lat/lon) til skjerm-koordinater
 * - Animerte busser beveger seg langs ruter basert på ekte GPS-koordinater
 * - Alle stoppesteder har reelle Halden-koordinater (59.1°N, 11.4°E)
 * 
 * HOVEDDELER:
 * - Login/Registrering
 * - Reise: Søk etter reiser, favoritter, historikk
 * - Kart: Interaktivt kart med stopp, ruter og busser
 * - Billett: Kjøp og vis billetter med QR-kode
 * - Profil: Brukerinfo og innstillinger
 */
public class Main_Ui_V2 extends Application {

    // Core services
    Navigasjonstjeneste navigasjonstjeneste;
    Datadepot datadepot;
    
    // Bruker-relatert
    private Integer valgtBrukerId = null;
    private String valgtBrukerNavn = null;
    
    // Navigasjon (0=Hjem, 1=Reise, 2=Kart, 3=Billett, 4=Profil)
    // Hjem brukes ikke i navigasjon, starter på Reise
    public ImInt aktivSide = new ImInt(1);
    
    // Reisesøk
    public String[] stoppestedNavn;
    public ImInt valgtFraStopp = new ImInt(0);
    public ImInt valgtTilStopp = new ImInt(0);
    
    // Kart-relatert
    public ImInt valgtStoppested = new ImInt(0);
    private KartRenderer kartRenderer = null;
    // Raw GPS (simulated current position)
    private double rawGPSLat = 59.124;
    private double rawGPSLon = 11.387;
    
    // Realtime info
    private Stoppested naermesteStopp = null;
    private double avstandTilNaermeste = 0.0;
    
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
    private String[] billettTypeNavn = {"Enkel (90 min)", "Periode (30 dager)", "7-dagers", "Familie"};
    private ImInt kjopBrukerGruppe = new ImInt(0);
    private boolean visQRKode = false;
    
    // Favorites & History
    private List<String> favorittStoppesteder = new ArrayList<>();
    private List<String> sisteReiseSok = new ArrayList<>();
    
    // GPS initialisering
    private boolean gpsInitialisert = false;
    
    // Kart variabler
    private double kartCenterLat = 59.124;  // Halden sentrum
    private double kartCenterLon = 11.387;
    private float kartZoom = 1.5f;
    private boolean kartDragging = false;
    private float kartDragStartX = 0;
    private float kartDragStartY = 0;
    private double kartDragStartLat = 0;
    private double kartDragStartLon = 0;
    
    // Rutevsualisering på kart
    private boolean visRutepaKart = false;
    private Reiseforslag valgtReiseforslag = null;
    private int ruteFraStoppestedIndex = -1;
    private int ruteTilStoppestedIndex = -1;
    
    // Buss-animasjon
    private java.util.Map<Integer, Double> bussPosisjoner = new java.util.HashMap<>();
    private long lastBusUpdate = 0;
    
    // Debug
    private boolean visDebugMeny = false;
    
    // Østfold Trafikk Colors
    private final float[] COLOR_TEAL = new float[]{0.2f, 0.7f, 0.7f, 1.0f};
    private final float[] COLOR_TEAL_DARK = new float[]{0.15f, 0.55f, 0.55f, 1.0f};
    private final float[] COLOR_WHITE = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    private final float[] COLOR_LIGHT_BG = new float[]{0.96f, 0.96f, 0.96f, 1.0f}; // #F5F5F5
    private final float[] COLOR_TEXT_DARK = new float[]{0.2f, 0.2f, 0.2f, 1.0f};
    private final float[] COLOR_TEXT_LIGHT = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Østfold trafikk premium");
        config.setWidth(400);
        config.setHeight(800);
    }

    @Override
    public void process() {
        // Initialize KartRenderer if not done
        if (kartRenderer == null && datadepot != null) {
            kartRenderer = new KartRenderer(datadepot, valgtStoppested);
            System.out.println("✓ KartRenderer initialisert");
        }
        // Vis velkomstside først
        if (visVelkomst) {
            tegnVelkomst();
            return;
        }
        
        // Hvis ikke logget inn, vis login-skjerm
        if (visLogin) {
            tegnLogin();
            return;
        }

        // Update GPS position with Kalman filtering
        oppdaterGPSPosisjon();
        
        // Oppdater bruker-posisjon i kartRenderer
        if (kartRenderer != null) {
            double[] pos = hentGlattetPosisjon();
            kartRenderer.oppdaterBrukerPosisjon(pos[0], pos[1]);
        }

        // Tegn header
        tegnHeader();

        // Vis aktiv side
        if (aktivSide.get() == 1) {
            tegnReise();
        } else if (aktivSide.get() == 2) {
            tegnKart();
        } else if (aktivSide.get() == 3) {
            tegnBillett();
        } else if (aktivSide.get() == 4) {
            tegnProfil();
        }
        
        // Bunn-navigasjon (alltid synlig etter login)
        tegnBottomNav();
        
        // Debug-meny (kun synlig hvis aktivert fra Bruker-siden)
        if (visDebugMeny) {
            tegnDebugMeny();
        }
    }

    // ==================== VELKOMST ====================
    
    private void tegnVelkomst() {
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], COLOR_WHITE[3]);
        
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.setNextWindowPos(0, 0);
        
        ImGui.begin("Velkommen", imgui.flag.ImGuiWindowFlags.NoResize | imgui.flag.ImGuiWindowFlags.NoMove | imgui.flag.ImGuiWindowFlags.NoTitleBar);

        ImGui.dummy(0, windowHeight * 0.15f);
        
        // Logo/Title - centered
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.setWindowFontScale(2.0f);
        float titleWidth = ImGui.calcTextSize("Østfold Trafikk").x;
        ImGui.dummy((windowWidth - titleWidth) / 2, 0);
        ImGui.sameLine();
        ImGui.text("Østfold Trafikk");
        ImGui.setWindowFontScale(1.0f);
        ImGui.popStyleColor();
        
        ImGui.dummy(0, 15);
        
        // Subtitle - centered
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.7f, 0.7f, 0.7f, 1.0f);
        float subtitleWidth = ImGui.calcTextSize("Planlegg din neste reise med oss").x;
        ImGui.dummy((windowWidth - subtitleWidth) / 2, 0);
        ImGui.sameLine();
        ImGui.text("Planlegg din neste reise med oss");
        ImGui.popStyleColor();
        
        ImGui.dummy(0, windowHeight * 0.2f);
        
        // Center buttons
        float buttonWidth = windowWidth - 60;
        float buttonHeight = 55;
        ImGui.dummy((windowWidth - buttonWidth) / 2, 0);
        ImGui.sameLine();
        
        ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, COLOR_TEAL_DARK[0] * 0.9f, COLOR_TEAL_DARK[1] * 0.9f, COLOR_TEAL_DARK[2] * 0.9f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
        if (ImGui.button("Logg inn", buttonWidth, buttonHeight)) {
            visVelkomst = false;
            visLogin = true;
            erRegistreringsmodus = false;
        }
        ImGui.popStyleColor(4);
        
        ImGui.dummy(0, 15);
        
        ImGui.dummy(30, 0);
        ImGui.sameLine();
        
        ImGui.pushStyleColor(ImGuiCol.Button, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_LIGHT_BG[0], COLOR_LIGHT_BG[1], COLOR_LIGHT_BG[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.85f, 0.85f, 0.85f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.pushStyleColor(ImGuiCol.Border, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.FrameBorderSize, 2.0f);
        if (ImGui.button("Opprett ny konto", buttonWidth, buttonHeight)) {
            visVelkomst = false;
            visLogin = true;
            erRegistreringsmodus = true;
        }
        ImGui.popStyleVar();
        ImGui.popStyleColor(5);
        
        ImGui.dummy(0, 60);
        
        // Footer
        ImGui.dummy((windowWidth - 240) / 2, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, 0.6f, 0.6f, 0.6f, 1.0f);
        ImGui.text("Din partner for kollektivreiser");
        ImGui.popStyleColor();

        ImGui.end();
        ImGui.popStyleColor();
    }

    // ==================== LOGIN ====================
    
    private void tegnLogin() {
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], COLOR_WHITE[3]);
        
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.setNextWindowPos(0, 0);
        
        String windowTitle = erRegistreringsmodus ? "Opprett ny konto" : "Logg inn";
        ImGui.begin(windowTitle, imgui.flag.ImGuiWindowFlags.NoResize | imgui.flag.ImGuiWindowFlags.NoMove | imgui.flag.ImGuiWindowFlags.NoTitleBar);

        ImGui.dummy(0, 40);
        
        // Logo centered
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.setWindowFontScale(1.8f);
        float titleWidth = ImGui.calcTextSize("Østfold Trafikk").x;
        ImGui.dummy((windowWidth - titleWidth) / 2, 0);
        ImGui.sameLine();
        ImGui.text("Østfold Trafikk");
        ImGui.setWindowFontScale(1.0f);
        ImGui.popStyleColor();
        ImGui.dummy(0, 40);
        
        // Input section with clean design
        ImGui.dummy(20, 0);
        ImGui.sameLine();
        ImGui.beginChild("LoginInputs", windowWidth - 40, erRegistreringsmodus ? 350 : 250, true, ImGuiWindowFlags.NoScrollbar);
        ImGui.dummy(0, 15);
        
        // Epost / Brukernavn
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.text(erRegistreringsmodus ? "EPOST" : "BRUKERNAVN");
        ImGui.popStyleColor();
        ImGui.setNextItemWidth(windowWidth - 70);
        ImGui.inputText("##loginNavn", loginNavn);
        ImGui.dummy(0, 12);

        // Passord
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.text("PASSORD");
        ImGui.popStyleColor();
        ImGui.setNextItemWidth(windowWidth - 70);
        ImGui.inputText("##loginPassord", loginPassord, ImGuiInputTextFlags.Password);
        ImGui.dummy(0, 12);

        if (erRegistreringsmodus) {
            // Bekreft Passord
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
            ImGui.text("BEKREFT PASS");
            ImGui.popStyleColor();
            ImGui.setNextItemWidth(windowWidth - 70);
            ImGui.inputText("##bekreftPassord", loginPassord, ImGuiInputTextFlags.Password);
            ImGui.dummy(0, 12);
            
            // Fødselsdato
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
            ImGui.text("FØDSELSDATO");
            ImGui.popStyleColor();
            ImGui.setNextItemWidth(windowWidth - 70);
            ImGui.inputText("##fodselsdato", registrerFodselsdato);
            ImGui.dummy(0, 5);
        }
        
        ImGui.dummy(0, 5);
        ImGui.endChild();
        
        ImGui.dummy(0, 15);

        if (!loginFeil.isEmpty()) {
            ImGui.dummy(20, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
            ImGui.textWrapped(loginFeil);
            ImGui.popStyleColor();
            ImGui.dummy(0, 10);
        }

        if (!erRegistreringsmodus) {
            ImGui.dummy(20, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], 1.0f);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
            if (ImGui.button("Logg inn", windowWidth - 40, 50)) {
                ImGui.popStyleColor(3);
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
                        aktivSide.set(1);
                    }
                }
            }
            } else {
                ImGui.popStyleColor(3);
            }
        }
        
        if (erRegistreringsmodus) {
            ImGui.dummy(20, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], 1.0f);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
            if (ImGui.button("Opprett konto", windowWidth - 40, 50)) {
            loginFeil = "";
            String navn = loginNavn.get().trim();
            if (navn.isEmpty()) {
                loginFeil = "Skriv ønsket epost/brukernavn.";
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
                    aktivSide.set(1);
                } catch (Exception e) {
                    loginFeil = "Ugyldig fødselsdato format. Bruk YYYY-MM-DD (f.eks. 2000-01-15)";
                }
            }
            }
            ImGui.popStyleColor(3);
        }
        
        ImGui.dummy(0, 30);
        
        // Toggle between login/register
        ImGui.dummy(20, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
        if (!erRegistreringsmodus) {
            ImGui.text("Har du ikke en konto?");
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
            if (ImGui.smallButton("Registrer deg")) {
                erRegistreringsmodus = true;
                loginFeil = "";
            }
            ImGui.popStyleColor();
        } else {
            ImGui.text("Har du allerede en konto?");
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
            if (ImGui.smallButton("Logg inn")) {
                erRegistreringsmodus = false;
                loginFeil = "";
            }
            ImGui.popStyleColor();
        }
        ImGui.popStyleColor();

        ImGui.end();
        ImGui.popStyleColor();
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

    // ==================== HEADER ====================
    
    private void tegnHeader() {
        float headerHeight = 70;
        float screenWidth = ImGui.getIO().getDisplaySizeX();
        ImGui.setNextWindowSize(screenWidth, headerHeight);
        ImGui.setNextWindowPos(0, 0);
        
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        ImGui.begin("##header", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove);
        
        ImGui.dummy(0, 15);
        
        // Title
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
        ImGui.setWindowFontScale(1.5f);
        ImGui.text("Knute");
        ImGui.setWindowFontScale(1.0f);
        ImGui.popStyleColor();
        
        // Profil button (top right)
        ImGui.sameLine();
        float profilButtonWidth = 50;
        ImGui.dummy(screenWidth - 180, 0);
        ImGui.sameLine();
        
        ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL_DARK[0] * 0.9f, COLOR_TEAL_DARK[1] * 0.9f, COLOR_TEAL_DARK[2] * 0.9f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
        if (ImGui.button("P", profilButtonWidth, profilButtonWidth)) {
            aktivSide.set(4);
        }
        ImGui.popStyleColor(3);
        
        ImGui.end();
        ImGui.popStyleColor();
    }

    // ==================== HJEM ====================
    
    private void tegnHjem() {
        float windowWidth = 900;
        ImGui.setNextWindowSize(windowWidth, 700);
        ImGui.setNextWindowPos(100, 50);
        
        ImGui.begin("Hjem", imgui.flag.ImGuiWindowFlags.NoResize);
        
        ImGui.dummy(0, 10);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        String velkomstTekst = "Velkommen, " + (valgtBrukerNavn != null ? valgtBrukerNavn : "gjest") + "!";
        ImGui.text(velkomstTekst);
        ImGui.popStyleColor();
        ImGui.separator();
        ImGui.dummy(0, 15);
        
        // Next journey section
        ImGui.beginChild("NesteReise", windowWidth - 20, 250, true);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.text("Din neste reise:");
        ImGui.popStyleColor();
        ImGui.separator();
        ImGui.dummy(0, 10);
        
        if (!funnetReiser.isEmpty()) {
            Reiseforslag neste = funnetReiser.get(0);
            ImGui.textWrapped(neste.toString());
            ImGui.dummy(0, 10);
            
            if (ImGui.button("Se detaljer i Reise", 200, 35)) {
                // Forhåndsutfyll Reise med valgt reise og naviger dit
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
                aktivSide.set(1); // Reise-siden
            }
        } else {
            ImGui.text("Ingen reise planlagt.");
            ImGui.dummy(0, 5);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.7f, 0.7f, 0.7f, 1.0f);
            ImGui.text("Bruk 'Reise' for å søke etter reiser.");
            ImGui.popStyleColor();
        }
        ImGui.endChild();
        
        ImGui.dummy(0, 15);
        
        // Active tickets section
        ImGui.beginChild("AktiveBilletter", windowWidth - 20, 220, true);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
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
                ImGui.text("Gå til 'Billett' for å kjøpe.");
                ImGui.popStyleColor();
            }
        } else {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 0.6f, 0.0f, 1.0f);
            ImGui.text("Logg inn for å se dine billetter.");
            ImGui.popStyleColor();
        }
        ImGui.endChild();
        
        ImGui.end();
    }    // ==================== REISE (MODERNE RUTER-STIL) ====================
    
    private void tegnReise() {
        float screenWidth = ImGui.getIO().getDisplaySizeX();
        float screenHeight = ImGui.getIO().getDisplaySizeY();
        float headerHeight = 70;
        float footerHeight = 80;
        float contentHeight = screenHeight - headerHeight - footerHeight;
        
        ImGui.setNextWindowSize(screenWidth, contentHeight);
        ImGui.setNextWindowPos(0, headerHeight);
        
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.96f, 0.96f, 0.96f, 1.0f); // Lys grå bakgrunn
        ImGui.begin("Reise", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoTitleBar);

        ImGui.dummy(0, 15);
        
        // Clean input section matching sketch
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.beginChild("InputSection", screenWidth - 30, 200, true, ImGuiWindowFlags.NoScrollbar);
        ImGui.dummy(0, 10);
        
        // FRA
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.text("FRA");
        ImGui.popStyleColor();
        ImGui.setNextItemWidth(screenWidth - 50);
        if (stoppestedNavn != null && stoppestedNavn.length > 0) {
            ImGui.combo("##frastopp", valgtFraStopp, stoppestedNavn);
        }
        ImGui.dummy(0, 8);
        
        // TIL
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.text("TIL");
        ImGui.popStyleColor();
        ImGui.setNextItemWidth(screenWidth - 50);
        if (stoppestedNavn != null && stoppestedNavn.length > 0) {
            ImGui.combo("##tilstopp", valgtTilStopp, stoppestedNavn);
        }
        ImGui.dummy(0, 8);
        
        // TID
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.text("TID");
        ImGui.popStyleColor();
        ImGui.setNextItemWidth((screenWidth - 60) / 2);
        ImGui.inputText("##avreisetid", avreiseTidInput);
        ImGui.sameLine();
        ImGui.setNextItemWidth((screenWidth - 60) / 2);
        ImGui.inputText("##avreisedato", avreiseDatoInput);
        
        ImGui.dummy(0, 5);
        ImGui.endChild();
        
        ImGui.dummy(0, 10);
        
        // Search button
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
        if (ImGui.button("SOK", screenWidth - 30, 45)) {
            sokEtterReiser();
        }
        ImGui.popStyleColor(3);
        
        ImGui.dummy(0, 15);
        
        // Siste søk section
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
        ImGui.text("🕒 Siste søk");
        ImGui.popStyleColor();
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.beginChild("SisteSok", screenWidth - 30, 80, true);
        if (sisteReiseSok.isEmpty()) {
            ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
            ImGui.text("Ingen tidligere søk");
            ImGui.popStyleColor();
        } else {
            for (String sok : sisteReiseSok) {
                ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
                ImGui.bulletText(sok);
                ImGui.popStyleColor();
            }
        }
        ImGui.endChild();
        
        ImGui.dummy(0, 10);
        
        // Favoritter section
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
        ImGui.text("FAVORITTER:");
        ImGui.popStyleColor();
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.beginChild("Favoritter", screenWidth - 30, 80, true);
        if (favorittStoppesteder.isEmpty()) {
            ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
            ImGui.text("Ingen favoritter lagt til");
            ImGui.popStyleColor();
        } else {
            for (String fav : favorittStoppesteder) {
                ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.2f);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.4f);
                ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
                if (ImGui.button("* " + fav + "##fav" + fav)) {
                    // Set as "Fra" stop
                    for (int i = 0; i < stoppestedNavn.length; i++) {
                        if (stoppestedNavn[i].equals(fav)) {
                            valgtFraStopp.set(i);
                            break;
                        }
                    }
                }
                ImGui.popStyleColor(3);
                ImGui.sameLine();
            }
        }
        ImGui.endChild();
        
        ImGui.dummy(0, 10);

        visReiseforslag();

        ImGui.end();
        ImGui.popStyleColor();
    }
    
    private void sokEtterReiser() {
        funnetReiser.clear();
        feilmeldingSok = "";
        visAlleReiser = false;
        
        if (datadepot == null || datadepot.hentStoppesteder() == null) {
            feilmeldingSok = "Data ikke lastet enda. Vent litt...";
            return;
        }
        
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
            } else {
                // Add to search history
                String sokString = fra.navn + " → " + til.navn + " (" + onsketTid.toString().substring(0, 5) + ")";
                sisteReiseSok.add(0, sokString); // Add to beginning
                if (sisteReiseSok.size() > 5) {
                    sisteReiseSok.remove(5); // Keep max 5 searches
                }
            }
            
        } catch (Exception e) {
            feilmeldingSok = "Ugyldig tid eller dato format. Bruk HH:MM og YYYY-MM-DD (f.eks. 12:30 og 2025-11-15)";
            System.err.println("Feil ved parsing av tid/dato eller søk: " + e.getMessage());
        }
    }
    
    private void visReiseforslag() {
        if (!feilmeldingSok.isEmpty()) {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
            ImGui.textWrapped(feilmeldingSok);
            ImGui.popStyleColor();
        } else if (funnetReiser.isEmpty()) {
            ImGui.text("Ingen reiseforslag funnet.");
        } else {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
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
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
                    ImGui.text("Reiserute:");
                    ImGui.popStyleColor();
                    ImGui.dummy(0, 5);
                    
                    if (fraIndex != -1 && tilIndex != -1) {
                        // "Vis på kart" button
                        ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
                        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], 1.0f);
                        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
                        if (ImGui.button("Vis pa kart##kart" + i, 150, 35)) {
                            // Vis rute på kartet via KartRenderer
                            if (kartRenderer != null) {
                                kartRenderer.visReisePaKart(rf, valgtFraStopp.get(), valgtTilStopp.get());
                            }
                            
                            // Sentrer kartet på fra-stopp
                            Stoppested fromStop = datadepot.hentStoppesteder().get(valgtFraStopp.get());
                            if (kartRenderer != null) {
                                kartRenderer.sentrerPa(fromStop.getLatitude(), fromStop.getLongitude());
                            }
                            
                            // Bytt til Kart-side
                            aktivSide.set(2);
                        }
                        ImGui.popStyleColor(3);
                        ImGui.dummy(0, 5);
                        
                        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
                        for (int j = fraIndex; j <= tilIndex; j++) {
                            LocalTime tidForStopp = navigasjonstjeneste.beregnEstimertAnkomst(forsteAvgang.tidspunkt, fraIndex, j);
                            String stopIcon = (j == fraIndex) ? "🚏 [START]" : (j == tilIndex) ? "🏁 [SLUTT]" : "   •";
                            ImGui.text(stopIcon + " " + tidForStopp.toString() + " - " + rute.stopp.get(j).navn);
                        }
                        ImGui.popStyleColor();
                        
                        // Show transfer info
                        if (rf.avganger.size() > 1) {
                            ImGui.dummy(0, 5);
                            ImGui.separator();
                            ImGui.dummy(0, 5);
                            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 0.6f, 0.0f, 1.0f);
                            ImGui.text("⚠ Bytte kreves:");
                            ImGui.popStyleColor();
                            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
                            for (int k = 1; k < rf.avganger.size(); k++) {
                                Avgang ekstraAvgang = rf.avganger.get(k);
                                ImGui.text("  → Bytt til rute " + ekstraAvgang.ruteID + " kl. " + ekstraAvgang.tidspunkt);
                            }
                            ImGui.popStyleColor();
                        }
                        
                        // Show all matching departures for this route
                        ImGui.dummy(0, 10);
                        ImGui.separator();
                        ImGui.dummy(0, 5);
                        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
                        ImGui.text("Andre avganger på denne ruten:");
                        ImGui.popStyleColor();
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
                        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
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
                            ImGui.popStyleColor();
                            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.6f, 0.6f, 0.6f, 1.0f);
                            ImGui.text("  Ingen andre avganger funnet");
                            ImGui.popStyleColor();
                        } else {
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

    // ==================== KART ====================
    
    private void tegnKart() {
        float screenWidth = ImGui.getIO().getDisplaySizeX();
        float screenHeight = ImGui.getIO().getDisplaySizeY();
        float headerHeight = 70;
        float footerHeight = 80;
        float contentHeight = screenHeight - headerHeight - footerHeight;
        
        ImGui.setNextWindowSize(screenWidth, contentHeight);
        ImGui.setNextWindowPos(0, headerHeight);
        
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COLOR_LIGHT_BG[0], COLOR_LIGHT_BG[1], COLOR_LIGHT_BG[2], 1.0f);
        ImGui.begin("Kart", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoTitleBar);
        
        ImGui.dummy(0, 5);
        
        // Zoom controls and location button
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
        
        if (ImGui.button("Min posisjon", 140, 35)) {
            // Center on filtered user position
            double[] brukerPos = hentGlattetPosisjon();
            kartCenterLat = brukerPos[0];
            kartCenterLon = brukerPos[1];
            kartZoom = 2.0f;
        }
        ImGui.sameLine();
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        if (ImGui.button("➕", 35, 35)) {
            kartZoom = Math.min(3.0f, kartZoom + 0.25f);
        }
        ImGui.sameLine();
        if (ImGui.button("➖", 35, 35)) {
            kartZoom = Math.max(0.5f, kartZoom - 0.25f);
        }
        
        // Show "Clear route" button if visualizing route
        if (visRutepaKart) {
            ImGui.sameLine();
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Button, 0.8f, 0.3f, 0.3f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.9f, 0.4f, 0.4f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
            if (ImGui.button("✕ Fjern rute", 120, 35)) {
                visRutepaKart = false;
                valgtReiseforslag = null;
                ruteFraStoppestedIndex = -1;
                ruteTilStoppestedIndex = -1;
            }
            ImGui.popStyleColor(3);
        }
        ImGui.popStyleColor(3);
        
        ImGui.dummy(0, 5);
        
        // Map canvas (70% of content)
        float mapHeight = contentHeight * 0.70f - 50;
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.beginChild("MapCanvas", screenWidth - 20, mapHeight, true);
        if (kartRenderer != null) {
            kartRenderer.tegnKart(screenWidth - 20, mapHeight);
        }
        ImGui.endChild();
        
        ImGui.dummy(0, 5);
        
        // Stoppested list below map (30% of content) - VERTICAL scrolling
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.text("🚏 Stoppesteder");
        ImGui.popStyleColor();
        
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.beginChild("StopList", screenWidth - 20, contentHeight * 0.30f - 70, true, ImGuiWindowFlags.AlwaysVerticalScrollbar);
        
        if (stoppestedNavn != null && datadepot != null && datadepot.hentStoppesteder() != null) {
            java.util.List<Stoppested> stoppesteder = datadepot.hentStoppesteder();
            
            // Display stops VERTICALLY (one per row)
            for (int i = 0; i < stoppesteder.size(); i++) {
                Stoppested stopp = stoppesteder.get(i);
                boolean isSelected = valgtStoppested.get() == i;
                boolean isFavoritt = favorittStoppesteder.contains(stopp.navn);
                
                // Stop card - FULL WIDTH
                ImGui.beginGroup();
                if (isSelected) {
                    ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.2f);
                    ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.3f);
                    ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
                } else {
                    ImGui.pushStyleColor(ImGuiCol.Button, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], 1.0f);
                    ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_LIGHT_BG[0], COLOR_LIGHT_BG[1], COLOR_LIGHT_BG[2], 1.0f);
                    ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
                }
                
                String favorittIcon = isFavoritt ? "*" : "";
                if (ImGui.button(favorittIcon + " 🚏 " + stopp.navn + "##stop" + i, screenWidth - 50, 45)) {
                    valgtStoppested.set(i);
                    
                    // Center map on selected stop and zoom in
                    kartCenterLat = stopp.getLatitude();
                    kartCenterLon = stopp.getLongitude();
                    kartZoom = 2.0f;
                }
                ImGui.popStyleColor(3);
                
                // Show departures BELOW (not horizontally) if selected
                if (isSelected && stopp.hentAvganger() != null && !stopp.hentAvganger().isEmpty()) {
                    ImGui.dummy(0, 5);
                    ImGui.indent(15);
                    
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
                    ImGui.text("Avganger:");
                    ImGui.popStyleColor();
                    ImGui.dummy(0, 3);
                    
                    // Group by route
                    java.util.Map<Integer, java.util.List<Avgang>> ruteMap = new java.util.HashMap<>();
                    for (Avgang avg : stopp.hentAvganger()) {
                        ruteMap.computeIfAbsent(avg.ruteID, k -> new java.util.ArrayList<>()).add(avg);
                    }
                    
                    // Show each route with its departures
                    for (java.util.Map.Entry<Integer, java.util.List<Avgang>> entry : ruteMap.entrySet()) {
                        int ruteID = entry.getKey();
                        java.util.List<Avgang> avganger = entry.getValue();
                        float[] ruteColor = GPSTjeneste.getRouteColor(ruteID);
                        
                        // Route badge
                        ImGui.pushStyleColor(ImGuiCol.Button, ruteColor[0], ruteColor[1], ruteColor[2], 1.0f);
                        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ruteColor[0], ruteColor[1], ruteColor[2], 1.0f);
                        ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
                        ImGui.button("■ " + ruteID, 60, 25);
                        ImGui.popStyleColor(3);
                        
                        // Show first 5 departures horizontally on SAME line as badge
                        ImGui.sameLine();
                        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
                        for (int j = 0; j < Math.min(5, avganger.size()); j++) {
                            if (j > 0) ImGui.sameLine();
                            ImGui.text(avganger.get(j).tidspunkt.toString());
                        }
                        if (avganger.size() > 5) {
                            ImGui.sameLine();
                            ImGui.pushStyleColor(ImGuiCol.Text, 0.6f, 0.6f, 0.6f, 1.0f);
                            ImGui.text("...");
                            ImGui.popStyleColor();
                        }
                        ImGui.popStyleColor();
                    }
                    
                    ImGui.unindent(15);
                    ImGui.dummy(0, 5);
                    
                    // Add favorite button
                    ImGui.indent(15);
                    ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.8f);
                    ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
                    ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
                    if (isFavoritt) {
                        if (ImGui.button("- Fjern favoritt##fav" + i, 150, 30)) {
                            favorittStoppesteder.remove(stopp.navn);
                        }
                    } else {
                        if (ImGui.button("+ Legg til favoritt##fav" + i, 150, 30)) {
                            if (!favorittStoppesteder.contains(stopp.navn)) {
                                favorittStoppesteder.add(stopp.navn);
                            }
                        }
                    }
                    ImGui.popStyleColor(3);
                    ImGui.unindent(15);
                }
                
                ImGui.endGroup();
                // NO ImGui.sameLine() here - each stop on NEW line!
                ImGui.dummy(0, 5);
            }
        }
        
        ImGui.endChild();
        
        ImGui.end();
        ImGui.popStyleColor();
    }
    
    private void tegnKartVisualisering(float canvasWidth, float canvasHeight) {
        imgui.ImDrawList drawList = ImGui.getWindowDrawList();
        float[] cursorPos = {ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY()};
        
        // Enkelt kart-bakgrunn (lys blå vann)
        int waterColor = ImGui.getColorU32(0.70f, 0.85f, 0.95f, 1.0f);
        drawList.addRectFilled(cursorPos[0], cursorPos[1], 
                               cursorPos[0] + canvasWidth, cursorPos[1] + canvasHeight, waterColor);
        
        // Land (beige)
        int landColor = ImGui.getColorU32(0.93f, 0.94f, 0.89f, 1.0f);
        float landMargin = 30;
        drawList.addRectFilled(cursorPos[0] + landMargin, cursorPos[1] + landMargin, 
                               cursorPos[0] + canvasWidth - landMargin, 
                               cursorPos[1] + canvasHeight - landMargin, landColor);
        
        if (datadepot == null || datadepot.hentStoppesteder() == null) return;
        
        // Handle mouse interactions
        if (ImGui.isWindowHovered()) {
            // Mouse wheel zoom
            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel != 0) {
                kartZoom += wheel * 0.1f;
                kartZoom = Math.max(0.5f, Math.min(3.0f, kartZoom));
            }
            
            // Click and drag to pan
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
                    
                    // Convert pixel drag to GPS coordinates
                    kartCenterLon = kartDragStartLon - (dx / (kartZoom * 10000 * Math.cos(Math.toRadians(kartCenterLat))));
                    kartCenterLat = kartDragStartLat + (dy / (kartZoom * 10000));
                }
            } else {
                kartDragging = false;
            }
        }
        
        // Oppdater buss-posisjoner (animasjon)
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
        
        // Tegn ruter (fargede linjer)
        for (Rute rute : datadepot.hentRuter()) {
            float[] ruteColor = GPSTjeneste.getRouteColor(rute.id);
            boolean isSelectedRoute = visRutepaKart && valgtReiseforslag != null && 
                                     valgtReiseforslag.avganger.get(0).ruteID == rute.id;
            
            float lineWidth = isSelectedRoute ? 8.0f : 4.0f;
            float alpha = isSelectedRoute ? 1.0f : 0.7f;
            int lineColor = ImGui.getColorU32(ruteColor[0], ruteColor[1], ruteColor[2], alpha);
            int shadowColor = ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.2f);
            
            for (int i = 0; i < rute.stopp.size() - 1; i++) {
                Stoppested s1 = rute.stopp.get(i);
                Stoppested s2 = rute.stopp.get(i + 1);
                
                // If this is selected route, only draw the segment between from and to stops
                if (isSelectedRoute) {
                    Stoppested fraStopp = datadepot.hentStoppesteder().get(ruteFraStoppestedIndex);
                    Stoppested tilStopp = datadepot.hentStoppesteder().get(ruteTilStoppestedIndex);
                    int fraIndex = rute.stopp.indexOf(fraStopp);
                    int tilIndex = rute.stopp.indexOf(tilStopp);
                    
                    // Only draw if this segment is part of the journey
                    if (i < fraIndex || i >= tilIndex) {
                        continue;  // Skip this segment
                    }
                }
                
                float[] pos1 = GPSTjeneste.convertGPSToScreen(s1.getLatitude(), s1.getLongitude(),
                                                             kartCenterLat, kartCenterLon, 
                                                             kartZoom, canvasWidth, canvasHeight);
                float[] pos2 = GPSTjeneste.convertGPSToScreen(s2.getLatitude(), s2.getLongitude(),
                                                             kartCenterLat, kartCenterLon, 
                                                             kartZoom, canvasWidth, canvasHeight);
                
                // Draw shadow first (offset by 2 pixels)
                drawList.addLine(cursorPos[0] + pos1[0] + 2, cursorPos[1] + pos1[1] + 2,
                               cursorPos[0] + pos2[0] + 2, cursorPos[1] + pos2[1] + 2, 
                               shadowColor, lineWidth + 1);
                
                // Draw main route line
                drawList.addLine(cursorPos[0] + pos1[0], cursorPos[1] + pos1[1],
                               cursorPos[0] + pos2[0], cursorPos[1] + pos2[1], 
                               lineColor, lineWidth);
                
                // Vis distanse på valgt rute
                if (isSelectedRoute && kartZoom > 1.2f) {
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
            }
            
            // Tegn animert buss
            if (kartZoom > 1.0f) {
                double busPos = bussPosisjoner.getOrDefault(rute.id, 0.0);
                int segIdx = (int)busPos;
                double progress = busPos - segIdx;
                
                if (segIdx < rute.stopp.size() - 1) {
                    Stoppested fra = rute.stopp.get(segIdx);
                    Stoppested til = rute.stopp.get(segIdx + 1);
                    
                    // Kalkuler buss-posisjon
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
                    // Buss
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
        }
        
        // Draw stops with better styling
        java.util.List<Stoppested> stoppesteder = datadepot.hentStoppesteder();
        float stopRadius = 8.0f + (kartZoom * 2.0f);
        
        for (int i = 0; i < stoppesteder.size(); i++) {
            Stoppested stopp = stoppesteder.get(i);
            float[] pos = GPSTjeneste.convertGPSToScreen(stopp.getLatitude(), stopp.getLongitude(),
                                                        kartCenterLat, kartCenterLon, 
                                                        kartZoom, canvasWidth, canvasHeight);
            
            float screenX = cursorPos[0] + pos[0];
            float screenY = cursorPos[1] + pos[1];
            
            // Check if mouse is hovering over this stop
            float mouseX = ImGui.getMousePosX();
            float mouseY = ImGui.getMousePosY();
            float distToMouse = (float)Math.sqrt((mouseX - screenX) * (mouseX - screenX) + 
                                                (mouseY - screenY) * (mouseY - screenY));
            
            boolean isHovered = distToMouse < stopRadius + 5;
            boolean isSelected = valgtStoppested.get() == i;
            
            // Check if this is start or end of journey
            boolean isStartStop = visRutepaKart && i == ruteFraStoppestedIndex;
            boolean isEndStop = visRutepaKart && i == ruteTilStoppestedIndex;
            
            // Click detection
            if (isHovered && ImGui.isMouseClicked(0)) {
                valgtStoppested.set(i);
                isSelected = true;
            }
            
            // Draw stop circle with special markers for start/end
            // Add shadow for all stops
            int shadowColor = ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.3f);
            drawList.addCircleFilled(screenX + 2, screenY + 2, stopRadius + 2, shadowColor, 12);
            
            if (isStartStop) {
                // Start stop: Green circle with larger size and icon
                int startColor = ImGui.getColorU32(0.2f, 0.8f, 0.3f, 1.0f);
                int startOutline = ImGui.getColorU32(0.15f, 0.6f, 0.2f, 1.0f);
                drawList.addCircleFilled(screenX, screenY, stopRadius + 8, startColor, 16);
                drawList.addCircle(screenX, screenY, stopRadius + 8, startOutline, 16, 3.0f);
                drawList.addCircle(screenX, screenY, stopRadius + 10, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 16, 2.0f);
                
                // Draw "START" label with shadow
                float labelX = screenX + stopRadius + 12;
                float labelY = screenY - 12;
                String label = "🚏 START: " + stopp.navn;
                float labelWidth = ImGui.calcTextSize(label).x + 12;
                float labelHeight = ImGui.calcTextSize(label).y + 8;
                
                // Label shadow
                drawList.addRectFilled(labelX + 2, labelY + 2, labelX + labelWidth + 2, labelY + labelHeight + 2,
                                      ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.2f), 4.0f);
                drawList.addRectFilled(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                      ImGui.getColorU32(0.2f, 0.8f, 0.3f, 0.98f), 4.0f);
                drawList.addRect(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 4.0f, 0, 2.0f);
                drawList.addText(labelX + 6, labelY + 4, 
                               ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), label);
            } else if (isEndStop) {
                // End stop: Red circle with larger size and icon
                int endColor = ImGui.getColorU32(0.9f, 0.2f, 0.2f, 1.0f);
                int endOutline = ImGui.getColorU32(0.7f, 0.1f, 0.1f, 1.0f);
                drawList.addCircleFilled(screenX, screenY, stopRadius + 8, endColor, 16);
                drawList.addCircle(screenX, screenY, stopRadius + 8, endOutline, 16, 3.0f);
                drawList.addCircle(screenX, screenY, stopRadius + 10, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 16, 2.0f);
                
                // Draw "SLUTT" label with shadow
                float labelX = screenX + stopRadius + 12;
                float labelY = screenY - 12;
                String label = "🏁 SLUTT: " + stopp.navn;
                float labelWidth = ImGui.calcTextSize(label).x + 12;
                float labelHeight = ImGui.calcTextSize(label).y + 8;
                
                // Label shadow
                drawList.addRectFilled(labelX + 2, labelY + 2, labelX + labelWidth + 2, labelY + labelHeight + 2,
                                      ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.2f), 4.0f);
                drawList.addRectFilled(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                      ImGui.getColorU32(0.9f, 0.2f, 0.2f, 0.98f), 4.0f);
                drawList.addRect(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 4.0f, 0, 2.0f);
                drawList.addText(labelX + 6, labelY + 4, 
                               ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), label);
            } else if (isSelected) {
                // Selected: larger teal circle with white border
                int selectedColor = ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
                drawList.addCircleFilled(screenX, screenY, stopRadius + 5, selectedColor, 16);
                drawList.addCircle(screenX, screenY, stopRadius + 5, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 16, 3.0f);
                
                // Inner white dot
                drawList.addCircleFilled(screenX, screenY, stopRadius - 2, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 12);
                
                // Draw label with shadow
                float labelX = screenX + stopRadius + 8;
                float labelY = screenY - 10;
                String label = stopp.navn;
                float labelWidth = ImGui.calcTextSize(label).x + 12;
                float labelHeight = ImGui.calcTextSize(label).y + 8;
                
                // Label shadow
                drawList.addRectFilled(labelX + 2, labelY + 2, labelX + labelWidth + 2, labelY + labelHeight + 2,
                                      ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.2f), 4.0f);
                drawList.addRectFilled(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                      ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.98f), 4.0f);
                drawList.addRect(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f), 0, 0, 1.5f);
                drawList.addText(labelX + 5, labelY + 3, 
                               ImGui.getColorU32(0.2f, 0.2f, 0.2f, 1.0f), label);
            } else if (isHovered) {
                // Hovered: highlighted
                int hoverColor = ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.8f);
                drawList.addCircleFilled(screenX, screenY, stopRadius + 2, hoverColor, 12);
                
                // Draw label
                float labelX = screenX + stopRadius + 5;
                float labelY = screenY - 8;
                String label = stopp.navn;
                float labelWidth = ImGui.calcTextSize(label).x + 10;
                float labelHeight = ImGui.calcTextSize(label).y + 6;
                
                drawList.addRectFilled(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                      ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.95f));
                drawList.addRect(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f), 4.0f, 0, 2.0f);
                drawList.addText(labelX + 6, labelY + 4, 
                               ImGui.getColorU32(0.2f, 0.2f, 0.2f, 1.0f), label);
            } else if (isHovered) {
                // Hovered: highlighted with pulse effect
                int hoverColor = ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.9f);
                drawList.addCircleFilled(screenX, screenY, stopRadius + 3, hoverColor, 16);
                drawList.addCircle(screenX, screenY, stopRadius + 3, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 16, 2.5f);
                
                // Draw label
                float labelX = screenX + stopRadius + 8;
                float labelY = screenY - 10;
                String label = stopp.navn;
                float labelWidth = ImGui.calcTextSize(label).x + 12;
                float labelHeight = ImGui.calcTextSize(label).y + 8;
                
                // Label shadow
                drawList.addRectFilled(labelX + 2, labelY + 2, labelX + labelWidth + 2, labelY + labelHeight + 2,
                                      ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.2f), 4.0f);
                drawList.addRectFilled(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                      ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.98f), 4.0f);
                drawList.addRect(labelX, labelY, labelX + labelWidth, labelY + labelHeight,
                                ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f), 4.0f, 0, 2.0f);
                drawList.addText(labelX + 6, labelY + 4, 
                               ImGui.getColorU32(0.2f, 0.2f, 0.2f, 1.0f), label);
            } else {
                // Normal: white circle with teal border
                drawList.addCircleFilled(screenX, screenY, stopRadius, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 12);
                drawList.addCircle(screenX, screenY, stopRadius, ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.8f), 12, 2.5f);
                
                // Small inner dot
                drawList.addCircleFilled(screenX, screenY, stopRadius * 0.4f, 
                                       ImGui.getColorU32(COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.6f), 8);
            }
        }
        
        // Tegn brukerposisjon (Kalman-filter glattet GPS)
        double[] gpsPos = hentGlattetPosisjon();
        float[] screenPos = GPSTjeneste.convertGPSToScreen(gpsPos[0], gpsPos[1],
                                                           kartCenterLat, kartCenterLon,
                                                           kartZoom, canvasWidth, canvasHeight);
        float posX = cursorPos[0] + screenPos[0];
        float posY = cursorPos[1] + screenPos[1];
        
        // Pulserende ring
        float pulseR = 18.0f + (float)(Math.sin(System.currentTimeMillis() / 500.0) * 4.0f);
        drawList.addCircleFilled(posX, posY, pulseR, ImGui.getColorU32(0.2f, 0.5f, 1.0f, 0.3f), 20);
        
        // Nøyaktighetsring
        drawList.addCircleFilled(posX, posY, 15.0f, ImGui.getColorU32(0.3f, 0.6f, 1.0f, 0.2f), 20);
        drawList.addCircle(posX, posY, 15.0f, ImGui.getColorU32(0.3f, 0.6f, 1.0f, 0.5f), 20, 1.5f);
        
        // Hovedpunkt med skygge
        drawList.addCircleFilled(posX + 1, posY + 1, 10.0f, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.4f), 16);
        drawList.addCircleFilled(posX, posY, 10.0f, ImGui.getColorU32(0.2f, 0.5f, 1.0f, 1.0f), 16);
        drawList.addCircle(posX, posY, 10.0f, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 16, 3.0f);
        drawList.addCircleFilled(posX, posY, 4.0f, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 12);
        
        // Add invisible button to capture mouse events
        ImGui.invisibleButton("##mapcanvas", canvasWidth, canvasHeight);
        
        // Zoom controls
        ImGui.setCursorScreenPos(cursorPos[0] + 10, cursorPos[1] + 10);
        if (ImGui.button("+", 30, 30)) {
            kartZoom = Math.min(3.0f, kartZoom + 0.2f);
        }
        ImGui.setCursorScreenPos(cursorPos[0] + 10, cursorPos[1] + 45);
        if (ImGui.button("-", 30, 30)) {
            kartZoom = Math.max(0.5f, kartZoom - 0.2f);
        }
    }

    // ==================== BILLETT ====================
    
    private void tegnBillett() {
        float screenWidth = ImGui.getIO().getDisplaySizeX();
        float screenHeight = ImGui.getIO().getDisplaySizeY();
        float headerHeight = 70;
        float footerHeight = 80;
        float contentHeight = screenHeight - headerHeight - footerHeight;
        
        ImGui.setNextWindowSize(screenWidth, contentHeight);
        ImGui.setNextWindowPos(0, headerHeight);
        
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], 1.0f);
        ImGui.begin("Billett", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoTitleBar);
        
        if (valgtBrukerId == null) {
            ImGui.dummy(0, 50);
            ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
            ImGui.text("Ingen bruker valgt.");
            ImGui.popStyleColor();
            ImGui.end();
            ImGui.popStyleColor();
            return;
        }

        Bruker bruker = datadepot.hentBruker(valgtBrukerId);
        
        ImGui.dummy(0, 15);
        
        // AKTIVE section (matching sketch)
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
        ImGui.text("AKTIVE BILLETTER");
        ImGui.popStyleColor();
        
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.beginChild("AktiveBilletter", screenWidth - 30, 150, true);
        ImGui.dummy(0, 10);
        
        if (bruker.aktiveBilletter == null || bruker.aktiveBilletter.isEmpty()) {
            ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
            ImGui.text("Ingen aktive billetter");
            ImGui.popStyleColor();
        } else {
            for (int i = 0; i < bruker.aktiveBilletter.size(); i++) {
                Billett bil = bruker.aktiveBilletter.get(i);
                ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
                ImGui.text("✓ " + bil.type);
                ImGui.popStyleColor();
                ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
                ImGui.text("   Kjøpt: " + bil.startTid.toLocalTime() + " | Gyldig til: " + bil.sluttTid.toLocalTime());
                ImGui.sameLine();
                ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.3f);
                ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
                if (ImGui.smallButton("📱 QR##" + i)) {
                    visQRKode = !visQRKode;
                }
                ImGui.popStyleColor(2);
                ImGui.popStyleColor();
                
                // Show QR code placeholder
                if (visQRKode && i == 0) {
                    ImGui.dummy(10, 0);
                    ImGui.sameLine();
                    ImGui.beginChild("QRCode", 120, 120, true);
                    ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
                    ImGui.setCursorPosX(20);
                    ImGui.setCursorPosY(40);
                    ImGui.text("   ▄▄▄▄▄▄▄");
                    ImGui.setCursorPosX(20);
                    ImGui.text("   █     █");
                    ImGui.setCursorPosX(20);
                    ImGui.text("   █ QR  █");
                    ImGui.setCursorPosX(20);
                    ImGui.text("   █     █");
                    ImGui.setCursorPosX(20);
                    ImGui.text("   ▀▀▀▀▀▀▀");
                    ImGui.popStyleColor();
                    ImGui.endChild();
                }
                ImGui.dummy(0, 5);
            }
        }
        ImGui.endChild();
        
        ImGui.dummy(0, 15);
        
        // Kjøp for andre button
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Button, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_LIGHT_BG[0], COLOR_LIGHT_BG[1], COLOR_LIGHT_BG[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
        ImGui.pushStyleColor(ImGuiCol.Border, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.FrameBorderSize, 2.0f);
        if (ImGui.button("👥 Kjøp for andre", screenWidth - 30, 45)) {
            // Open purchase for others
        }
        ImGui.popStyleVar();
        ImGui.popStyleColor(4);
        
        ImGui.dummy(0, 10);
        
        // Hurtig kjøp / Favoritt button
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
        if (ImGui.button("⚡ Hurtig kjøp / Favoritt", screenWidth - 30, 45)) {
            // Quick purchase
        }
        ImGui.popStyleColor(3);
        
        ImGui.dummy(0, 15);

        // Kjøp billett section
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
        ImGui.text("Kjøp ny billett:");
        ImGui.popStyleColor();
        ImGui.dummy(0, 5);
        
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
        ImGui.text("Billetttype:");
        ImGui.popStyleColor();
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.setNextItemWidth(screenWidth - 30);
        ImGui.combo("##billetttype", valgtBillettType, billettTypeNavn);
        ImGui.dummy(0, 5);
        
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.text("Brukergruppe for pris:");
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.setNextItemWidth(screenWidth - 30);
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
        
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], COLOR_TEAL_DARK[3]);
        ImGui.text(priceInfo);
        ImGui.popStyleColor();
        ImGui.dummy(0, 10);
        
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL_DARK[0], COLOR_TEAL_DARK[1], COLOR_TEAL_DARK[2], 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
        if (ImGui.button("Kjøp billett", screenWidth - 20, 40)) {
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
        ImGui.popStyleColor(3);
        
        if (!feilmeldingSok.isEmpty()) {
            ImGui.dummy(0, 5);
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            if (feilmeldingSok.startsWith("✓")) {
                ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
            } else {
                ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
            }
            ImGui.textWrapped(feilmeldingSok);
            ImGui.popStyleColor();
        }

        ImGui.end();
        ImGui.popStyleColor();
    }

    // ==================== PROFIL ====================
    
    private void tegnProfil() {
        float screenWidth = ImGui.getIO().getDisplaySizeX();
        float screenHeight = ImGui.getIO().getDisplaySizeY();
        float headerHeight = 70;
        float footerHeight = 80;
        float contentHeight = screenHeight - headerHeight - footerHeight;
        
        ImGui.setNextWindowSize(screenWidth, contentHeight);
        ImGui.setNextWindowPos(0, headerHeight);
        
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COLOR_LIGHT_BG[0], COLOR_LIGHT_BG[1], COLOR_LIGHT_BG[2], 1.0f);
        ImGui.begin("Profil", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoTitleBar);
        
        if (valgtBrukerId == null) {
            ImGui.dummy(0, 10);
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.6f, 0.0f, 1.0f);
            ImGui.text("Ingen bruker logget inn.");
            ImGui.popStyleColor();
        } else {
            Bruker bruker = datadepot.hentBruker(valgtBrukerId);
            
            ImGui.dummy(0, 10);
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
            ImGui.setWindowFontScale(1.3f);
            ImGui.text("Profil");
            ImGui.setWindowFontScale(1.0f);
            ImGui.popStyleColor();
            ImGui.dummy(0, 10);
            
            // User info section
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            ImGui.beginChild("BrukerInfo", screenWidth - 20, 200, true);
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
            ImGui.text("Navn: " + bruker.navn);
            ImGui.text("ID: " + bruker.id);
            ImGui.text("Fødselsdato: " + bruker.fodselsDato);
            ImGui.text("Brukergruppe: " + bruker.brukerGruppe);
            ImGui.popStyleColor();
            ImGui.dummy(0, 20);
            
            int antallAktiveBilletter = bruker.aktiveBilletter != null ? bruker.aktiveBilletter.size() : 0;
            int antallGamleBilletter = bruker.gamleBilletter != null ? bruker.gamleBilletter.size() : 0;
            
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
            ImGui.text("Aktive billetter: " + antallAktiveBilletter);
            ImGui.popStyleColor();
            
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
            ImGui.text("Tidligere reiser: " + antallGamleBilletter);
            ImGui.popStyleColor();
            ImGui.endChild();
            
            ImGui.dummy(0, 15);
            
            // Favoritter section in profile
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
            ImGui.text("* Mine favoritt-stoppesteder (" + favorittStoppesteder.size() + "):");
            ImGui.popStyleColor();
            ImGui.dummy(0, 5);
            
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            ImGui.beginChild("FavoritterListe", screenWidth - 20, 120, true);
            if (favorittStoppesteder.isEmpty()) {
                ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
                ImGui.text("Ingen favoritter. Legg til fra Kart-siden!");
                ImGui.popStyleColor();
            } else {
                for (int i = 0; i < favorittStoppesteder.size(); i++) {
                    String fav = favorittStoppesteder.get(i);
                    ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], COLOR_TEAL[3]);
                    ImGui.text("* " + fav);
                    ImGui.popStyleColor();
                    ImGui.sameLine();
                    ImGui.pushStyleColor(ImGuiCol.Button, 0.9f, 0.3f, 0.3f, 0.3f);
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.9f, 0.3f, 0.3f, 1.0f);
                    if (ImGui.smallButton("✕##" + i)) {
                        favorittStoppesteder.remove(i);
                        break;
                    }
                    ImGui.popStyleColor(2);
                }
            }
            ImGui.endChild();
            
            ImGui.dummy(0, 15);
            
            // Settings section
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], COLOR_TEXT_DARK[3]);
            ImGui.text("Innstillinger:");
            ImGui.popStyleColor();
            ImGui.dummy(0, 5);
            
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            if (ImGui.checkbox("Vis debug-meny", visDebugMeny)) {
                visDebugMeny = !visDebugMeny;
            }
            
            ImGui.dummy(0, 20);
            
            // Logout button
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Button, 1.0f, 0.3f, 0.3f, 0.8f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 1.0f, 0.3f, 0.3f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_LIGHT[0], COLOR_TEXT_LIGHT[1], COLOR_TEXT_LIGHT[2], COLOR_TEXT_LIGHT[3]);
            if (ImGui.button("Logg ut", screenWidth - 20, 40)) {
                valgtBrukerId = null;
                valgtBrukerNavn = null;
                visLogin = false;
                visVelkomst = true;
                visDebugMeny = false;
            }
            ImGui.popStyleColor(3);
        }
        
        ImGui.end();
        ImGui.popStyleColor();
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
        float navHeight = 80;
        float screenWidth = ImGui.getIO().getDisplaySizeX();
        ImGui.setNextWindowSize(screenWidth, navHeight);
        ImGui.setNextWindowPos(0, ImGui.getIO().getDisplaySizeY() - navHeight);
        
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], 1.0f);
        ImGui.begin("##bottomnav", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove);
        
        ImGui.dummy(0, 5);
        
        float buttonWidth = (screenWidth - 40) / 3;
        float buttonHeight = 60;
        float spacing = 10;
        
        ImGui.dummy(10, 0);
        ImGui.sameLine();
        
        // Reise button with icon
        if (aktivSide.get() == 1) {
            ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.2f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.3f);
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        } else {
            ImGui.pushStyleColor(ImGuiCol.Button, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], 0.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_LIGHT_BG[0], COLOR_LIGHT_BG[1], COLOR_LIGHT_BG[2], 1.0f);
            ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
        }
        ImGui.beginGroup();
        if (ImGui.button("SOK\nReise", buttonWidth, buttonHeight)) aktivSide.set(1);
        ImGui.endGroup();
        ImGui.popStyleColor(3);
        
        ImGui.sameLine();
        ImGui.dummy(spacing, 0);
        ImGui.sameLine();
        
        // Kart button with icon
        if (aktivSide.get() == 2) {
            ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.2f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.3f);
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        } else {
            ImGui.pushStyleColor(ImGuiCol.Button, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], 0.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_LIGHT_BG[0], COLOR_LIGHT_BG[1], COLOR_LIGHT_BG[2], 1.0f);
            ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
        }
        ImGui.beginGroup();
        if (ImGui.button("MAP\nKart", buttonWidth, buttonHeight)) aktivSide.set(2);
        ImGui.endGroup();
        ImGui.popStyleColor(3);
        
        ImGui.sameLine();
        ImGui.dummy(spacing, 0);
        ImGui.sameLine();
        
        // Billett button with icon
        if (aktivSide.get() == 3) {
            ImGui.pushStyleColor(ImGuiCol.Button, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.2f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 0.3f);
            ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
        } else {
            ImGui.pushStyleColor(ImGuiCol.Button, COLOR_WHITE[0], COLOR_WHITE[1], COLOR_WHITE[2], 0.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, COLOR_LIGHT_BG[0], COLOR_LIGHT_BG[1], COLOR_LIGHT_BG[2], 1.0f);
            ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
        }
        ImGui.beginGroup();
        if (ImGui.button("TIX\nBillett", buttonWidth, buttonHeight)) aktivSide.set(3);
        ImGui.endGroup();
        ImGui.popStyleColor(3);
        
        ImGui.end();
        ImGui.popStyleColor();
    }

    // ==================== GPS & KALMAN FILTER ====================
    
    /**
     * SANNTIDSAVGANGER - Vis neste avganger fra nærmeste stopp (som Ruter/Entur)
     */
    private void tegnSanntidsAvganger(float screenWidth) {
        // Finn nærmeste stoppested
        finnNaermesteStopp();
        
        if (naermesteStopp == null) return;
        
        ImGui.dummy(15, 0);
        ImGui.sameLine();
        ImGui.beginChild("SanntidsKort", screenWidth - 30, 280, true);
        
        // Header
        ImGui.dummy(0, 10);
        ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEXT_DARK[0], COLOR_TEXT_DARK[1], COLOR_TEXT_DARK[2], 1.0f);
        ImGui.pushFont(ImGui.getFont());
        ImGui.text("POSISJON: " + naermesteStopp.navn);
        ImGui.popFont();
        ImGui.popStyleColor();
        
        ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
        ImGui.text(String.format("%.0f meter fra deg", avstandTilNaermeste * 1000));
        ImGui.popStyleColor();
        
        ImGui.dummy(0, 10);
        ImGui.separator();
        ImGui.dummy(0, 10);
        
        // Vis neste 3 avganger
        LocalTime now = LocalTime.now();
        java.util.List<Avgang> kommendeAvganger = new java.util.ArrayList<>();
        
        for (Avgang avg : naermesteStopp.avganger) {
            if (avg.tidspunkt != null && avg.tidspunkt.isAfter(now)) {
                kommendeAvganger.add(avg);
            }
        }
        
        // Sorter etter tid
        kommendeAvganger.sort((a, b) -> a.tidspunkt.compareTo(b.tidspunkt));
        
        if (kommendeAvganger.isEmpty()) {
            ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
            ImGui.text("Ingen flere avganger i dag");
            ImGui.popStyleColor();
        } else {
            int count = 0;
            for (Avgang avg : kommendeAvganger) {
                if (count >= 3) break; // Vis maks 3
                
                // Beregn minutter til avgang (LocalTime)
                long minutterTil = java.time.temporal.ChronoUnit.MINUTES.between(now, avg.tidspunkt);
                
                // Stort kort per avgang (som Ruter)
                ImGui.pushStyleColor(ImGuiCol.ChildBg, 1.0f, 1.0f, 1.0f, 1.0f);
                ImGui.beginChild("avg" + count, screenWidth - 60, 50, true);
                
                // Rutenummer (stort)
                ImGui.dummy(10, 5);
                ImGui.sameLine();
                ImGui.pushStyleColor(ImGuiCol.Text, COLOR_TEAL[0], COLOR_TEAL[1], COLOR_TEAL[2], 1.0f);
                ImGui.text("Rute " + avg.ruteID);
                ImGui.popStyleColor();
                
                // Tid til avgang (høyre side)
                ImGui.sameLine(screenWidth - 150);
                if (minutterTil < 1) {
                    ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.3f, 0.0f, 1.0f);
                    ImGui.text("NÅ");
                    ImGui.popStyleColor();
                } else if (minutterTil < 5) {
                    ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.6f, 0.0f, 1.0f);
                    ImGui.text(minutterTil + " min");
                    ImGui.popStyleColor();
                } else {
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.3f, 0.3f, 0.3f, 1.0f);
                    ImGui.text(minutterTil + " min");
                    ImGui.popStyleColor();
                }
                
                // Destinasjon
                ImGui.dummy(10, 0);
                ImGui.sameLine();
                Rute rute = datadepot.hentRute(avg.ruteID);
                if (rute != null && !rute.stopp.isEmpty()) {
                    Stoppested siste = rute.stopp.get(rute.stopp.size() - 1);
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
                    ImGui.text("→ " + siste.navn);
                    ImGui.popStyleColor();
                }
                
                ImGui.endChild();
                ImGui.popStyleColor();
                ImGui.dummy(0, 5);
                
                count++;
            }
        }
        
        ImGui.endChild();
    }
    
    /**
     * Finn nærmeste stoppested basert på GPS-posisjon
     */
    private void finnNaermesteStopp() {
        if (datadepot == null || datadepot.hentStoppesteder() == null) return;
        
        double[] pos = hentGlattetPosisjon();
        double brukerLat = pos[0];
        double brukerLon = pos[1];
        
        double minAvstand = Double.MAX_VALUE;
        Stoppested naermeste = null;
        
        for (Stoppested stopp : datadepot.hentStoppesteder()) {
            double avstand = GPSTjeneste.beregnAvstand(
                brukerLat, brukerLon,
                stopp.getLatitude(), stopp.getLongitude()
            );
            
            if (avstand < minAvstand) {
                minAvstand = avstand;
                naermeste = stopp;
            }
        }
        
        naermesteStopp = naermeste;
        avstandTilNaermeste = minAvstand;
    }
    
    /**
     * Simulate GPS updates with realistic noise
     * In production, this would be replaced with actual GPS sensor readings
     */
    private void oppdaterGPSPosisjon() {
        // Simulate GPS measurement
        rawGPSLat = 59.124;
        rawGPSLon = 11.387;
        
        if (!gpsInitialisert) {
            gpsInitialisert = true;
            System.out.println("GPS initialisert");
        }
    }
    
    /**
     * Get user position
     */
    private double[] hentGlattetPosisjon() {
        return new double[] {rawGPSLat, rawGPSLon};
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