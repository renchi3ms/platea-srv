package com.server;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {

        try (
                Connection conn = Database.connect();
                Statement stmt = conn.createStatement()
        ) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recintes (
                    id TEXT PRIMARY KEY,
                    nom TEXT NOT NULL
                )
            """);

            // ZONES
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS zones (
                    id TEXT PRIMARY KEY,
                    nom TEXT NOT NULL,
                    tipus TEXT NOT NULL,
                    aforament INTEGER NOT NULL,
                    recinte_id TEXT NOT NULL,
                    FOREIGN KEY (recinte_id) REFERENCES recintes(id)
                )
            """);

            // SEIENTS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS seients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    zona_id TEXT NOT NULL,
                    fila INTEGER NOT NULL,
                    numero INTEGER NOT NULL,
                    estat TEXT NOT NULL DEFAULT 'lliure',
                    venda_ref TEXT,
                    esdeveniment_id TEXT NOT NULL,
                    FOREIGN KEY (zona_id) REFERENCES zones(id),
                    UNIQUE(esdeveniment_id, zona_id, fila, numero)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS esdeveniments (
                    id TEXT PRIMARY KEY,
                    nom TEXT NOT NULL,
                    data_hora TEXT NOT NULL,
                    categoria TEXT NOT NULL,
                    descripcio TEXT,
                    imatge TEXT,
                    recinte_id TEXT NOT NULL,
                    FOREIGN KEY (recinte_id) REFERENCES recintes(id)
                )
            """);

            // TIPUS_ENTRADA
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tipus_entrada (
                    id TEXT PRIMARY KEY,
                    nom TEXT NOT NULL,
                    preu REAL NOT NULL,
                    quota INTEGER NOT NULL,
                    zona_id TEXT NOT NULL,
                    esdeveniment_id TEXT NOT NULL,
                    FOREIGN KEY (zona_id) REFERENCES zones(id),
                    FOREIGN KEY (esdeveniment_id) REFERENCES esdeveniments(id)
                )
            """);

            // COMPRES
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS compres (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    codi TEXT UNIQUE NOT NULL,
                    comprador_nom TEXT NOT NULL,
                    comprador_correu TEXT NOT NULL,
                    data_compra TEXT NOT NULL
                )
            """);

            // ENTRADES
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS entrades (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    localitzador TEXT UNIQUE NOT NULL,
                    tipus_entrada_id TEXT NOT NULL,
                    seient_id INTEGER,
                    compra_id INTEGER NOT NULL,
                    preu_pagat REAL NOT NULL,
                    data_emissio TEXT NOT NULL,
                    estat TEXT NOT NULL,
                    FOREIGN KEY (tipus_entrada_id) REFERENCES tipus_entrada(id),
                    FOREIGN KEY (seient_id) REFERENCES seients(id),
                    FOREIGN KEY (compra_id) REFERENCES compres(id)
                )
            """);

            System.out.println("Base de dades inicialitzada correctament");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}