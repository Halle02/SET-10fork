package com.set10.database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import com.set10.core.IDatabase;
import com.set10.core.Rute;
import com.set10.core.Stoppested;
import com.set10.core.Avgang;
import com.set10.core.Billett;
import com.set10.core.Bruker;
import com.set10.core.Datadepot;


/* 
 * Dataformat skamløst stjålet fra .obj:
 * a: avgang
 * s: stoppested
 * i: billett
 * b: bruker
 * r: rute
*/

public class DatabaseText implements IDatabase{
    String path = "data\\data.txt";
    LocalDate standardFodselsdato = LocalDate.of(2000, 1, 1);

    public DatabaseText(){
    }

    public DatabaseText(String path){
        this.path = path;
    }

    public void serialiser(Datadepot datadepot) throws Exception{
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));

        for(int i = 0; i < datadepot.avgangCache.size(); i++){
            Avgang avgang = datadepot.avgangCache.get(i);
            writer.append("a;" + avgang.id + ";" + avgang.ruteID+";" + avgang.stoppestedID+ ";" + avgang.tidspunkt +"\n");
        }

        for(int i = 0; i < datadepot.stoppestedCache.size(); i++){
            Stoppested stopp = datadepot.stoppestedCache.get(i);

            String avganger = ""; 
            for(int j = 0; j < stopp.avganger.size()-1; j++){
                avganger += stopp.avganger.get(j).id +",";
            }
            avganger += stopp.avganger.getLast().id;

            writer.append("s;" + stopp.id + ";"+ stopp.navn + ";" + avganger + "\n");
        }

        for(int i = 0; i < datadepot.ruteCache.size(); i++){
            Rute rute = datadepot.ruteCache.get(i);
            String stopp = "";
            if (rute.stopp.size() != 0){
                for(int j = 0; j < rute.stopp.size()-1; j++){
                    stopp += rute.stopp.get(j).id +",";
                }
                stopp += rute.stopp.getLast().id;
            }else{
                System.err.println(rute +" har ingen stopp!");
            }
            writer.append("r;"+rute.id +";"+stopp+"\n");
        }

        for(int i = 0; i < datadepot.brukerCache.size(); i++){
            Bruker bruker = datadepot.brukerCache.get(i);
            String aktiveb = "";
            if (!bruker.aktiveBilletter.isEmpty()){
                for(int j = 0; j < bruker.aktiveBilletter.size()-1; j++){
                    aktiveb += bruker.aktiveBilletter.get(j).id +",";
                }
                aktiveb += bruker.aktiveBilletter.getLast().id;
            }

            String gamleb = "";
            if(!bruker.gamleBiletter.isEmpty()){
                for(int j = 0; j < bruker.gamleBiletter.size()-1; j++){
                    gamleb += bruker.gamleBiletter.get(j).id +",";
                }
                gamleb += bruker.gamleBiletter.getLast().id;
            }
            writer.append("b;" + bruker.id + ";" + bruker.navn+";"+aktiveb + ";" + gamleb + "\n");
        }

        for(int i = 0; i < datadepot.billettCache.size(); i++){
            Billett billett = datadepot.billettCache.get(i);
            writer.append("i;" + billett.id + ";" + billett.type + ";" +billett.startTid + ";" + billett.sluttTid + "\n");
        }

        
        writer.close();
    }

    public void deserialiser(Datadepot datadepot) throws Exception{
        BufferedReader reader = new BufferedReader(new FileReader(path));

        datadepot.brukerCache = new ArrayList<>();
        datadepot.billettCache = new ArrayList<>();
        datadepot.stoppestedCache = new ArrayList<>();
        datadepot.ruteCache = new ArrayList<>();
        datadepot.avgangCache = new ArrayList<>();

        ArrayList<String> userLines = new ArrayList<>();
        String line;
        
        // PASS 1: Load all data except users (bruker)
        while ((line = reader.readLine()) != null){
            if(line.startsWith("a;")){
                String[] bits = line.split(";");
                datadepot.avgangCache.add(
                    new Avgang(
                        Integer.parseInt(bits[1]),  // id
                        Integer.parseInt(bits[2]),  // ruteid
                        Integer.parseInt(bits[3]),  // stoppestedid
                        LocalTime.parse(bits[4])    // tidspunkt
                        )
                );
                
            }else if(line.startsWith("s;")){
                String[] bits = line.split(";");

                String[] avgIdxs = bits[3].split(",");
                ArrayList<Avgang> avganger = new ArrayList<>();
                for(String string : avgIdxs){
                    int idx = Integer.parseInt(string);
                    avganger.add(datadepot.avgangCache.get(idx));
                }
                Stoppested stopp = new Stoppested(Integer.parseInt(bits[1]), bits[2]);
                stopp.avganger = avganger;

                datadepot.stoppestedCache.add(stopp);

            }else if(line.startsWith("r;")){
                String[] bits = line.split(";");

                ArrayList<Stoppested> stopp = new ArrayList<>();
                if(bits.length > 2){
                    String[] stoppidxs = bits[2].split(",");
                    for(String string : stoppidxs){
                        int idx = Integer.parseInt(string);
                        stopp.add(datadepot.stoppestedCache.get(idx));
                    }
                }


                datadepot.ruteCache.add(
                    new Rute(Integer.parseInt(bits[1]),stopp)
                );

            }else if(line.startsWith("i;")){
                String[] bits = line.split(";");
                Billett billett = new Billett(Billett.Type.valueOf(bits[2]), LocalDateTime.parse(bits[3]));
                billett.id = Integer.parseInt(bits[1]);
                billett.sluttTid = LocalDateTime.parse(bits[4]);
                datadepot.billettCache.add(billett);

            }else if(line.startsWith("b;")){
                // Store user lines for PASS 2
                userLines.add(line);
            } else if(line.startsWith("i;")){
                String[] bits = line.split(";");
                Billett billett = new Billett(Billett.Type.valueOf(bits[2]), LocalDateTime.parse(bits[3]));
                billett.id = Integer.parseInt(bits[1]);
                billett.sluttTid = LocalDateTime.parse(bits[4]);
                datadepot.billettCache.add(billett);
            }
        }
        reader.close();
        
        // PASS 2: Process user lines (now that all tickets are loaded)
        for(String userLine : userLines){
            String[] bits = userLine.split(";");
            Bruker bruker = new Bruker(Integer.parseInt(bits[1]), bits[2], standardFodselsdato);

            if(bits.length > 3 && !bits[3].isEmpty()){
                String[] aktiveBilletterStr = bits[3].split(",");
                for(String strId : aktiveBilletterStr){
                    try {
                        int billettId = Integer.parseInt(strId);
                        Billett billett = hentBillettEtterId(datadepot, billettId);
                        if (billett != null) {
                            bruker.aktiveBilletter.add(billett);
                        } else {
                            System.err.println("[ADVARSEL] Fant ikke aktiv billett med ID: " + billettId + " for bruker " + bruker.navn);
                        }
                    } catch (Exception e) {
                        System.err.println("[ADVARSEL] Feilet å laste aktiv billett. Datafeil: " + strId);
                    }
                }

                if(bits.length > 4 && !bits[4].isEmpty()){
                    String[] gamleBilletterStr = bits[4].split(",");
                    for(String strId : gamleBilletterStr){
                        try {
                            int billettId = Integer.parseInt(strId);
                            Billett billett = hentBillettEtterId(datadepot, billettId);
                            if (billett != null) {
                                bruker.gamleBiletter.add(billett);
                            } else {
                                System.err.println("[ADVARSEL] Fant ikke gammel billett med ID: " + billettId + " for bruker " + bruker.navn);
                            }
                        } catch (Exception e) {
                            System.err.println("[ADVARSEL] Feilet å laste gammel billett. Datafeil: " + strId);
                        }
                    }
                }
            }

            datadepot.brukerCache.add(bruker);
        }

    }

    private Billett hentBillettEtterId(Datadepot datadepot, int billettId) {
        for (Billett billett : datadepot.billettCache) {
            if (billett.id == billettId) {
                return billett;
            }
        }
        return null;
    }
}
