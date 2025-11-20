package com.set10.core;

// import com.set10.core.Datadepot;
// import com.set10.core.Stoppested;
import com.set10.database.DatabaseText;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

// import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestDatadepot {
    
    @Test
    @DisplayName("testSerialiseringOgDeserialisering")
    void testSerialiseringOgDeserialisering(){
        DatabaseText dbt = new DatabaseText("data\\testdata.txt");
        Datadepot depot = new Datadepot(dbt);
        depot.opprettDummydata();
        assertDoesNotThrow(depot::lagreTilDisk);
        assertDoesNotThrow(depot::lasteFraDisk);
    }

    @Test
    @DisplayName("serialiseringsintegritet for brukere")
    void testBrukerSerialiseringsintegritet(){
        DatabaseText dbt = new DatabaseText("data\\testdata.txt");
        Datadepot depot = new Datadepot(dbt); 
        Datadepot depot2 = new Datadepot(dbt); 

        depot.opprettDummydata();

        try {depot.lagreTilDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        try {depot2.lasteFraDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        assertEquals(depot.brukerCache.size(), depot2.brukerCache.size());

        for(int i = 0; i< depot.brukerCache.size(); i++){
            Bruker b1 = depot.brukerCache.get(i);
            Bruker b2 = depot2.brukerCache.get(i);
            assertEquals(b1.id, b2.id);
        }
    }

    @Test
    @DisplayName("serialiseringsintegritet for Billetter")
    void testBillettSerialiseringsintegritet(){
        DatabaseText dbt = new DatabaseText("data\\testdata.txt");
        Datadepot depot = new Datadepot(dbt); 
        Datadepot depot2 = new Datadepot(dbt); 

        depot.opprettDummydata();

        try {depot.lagreTilDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        try {depot2.lasteFraDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        assertEquals(depot.billettCache.size(), depot2.billettCache.size());
        
        for(int i = 0; i< depot.billettCache.size(); i++){
            Billett b1 = depot.billettCache.get(i);
            Billett b2 = depot2.billettCache.get(i);
            assertEquals(b1.id, b2.id);
        }
    }

    @Test
    @DisplayName("serialiseringsintegritet for stoppested")
    void testStoppestedSerialiseringsintegritet(){
        DatabaseText dbt = new DatabaseText("data\\testdata.txt");
        Datadepot depot = new Datadepot(dbt); 
        Datadepot depot2 = new Datadepot(dbt); 

        depot.opprettDummydata();

        try {depot.lagreTilDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        try {depot2.lasteFraDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        assertEquals(depot.stoppestedCache.size(), depot2.stoppestedCache.size());

        for(int i = 0; i < depot.stoppestedCache.size(); i++){
            Stoppested b1 = depot.stoppestedCache.get(i);
            Stoppested b2 = depot2.stoppestedCache.get(i);
            assertEquals(b1.id, b2.id);
        }

    }

    
    
    @Test
    @DisplayName("serialiseringsintegritet for stoppested")
    void testRuterSerialiseringsintegritet(){
        DatabaseText dbt = new DatabaseText("data\\testdata.txt");
        Datadepot depot = new Datadepot(dbt); 
        Datadepot depot2 = new Datadepot(dbt); 

        depot.opprettDummydata();

        try {depot.lagreTilDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        try {depot2.lasteFraDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        assertEquals(depot.ruteCache.size(), depot2.ruteCache.size());
        for(int i = 0; i< depot.ruteCache.size(); i++){
            Rute b1 = depot.ruteCache.get(i);
            Rute b2 = depot2.ruteCache.get(i);
            assertEquals(b1.id, b2.id);
        }
    }
    
    @Test
    @DisplayName("serialiseringsintegritet for avganger")
    void testAvgangSerialiseringsintegritet(){
        DatabaseText dbt = new DatabaseText("data\\testdata.txt");
        Datadepot depot = new Datadepot(dbt); 
        Datadepot depot2 = new Datadepot(dbt); 

        depot.opprettDummydata();

        try {depot.lagreTilDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        try {depot2.lasteFraDisk();}
        catch(Exception e){
            e.printStackTrace();
        }

        assertEquals(depot.avgangCache.size(), depot2.avgangCache.size());

        for(int i = 0; i< depot.avgangCache.size(); i++){
            Avgang b1 = depot.avgangCache.get(i);
            Avgang b2 = depot2.avgangCache.get(i);
            assertEquals(b1.id, b2.id);
        }

    }
    
    
}
