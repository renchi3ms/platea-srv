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
            System.out.println("4. purchase");
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
                case "4":
                    sendPurchase(scanner);
                    break;
                case "0":
                    System.out.println("Bye!");

                    if (wsClient != null) {
                        wsClient.forceExit();
                    }

                    System.exit(0);
                    break;
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

    private static void sendPurchase(Scanner scanner) {
        System.out.print("ID de l'esdeveniment: ");
        String esdevenimentId = scanner.nextLine().trim();

        System.out.print("ID del tipus d'entrada: ");
        String tipusEntradaId = scanner.nextLine().trim();

        System.out.print("Quantitat: ");
        int quantitat;

        try {
            quantitat = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Quantitat no valida.");
            return;
        }

        System.out.print("Nom del comprador: ");
        String nombre = scanner.nextLine().trim();

        System.out.print("Correu del comprador: ");
        String correo = scanner.nextLine().trim();

        JSONObject entrada = new JSONObject();
        entrada.put("esdevenimentId", esdevenimentId);
        entrada.put("tipusEntradaId", tipusEntradaId);
        entrada.put("quantitat", quantitat);

        JSONArray entradas = new JSONArray();
        entradas.put(entrada);

        JSONObject msg = new JSONObject();
        msg.put("type", "purchase");
        msg.put("nombre", nombre);
        msg.put("correo", correo);
        msg.put("entradas", entradas);

        inbox.clear();
        wsClient.safeSend(msg.toString());

        try {
            String response = inbox.poll(5, TimeUnit.SECONDS);

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
        wsClient = UtilsWS.getSharedInstance("wss://platea4.ieti.site:443");

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
            case "purchase": {
                String status = msgObj.optString("status", "ERROR");

                if ("OK".equals(status)) {
                    System.out.println("COMPRA CORRECTA");

                    JSONArray localitzadors = msgObj.getJSONArray("localitzadors");

                    for (int i = 0; i < localitzadors.length(); i++) {
                        JSONObject reserva = localitzadors.getJSONObject(i);

                        System.out.println(
                            "Esdeveniment: " + reserva.getString("esdevenimentId")
                            + " | Tipus entrada: " + reserva.getString("tipusEntradaId")
                            + " | Quantitat: " + reserva.getInt("quantitat")
                            + " | Localitzador: " + reserva.getString("localitzador")
                        );
                    }
                } else {
                    System.out.println("ERROR EN LA COMPRA: "
                            + msgObj.optString("message", "Error desconegut."));
                }
                break;
            }
        }
    }

    private static void wsError(String response) {
        System.out.println("WS Error: " + response);
    }
}
