//Laget denne så jeg ikke skulle fakke opp i main

package com.set10.application;


import com.set10.core.Datadepot;

import com.set10.database.DatabaseText;


import com.set10.core.Navigasjonstjeneste;
import com.set10.core.Stoppested;
import com.set10.core.Rute;
import com.set10.core.Avgang;
import com.set10.core.Billett;
import com.set10.core.Bruker;
import com.set10.database.DatabaseText;

import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;

public class Main extends Application {

    Navigasjonstjeneste navigasjonstjeneste;
    Datadepot datadepot;
    private Integer valgtBrukerId = null;
    private String valgtBrukerNavn = null;

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Østfold trafikk premium");
    }

    // Denne kjøres (forhåpentligvis) 60 ganger i sekundet, og er hvor logikk for gui og lignende legges inn
    @Override
    public void process() {
        ImGui.begin("Debug meny");
        ImGui.setNextItemWidth(220);
        // Velger bruker
        if (ImGui.beginCombo("##brukerCombo" , valgtBrukerNavn == null ? "Velg bruker" : valgtBrukerNavn)) {

            for (Bruker b : datadepot.hentBrukere()) {
        
                String navn = b.hentVisningsnavn();  
                boolean selected = (valgtBrukerId != null && valgtBrukerId == b.id);

                if (ImGui.selectable(navn, selected)) {
                    valgtBrukerId = b.id;
                    valgtBrukerNavn = navn;
                }

                if (selected)
                    ImGui.setItemDefaultFocus();
            }
            ImGui.endCombo();

        }
        ImGui.sameLine();


        if (valgtBrukerId != null) {
            ImGui.text("Logget inn som: " + valgtBrukerNavn);
            ImGui.separator();


        // Ruter
        ImGui.spacing();
        ImGui.spacing();
        ImGui.spacing();
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
                        if (ImGui.treeNode(rute.toString())) {
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
        // Billetter
        if (ImGui.collapsingHeader("Billetter")) {
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
        /* 
        try{datadepot.lagreTilDisk();}
        catch(Exception e){
             System.err.println("[ERROR] Kan ikke lagre til fil ->" + e);
         }

        try{datadepot.lasteFraDisk();}
        catch(Exception e){
            System.err.println("[ERROR] Kan ikke laste inn fra fil ->" + e); 
        }*/
    }
    
    // Starter bare applikasjonen. Burde kanskje ikke røres
    public static void main(String[] args) {
        launch(new Main());
    }
}