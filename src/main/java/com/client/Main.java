package com.client;

import org.json.JSONArray;
import org.json.JSONObject;

import com.model.Esdeveniment;
import com.model.Recinte;
import com.model.TipusEntrada;
import com.model.Zona;

import javafx.application.Application;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Main extends Application {

    public static UtilsWS wsClient;

    // Cua compartida: el fil lector del WS hi posa els missatges rebuts i
    // el fil del menu els imprimeix abans de tornar a mostrar el menu.
    private static final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();

    public static void main(String[] args) {

        // Iniciar app JavaFX   
        launch(args);

    }
    
    @Override
    public void start(Stage stage) throws Exception {
        connectToServer();

        // Menu interactiu per triar quin missatge enviar
        Thread menuThread = new Thread(Main::showMenu);
        menuThread.setDaemon(true);
        menuThread.start();
    }

    private static void showMenu() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n");
            System.out.println("=== Menu de missatges ===");
            System.out.println("1. ping");
            System.out.println("2. bounce");
            System.out.println("3. esdev");
            System.out.println("0. sortir");
            System.out.print("Tria una opcio: ");
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    sendMessage("ping");
                    break;
                case "2":
                    sendMessage("bounce");
                    break;
                case "3":
                    sendMessage("esdev");
                    break;
                case "0":
                    System.out.println("Bye!");
                    return;
                default:
                    System.out.println("Not valid option: " + option);
                    break;
            }
        }
    }

    private static void sendMessage(String type) {
        JSONObject msg = new JSONObject();
        msg.put("type", type);

        // Buidar la cua per esperar nomes la resposta d'AQUEST missatge
        inbox.clear();
        wsClient.safeSend(msg.toString());

        // Esperar la resposta abans de tornar a mostrar el menu
        try {
            String response = inbox.poll(2, TimeUnit.SECONDS);
            if (response != null) {
                printMessage(response);
            } else {
                System.out.println("Sense resposta del servidor.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() { 
        if (wsClient != null) {
            wsClient.forceExit();
        }
    }

    public static void connectToServer() {
        wsClient = UtilsWS.getSharedInstance("ws://localhost:3000");

        wsClient.onOpen((response) -> {
        });

        wsClient.onMessage((response) -> {
            inbox.offer(response);
        });

        wsClient.onError((response) -> {
            wsError(response);
        });
    }
   

    private static void printMessage(String response) {

        JSONObject msgObj = new JSONObject(response);

        switch (msgObj.getString("type")) {
            case "pong":
                System.out.println("Pong recibido: " + response);
                break;

            case "bounce":
                System.out.println("Bounce recibido: " + response);
                break;

            case "welcome":
                break;

            case "esdev": {
                JSONObject esdevsJSON = msgObj.getJSONObject("esdeveniments");
                ArrayList<Esdeveniment> esdeveniments = new ArrayList<>();

                for (String esdevId : esdevsJSON.keySet()) {
                    JSONObject eJSON = esdevsJSON.getJSONObject(esdevId);

                    Esdeveniment esdev = new Esdeveniment(
                        eJSON.getString("id"),
                        eJSON.getString("nom"),
                        LocalDateTime.parse(eJSON.getString("dataHora")),
                        eJSON.getString("categoria"),
                        eJSON.getString("descripcio"),
                        eJSON.getString("imatge"),
                        new Recinte(eJSON.getString("recinte"), "")
                    );

                    JSONArray entradesJSON = eJSON.getJSONArray("tipusEntrades");
                    for (int i = 0; i < entradesJSON.length(); i++) {
                        JSONObject teJSON = entradesJSON.getJSONObject(i);

                        Zona zona = new Zona(teJSON.getString("zona"), "", "", 0);

                        TipusEntrada entrada = new TipusEntrada(
                            teJSON.getString("id"),
                            teJSON.getString("nom"),
                            zona,
                            teJSON.getDouble("preu"),
                            teJSON.getInt("quota")
                        );

                        esdev.afegirTipusEntrada(entrada);
                    }

                    esdeveniments.add(esdev);
                }

                for (Esdeveniment e : esdeveniments) {
                    System.out.println("Esdeveniment: " + e.getId() + " - " + e.getNom());
                    for (TipusEntrada te : e.getTipusEntrades()) {
                        System.out.println("  Entrada: " + te.getId() + " - " + te.getNom()
                            + " (zona " + te.getZona().getId() + ", preu " + te.getPreu()
                            + ", quota " + te.getQuota() + ")");
                    }
                }
                break;
            }
        }
    }

    private static void wsError(String response) {
        System.out.println("WS Error: " + response);
    }
}
