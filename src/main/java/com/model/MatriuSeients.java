package com.model;

/**
 * Rejilla rectangular de asientos de un recinto (matriz global).
 * Las celdas sin asiento (huecos) valen null tanto en la matriz de
 * asientos como en la plantilla de zonas (id de zona por casilla).
 */
public class MatriuSeients {

    private int files;
    private int columnes;
    private String[][] zones; // id de zona de cada casella, null si esta buida
    private Seient[][] matriu;

    public MatriuSeients(int files, int columnes) {
        this.files = files;
        this.columnes = columnes;
        this.zones = new String[files][columnes];
        this.matriu = new Seient[files][columnes];
    }

    /** Devuelve el seient de la posicio indicada o null si esta fora de la matriu. */
    public Seient get(int fila, int columna) {

        if (fila < 0 || fila >= files || columna < 0 || columna >= columnes) {
            return null;
        }

        return matriu[fila][columna];
    }

    public void set(int fila, int columna, Seient seient) {
        matriu[fila][columna] = seient;
    }

    /** Devuelve el id de zona de la casella indicada o null si esta buida. */
    public String getZona(int fila, int columna) {

        if (fila < 0 || fila >= files || columna < 0 || columna >= columnes) {
            return null;
        }

        return zones[fila][columna];
    }

    public void setZona(int fila, int columna, String zonaId) {
        zones[fila][columna] = zonaId;
    }

    public int getFiles() {
        return files;
    }

    public int getColumnes() {
        return columnes;
    }

    public Seient[][] getMatriu() {
        return matriu;
    }
}
