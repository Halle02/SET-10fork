package com.set10.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;



public class Bruker {
    public int id;
    public String navn;
    public LocalDate fodselsDato;
    public  BrukerGruppe brukerGruppe;
    public ArrayList<Billett> aktiveBilletter;
    public ArrayList<Billett> gamleBiletter;
    
    
    public enum BrukerGruppe{
        auto,
        barn,
        ungdom,
        voksen,
        student,
        honnor
    }

    public Bruker(int id, String navn, LocalDate fodselsDato ) {
        this.id = id;
        this.navn = navn;
        this.fodselsDato = fodselsDato;
        this.brukerGruppe = BrukerGruppe.auto;
        this.aktiveBilletter = new ArrayList<>();
        this.gamleBiletter = new ArrayList<>();
    }

    public Bruker(String navn, LocalDate fodselsDato) {
        this.navn = navn;
        this.fodselsDato = fodselsDato;
        this.brukerGruppe = BrukerGruppe.auto;
        this.aktiveBilletter = new ArrayList<>();
        this.gamleBiletter = new ArrayList<>();
    }

    public int finnAlder(){
        return Period.between(fodselsDato, LocalDate.now()).getYears();
    }

    public void kjopBillett(Billett.Type type, LocalDateTime startTid) {
        Billett nyBillett = new Billett(type, startTid);
        aktiveBilletter.add(nyBillett);
        System.out.println(navn + " kjøpte en " + type + " billett.");
    }

    public void kjopBillettTilAnnenBruker(Billett.Type type, LocalDateTime startTid, Bruker mottaker) {
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