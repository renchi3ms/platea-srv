package com.server;

import com.model.Esdeveniment;
import com.model.MatriuSeients;
import com.model.Seient;
import com.model.TipusEntrada;
import com.model.Venda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servei per registrar i consultar les compres reals d'entrades.
 *
 * Les compres es guarden a les taules "compres", "entrades" i "seients"
 * de la base de dades. Cada entrada ocupa un seient de la seva zona:
 * les zones generals tenen seients sense numerar (fila = 0) que
 * s'ocupen en ordre de fila a columna de la plantilla del recinte.
 */
public class ServeiCompres {

    /** Registra una compra: crea la compra, les entrades i marca els seients com a venduts. */
    public void guardarCompra(Esdeveniment esdev, String nom, String correu,
                              String tipusId, String localitzador, int quantitat) {

        TipusEntrada te = esdev.getTipusEntrades().stream()
                .filter(t -> t.getId().equals(tipusId))
                .findFirst()
                .orElse(null);

        if (te == null) {
            throw new IllegalStateException("Tipus d'entrada no trobat: " + tipusId);
        }

        String zonaId = te.getZona().getId();
        boolean general = "general".equalsIgnoreCase(te.getZona().getTipus());

        try (Connection conn = Database.connect()) {
            conn.setAutoCommit(false);

            try {
                LocalDateTime ara = LocalDateTime.now();
                String codi = generarCodi(conn);

                long compraId;

                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO compres(codi, comprador_nom, comprador_correu, data_compra) "
                                + "VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {

                    stmt.setString(1, codi);
                    stmt.setString(2, nom);
                    stmt.setString(3, correu);
                    stmt.setString(4, ara.toString());
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("No s'ha pogut crear la compra.");
                        }
                        compraId = rs.getLong(1);
                    }
                }

                Set<String> ocupats = seientsOcupats(conn, esdev.getId());
                ArrayList<int[]> candidats = nousSeients(esdev, zonaId, general, ocupats, quantitat);

                if (candidats.size() < quantitat) {
                    throw new IllegalStateException("No qu prou seients lliures a la zona " + zonaId + ".");
                }

                try (PreparedStatement stmtSeient = conn.prepareStatement(
                        "INSERT INTO seients(zona_id, fila, numero, estat, venda_ref, esdeveniment_id) "
                                + "VALUES (?, ?, ?, 'venut', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement stmtEntrada = conn.prepareStatement(
                             "INSERT INTO entrades(localitzador, tipus_entrada_id, seient_id, compra_id, "
                                     + "preu_pagat, data_emissio, estat) VALUES (?, ?, ?, ?, ?, ?, 'venuda')")) {

                    for (int i = 0; i < quantitat; i++) {

                        int[] seient = candidats.get(i);

                        stmtSeient.setString(1, zonaId);
                        stmtSeient.setInt(2, seient[0]);
                        stmtSeient.setInt(3, seient[1]);
                        stmtSeient.setString(4, codi);
                        stmtSeient.setString(5, esdev.getId());
                        stmtSeient.executeUpdate();

                        long seientId;

                        try (ResultSet rs = stmtSeient.getGeneratedKeys()) {
                            if (!rs.next()) {
                                throw new IllegalStateException("No s'ha pogut reservar el seient.");
                            }
                            seientId = rs.getLong(1);
                        }

                        stmtEntrada.setString(1, i == 0 ? localitzador : localitzador + "-" + (i + 1));
                        stmtEntrada.setString(2, tipusId);
                        stmtEntrada.setLong(3, seientId);
                        stmtEntrada.setLong(4, compraId);
                        stmtEntrada.setDouble(5, te.getPreu());
                        stmtEntrada.setString(6, ara.toString());
                        stmtEntrada.executeUpdate();
                    }
                }

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() == null
                    ? "Error en guardar la compra."
                    : e.getMessage(), e);
        }
    }

    /** Carrega les vendes reals d'un esdeveniment (una per entrada venuda). */
    public List<Venda> carregarVendes(Esdeveniment esdev) {

        List<Venda> vendes = new ArrayList<>();

        String sql =
                "SELECT e.localitzador, e.tipus_entrada_id, e.seient_id, e.preu_pagat, e.data_emissio, "
                        + "c.codi, c.comprador_nom, c.comprador_correu, "
                        + "s.zona_id, s.fila, s.numero "
                        + "FROM entrades e "
                        + "JOIN compres c ON c.id = e.compra_id "
                        + "JOIN seients s ON s.id = e.seient_id "
                        + "WHERE s.esdeveniment_id = ? AND e.estat = 'venuda'";

        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, esdev.getId());

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    Seient seient = new Seient(
                            rs.getString("zona_id"),
                            rs.getInt("fila"),
                            rs.getInt("numero"));

                    seient.setEstat("venut");
                    seient.setVendaRef(rs.getString("codi"));

                    Venda venda = new Venda(
                            rs.getString("codi"),
                            rs.getString("localitzador"),
                            rs.getString("comprador_nom"),
                            rs.getString("comprador_correu"),
                            LocalDateTime.parse(rs.getString("data_emissio")),
                            rs.getDouble("preu_pagat"),
                            rs.getString("tipus_entrada_id"),
                            seient);

                    vendes.add(venda);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return vendes;
    }

    /** Claus (zona-fila-numero) dels seients ja venuts d'un esdeveniment. */
    private Set<String> seientsOcupats(Connection conn, String esdevId) throws Exception {

        Set<String> ocupats = new HashSet<>();

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT zona_id, fila, numero FROM seients "
                        + "WHERE esdeveniment_id = ? AND estat = 'venut'")) {

            stmt.setString(1, esdevId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    ocupats.add(rs.getString(1) + "-" + rs.getInt(2) + "-" + rs.getInt(3));
                }
            }
        }

        return ocupats;
    }

    /**
     * Busca els primers seients lliures de la zona a la plantilla del recinte,
     * en ordre de fila a columna. Les zones generals fan seients amb fila 0 i
     * numero = fila de la reixilla * columnes + columna + 1, coherents amb els
     * que construeix ServeiMapa.
     */
    private ArrayList<int[]> nousSeients(Esdeveniment esdev, String zonaId, boolean general,
                                         Set<String> ocupats, int quantitat) {

        ArrayList<int[]> candidats = new ArrayList<>();
        MatriuSeients plantilla = esdev.getRecinte().getPlantilla();

        if (plantilla == null) {
            return candidats;
        }

        for (int f = 0; f < plantilla.getFiles() && candidats.size() < quantitat; f++) {

            for (int c = 0; c < plantilla.getColumnes() && candidats.size() < quantitat; c++) {

                if (!zonaId.equals(plantilla.getZona(f, c))) {
                    continue;
                }

                int fila = general ? 0 : f + 1;
                int numero = general ? f * plantilla.getColumnes() + c + 1 : c + 1;

                if (!ocupats.contains(zonaId + "-" + fila + "-" + numero)) {
                    candidats.add(new int[]{fila, numero});
                }
            }
        }

        return candidats;
    }

    /** Genera un codi de compra unic (V + nanosegons). */
    private String generarCodi(Connection conn) throws Exception {

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT 'V' || CAST((julianday('now') - 2440587.5) * 86400000000000 AS INTEGER)")) {

            if (rs.next()) {
                return rs.getString(1);
            }
        }

        throw new IllegalStateException("No s'ha pogut generar el codi de compra.");
    }
}
