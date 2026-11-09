package com.server;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

import com.model.MatriuSeients;

/**
 * Llegeix la plantilla literal d'un recinte des de assets/mapes/<id>.json.
 * Format: { "files": n, "columnes": m, "zones": [[zonaId|null, ...], ...] }.
 */
public class ServeiMapes {

    /** Construeix la MatriuSeients del recinte indicat o llança excepcio si falta el fitxer. */
    public MatriuSeients carregar(String recinteId) {

        String fitxer = "assets/mapes/" + recinteId + ".json";

        try (InputStream in = ServeiMapes.class
                .getClassLoader()
                .getResourceAsStream(fitxer)) {

            if (in == null) {
                throw new IllegalStateException("No es troba el mapa: " + fitxer);
            }

            JSONObject json = new JSONObject(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8));

            int files = json.getInt("files");
            int columnes = json.getInt("columnes");

            MatriuSeients plantilla = new MatriuSeients(files, columnes);

            JSONArray zones = json.getJSONArray("zones");

            for (int f = 0; f < files; f++) {

                JSONArray fila = zones.getJSONArray(f);

                for (int c = 0; c < columnes; c++) {

                    if (!fila.isNull(c)) {
                        plantilla.setZona(f, c, fila.getString(c));
                    }
                }
            }

            return plantilla;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Error llegint el mapa: " + fitxer, e);
        }
    }
}
