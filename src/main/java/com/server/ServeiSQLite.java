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

    public void guardarCataleg(Cataleg cataleg) {
        try (Connection conn = Database.connect()) {
            String sqlRecinte = "INSERT OR IGNORE INTO recintes(id, nom) VALUES (?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sqlRecinte)) {
                for (Recinte r : cataleg.getRecintes()) {

                    stmt.setString(1, r.getId());
                    stmt.setString(2, r.getNom());

                    stmt.executeUpdate();
                }
            }
            String sqlZona =
                "INSERT OR IGNORE INTO zones(id, nom, tipus, aforament, recinte_id) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sqlZona)) {

                for (Recinte r : cataleg.getRecintes()) {

                    for (Zona z : r.getZones()) {

                        stmt.setString(1, z.getId());
                        stmt.setString(2, z.getNom());
                        stmt.setString(3, z.getTipus());
                        stmt.setInt(4, z.getAforament());
                        stmt.setString(5, r.getId());

                        stmt.executeUpdate();
                    }
                }
            }

            String sqlEsdev ="INSERT OR IGNORE INTO esdeveniments(id, nom, data_hora, categoria, descripcio, imatge, recinte_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sqlEsdev)) {

                for (Esdeveniment e : cataleg.getEsdeveniments()) {

                    stmt.setString(1, e.getId());
                    stmt.setString(2, e.getNom());
                    stmt.setString(3, e.getDataHora().toString());
                    stmt.setString(4, e.getCategoria());
                    stmt.setString(5, e.getDescripcio());
                    stmt.setString(6, e.getImatge());
                    stmt.setString(7, e.getRecinte().getId());

                    stmt.executeUpdate();
                }
            }

            String sqlEntrada =
                "INSERT OR IGNORE INTO tipus_entrada(id, nom, preu, quota, zona_id, esdeveniment_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sqlEntrada)) {

                for (Esdeveniment e : cataleg.getEsdeveniments()) {

                    for (TipusEntrada te : e.getTipusEntrades()) {

                        stmt.setString(1, te.getId());
                        stmt.setString(2, te.getNom());
                        stmt.setDouble(3, te.getPreu());
                        stmt.setInt(4, te.getQuota());
                        stmt.setString(5, te.getZona().getId());
                        stmt.setString(6, e.getId());

                    stmt.executeUpdate();
                    }
                }
            }

            System.out.println("Catàleg migrat correctament a SQLite");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
