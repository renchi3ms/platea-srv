package com.server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import org.json.JSONArray;
import org.json.JSONObject;

import com.model.Cataleg;
import com.model.Esdeveniment;
import com.model.Recinte;
import com.model.TipusEntrada;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import java.sql.Connection;
import com.server.Database;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servidor WebSocket que manté l'estat complet dels clients i objectes seleccionables.
 *
 * Protocol simplificat:
 *  - Client -> Server:  { "type": "clientData", "data": { ...ClientData... } }
 *  - Server -> Clients: { "type": "state", "clientId": <clientId>, "clients": [ ...ClientData... ], "gameObjects": { ... }, "countdown": n? }
 */
public class Main extends WebSocketServer {

    /** Port per defecte on escolta el servidor. */
    public static final int DEFAULT_PORT = 3000;

    // Claus JSON
    private static final String K_TYPE = "type";
    private static final String K_VALUE = "value";
    private static final String K_RECINTE = "recinte";
    private static final String FITXER_XML = "assets/plantilla.xml";
    private static final String K_ESDEVENIMENTS = "esdeveniments";

    private static final String K_ESDEV_ID = "id";
    private static final String K_ESDEV_NOM = "nom";
    private static final String K_ESDEV_DATAHORA = "dataHora";
    private static final String K_ESDEV_CATEGORIA = "categoria";
    private static final String K_ESDEV_DESCRIPIO = "descripcio";
    private static final String K_ESDEV_IMATGE = "imatge";
    private static final String K_ESDEV_RECINTE = "recinte";
    private static final String K_ESDEV_TIPUSENTRADES = "tipusEntrades";

    private static final String K_ENTRADA_ID = "id";
    private static final String K_ENTRADA_NOM = "nom";
    private static final String K_ENTRADA_ZONA = "zona";
    private static final String K_ENTRADA_PREU = "preu";
    private static final String K_ENTRADA_QUOTA = "quota";

    /**
     * Crea un servidor WebSocket que escolta a l'adreça indicada.
     *
     * @param address adreça i port d'escolta del servidor
     */
    public Main(InetSocketAddress address) {
        super(address);
    }

    // ----------------- WebSocketServer overrides -----------------

    /** Assigna un nom i color al client i envia l’STATE complet. */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("WebSocket client connected: ");

        JSONObject bienvenida = new JSONObject();
        bienvenida.put("type", "welcome");
        bienvenida.put("message", "Bienvenido al servidor");

        conn.send(bienvenida.toString());
        
    }

    /** Elimina el client del registre i envia l’STATE complet. */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("WebSocket client disconnected: ");
    }

    /** Processa els missatges rebuts. */
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject obj = new JSONObject(message);

            String type = obj.optString(K_TYPE, "");

            switch (type) {
                case "ping" -> {
                    JSONObject response = new JSONObject();
                    response.put(K_TYPE, "pong");
                    conn.send(response.toString());
                }

                case "bounce" -> {
                    conn.send(obj.toString());
                }

                case "esdev" -> {
                    Cataleg cataleg = new ServeiSQLite().carregarCataleg();
                    ArrayList<Esdeveniment> esdeveniments = new ArrayList<Esdeveniment>();
                    
                    for (Esdeveniment e : cataleg.getEsdeveniments()) {
                        esdeveniments.add(e);
                    }

                    JSONObject response = new JSONObject();

                    response.put(K_TYPE, "esdev");

                    conn.send(getEsdevResponse(esdeveniments, response).toString());

                }

                case "purchase" -> {
                    JSONObject response = new JSONObject();
                    response.put(K_TYPE, "purchase");

                    try {
                        String nombre = obj.getString("nombre").trim();
                        String correo = obj.getString("correo").trim();
                        JSONArray entradas = obj.getJSONArray("entradas");

                        if (nombre.isEmpty() || correo.isEmpty() || entradas.isEmpty()) {
                            throw new IllegalArgumentException("Falten dades de la compra.");
                        }

                        Cataleg cataleg = new ServeiXML().carregarCataleg(FITXER_XML);
                        JSONArray localitzadors = new JSONArray();

                        // Validar todas las entradas antes de guardar la reserva.
                        for (int i = 0; i < entradas.length(); i++) {
                            JSONObject entradaJSON = entradas.getJSONObject(i);

                            String esdevId = entradaJSON.getString("esdevenimentId");
                            String tipusId = entradaJSON.getString("tipusEntradaId");
                            int quantitat = entradaJSON.getInt("quantitat");

                            if (quantitat <= 0) {
                                throw new IllegalArgumentException("Quantitat no vàlida.");
                            }

                            Esdeveniment esdev = cataleg.getEsdeveniments().stream()
                                    .filter(e -> e.getId().equals(esdevId))
                                    .findFirst()
                                    .orElse(null);

                            if (esdev == null) {
                                throw new IllegalArgumentException("Esdeveniment no trobat: " + esdevId);
                            }

                            TipusEntrada tipusEntrada = esdev.getTipusEntrades().stream()
                                    .filter(t -> t.getId().equals(tipusId))
                                    .findFirst()
                                    .orElse(null);

                            if (tipusEntrada == null) {
                                throw new IllegalArgumentException("Tipus d'entrada no trobat: " + tipusId);
                            }

                            if (!"general".equalsIgnoreCase(tipusEntrada.getZona().getTipus())) {
                                throw new IllegalArgumentException(
                                        "Només es poden comprar entrades generals."
                                );
                            }
                        }

                        // Crear y guardar las reservas.
                        for (int i = 0; i < entradas.length(); i++) {
                            JSONObject entradaJSON = entradas.getJSONObject(i);

                            String esdevId = entradaJSON.getString("esdevenimentId");
                            String tipusId = entradaJSON.getString("tipusEntradaId");
                            int quantitat = entradaJSON.getInt("quantitat");

                            String localitzador = UUID.randomUUID()
                                    .toString()
                                    .replace("-", "")
                                    .substring(0, 12)
                                    .toUpperCase();

                            JSONObject reserva = new JSONObject();
                            reserva.put("tipusEntrada", tipusId);
                            reserva.put("localitzador", localitzador);
                            reserva.put("nom", nombre);
                            reserva.put("correu", correo);
                            reserva.put("quantitat", quantitat);

                            guardarReserva(esdevId, reserva);

                            JSONObject resultatEntrada = new JSONObject();
                            resultatEntrada.put("esdevenimentId", esdevId);
                            resultatEntrada.put("tipusEntradaId", tipusId);
                            resultatEntrada.put("localitzador", localitzador);
                            resultatEntrada.put("quantitat", quantitat);

                            localitzadors.put(resultatEntrada);
                        }

                        response.put("status", "OK");
                        response.put("localitzadors", localitzadors);

                    } catch (Exception e) {
                        response.put("status", "ERROR");
                        response.put("message", e.getMessage() != null
                                ? e.getMessage()
                                : "Error en processar la reserva.");
                    }

                    conn.send(response.toString());
                }

                case "mapa" -> {
                    JSONObject response = new JSONObject();
                    response.put(K_TYPE, "mapa");

                    Cataleg cataleg = new ServeiXML().carregarCataleg(FITXER_XML);
                    String esdevId = obj.getString("esdev");

                    Esdeveniment esdev = cataleg.getEsdeveniments().stream()
                            .filter(e -> e.getId().equals(esdevId))
                            .findFirst()
                            .orElse(null);

                    if (esdev == null) {
                        response.put("error", "Esdev not found");
                        conn.send(response.toString());
                        break;
                    }

                    MapaResultat resultat = new ServeiMapa().construirMapa(esdev);

                    conn.send(getMapaResponse(esdev, resultat, response).toString());

                }
            }

        } catch (Exception ex) {
            System.out.println("JSON inválido");
        }
    }

    /** Log d'error global o de socket concret. */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    /** Arrencada: log i configuració del timeout de connexió perduda. */
    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
    }

    // ----------------- Lifecycle util -----------------

    /** Punt d'entrada. */
    /*
    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();

        System.out.println("Server running on port " + DEFAULT_PORT + ". Press Ctrl+C to stop it.");
    }
    */
    public static void main(String[] args) {
        try {
            Connection conn = Database.connect();
            System.out.println("Conexió SQLite OK");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        DatabaseInitializer.initialize();
        try {
            ServeiXML serveiXML = new ServeiXML();

            Cataleg cataleg = serveiXML.carregarCataleg("assets/plantilla.xml");
            ServeiSQLite serveiSQLite = new ServeiSQLite();

            serveiSQLite.guardarCataleg(cataleg);

        } catch (Exception e) {
           e.printStackTrace();
    }
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();

        System.out.println("Server running on port " + DEFAULT_PORT + "zss Ctrl+C to stop it.");
    }
    private static void guardarReserva(String esdevId, JSONObject reserva)
        throws IOException {

        Path carpeta = Paths.get("data");
        Files.createDirectories(carpeta);

        Path archivo = carpeta.resolve(esdevId + ".json");

        JSONArray reservas;

        if (Files.exists(archivo)) {
            String contenido = Files.readString(archivo);
            reservas = new JSONArray(contenido);
        } else {
            reservas = new JSONArray();
        }

        reservas.put(reserva);

        Files.writeString(archivo, reservas.toString(4));
    }

    private static JSONObject getEsdevResponse(ArrayList<Esdeveniment> esdeveniments, JSONObject response) {
        

        JSONObject esdevsJSON = new JSONObject();

        for (Esdeveniment e : esdeveniments) {
            JSONObject eJSON = new JSONObject();
            eJSON.put(K_ESDEV_ID, e.getId());
            eJSON.put(K_ESDEV_NOM, e.getNom());
            eJSON.put(K_ESDEV_DATAHORA, e.getDataHora().toString());
            eJSON.put(K_ESDEV_CATEGORIA, e.getCategoria());
            eJSON.put(K_ESDEV_DESCRIPIO, e.getDescripcio());
            eJSON.put(K_ESDEV_IMATGE, e.getImatge());
            eJSON.put(K_ESDEV_RECINTE, e.getRecinte().getId());

            JSONArray entradesJSON = new JSONArray();
            for (TipusEntrada te : e.getTipusEntrades()) {
                JSONObject teJSON = new JSONObject();
                teJSON.put(K_ENTRADA_ID, te.getId());
                teJSON.put(K_ENTRADA_NOM, te.getNom());
                teJSON.put(K_ENTRADA_ZONA, te.getZona().getId());
                teJSON.put("tipoZona", te.getZona().getTipus());
                teJSON.put(K_ENTRADA_PREU, te.getPreu());
                teJSON.put(K_ENTRADA_QUOTA, te.getQuota());
                entradesJSON.put(teJSON);
            }

            eJSON.put(K_ESDEV_TIPUSENTRADES, entradesJSON);

            esdevsJSON.put(e.getId(), eJSON);
        }

        response.put(K_ESDEVENIMENTS, esdevsJSON);
        return response;
    }

}
