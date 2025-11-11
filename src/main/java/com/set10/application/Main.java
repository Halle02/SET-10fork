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

public class Main extends Application {

    Navigasjonstjeneste navigasjonstjeneste;
    Datadepot datadepot;
    private Integer valgtBrukerId = null;
    private String valgtBrukerNavn = null;

    public String[] stoppestedNavn;
    public ImInt valgtFraStopp = new ImInt(0);
    public ImInt valgtTilStopp = new ImInt(0);
    public ImString avreiseTidInput = new ImString("12:30", 128);
    public List<Reiseforslag> funnetReiser = new ArrayList<>();
    public String feilmeldingSok = "";


    @Override
    protected void configure(Configuration config) {
        config.setTitle("Østfold trafikk premium");
    }

    // Denne kjøres (forhåpentligvis) 60 ganger i sekundet, og er hvor logikk for gui og lignende legges inn
    @Override
    public void process() {

        ImGui.begin("Reisesøk"); 

        if (stoppestedNavn != null && stoppestedNavn.length > 0) {
            ImGui.combo("Fra stoppested", valgtFraStopp, stoppestedNavn);
            ImGui.combo("Til stoppested", valgtTilStopp, stoppestedNavn);
        } else {
            ImGui.text("Laster stoppesteder...");
        }

        ImGui.separator();
        ImGui.text("Avreisetid (format HH:MM):");
        ImGui.inputText("##avreisetid", avreiseTidInput);
        ImGui.separator();

        ImGui.beginGroup();
        if (ImGui.button("Finn din neste reise")) {
            funnetReiser.clear(); 
            feilmeldingSok = "";  
            try {
                if (valgtFraStopp.get() < 0 || valgtFraStopp.get() >= datadepot.hentStoppesteder().size() ||
                    valgtTilStopp.get() < 0 || valgtTilStopp.get() >= datadepot.hentStoppesteder().size()) {
                    feilmeldingSok = "Ugyldig valg av stoppested.";
                } else {
                    Stoppested fra = datadepot.hentStoppesteder().get(valgtFraStopp.get());
                    Stoppested til = datadepot.hentStoppesteder().get(valgtTilStopp.get());

                    if (fra == til) {
                        feilmeldingSok = "Start- og stoppested kan ikke være det samme.";
                    } else {
                        LocalTime onsketTid = LocalTime.parse(avreiseTidInput.get());
                        LocalDateTime avreiseDatoTid = onsketTid.atDate(LocalDate.now()); 
                        Reisesok sok = new Reisesok(fra, til, avreiseDatoTid, null);
                        funnetReiser = navigasjonstjeneste.FinnReiser(sok);
                        if (funnetReiser.isEmpty()) {
                             feilmeldingSok = "Ingen reiseforslag funnet for valgt tidspunkt.";
                        }
                    }
                }

            } catch (Exception e) {
                feilmeldingSok = "Ugyldig tid format. Bruk HH:MM (f.eks. 12:30)";
                System.err.println("Feil ved parsing av tid eller søk: " + e.getMessage());
            }
        }

        ImGui.sameLine();
        if (ImGui.button("Kjøp Enkel-billett")) {
            if (valgtBrukerId == null) {
                feilmeldingSok = "Vennligst velg en bruker i 'Debug meny' for å kjøpe billett";
            } else {
                try {
                    Bruker bruker = datadepot.hentBruker(valgtBrukerId);
                    Billett nyBillett = new Billett(Billett.Type.Enkel, LocalDateTime.now());
                    datadepot.opprettBillett(nyBillett);
                    bruker.aktiveBilletter.add(nyBillett);
                    datadepot.lagreTilDisk();
                    feilmeldingSok = "Kjøpte billett for " + bruker.navn + "! Gyldig i 90 minutter.";
                    
                } catch (Exception e) {
                    feilmeldingSok = "Feil ved kjøp av billett: " + e.getMessage();
                    System.err.println("Feil ved kjøp av billett: " + e);
                }
            }
        }
        ImGui.endGroup();

        ImGui.separator();
        if (!feilmeldingSok.isEmpty()) {
            ImGui.text(feilmeldingSok);
        } else if (funnetReiser.isEmpty() && feilmeldingSok.isEmpty()) {
            ImGui.text("Ingen reiseforslag funnet. Søk etter en reise.");
        } else if (!funnetReiser.isEmpty()) { // Viser kun den neste mulige reisen i listen
            ImGui.text("Neste reise:"); 
            ImGui.textWrapped(funnetReiser.get(0).toString()); 
        }
        ImGui.end();


        ImGui.begin("Debug meny");

        // ----------------------------------------------------
        // Bruker-valg Combo-boks
        // ----------------------------------------------------

        ImGui.setNextItemWidth(220);
        // Velger bruker
        if (ImGui.beginCombo("##brukerCombo" , valgtBrukerNavn == null ? "Velg bruker" : valgtBrukerNavn)) {

            for (Bruker b : datadepot.hentBrukere()) {
                String navn = b.navn;
                boolean selected = (valgtBrukerId != null && valgtBrukerId == b.id);

                if (ImGui.selectable(navn, selected)) {
                    valgtBrukerId = b.id;
                    valgtBrukerNavn = navn; // Bruker kun navnet for visning i combo
                }

                if (selected)
                    ImGui.setItemDefaultFocus();
            }
            ImGui.endCombo();
        }
        ImGui.sameLine();


        if (valgtBrukerId != null) {
            
            Bruker valgtBruker = null;

            try {
                 valgtBruker = datadepot.hentBruker(valgtBrukerId);
            } catch (IndexOutOfBoundsException e) {
                 System.err.println("Fant ikke bruker med ID: " + valgtBrukerId + ". ID er ute av sync.");
                 valgtBrukerId = null; 
                 valgtBrukerNavn = null;
                 ImGui.text("FEIL: Klarte ikke hente brukerdata. Velg bruker på nytt.");
                 ImGui.end(); 
                 return; // Avslutter hvis vi ikke finner brukeren
            }
            
            if (valgtBruker != null) {

                ImGui.text("Logget inn som: " + valgtBrukerNavn + " (ID: " + valgtBrukerId + ")");
                ImGui.separator();

                // Ruter
                ImGui.spacing();
                if (ImGui.collapsingHeader("Ruter")) {
                    ImGui.separator();
                    for (Rute rute : datadepot.hentRuter()) {
                        if (ImGui.treeNode(rute.toString())) {
                            for (Stoppested stopp : rute.stopp) {
                                ImGui.text(stopp.toString());
                            }
                            ImGui.treePop();
                        }
                        ImGui.separator();
                    }
                }

                // Stoppesteder
                if (ImGui.collapsingHeader("Stoppesteder")) {
                    ImGui.separator();
                    for (Stoppested stoppested : datadepot.hentStoppesteder()) {
                        if (ImGui.treeNode(stoppested.toString())) { 
                            for (Rute rute : datadepot.hentRuter()) {
                                if (rute.stopp.contains(stoppested)) { 
                                    if (ImGui.treeNode("Rute " + rute.id + ": " + rute.stopp.get(0).navn + " til " + rute.stopp.get(rute.stopp.size() - 1).navn)) {
                                        for (Avgang a : stoppested.hentAvganger()) {
                                            if (a.ruteID == rute.id) {
                                                ImGui.text(a.toString());
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
                
                // Billetter
                if (ImGui.collapsingHeader("Billetter")) {
                    ImGui.separator();
                    if (valgtBruker.aktiveBilletter.isEmpty()) {
                        ImGui.text("Brukeren har ingen aktive billetter.");
                    } else {
                        ImGui.text("Aktive billetter for " + valgtBruker.navn + ":");
                        for (Billett billett : valgtBruker.aktiveBilletter) {
                            ImGui.text(billett.toString());
                            ImGui.sameLine();
                            ImGui.text("(Gyldig til: " + billett.sluttTid.toLocalTime() + ")"); 
                        }
                    }
                    ImGui.spacing();
                    if (!valgtBruker.gamleBiletter.isEmpty()) {
                         if (ImGui.treeNode("Tidligere billetter")) {
                            for (Billett billett : valgtBruker.gamleBiletter) {
                                ImGui.text(billett.toString());
                            }
                            ImGui.treePop();
                         }
                    }
                }
            }
        }

        ImGui.end();

        // uncomment hvis du vil se mer på hva imgui kan gjøre.
        // ImGui.showDemoWindow();
    }

    // Dette er initialiseringskode, som kjøres før oppstart av programmet.
    @Override
    protected void preRun(){

        datadepot = new Datadepot(new DatabaseText());
        datadepot.opprettDummydata();
        
        navigasjonstjeneste = new Navigasjonstjeneste();
        navigasjonstjeneste.dataDepot = datadepot;


        if (datadepot.hentStoppesteder() != null) {
             stoppestedNavn = datadepot.hentStoppesteder().stream()
                                 .map(s -> s.navn)
                                 .toArray(String[]::new);
        } else { stoppestedNavn = new String[0];}
        

        try{datadepot.lagreTilDisk();}
        catch(Exception e){
             System.err.println("ERROR: Kan ikke lagre til fil " + e);
           }

        try{datadepot.lasteFraDisk();}
        catch(Exception e){
             System.err.println("ERROR: Kan ikke laste inn fra fil " + e); 
        }
        
    }

    protected void stop() {
        try {
            datadepot.lagreTilDisk();
            System.out.println("Data lagret automatisk ved lukking");
        } catch (Exception e) {
            System.err.println("Feil ved automatisk lagring: " + e.getMessage());
        }

    }
    
    // Starter bare applikasjonen. Burde kanskje ikke røres
    public static void main(String[] args) {
        launch(new Main());
    }
}