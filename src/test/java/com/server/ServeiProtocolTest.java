package com.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ServeiProtocolTest {

    private static Main server;
    private static int port;

    @BeforeAll
    static void arrencarServidor() throws Exception {
        try (ServerSocket dummy = new ServerSocket(0)) {
            port = dummy.getLocalPort();
        }

        server = new Main(new InetSocketAddress("localhost", port));
        server.start();

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

    @AfterAll
    static void aturarServidor() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    private static JSONObject demanarEsdev(String missatge) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> resposta = new AtomicReference<>();

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
            client.send(missatge);
            assertTrue(latch.await(5, TimeUnit.SECONDS),
                    "El servidor no ha respost");
            return new JSONObject(resposta.get());
        } finally {
            client.closeBlocking();
        }
    }

    @Test
    void respostaConteElsEsdevenimentsDeR1() throws Exception {
        JSONObject resposta = demanarEsdev("{\"type\":\"esdev\",\"recinte\":\"R1\"}");

        assertEquals("esdev", resposta.getString("type"));
        assertTrue(resposta.has("esdeveniments"));

        JSONObject esdevs = resposta.getJSONObject("esdeveniments");
        Set<String> ids = new java.util.HashSet<>();
        for (Iterator<String> it = esdevs.keys(); it.hasNext();) {
            ids.add(it.next());
        }

        assertEquals(Set.of("E1", "E5", "E7", "E10"), ids);
    }

    @Test
    void cadaEsdevenimentTeLesSevesEntrades() throws Exception {
        JSONObject resposta = demanarEsdev("{\"type\":\"esdev\",\"recinte\":\"R1\"}");

        JSONObject esdevs = resposta.getJSONObject("esdeveniments");

        for (Iterator<String> it = esdevs.keys(); it.hasNext();) {
            String id = it.next();
            JSONObject e = esdevs.getJSONObject(id);

            assertTrue(e.has("nom"));
            assertTrue(e.has("dataHora"));
            assertTrue(e.has("recinte"));

            JSONArray entrades = e.getJSONArray("tipusEntrades");
            assertTrue(entrades.length() > 0,
                    "L'esdeveniment " + id + " no te entrades");

            for (int i = 0; i < entrades.length(); i++) {
                JSONObject entrada = entrades.getJSONObject(i);
                for (String camp : new String[] {"id", "nom", "zona", "preu", "quota"}) {
                    assertTrue(entrada.has(camp),
                            "Falta el camp " + camp + " a les entrades de " + id);
                }
            }
        }
    }

    @Test
    void entradesDeE1TenenPreuIQuota() throws Exception {
        JSONObject resposta = demanarEsdev("{\"type\":\"esdev\",\"recinte\":\"R1\"}");

        JSONObject e1 = resposta.getJSONObject("esdeveniments").getJSONObject("E1");
        JSONArray entrades = e1.getJSONArray("tipusEntrades");

        assertEquals(3, entrades.length());

        JSONObject t1 = trobarEntrada(entrades, "T1");
        assertEquals("Z1", t1.getString("zona"));
        assertEquals(18.00, t1.getDouble("preu"), 0.001);
        assertEquals(50, t1.getInt("quota"));
    }

    @Test
    void recinteInexistentResponBuit() throws Exception {
        JSONObject resposta = demanarEsdev("{\"type\":\"esdev\",\"recinte\":\"R9\"}");

        assertEquals("esdev", resposta.getString("type"));
        assertEquals(0, resposta.getJSONObject("esdeveniments").length());
    }

    @Test
    void missatgeInvalidoNoTancaLaConnexio() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> resposta = new AtomicReference<>();

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
            assertTrue(client.connectBlocking(5, TimeUnit.SECONDS));

            client.send("això no és json");
            Thread.sleep(500);

            client.send("{\"type\":\"esdev\",\"recinte\":\"R2\"}");

            assertTrue(latch.await(5, TimeUnit.SECONDS),
                    "El servidor ha deixat de respondre després d'un missatge invàlid");

            JSONObject resp = new JSONObject(resposta.get());
            JSONObject esdevs = resp.getJSONObject("esdeveniments");

            assertEquals(Set.of("E2", "E4", "E6", "E9"), keys(esdevs));
        } finally {
            client.closeBlocking();
        }
    }

    private static Set<String> keys(JSONObject obj) {
        Set<String> keys = new java.util.HashSet<>();
        for (Iterator<String> it = obj.keys(); it.hasNext();) {
            keys.add(it.next());
        }
        return keys;
    }

    private static JSONObject trobarEntrada(JSONArray entrades, String id) {
        for (int i = 0; i < entrades.length(); i++) {
            JSONObject e = entrades.getJSONObject(i);
            if (id.equals(e.getString("id"))) {
                return e;
            }
        }
        fail("No s'ha trobat l'entrada " + id);
        return null;
    }
}
