package com.server;

import java.util.List;

public class SeientsOcupatsException extends RuntimeException {

    private final List<String> seients;

    public SeientsOcupatsException(List<String> seients) {
        super("Seients ja ocupats: " + String.join(", ", seients));
        this.seients = seients;
    }

    public List<String> getSeients() {
        return seients;
    }
}