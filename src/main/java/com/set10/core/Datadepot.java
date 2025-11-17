package com.set10.core;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Holder på data som applikasjonen behøver.
 * Opprettholder 
 */
public class Datadepot {

    public IDatabase database;

    public ArrayList<Bruker> brukerCache = new ArrayList<>(); 
    public ArrayList<Billett> billettCache = new ArrayList<>(); 

    public ArrayList<Stoppested> stoppestedCache = new ArrayList<>();
    public ArrayList<Rute> ruteCache = new ArrayList<>();
    public ArrayList<Avgang> avgangCache = new ArrayList<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Burde fungere som Init
    public Datadepot(IDatabase database){
        this.database = database;
    }

    public void opprettDummydata(){
        
        if (brukerCache.isEmpty() && ruteCache.isEmpty() && stoppestedCache.isEmpty()) {
            System.out.println("Oppretter dummydata...");
        //Brukere
        //Kun admin kan se debug meny 
        opprettBruker(new Bruker("Administrator", LocalDate.of(1980, 1, 1), "1234"));
        opprettBruker(new Bruker("Jonas Olsen", LocalDate.of(1995, 5, 20), "1234"));
        opprettBruker(new Bruker("Issac Evinskog", LocalDate.of(2005, 10, 15), "1234"));
        opprettBruker(new Bruker("Erika Hansen", LocalDate.of(2015, 3, 1), "1234"));
        opprettBruker(new Bruker("Olga Bentsdotter", LocalDate.of(1950, 7, 25), "1234"));
        


       //Ruter  
        Rute r33 = new Rute(33);
        opprettRute(r33);
        Rute r34 = new Rute(34);
        opprettRute(r34);
        Rute r35 = new Rute(35);
        opprettRute(r35);


        //Stoppesteder og avganger
        
        // Halden område (59.12°N, 11.39°E)
        opprettStoppested(new Stoppested(0, "Halden bussterminal", 59.1240, 11.3870)); //0
            leggTilAvgangerForStopp(r34.id, 0, genererAvganger("05:25", "23:10", 15));
            leggTilAvgangerForStopp(r33.id, 0, genererAvganger("05:10", "23:10", 15));
            leggTilAvgangerForStopp(r35.id, 0, genererAvganger("05:40", "22:55", 15));
           
            //retur til bussterminal
            leggTilAvgangerForStopp(r34.id, 0, genererAvganger("05:55", "23:40", 15));
            leggTilAvgangerForStopp(r33.id, 0, genererAvganger("05:40", "23:55", 15));
            leggTilAvgangerForStopp(r35.id, 0, genererAvganger("06:10", "23:25", 15));
        
        opprettStoppested(new Stoppested(1, "Fiskebrygga", 59.1255, 11.3885)); //1
            leggTilAvgangerForStopp(r34.id, 1, genererAvganger("05:26", "23:11", 15));
            leggTilAvgangerForStopp(r33.id, 1, genererAvganger("05:11", "23:11", 15));
            leggTilAvgangerForStopp(r35.id, 1, genererAvganger("05:41", "22:56", 15));

        opprettStoppested(new Stoppested(2, "Kulturkvartalet", 59.1270, 11.3900)); //2
            leggTilAvgangerForStopp(r34.id, 2, genererAvganger("05:27", "23:12", 15));
            leggTilAvgangerForStopp(r33.id, 2, genererAvganger("05:12", "23:12", 15));
            leggTilAvgangerForStopp(r35.id, 2, genererAvganger("05:42", "22:57", 15));


        opprettStoppested(new Stoppested(3, "Parken", 59.1285, 11.3915)); //3
            leggTilAvgangerForStopp(r34.id, 3, genererAvganger("05:28", "23:13", 15));
            leggTilAvgangerForStopp(r33.id, 3, genererAvganger("05:13", "23:13", 15));
            leggTilAvgangerForStopp(r35.id, 3, genererAvganger("05:43", "22:58", 15));

        opprettStoppested(new Stoppested(4, "Snippen Halden", 59.1300, 11.3930)); //4
            leggTilAvgangerForStopp(r34.id, 4, genererAvganger("05:29", "23:14", 15));
            
        opprettStoppested(new Stoppested(5, "Østre Lie", 59.1320, 11.3950));//5
            leggTilAvgangerForStopp(r34.id, 5, genererAvganger("05:36", "23:21", 15));
       
        opprettStoppested(new Stoppested(6, "Bjørklund", 59.1340, 11.3970)); //6
            leggTilAvgangerForStopp(r34.id, 6, genererAvganger("05:39", "23:24", 15));
        
        opprettStoppested(new Stoppested(7, "Remmen Høgskolen", 59.1360, 11.3990)); //7
            leggTilAvgangerForStopp(r34.id, 7, genererAvganger("05:42", "23:27", 15));
       
        opprettStoppested(new Stoppested(8, "Park Hotell", 59.1380, 11.4010)); //8
            leggTilAvgangerForStopp(r34.id, 8, genererAvganger("05:45", "23:30", 15));
        
        opprettStoppested(new Stoppested(9, "Skofabrikken", 59.1400, 11.4030)); //9
            leggTilAvgangerForStopp(r34.id, 9, genererAvganger("05:48", "23:33", 15));
            leggTilAvgangerForStopp(r33.id, 9, genererAvganger("05:14", "23:14", 15));
        
        opprettStoppested(new Stoppested(10, "Stranda", 59.1220, 11.3850)); //10
            leggTilAvgangerForStopp(r34.id, 10, genererAvganger("05:50", "23:35", 15));
            leggTilAvgangerForStopp(r35.id, 10, genererAvganger("06:05", "23:20", 15));

        opprettStoppested(new Stoppested(11, "Bybrua Halden", 59.1205, 11.3835)); //11
            leggTilAvgangerForStopp(r34.id, 11, genererAvganger("05:51", "23:36", 15));
            leggTilAvgangerForStopp(r35.id, 11, genererAvganger("06:07", "23:22", 15));

        opprettStoppested(new Stoppested(12, "Borgergata", 59.1190, 11.3820)); //12
            leggTilAvgangerForStopp(r34.id, 12, genererAvganger("05:52", "23:37", 15));
            leggTilAvgangerForStopp(r35.id, 12, genererAvganger("06:08", "23:23", 15));

        opprettStoppested(new Stoppested(13, "Halden stasjon", 59.1175, 11.3805)); //13
            leggTilAvgangerForStopp(r34.id, 13, genererAvganger("05:53", "23:38", 15));
            leggTilAvgangerForStopp(r33.id, 13, genererAvganger("05:38", "23:53", 15)); 
            leggTilAvgangerForStopp(r35.id, 13, genererAvganger("06:08", "23:23", 15));

        opprettStoppested(new Stoppested(14, "Halden politistasjon", 59.1160, 11.3790)); //14
            leggTilAvgangerForStopp(r33.id, 14, genererAvganger("05:10", "23:10", 15));
            
        opprettStoppested(new Stoppested(15, "Tistedalveien", 59.1145, 11.3775)); //15
            leggTilAvgangerForStopp(r33.id, 15, genererAvganger("05:17", "23:17", 15));
        
        opprettStoppested(new Stoppested(16, "Øbergveien", 59.1130, 11.3760)); //16
            leggTilAvgangerForStopp(r33.id, 16, genererAvganger("05:20", "23:20", 15));
        
        opprettStoppested(new Stoppested(17, "Vold skog", 59.1115, 11.3745)); //17
            leggTilAvgangerForStopp(r33.id, 17, genererAvganger("05:22", "23:22", 15));
        
        opprettStoppested(new Stoppested(18, "Øbergkrysset", 59.1100, 11.3730)); //18
            leggTilAvgangerForStopp(r33.id, 18, genererAvganger("05:26", "23:26", 15));
        
        opprettStoppested(new Stoppested(19, "Risum", 59.1085, 11.3715)); //19    
            leggTilAvgangerForStopp(r33.id, 19, genererAvganger("05:29", "23:29", 15)); 
        
        opprettStoppested(new Stoppested(20, "Kommandantveien Risum", 59.1070, 11.3700)); //20
            leggTilAvgangerForStopp(r33.id, 20, genererAvganger("05:30", "23:30", 15));
        
        opprettStoppested(new Stoppested(21, "Kommandantveien", 59.1055, 11.3685)); //21
            leggTilAvgangerForStopp(r33.id, 21, genererAvganger("05:32", "23:32", 15));

        opprettStoppested(new Stoppested(22, "Halden Bil", 59.1250, 11.3750)); //22
            leggTilAvgangerForStopp(r35.id, 22, genererAvganger("05:44", "22:59", 15));

        opprettStoppested(new Stoppested(23, "Åsveien", 59.1235, 11.3735)); //23
            leggTilAvgangerForStopp(r35.id, 23, genererAvganger("05:44", "22:59", 15));

        opprettStoppested(new Stoppested(24, "Kullveien", 59.1220, 11.3720));//24
            leggTilAvgangerForStopp(r35.id, 24, genererAvganger("05:45", "23:00", 15));

        opprettStoppested(new Stoppested(25, "Brødløs sør", 59.1205, 11.3705)); //25
            leggTilAvgangerForStopp(r35.id, 25, genererAvganger("05:46", "23:01", 15));

        opprettStoppested(new Stoppested(26, "Kiosken B.R.A. veien", 59.1190, 11.3690)); //26
            leggTilAvgangerForStopp(r35.id, 26, genererAvganger("05:46", "23:01", 15));

        opprettStoppested(new Stoppested(27, "Næridsrød", 59.1175, 11.3675));//27
            leggTilAvgangerForStopp(r35.id, 27, genererAvganger("05:47", "23:02", 15));

        opprettStoppested(new Stoppested(28, "Blokkveien 2", 59.1160, 11.3660));//28
            leggTilAvgangerForStopp(r35.id, 28, genererAvganger("05:48", "23:03", 15));

        opprettStoppested(new Stoppested(29, "Blokkveien 18", 59.1145, 11.3645)); //29
            leggTilAvgangerForStopp(r35.id, 29, genererAvganger("05:49", "23:04", 15));

        opprettStoppested(new Stoppested(30, "Blokkveien 28", 59.1130, 11.3630)); //30
            leggTilAvgangerForStopp(r35.id, 30, genererAvganger("05:50", "23:05", 15));

        opprettStoppested(new Stoppested(31, "Atomveien", 59.1115, 11.3615)); //31
            leggTilAvgangerForStopp(r35.id, 31, genererAvganger("05:51", "23:06", 15));

        opprettStoppested(new Stoppested(32, "Bergheim", 59.1100, 11.3600)); //32
            leggTilAvgangerForStopp(r35.id, 32, genererAvganger("05:54", "23:09", 15));

        opprettStoppested(new Stoppested(33, "Veden", 59.1085, 11.3585)); //33
            leggTilAvgangerForStopp(r35.id, 33, genererAvganger("05:57", "23:12", 15));

        opprettStoppested(new Stoppested(34, "Tistedalen", 59.1070, 11.3570)); //34
            leggTilAvgangerForStopp(r35.id, 34, genererAvganger("06:00", "23:15", 15));


         }
    }

    private void leggTilAvgangerForStopp(int ruteID, int stoppID, String... tider) {
        Stoppested stopp = hentStoppested(stoppID);
        if (stopp == null) return;

        Rute rute = hentRute(ruteID);
        if (rute == null) return;
        
        rute.leggTilStopp(stopp);
        
        for (String t : tider) {
            LocalTime lt = LocalTime.parse(t, TIME_FMT);
            Avgang avg = new Avgang(ruteID, stoppID, lt);
            opprettAvgang(avg);
            stopp.leggTilAvgang(avg);
            //avgangCache.add(avg); 
        }
    }
    
    // Hjelpemetode: Generer avganger med regelmessige intervaller
    private String[] genererAvganger(String startTid, String sluttTid, int intervallMinutter) {
        LocalTime start = LocalTime.parse(startTid, TIME_FMT);
        LocalTime slutt = LocalTime.parse(sluttTid, TIME_FMT);
        List<String> avganger = new ArrayList<>();
        
        LocalTime current = start;
        while (!current.isAfter(slutt)) {
            avganger.add(current.format(TIME_FMT));
            current = current.plusMinutes(intervallMinutter);
        }
        
        return avganger.toArray(new String[0]);
    }

    public void lagreTilDisk() throws Exception{
        database.serialiser(this);
    }

    public void lasteFraDisk() throws Exception{
        database.deserialiser(this);
    }

    // Returnerer id til nylaget objekt 
    public int opprettBruker(Bruker bruker){
        brukerCache.add(bruker);
        bruker.id = brukerCache.size()-1;
        return bruker.id ;
    }

    public Bruker hentBruker(int id){
        return 
        brukerCache.get(id);
    }

    public ArrayList<Bruker> hentBrukere(){
        return brukerCache;
    }

    // Returnerer id til nylaget objekt 
    public int opprettStoppested(Stoppested stoppested){
        stoppestedCache.add(stoppested);
        stoppested.id = stoppestedCache.size()-1;
        return stoppested.id;
    }

    public Stoppested hentStoppested(int id){
        return stoppestedCache.get(id);
    }

    public ArrayList<Stoppested> hentStoppesteder(){
        return stoppestedCache;
    }

    // Returnerer id til nylaget objekt 
    /*public int opprettRute(Rute rute){
        ruteCache.add(rute);
        rute.id = ruteCache.size()-1;
        return rute.id;
    } */
    public int opprettRute(Rute rute) {
        ruteCache.add(rute);
        return rute.id;
    }

   /*public Rute hentRute(int id){
        return ruteCache.get(id);
    }*/
    public Rute hentRute(int id) {
        for (Rute r : ruteCache) {
            if (r.id == id) {
                return r;
            }
        }
        return null;
    }
    
    public ArrayList<Rute> hentRuter(){
        return ruteCache;
    }

    // Returnerer id til nylaget objekt 
    public int opprettAvgang(Avgang avgang){
        avgangCache.add(avgang);
        avgang.id = avgangCache.size()-1;
        return avgang.id;
    }

    public ArrayList<Avgang> hentAvganger() {
        return avgangCache;
    }

    public int opprettBillett(Billett billett) {
        billett.id = billettCache.size() + 1;
        billettCache.add(billett);
        System.out.println("Opprettet billett med ID: " + billett.id);
        return billett.id;
    }

}
