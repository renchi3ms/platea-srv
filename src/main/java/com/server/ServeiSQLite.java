
package com.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.HashMap;

import com.model.Cataleg;
import com.model.Recinte;
import com.model.Zona;
import com.model.Esdeveniment;
import com.model.TipusEntrada;

public class ServeiSQLite {

    public Cataleg carregarCataleg() {

        Cataleg cataleg = new Cataleg();

        HashMap<String, Recinte> recintes = new HashMap<>();
        HashMap<String, Zona> zones = new HashMap<>();
        HashMap<String, Esdeveniment> esdeveniments = new HashMap<>();

        try (Connection conn = Database.connect()) {

            // 1. CARREGAR RECINTES
            String sqlRecintes = "SELECT * FROM recintes";

            try (PreparedStatement stmt = conn.prepareStatement(sqlRecintes);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    Recinte recinte = new Recinte(
                        rs.getString("id"),
                        rs.getString("nom")
                    );

                    recintes.put(recinte.getId(), recinte);
                    cataleg.afegirRecinte(recinte);
                }
            }

            // 2. CARREGAR ZONES
            String sqlZones = "SELECT * FROM zones";

            try (PreparedStatement stmt = conn.prepareStatement(sqlZones);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    Zona zona = new Zona(
                        rs.getString("id"),
                        rs.getString("nom"),
                        rs.getString("tipus"),
                        rs.getInt("aforament")
                    );

                    String recinteId = rs.getString("recinte_id");

                    Recinte recinte = recintes.get(recinteId);

                    if (recinte != null) {
                        recinte.afegirZona(zona);
                    }

                    zones.put(zona.getId(), zona);
                }
            }

            // 3. CARREGAR ESDEVENIMENTS
            String sqlEsdeveniments = "SELECT * FROM esdeveniments";

            try (PreparedStatement stmt = conn.prepareStatement(sqlEsdeveniments);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    String recinteId = rs.getString("recinte_id");

                    Recinte recinte = recintes.get(recinteId);

                    if (recinte == null) {
                        continue;
                    }

                    Esdeveniment esdeveniment = new Esdeveniment(
                        rs.getString("id"),
                        rs.getString("nom"),
                        LocalDateTime.parse(rs.getString("data_hora")),
                        rs.getString("categoria"),
                        rs.getString("descripcio"),
                        rs.getString("imatge"),
                        recinte
                    );

                    esdeveniments.put(esdeveniment.getId(), esdeveniment);
                    cataleg.afegirEsdeveniment(esdeveniment);
                }
            }

            // 4. CARREGAR TIPUS D'ENTRADA
            String sqlEntrades = "SELECT * FROM tipus_entrada";

            try (PreparedStatement stmt = conn.prepareStatement(sqlEntrades);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    String zonaId = rs.getString("zona_id");
                    String esdevenimentId = rs.getString("esdeveniment_id");

                    Zona zona = zones.get(zonaId);
                    Esdeveniment esdeveniment = esdeveniments.get(esdevenimentId);

                    if (zona == null || esdeveniment == null) {
                        continue;
                    }

                    TipusEntrada entrada = new TipusEntrada(
                        rs.getString("id"),
                        rs.getString("nom"),
                        zona,
                        rs.getDouble("preu"),
                        rs.getInt("quota")
                    );

                    esdeveniment.afegirTipusEntrada(entrada);
                }
            }

            System.out.println("Catàleg carregat correctament des de SQLite");

        } catch (Exception e) {
            System.out.println("Error carregant el catàleg des de SQLite:");
            e.printStackTrace();
        }

        return cataleg;
    }
}
