package com.server;

import com.model.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

public class ServeiXML {

    public Cataleg carregarCataleg(String fitxer) {

        Cataleg cataleg = new Cataleg();

        try {

            HashMap<String, Recinte> mapaRecintes = new HashMap<>();
            HashMap<String, Zona> mapaZones = new HashMap<>();

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            InputStream archivo = ServeiXML.class
                    .getClassLoader()
                    .getResourceAsStream(fitxer);

            if (archivo == null) {
                throw new FileNotFoundException(
                        "No se encuentra el archivo: " + fitxer);
            }

            Document doc;

            try (InputStream xml = archivo) {
                doc = builder.parse(xml);
            }

            doc.getDocumentElement().normalize();

            NodeList recintesXML =
                    doc.getElementsByTagName("recinte");

            for (int i = 0; i < recintesXML.getLength(); i++) {

                Element recinteXML =
                        (Element) recintesXML.item(i);

                String id =
                        recinteXML.getAttribute("id");

                String nom =
                        recinteXML.getAttribute("nom");

                Recinte recinte =
                        new Recinte(id, nom);

                NodeList zonesXML =
                        recinteXML.getElementsByTagName("zona");

                for (int j = 0; j < zonesXML.getLength(); j++) {

                    Element zonaXML =
                            (Element) zonesXML.item(j);

                    Zona zona =
                            new Zona(
                                    zonaXML.getAttribute("id"),
                                    zonaXML.getAttribute("nom"),
                                    zonaXML.getAttribute("tipus"),
                                    Integer.parseInt(
                                            zonaXML.getAttribute("aforament"))
                            );

                    recinte.afegirZona(zona);

                    mapaZones.put(zona.getId(), zona);
                }

                MatriuSeients plantilla =
                        carregarMapa("assets/mapes/" + id + ".map");

                if (plantilla == null) {
                    throw new IllegalStateException(
                            "No es troba el mapa de " + id);
                }

                recinte.setPlantilla(plantilla);

                cataleg.afegirRecinte(recinte);

                mapaRecintes.put(id, recinte);
            }

            NodeList esdevenimentsXML =
                    doc.getElementsByTagName("esdeveniment");

            for (int i = 0; i < esdevenimentsXML.getLength(); i++) {

                Element eventXML =
                        (Element) esdevenimentsXML.item(i);

                String id =
                        eventXML.getAttribute("id");

                String recinteId =
                        eventXML.getAttribute("recinte");

                Esdeveniment event =
                        new Esdeveniment(
                                id,
                                eventXML.getElementsByTagName("nom")
                                        .item(0).getTextContent(),
                                LocalDateTime.parse(
                                        eventXML.getElementsByTagName("dataHora")
                                                .item(0).getTextContent()),
                                eventXML.getElementsByTagName("categoria")
                                        .item(0).getTextContent(),
                                eventXML.getElementsByTagName("descripcio")
                                        .item(0).getTextContent(),
                                eventXML.getElementsByTagName("imatge")
                                        .item(0).getTextContent(),
                                mapaRecintes.get(recinteId)
                        );

                NodeList entradesXML =
                        eventXML.getElementsByTagName("tipusEntrada");

                for (int j = 0; j < entradesXML.getLength(); j++) {

                    Element entradaXML =
                            (Element) entradesXML.item(j);

                    String zonaId =
                            entradaXML.getAttribute("zona");

                    TipusEntrada entrada =
                            new TipusEntrada(
                                    entradaXML.getAttribute("id"),
                                    entradaXML.getAttribute("nom"),
                                    mapaZones.get(zonaId),
                                    Double.parseDouble(
                                            entradaXML.getAttribute("preu")),
                                    Integer.parseInt(
                                            entradaXML.getAttribute("quota"))
                            );

                    event.afegirTipusEntrada(entrada);
                }

                cataleg.afegirEsdeveniment(event);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cataleg;
    }

    /**
     * Llegeix el fitxer de mapa (rejilla literal d'un recinte) i en construeix
     * la plantilla: cada token es un id de zona i "." es una casella buida.
     * Totes les files han de tenir el mateix nombre de tokens.
     */
    private MatriuSeients carregarMapa(String fitxer) {

        InputStream xml = ServeiXML.class
                .getClassLoader()
                .getResourceAsStream(fitxer);

        if (xml == null) {
            throw new IllegalStateException(
                    "No es troba el mapa: " + fitxer);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(xml, StandardCharsets.UTF_8))) {

            ArrayList<String[]> files = new ArrayList<>();
            int amplada = -1;

            String linea = reader.readLine();

            while (linea != null) {

                linea = linea.trim();

                if (!linea.isEmpty()) {

                    String[] tokens = linea.split("\\s+");

                    if (amplada == -1) {
                        amplada = tokens.length;
                    } else if (tokens.length != amplada) {
                        throw new IllegalStateException(
                                "Mapa " + fitxer + ": fila "
                                        + (files.size() + 1)
                                        + " te " + tokens.length
                                        + " tokens (se'n esperaven " + amplada + ")");
                    }

                    files.add(tokens);
                }

                linea = reader.readLine();
            }

            MatriuSeients plantilla =
                    new MatriuSeients(files.size(), amplada);

            for (int f = 0; f < files.size(); f++) {

                String[] tokens = files.get(f);

                for (int c = 0; c < tokens.length; c++) {

                    if (!".".equals(tokens[c])) {
                        plantilla.setZona(f, c, tokens[c]);
                    }
                }
            }

            return plantilla;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error llegint el mapa: " + fitxer, e);
        }
    }
}
