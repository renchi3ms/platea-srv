package com.model;

import java.util.ArrayList;


public class Recinte {

    private String id;
    private String nom;
    private MatriuSeients plantilla; // rejilla literal del recinto: zona de cada casilla
    private ArrayList<Zona> zones;

    public Recinte(String id, String nom) {
        this(id, nom, null);
    }

    public Recinte(String id, String nom, MatriuSeients plantilla) {
        this.id = id;
        this.nom = nom;
        this.plantilla = plantilla;
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

    public MatriuSeients getPlantilla() {
        return plantilla;
    }

    public void setPlantilla(MatriuSeients plantilla) {
        this.plantilla = plantilla;
    }

    public Zona getZona(String zonaId) {

        for (Zona z : zones) {
            if (z.getId().equals(zonaId)) {
                return z;
            }
        }

        return null;
    }

    public ArrayList<Zona> getZones() {
        return zones;
    }
}