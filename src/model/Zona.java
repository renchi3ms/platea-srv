package model;

public class Zona {

    private String id;
    private String nom;
    private String tipus;
    private int aforament;

    public Zona(String id, String nom, String tipus, int aforament) {
        this.id = id;
        this.nom = nom;
        this.tipus = tipus;
        this.aforament = aforament;
    }

    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getTipus() {
        return tipus;
    }

    public int getAforament() {
        return aforament;
    }
}