package com.server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import org.json.JSONObject;

import java.net.InetSocketAddress;

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
    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();

        System.out.println("Server running on port " + DEFAULT_PORT + ". Press Ctrl+C to stop it.");
    }
}
