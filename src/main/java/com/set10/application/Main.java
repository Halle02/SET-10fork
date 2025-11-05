package com.set10.application;


import com.set10.core.Datadepot;

import com.set10.database.DatabaseText;


import com.set10.core.Navigasjonstjeneste;
import com.set10.core.Stoppested;
import com.set10.core.Rute;
import com.set10.core.Avgang;
import com.set10.database.DatabaseText;
import com.set10.core.Reiseforslag;
import com.set10.core.Reisesok;
import com.set10.core.Stoppested;
import com.set10.core.Rute;

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

        if (ImGui.button("Finn din neste reise")) {
            funnetReiser.clear(); 
            feilmeldingSok = "";  
            try {
                Stoppested fra = datadepot.hentStoppesteder().get(valgtFraStopp.get());
                Stoppested til = datadepot.hentStoppesteder().get(valgtTilStopp.get());

                if (fra == til) {
                    feilmeldingSok = "Start- og stoppested kan ikke være det samme.";
                } else {
                    LocalTime onsketTid = LocalTime.parse(avreiseTidInput.get());
                    LocalDateTime avreiseDatoTid = LocalDateTime.of(LocalDate.now(), onsketTid);
                    Reisesok sok = new Reisesok(fra, til, avreiseDatoTid, null);
                    funnetReiser = navigasjonstjeneste.FinnReiser(sok);
                }

            } catch (Exception e) {
                feilmeldingSok = "Ugyldig tid format. Bruk HH:MM";
                System.err.println("Feil ved parsing av tid eller søk: " + e.getMessage());
            }
        }

        ImGui.separator();
        if (!feilmeldingSok.isEmpty()) {
            ImGui.text(feilmeldingSok);
        } else if (funnetReiser.isEmpty()) {
            ImGui.text("Ingen reiseforslag funnet.");
        } else {
            // viser bare den neste resien
            ImGui.text("Neste reise:"); 
            ImGui.text(funnetReiser.get(0).toString()); 
        }
        ImGui.end();


        ImGui.begin("Debug meny");
    
        if(ImGui.collapsingHeader("Stoppesteder")){
            ImGui.separator();
            for (Stoppested stoppested : datadepot.hentStoppesteder()){
                if(ImGui.treeNode(stoppested.toString())){
                    for (Rute rute : datadepot.hentRuter()){
                        if(ImGui.treeNode(rute.toString())){
                            for (Avgang a : stoppested.hentAvganger()) {
                                if (a.ruteID == rute.id) {
                                    ImGui.text(a.toString());
                         }
                            }
                        ImGui.treePop();
                        }
                    }
        
                    ImGui.treePop();
                }
                ImGui.separator();
            }
        }
        if(ImGui.collapsingHeader("Ruter")){
            ImGui.separator();
            for (Rute rute : datadepot.ruteCache) {
                if(ImGui.treeNode(rute.toString())){
                    for (Stoppested stopp : rute.stopp){
                        ImGui.text(stopp.toString());
                    }
                    ImGui.treePop();
                }   
                ImGui.separator();
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
            System.err.println("[ERROR] Kan ikke lagre til fil ->" + e);
        }

        // try{datadepot.lasteFraDisk();}
        // catch(Exception e){
        //     System.err.println("[ERROR] Kan ikke laste inn fra fil ->" + e);
        // }
    }
    
    // Starter bare applikasjonen. Burde kanskje ikke røres
    public static void main(String[] args) {
        launch(new Main());
    }
}