package com.set10.core;
import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;  Trengs hvis vi skal hente inn soner igjen
//import java.util.List;
import java.time.LocalDateTime;

public class Billett {  

    public enum Type{
        Enkel,
        Periode
    }

    public int id;
    public Type type;
    
    public LocalDateTime startTid;
    public LocalDateTime sluttTid;
    //public List<Integer> gyldigForSoner;


    public Billett(Type type, LocalDateTime startTid) {
        this.type = type;
        this.startTid = startTid;
        //this.gyldigForSoner = new ArrayList<>(); // Lager bare listen, en annen metode for å legge til soner.

        switch(type){
            case Enkel:
                this.sluttTid = startTid.plusMinutes(90);
                break;
            case Periode:
                this.sluttTid = startTid.plusDays(30);
                break;
            default:
                this.sluttTid = startTid;
        }

    }

    // Brukes ikke fordi alle stoppesteder har samme sone "Østfold".
    /* public void leggTilSone(int sone) {
        if (!gyldigForSoner.contains(sone)) {
            gyldigForSoner.add(sone);
        }
    } */

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return "\nBillett:\n" +
                "Type: " + type +
                "\nStarttid: " + startTid.format(formatter) +
                "\nSlutttid: " + sluttTid.format(formatter);
                //"\nGyldig for soner: " + gyldigForSoner.toString();
    }
}
