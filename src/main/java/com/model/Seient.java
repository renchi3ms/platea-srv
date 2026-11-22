package com.model;

/**
 * Representa un seient fis d'una zona numerada d'un recinte.
 * Les zones de tipus "general" no tenen seients.
 */
public class Seient {

    private String zonaId;
    private int fila;
    private int numero;
    private String estat; // "L" | "V"
    private String vendaRef; // codi de la venda associada, o null si és lliure

    public Seient(String zonaId, int fila, int numero) {
        this.zonaId = zonaId;
        this.fila = fila;
        this.numero = numero;
        this.estat = "L";
        this.vendaRef = null;
    }

    public boolean estaVengut() {
        return "V".equals(estat);
    }

    public String getZonaId() {
        return zonaId;
    }

    public int getFila() {
        return fila;
    }

    public int getNumero() {
        return numero;
    }

    public String getEstat() {
        return estat;
    }

    public void setEstat(String estat) {
        this.estat = estat;
    }

    public String getVendaRef() {
        return vendaRef;
    }

    public void setVendaRef(String vendaRef) {
        this.vendaRef = vendaRef;
    }

    /** Clau única del seient dins la zona, coherent amb UNIQUE(zona_id, fila, numero). */
    public String getKey() {
        return zonaId + "-" + fila + "-" + numero;
    }
}
