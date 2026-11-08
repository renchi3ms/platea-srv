package com.model;

import java.util.Map;

/**
 * Resultat de construir el mapa d'un esdeveniment: la matriu global
 * de seients i les vendes (mock) associades, clau = codi de venda.
 */
public class MapaResultat {

    private MatriuSeients matriu;
    private Map<String, Venda> vendes;

    public MapaResultat(MatriuSeients matriu, Map<String, Venda> vendes) {
        this.matriu = matriu;
        this.vendes = vendes;
    }

    public MatriuSeients getMatriu() {
        return matriu;
    }

    public Map<String, Venda> getVendes() {
        return vendes;
    }
}
