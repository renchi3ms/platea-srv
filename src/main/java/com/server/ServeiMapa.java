package com.server;

import com.model.Esdeveniment;
import com.model.MatriuSeients;
import com.model.MapaResultat;
import com.model.Recinte;
import com.model.Seient;
import com.model.TipusEntrada;
import com.model.Venda;
import com.model.Zona;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Servei que construeix la matriu global de seients d'un esdeveniment
 * a partir de la plantilla literal del recinte (assets/mapes/<id>.map).
 *
 * Cada casella de la plantilla que conte un id de zona genera un seient:
 * les zones "numerada" s'omplen amb seients numerats (fila = fila de la
 * reixilla, numero = columna de la reixilla) i les zones "general" amb
 * seients no numerats (fila = 0). Les caselles buides queden a null.
 *
 * Les vendes mock son deterministes per esdeveniment: els seients
 * numerats es venen en ordre aleatori i els de les zones general es van
 * ocupant en ordre de fila a columna (fila 0 primer), de manera que el
 * nombre d'ocupats permes comparar-se amb l'aforament de la zona.
 */
public class ServeiMapa {

    private static final String[] NOMS_COMPRADORS = {
        "Anna Vidal", "Pere Roca", "Marta Serra", "Jordi Puig",
        "Laia Font", "Oriol Masso", "Nuria Camps", "Marc Soler"
    };

    private static final String[] CORREUS_COMPRADORS = {
        "anna.vidal@mail.com", "pere.roca@mail.com", "marta.serra@mail.com",
        "jordi.puig@mail.com", "laia.font@mail.com", "oriol.masso@mail.com",
        "nuria.camps@mail.com", "marc.soler@mail.com"
    };

    /** Construeix la matriu global del recinte i hi aplica les vendes (mock determinista). */
    public MapaResultat construirMapa(Esdeveniment esdeveniment) {

        Recinte recinte = esdeveniment.getRecinte();
        MatriuSeients plantilla = recinte.getPlantilla();

        // 1. Copiar la plantilla: cada casella amb zona genera un seient
        MatriuSeients mapa =
                new MatriuSeients(plantilla.getFiles(), plantilla.getColumnes());

        ArrayList<Seient> numerats = new ArrayList<>();
        ArrayList<Seient> generals = new ArrayList<>();

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
                    generals.add(seient);
                } else {
                    seient = new Seient(zonaId, f + 1, c + 1);
                    numerats.add(seient);
                }

                mapa.set(f, c, seient);
            }
        }

        // 2. Estat dels seients: vendes mock deterministes per esdeveniment
        Map<String, Venda> vendes = new LinkedHashMap<>();
        aplicarVendes(esdeveniment, numerats, generals, vendes);

        return new MapaResultat(mapa, vendes);
    }

    /**
     * Marca seients com a venuts (mock). Els numerats es trien en ordre
     * aleatori (fins a un 30%) i els de zona general s'ocupen en ordre de
     * fila a columna (fins a un 50%). La llavor depen de l'esdeveniment,
     * aixi el mapa es reprodueix igual en cada peticio.
     */
    private void aplicarVendes(Esdeveniment esdeveniment,
                               ArrayList<Seient> numerats,
                               ArrayList<Seient> generals,
                               Map<String, Venda> vendes) {

        Map<String, TipusEntrada> tipusPerZona = new LinkedHashMap<>();

        for (TipusEntrada te : esdeveniment.getTipusEntrades()) {
            tipusPerZona.putIfAbsent(te.getZona().getId(), te);
        }

        Random random = new Random(esdeveniment.getId().hashCode());

        ArrayList<Seient> candidats = new ArrayList<>();

        // Seients numerats: entre 0 i un 30% queden venuts (ordre aleatori)
        int numNumerats = numerats.isEmpty()
                ? 0
                : random.nextInt(numerats.size() * 3 / 10 + 1);

        for (int i = 0; i < numNumerats && !numerats.isEmpty(); i++) {
            candidats.add(numerats.remove(random.nextInt(numerats.size())));
        }

        // Zones general: primer mig espai de cada zona queda ocupat
        int ocupats = 0;
        String zonaAnterior = null;

        for (Seient s : generals) {

            if (!s.getZonaId().equals(zonaAnterior)) {
                ocupats = 0;
                zonaAnterior = s.getZonaId();
            }

            if (ocupats < esdeveniment.getRecinte().getZona(s.getZonaId()).getAforament() / 2) {
                candidats.add(s);
                ocupats++;
            }
        }

        for (Seient s : candidats) {

            TipusEntrada te = tipusPerZona.get(s.getZonaId());

            if (te == null) {
                continue;
            }

            String codi = String.format("V%04d", vendes.size() + 1);

            Venda venda = new Venda(
                codi,
                "L" + codi,
                NOMS_COMPRADORS[random.nextInt(NOMS_COMPRADORS.length)],
                CORREUS_COMPRADORS[random.nextInt(CORREUS_COMPRADORS.length)],
                esdeveniment.getDataHora().minusDays(random.nextInt(30) + 1),
                te.getPreu(),
                te.getId(),
                s
            );

            vendes.put(codi, venda);
        }
    }
}
