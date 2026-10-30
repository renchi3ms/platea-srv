package model;

import java.util.ArrayList;

public class Cataleg {

    private ArrayList<Recinte> recintes;
    private ArrayList<Esdeveniment> esdeveniments;

    public Cataleg() {
        recintes = new ArrayList<>();
        esdeveniments = new ArrayList<>();
    }

    public void afegirRecinte(Recinte r) {
        recintes.add(r);

    }

    public void afegirEsdeveniment(Esdeveniment e) {
        esdeveniments.add(e);
    }

    public ArrayList<Recinte> getRecintes() {
        return recintes;
    }

    public ArrayList<Esdeveniment> getEsdeveniments() {
        return esdeveniments;
    }
}