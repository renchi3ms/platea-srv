package model;

import java.util.ArrayList;


public class Recinte {

    private String id;
    private String nom;
    private ArrayList<Zona> zones;

    public Recinte(String id, String nom) {
        this.id = id;
        this.nom = nom;
        this.zones = new ArrayList<>();
    }

    public void afegirZona(Zona zona) {
        zones.add(zona);
    }

    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public ArrayList<Zona> getZones() {
        return zones;
    }
}