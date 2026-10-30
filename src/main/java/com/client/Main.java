package com.client;

import org.json.JSONObject;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public static UtilsWS wsClient;

    public static void main(String[] args) {

        // Iniciar app JavaFX   
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        connectToServer();
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

        // Enviar mensaje ping
        JSONObject ping = new JSONObject();
        ping.put("type", "ping");
        wsClient.safeSend(ping.toString());

        // Enviar mensaje bounce
        JSONObject bounce = new JSONObject();
        bounce.put("type", "bounce");
        wsClient.safeSend(bounce.toString());
    });

    wsClient.onMessage((response) -> {
        wsMessage(response);
    });

    wsClient.onError((response) -> {
        wsError(response);
    });
}
   

    private static void wsMessage(String response) {

        JSONObject msgObj = new JSONObject(response);

        switch (msgObj.getString("type")) {
            case "pong":
                System.out.println("Pong recibido: " + response);
                break;

            case "bounce":
                System.out.println("Bounce recibido: " + response);
                break;
            case "welcome":
                System.out.println(msgObj.getString("message"));
                break;
        }
    }

    private static void wsError(String response) {
        System.out.println("WS Error: " + response);
    }
}
