package model;

public class TipusEntrada {

    private String id;
    private String nom;
    private Zona zona;
    private double preu;
    private int quota;

    public TipusEntrada(String id, String nom, Zona zona, double preu, int quota) {
        this.id = id;
        this.nom = nom;
        this.zona = zona;
        this.preu = preu;
        this.quota = quota;
    }

    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Zona getZona() {
        return zona;
    }

    public double getPreu() {
        return preu;
    }

    public int getQuota() {
        return quota;
    }
}