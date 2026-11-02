package com.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.model.Cataleg;
import com.model.Esdeveniment;
import com.model.TipusEntrada;

class ServeiXMLTest {

    private static Cataleg cataleg;

    @BeforeAll
    static void carregarCataleg() {
        cataleg = new ServeiXML().carregarCataleg("assets/plantilla.xml");
    }

    private static Esdeveniment buscarEsdeveniment(String id) {
        return cataleg.getEsdeveniments().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Test
    void carregaTotsElsEsdeveniments() {
        assertEquals(10, cataleg.getEsdeveniments().size());
    }

    @Test
    void cadaEsdevenimentTeTipusEntrada() {
        for (Esdeveniment e : cataleg.getEsdeveniments()) {
            assertFalseEmpty(e);
        }
    }

    private void assertFalseEmpty(Esdeveniment e) {
        assertNotNull(e.getTipusEntrades());
        assertTrue(e.getTipusEntrades().size() > 0,
                "L'esdeveniment " + e.getId() + " no te cap tipus d'entrada");
    }

    @Test
    void esdevenimentE1TeLesSevesEntrades() {
        Esdeveniment e1 = buscarEsdeveniment("E1");

        assertNotNull(e1);
        assertEquals("R1", e1.getRecinte().getId());

        List<String> ids = e1.getTipusEntrades().stream()
                .map(TipusEntrada::getId)
                .collect(Collectors.toList());

        assertEquals(List.of("T1", "T2", "T3"), ids);
    }

    @Test
    void entradaTePreuQuotaIZona() {
        Esdeveniment e1 = buscarEsdeveniment("E1");

        TipusEntrada t1 = e1.getTipusEntrades().get(0);

        assertEquals("Platea", t1.getNom());
        assertEquals("Z1", t1.getZona().getId());
        assertEquals(18.00, t1.getPreu(), 0.001);
        assertEquals(50, t1.getQuota());
    }

    @Test
    void capEsdevenimentQuedaSenseRecinte() {
        for (Esdeveniment e : cataleg.getEsdeveniments()) {
            assertNotNull(e.getRecinte(),
                    "L'esdeveniment " + e.getId() + " no te recinte");
        }
    }

    @Test
    void capEntradaQuedaSenseZona() {
        for (Esdeveniment e : cataleg.getEsdeveniments()) {
            for (TipusEntrada te : e.getTipusEntrades()) {
                assertNotNull(te.getZona(),
                        "L'entrada " + te.getId() + " no te zona");
            }
        }
    }
}
