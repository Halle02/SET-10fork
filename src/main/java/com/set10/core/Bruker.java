package com.set10.core;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Bruker {
    public int id;
    public String navn;
    public ArrayList<Billett> aktiveBilletter;
    public ArrayList<Billett> gamleBiletter;
    
    public Bruker(int id, String navn) {
        this.id = id;
        this.navn = navn;
        this.aktiveBilletter = new ArrayList<>();
        this.gamleBiletter = new ArrayList<>();
    }

    public Bruker(String navn) {
        this.navn = navn;
        this.aktiveBilletter = new ArrayList<>();
        this.gamleBiletter = new ArrayList<>();
    }

    public void kjopBillett(String type, LocalDateTime startTid) {
        Billett nyBillett = new Billett(type, startTid);
        aktiveBilletter.add(nyBillett);
        System.out.println(navn + " kjøpte en " + type + " billett.");
    }

    public void kjopBillettTilAnnenBruker(String type, LocalDateTime startTid, Bruker mottaker) {
        Billett nyBillett = new Billett(type, startTid);
        mottaker.aktiveBilletter.add(nyBillett);
        System.out.println(navn + " kjøpte " + type + " billett til " + mottaker.navn);
    }

    @Override
    public String toString() {
        return "BrukerId: " + id + " Navn: " + navn;
    }

    public String hentVisningsnavn() {
        return navn;
    }
}