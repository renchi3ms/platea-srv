package model;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Esdeveniment{
    private String id;
    private String nom;
    private LocalDateTime dataHora;
    private String categoria;
    private String descripcio;
    private String imatge;
    private Recinte recinte;

    private ArrayList<TipusEntrada> tipusEntrades;

    public Esdeveniment(String id,
                        String nom,
                        LocalDateTime dataHora,
                        String categoria,
                        String descripcio,
                        String imatge,
                        Recinte recinte) {

        this.id = id;
        this.nom = nom;
        this.dataHora = dataHora;
        this.categoria = categoria;
        this.descripcio = descripcio;
        this.imatge = imatge;
        this.recinte = recinte;
        this.tipusEntrades = new ArrayList<>();
    }

    public void afegirTipusEntrada(TipusEntrada entrada) {
        tipusEntrades.add(entrada);
    }

    public String getId() {
        return id;
    }
    public String getNom() {
        return nom;
    }
    public LocalDateTime getDataHora() {
        return dataHora;
    }
    public String getCategoria() {
        return categoria;
    }
    public String getDescripcio() {
        return descripcio;
    }
    public String getImatge() {
        return imatge;
    }
    public Recinte getRecinte() {
        return recinte;
    }   
    public ArrayList<TipusEntrada> getTipusEntrades() {
        return tipusEntrades;
    }
}