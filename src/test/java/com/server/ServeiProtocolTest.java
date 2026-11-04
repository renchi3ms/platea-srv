package com.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Comprova que el servidor WebSocket respon correctament a una petició
 * d'esdeveniments ("esdev") sense comprovar el contingut detallat de la
 * resposta.
 */
class ServeiProtocolTest {

    private static Main server;
    private static int port;

    /**
     * Arrenca el servidor en un port lliure qualsevol i espera que
     * estigui escoltant abans d'executar els tests.
     */
    @BeforeAll
    static void arrencarServidor() throws Exception {
        // Agafa un port lliure qualsevol per no xocar amb el servidor real
        try (ServerSocket dummy = new ServerSocket(0)) {
            port = dummy.getLocalPort();
        }

        server = new Main(new InetSocketAddress("localhost", port));
        server.start();

        // Espera (màx. 5 s) que el servidor accepti connexions al port
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("localhost", port), 200);
                return;
            } catch (Exception e) {
                Thread.sleep(100);
            }
        }
        fail("El servidor no ha arrencat al port " + port);
    }

    /** Atura el servidor en acabar els tests. */
    @AfterAll
    static void aturarServidor() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Envia una petició "esdev" per al recinte R1 i comprova que el
     * servidor respon amb un missatge del mateix tipus que conté
     * esdeveniments.
     */
    @Test
    void servidorResponALaPeticioEsdev() throws Exception {
        // El servidor respon de forma asíncron: esperem la resposta amb un latch
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> resposta = new AtomicReference<>();

        // Client de prova que només guarda la resposta de tipus "esdev"
        WebSocketClient client = new WebSocketClient(
                new URI("ws://localhost:" + port + "/")) {

            @Override
            public void onMessage(String message) {
                if ("esdev".equals(new JSONObject(message).optString("type"))) {
                    resposta.set(message);
                    latch.countDown();
                }
            }

            @Override
            public void onError(Exception ex) {
            }

            @Override
            public void onOpen(ServerHandshake handshakedata) {
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }
        };

        try {
            assertTrue(client.connectBlocking(5, TimeUnit.SECONDS),
                    "No s'ha pogut connectar al servidor");

            // Petició d'esdeveniments del recinte R1
            client.send("{\"type\":\"esdev\"}");

            assertTrue(latch.await(5, TimeUnit.SECONDS),
                    "El servidor no ha respost");

            // La resposta ha de ser del mateix tipus i portar esdeveniments
            JSONObject respostaJSON = new JSONObject(resposta.get());

            assertEquals("esdev", respostaJSON.getString("type"));
            assertTrue(respostaJSON.has("esdeveniments"));
            assertTrue(respostaJSON.getJSONObject("esdeveniments").length() > 0,
                    "El servidor ha respost sense cap esdeveniment");
        } finally {
            client.closeBlocking();
        }
    }
}
