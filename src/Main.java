import model.*;
import servei.*;

public class Main {
    public static void main(String[] args) {
        ServeiXML serveiXML = new ServeiXML();
        
        Cataleg cataleg = serveiXML.carregarCataleg("src/dades/plantilla.xml");

        System.out.println("Recintos cargados: " + cataleg.getRecintes().size());
        System.out.println("Eventos cargados: " + cataleg.getEsdeveniments().size());

        for (Esdeveniment e : cataleg.getEsdeveniments()) {
            System.out.println("Evento: " + e.getNom() + " | Recinto: " + e.getRecinte().getNom() +
                    " | Fecha y hora: " + e.getDataHora() + " | Categoría: " + e.getCategoria());
        }
    }
}
