package com.set10.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class Bruker {
    public int id;
    public String navn;
    public String passordHash; // SHA-256 hash av passord (ikke plaintext) (passord: 1234 for alle personas)
    public LocalDate fodselsDato;
    public BrukerGruppe brukerGruppe;
    public ArrayList<Billett> aktiveBilletter;
    public ArrayList<Billett> gamleBilletter;
    
    
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
        this.passordHash = null;
        this.fodselsDato = fodselsDato;
        this.brukerGruppe = BrukerGruppe.auto;
        this.aktiveBilletter = new ArrayList<>();
        this.gamleBilletter = new ArrayList<>();
    }

    public Bruker(int id, String navn, LocalDate fodselsDato, String passord ) {
        this.id = id;
        this.navn = navn;
        this.passordHash = passord != null && !passord.isEmpty() ? hashPassord(passord) : null;
        this.fodselsDato = fodselsDato;
        this.brukerGruppe = BrukerGruppe.auto;
        this.aktiveBilletter = new ArrayList<>();
        this.gamleBilletter = new ArrayList<>();
    }

    public Bruker(String navn, LocalDate fodselsDato) {
        this.navn = navn;
        this.passordHash = null;
        this.fodselsDato = fodselsDato;
        this.brukerGruppe = BrukerGruppe.auto;
        this.aktiveBilletter = new ArrayList<>();
        this.gamleBilletter = new ArrayList<>();
    }

    public Bruker(String navn, LocalDate fodselsDato, String passord) {
        this.navn = navn;
        this.passordHash = passord != null && !passord.isEmpty() ? hashPassord(passord) : null;
        this.fodselsDato = fodselsDato;
        this.brukerGruppe = BrukerGruppe.auto;
        this.aktiveBilletter = new ArrayList<>();
        this.gamleBilletter = new ArrayList<>();
    }

    public int finnAlder(){
        return Period.between(fodselsDato, LocalDate.now()).getYears();
    }

    /**
     * Beregner brukergruppe basert på alder uten å endre feltet.
     * Regler:
     * - barn: < 13 år
     * - ungdom: 13–19 år
     * - voksen: 20–66 år
     * - honnør: 67+ år
     */
    public BrukerGruppe beregnAldersbasertGruppe() {
        int alder = finnAlder();
        if (alder < 13) return BrukerGruppe.barn;
        if (alder < 20) return BrukerGruppe.ungdom;
        if (alder >= 67) return BrukerGruppe.honnor;
        return BrukerGruppe.voksen;
    }

    /**
     * Oppdaterer brukerens gruppe automatisk basert på alder.
     */
    public void oppdaterBrukerGruppeAuto() {
        this.brukerGruppe = beregnAldersbasertGruppe();
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

    /**
     * Moves expired tickets from aktiveBilletter to gamleBilletter.
     * Should be called before displaying tickets in the UI.
     */
    public void opdaterBillettStatus() {
        ArrayList<Billett> billettSomMaFlyttes = new ArrayList<>();
        
        for (Billett billett : aktiveBilletter) {
            if (!Validering.erBillettGyldigTid(billett)) {
                billettSomMaFlyttes.add(billett);
            }
        }
        
        // Flytt utgåtte billetter over til gamleBiletter
        for (Billett billett : billettSomMaFlyttes) {
            aktiveBilletter.remove(billett);
            gamleBilletter.add(billett);
        }
    }

    @Override
    public String toString() {
        return "BrukerId: " + id + " Navn: " + navn;
    }

    public String hentVisningsnavn() {
        return navn;
    }

    /**
     * Hashing av passord med SHA-256.
     */
    public static String hashPassord(String passord) {
        if (passord == null || passord.isEmpty()) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(passord.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 ikke tilgjengelig: " + e);
            return null;
        }
    }

    /**
     * Validerer at angitt passord samsvarer med lagret hash.
     */
    public boolean validerPassord(String passord) {
        if (this.passordHash == null) {
            // Bruker har ikke passord satt
            return passord == null || passord.isEmpty();
        }
        if (passord == null || passord.isEmpty()) return false;
        String angittHash = hashPassord(passord);
        return this.passordHash.equals(angittHash);
    }
}