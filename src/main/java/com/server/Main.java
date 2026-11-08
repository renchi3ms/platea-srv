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
                    Cataleg cataleg = new ServeiXML().carregarCataleg(FITXER_XML);
                    ArrayList<Esdeveniment> esdeveniments = new ArrayList<Esdeveniment>();
                    
                    for (Esdeveniment e : cataleg.getEsdeveniments()) {
                        esdeveniments.add(e);
                    }

                    JSONObject response = new JSONObject();

                    response.put(K_TYPE, "esdev");

                    conn.send(getEsdevResponse(esdeveniments, response).toString());

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

        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();

        System.out.println("Server running on port " + DEFAULT_PORT + "zss Ctrl+C to stop it.");
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
