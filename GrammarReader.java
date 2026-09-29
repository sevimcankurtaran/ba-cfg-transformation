import java.io.*;
import java.util.*;

public class GrammarReader {

    /** Liest eine Grammatikdatei ein (Zeilenformat: "A -> a b | c", siehe Abschnitt 5.2). */
    public static Grammatik leseGrammatik(String dateiname) throws IOException {
        Grammatik grammatik = new Grammatik();
        try (BufferedReader leser = new BufferedReader(new FileReader(dateiname))) {
            String zeile;
            while ((zeile = leser.readLine()) != null) {
                // Verarbeite nur Zeilen, die eine Produktionsregel enthalten
                if (zeile.contains("->")) {
                    String[] seiten = zeile.split("->");
                    String links = seiten[0].trim(); // Linkes Symbol (Nichtterminal)

                    // Verarbeite alle Alternativen auf der rechten Seite
                    String[] alternativen = seiten[1].trim().split("\\|");
                    for (String alternative : alternativen) {
                        // Prüfe, ob die rechte Seite nur `ε` enthält
                        if (alternative.trim().equals("ε")) {
                            grammatik.addProduktion(new Produktion(links, Collections.emptyList()));
                        } else {
                            // Zerlege die rechte Seite in einzelne Symbole
                            List<String> rechts = new ArrayList<>(Arrays.asList(alternative.trim().split("\\s+")));
                            grammatik.addProduktion(new Produktion(links, rechts));
                        }
                    }
                }
            }
        }

        // Startsymbol einmal festhalten: das erste Nichtterminal in Lesereihenfolge.
        // Nur hier ist das noch zuverlässig (siehe Grammatik.startsymbol).
        if (!grammatik.getNichtterminale().isEmpty()) {
            grammatik.setStartsymbol(grammatik.getNichtterminale().iterator().next());
        }

        return grammatik;
    }
}
