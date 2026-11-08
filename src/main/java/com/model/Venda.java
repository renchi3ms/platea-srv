package com.model;

import java.time.LocalDateTime;

/**
 * Venda d'una entrada. Refleja la informació combinada de les taules
 * "entrades" i "compres" de l'esquema de base de dades.
 */
public class Venda {

    private String codi; // referència de la compra (compres.codi)
    private String localitzador; // localitzador de l'entrada (va al QR)
    private String compradorNom;
    private String compradorCorreu;
    private LocalDateTime dataCompra;
    private double preuPagat;
    private String estat; // "venuda" | "validada" | "reemborsada"
    private String tipusEntradaId;
    private Seient seient; // null si és entrada general (sense seient)

    public Venda(String codi,
                 String localitzador,
                 String compradorNom,
                 String compradorCorreu,
                 LocalDateTime dataCompra,
                 double preuPagat,
                 String tipusEntradaId,
                 Seient seient) {

        this.codi = codi;
        this.localitzador = localitzador;
        this.compradorNom = compradorNom;
        this.compradorCorreu = compradorCorreu;
        this.dataCompra = dataCompra;
        this.preuPagat = preuPagat;
        this.estat = "venuda";
        this.tipusEntradaId = tipusEntradaId;
        this.seient = seient;

        if (seient != null) {
            seient.setEstat("venut");
            seient.setVendaRef(codi);
        }
    }

    public String getCodi() {
        return codi;
    }

    public String getLocalitzador() {
        return localitzador;
    }

    public String getCompradorNom() {
        return compradorNom;
    }

    public String getCompradorCorreu() {
        return compradorCorreu;
    }

    public LocalDateTime getDataCompra() {
        return dataCompra;
    }

    public double getPreuPagat() {
        return preuPagat;
    }

    public String getEstat() {
        return estat;
    }

    public void setEstat(String estat) {
        this.estat = estat;
    }

    public String getTipusEntradaId() {
        return tipusEntradaId;
    }

    public Seient getSeient() {
        return seient;
    }
}
