package servei;

import model.*;
import java.io.File;
import java.time.LocalDateTime;
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

            Document doc =
                    builder.parse(new File(fitxer));

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
}