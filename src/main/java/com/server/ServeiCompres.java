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
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.security.SecureRandom;

/**
 * Servei per registrar i consultar les compres reals d'entrades.
 *
 * Les compres es guarden a les taules "compres", "entrades" i "seients"
 * de la base de dades. Cada entrada ocupa un seient de la seva zona:
 * les zones generals tenen seients sense numerar (fila = 0) que
 * s'ocupen en ordre de fila a columna de la plantilla del recinte.
 */
public class ServeiCompres {

    /**
     * Registra una compra: crea la compra, les entrades i marca els seients com a venduts.
     * Rebre la Connection del caller: NO fa commit ni rollback, la transacció
     * completa de la cistella es confirma o es desfà des de Main.
     **/
    public String guardarCompra(Connection conn, Esdeveniment esdev, String nom, String correu,
                              String tipusId, String localitzador, int quantitat, List<int[]> seients) throws Exception {

        TipusEntrada te = esdev.getTipusEntrades().stream()
                .filter(t -> t.getId().equals(tipusId))
                .findFirst()
                .orElse(null);

        if (te == null) {
            throw new IllegalStateException("Tipus d'entrada no trobat: " + tipusId);
        }

        String zonaId = te.getZona().getId();
        boolean general = "general".equalsIgnoreCase(te.getZona().getTipus());
        
        int vendudes = quantsVenuts(conn, tipusId);
        if (vendudes + quantitat > te.getQuota()) {
            throw new IllegalStateException("Quota superada per a l'entrada " + te.getNom()
                    + " (quota: " + te.getQuota() + ").");
        }

        localitzador = generarLocalitzador(conn);

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
        ArrayList<int[]> candidats;

        if (general) {
            // General: se siguen auto-asignando los primeros libres
            candidats = nousSeients(esdev, zonaId, true, ocupats, quantitat);
            if (candidats.size() < quantitat) {
                throw new IllegalStateException("No qu prou seients lliures a la zona " + zonaId + ".");
            }
        } else {
            candidats = new ArrayList<>();
            ArrayList<String> fallits = new ArrayList<>();

            for (int[] s : seients) {
                String key = zonaId + "-" + s[0] + "-" + s[1];
                if (ocupats.contains(key)) {
                    fallits.add(key);
                } else {
                    candidats.add(s);
                }
            }

            if (!fallits.isEmpty()) {
                throw new SeientsOcupatsException(fallits);
            }
        }

        try (PreparedStatement stmtSeient = conn.prepareStatement(
                "INSERT INTO seients(zona_id, fila, numero, estat, venda_ref, esdeveniment_id) "
                        + "VALUES (?, ?, ?, 'V', ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtEntrada = conn.prepareStatement(
                     "INSERT INTO entrades(localitzador, tipus_entrada_id, seient_id, compra_id, "
                             + "preu_pagat, data_emissio, estat) VALUES (?, ?, ?, ?, ?, ?, 'venuda')")) {

            for (int i = 0; i < quantitat; i++) {

                try (PreparedStatement stmtSeient = conn.prepareStatement(
                        "INSERT INTO seients(zona_id, fila, numero, estat, venda_ref, esdeveniment_id) "
                                + "VALUES (?, ?, ?, 'V', ?, ?)",
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

        return localitzador;
    }

    /** Carrega les vendes reals d'un esdeveniment (una per entrada venuda). */
    public List<Venda> carregarVendes(Esdeveniment esdev) {

        List<Venda> vendes = new ArrayList<>();

        String sql = "SELECT e.localitzador, e.tipus_entrada_id, e.seient_id, e.preu_pagat, e.data_emissio, "
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

                    seient.setEstat("V");
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

    /** Recupera les entrades comprades per un comprador. */
    public JSONArray carregarEntradesComprador(String correu) {

        JSONArray entrades = new JSONArray();

        String sql = "SELECT es.nom AS esdeveniment, "
                + "te.nom AS tipus_entrada, "
                + "z.tipus AS tipus_zona, "
                + "CASE WHEN z.tipus = 'numerada' THEN s.fila ELSE NULL END AS fila, "
                + "CASE WHEN z.tipus = 'numerada' THEN s.numero ELSE NULL END AS numero, "
                + "c.comprador_nom AS titular, "
                + "e.localitzador "
                + "FROM entrades e "
                + "JOIN compres c ON c.id = e.compra_id "
                + "JOIN tipus_entrada te ON te.id = e.tipus_entrada_id "
                + "JOIN esdeveniments es ON es.id = te.esdeveniment_id "
                + "JOIN zones z ON z.id = te.zona_id "
                + "LEFT JOIN seients s ON s.id = e.seient_id "
                + "WHERE LOWER(TRIM(c.comprador_correu)) = LOWER(TRIM(?)) "
                + "AND e.estat = 'venuda' "
                + "ORDER BY e.data_emissio DESC";

        try (Connection conn = Database.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, correu);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    JSONObject entrada = new JSONObject();

                    entrada.put("esdeveniment", rs.getString("esdeveniment"));
                    entrada.put("tipusEntrada", rs.getString("tipus_entrada"));
                    entrada.put("tipusZona", rs.getString("tipus_zona"));
                    entrada.put("titular", rs.getString("titular"));
                    entrada.put("localitzador", rs.getString("localitzador"));

                    if (rs.getObject("fila") != null) {
                        entrada.put("fila", rs.getInt("fila"));
                        entrada.put("numero", rs.getInt("numero"));
                    } else {
                        entrada.put("fila", JSONObject.NULL);
                        entrada.put("numero", JSONObject.NULL);
                    }

                    entrades.put(entrada);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error en recuperar les entrades del comprador.", e);
        }

        return entrades;
    }

    /** Claus (zona-fila-numero) dels seients ja venuts d'un esdeveniment. */
    private Set<String> seientsOcupats(Connection conn, String esdevId) throws Exception {

        Set<String> ocupats = new HashSet<>();

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT zona_id, fila, numero FROM seients "
                        + "WHERE esdeveniment_id = ? AND estat = 'V'")) {

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
                    candidats.add(new int[] { fila, numero });
                }
            }
        }

        return candidats;
    }

    // Generar un localitzador únic de tres números y tres letras
    private String generarLocalitzador(Connection conn) throws Exception {

        String numeros = "0123456789";
        String letras = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();

        String localitzador;

        do {
            StringBuilder resultat = new StringBuilder();

            for (int i = 0; i < 3; i++) {
                resultat.append(numeros.charAt(random.nextInt(numeros.length())));
            }

            for (int i = 0; i < 3; i++) {
                resultat.append(letras.charAt(random.nextInt(letras.length())));
            }

            localitzador = resultat.toString();

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT 1 FROM entrades WHERE localitzador = ? LIMIT 1")) {

                stmt.setString(1, localitzador);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return localitzador;
                    }
                }
            }

        } while (true);
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

    public JSONObject validarEntrada(String localitzador) {
        JSONObject resposta = new JSONObject();
        String sql = "SELECT e.estat, " +
                "c.comprador_nom, " +
                "te.nom AS tipus_entrada, " +
                "s.fila, " +
                "s.numero " +
                "FROM entrades e " +
                "JOIN compres c ON c.id = e.compra_id " +
                "JOIN tipus_entrada te ON te.id = e.tipus_entrada_id " +
                "LEFT JOIN seients s ON s.id = e.seient_id " +
                "WHERE e.localitzador = ?";

        try (Connection conn = Database.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, localitzador);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    resposta.put("ok", false);
                    resposta.put("motiu", "Localitzador inexistent");
                    return resposta;
                }
                if ("validada".equals(rs.getString("estat"))) {
                    resposta.put("ok", false);
                    resposta.put("motiu", "Entrada ja validada");
                    return resposta;
                }
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE entrades SET estat = 'validada' WHERE localitzador = ?")) {
                    update.setString(1, localitzador);
                    update.executeUpdate();
                }
                resposta.put("ok", true);
                resposta.put("titular", rs.getString("comprador_nom"));
                resposta.put("tipusEntrada", rs.getString("tipus_entrada"));
                if (rs.getObject("fila") != null) {
                    resposta.put("fila", rs.getInt("fila"));
                    resposta.put("numero", rs.getInt("numero"));
                }
                return resposta;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error validant entrada", e);
        }
    }

    /** Retorna quantes entrades queden disponibles */

    private int quantsVenuts(Connection conn, String tipusId) throws Exception {

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM entrades "
                        + "WHERE tipus_entrada_id = ? AND estat = 'venuda'")) {

            stmt.setString(1, tipusId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
