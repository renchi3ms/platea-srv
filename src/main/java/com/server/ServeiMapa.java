package com.server;

import com.model.Esdeveniment;
import com.model.MatriuSeients;
import com.model.MapaResultat;
import com.model.Recinte;
import com.model.Seient;
import com.model.Venda;
import com.model.Zona;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servei que construeix la matriu global de seients d'un esdeveniment
 * a partir de la plantilla literal del recinte (assets/mapes/<id>.json).
 *
 * Cada casella de la plantilla que conte un id de zona genera un seient:
 * les zones "numerada" s'omplen amb seients numerats (fila = fila de la
 * reixilla, numero = columna de la reixilla) i les zones "general" amb
 * seients no numerats (fila = 0). Les caselles buides queden a null.
 *
 * Despres s'hi apliquen les vendes reals guardades a la base de dades:
 * els seients amb una venda associada queden marcats com a venuts.
 */
public class ServeiMapa {

    /** Construeix la matriu global del recinte i hi aplica les vendes reals. */
    public MapaResultat construirMapa(Esdeveniment esdeveniment) {

        Recinte recinte = esdeveniment.getRecinte();
        MatriuSeients plantilla = recinte.getPlantilla();

        // Copiar la plantilla: cada casella amb zona genera un seient
        MatriuSeients mapa =
                new MatriuSeients(plantilla.getFiles(), plantilla.getColumnes());

        for (int f = 0; f < plantilla.getFiles(); f++) {

            for (int c = 0; c < plantilla.getColumnes(); c++) {

                String zonaId = plantilla.getZona(f, c);

                if (zonaId == null) {
                    continue;
                }

                Zona zona = recinte.getZona(zonaId);

                if (zona == null) {
                    throw new IllegalStateException(
                            "Casella (" + f + "," + c + ") amb zona desconeguda: " + zonaId);
                }

                Seient seient;

                if ("general".equals(zona.getTipus())) {
                    seient = new Seient(zonaId, 0, f * plantilla.getColumnes() + c + 1);
                } else {
                    seient = new Seient(zonaId, f + 1, c + 1);
                }

                mapa.set(f, c, seient);
            }
        }

        // 2. Estat dels seients: vendes reals guardades a la base de dades
        List<Venda> vendes = new ServeiCompres().carregarVendes(esdeveniment);

        Map<String, Venda> vendesPerSeient = new HashMap<>();

        for (Venda venda : vendes) {

            if (venda.getSeient() != null) {
                vendesPerSeient.put(venda.getSeient().getKey(), venda);
            }
        }

        for (int f = 0; f < mapa.getFiles(); f++) {

            for (int c = 0; c < mapa.getColumnes(); c++) {

                Seient s = mapa.get(f, c);

                if (s == null) {
                    continue;
                }

                Venda venda = vendesPerSeient.get(s.getKey());

                if (venda != null) {
                    s.setEstat("venut");
                    s.setVendaRef(venda.getCodi());
                }
            }
        }

        Map<String, Venda> vendesPerCodi = new LinkedHashMap<>();

        for (Venda venda : vendes) {
            vendesPerCodi.putIfAbsent(venda.getCodi(), venda);
        }

        return new MapaResultat(mapa, vendesPerCodi);
    }
}
