import java.util.*;

/**
 * Left Factoring (LF), Aho, Definition 4.2, Abschnitt 4.1 / Implementierung 5.4.1.
 * Extrahiert je Gruppe gleich beginnender Produktionen deren längsten gemeinsamen
 * Präfix (LCP) und wendet das wiederholt an, bis nichts mehr faktorisierbar ist.
 */
public class LeftFactoring {

    public static Grammatik applyLeftFactoring(Grammatik grammatik) {
        Grammatik g = GrammarUtils.kopiereGrammatik(grammatik);
        boolean geaendert = true;

        // Iterativ anwenden, bis keine Änderung mehr möglich.
        // (Paper: "apply repeatedly, until it is no longer applicable")
        while (geaendert) {
            geaendert = false;

            // Snapshot der aktuellen Nichtterminale (inkl. neu erzeugter aus Voriterationen)
            List<String> nichtterminale = new ArrayList<>(g.getNichtterminale());

            for (String nt : nichtterminale) {
                List<Produktion> produktionen = GrammarUtils.getProduktionenFuer(g, nt);
                if (produktionen.size() < 2) continue;

                // Gruppieren nach erstem Symbol (ε-Produktionen überspringen)
                Map<String, List<Produktion>> gruppen = new LinkedHashMap<>();
                for (Produktion p : produktionen) {
                    if (p.getRechts().isEmpty()) continue; // ε hat keinen Präfix
                    String erstesSymbol = p.getRechts().get(0);
                    gruppen.computeIfAbsent(erstesSymbol, k -> new ArrayList<>()).add(p);
                }

                for (Map.Entry<String, List<Produktion>> entry : gruppen.entrySet()) {
                    List<Produktion> gruppe = entry.getValue();
                    if (gruppe.size() < 2) continue;

                    // Längsten gemeinsamen Präfix (LCP) aller Alternativen in der Gruppe berechnen
                    List<List<String>> alleRechtenSeiten = new ArrayList<>();
                    for (Produktion p : gruppe) {
                        alleRechtenSeiten.add(p.getRechts());
                    }
                    List<String> lcp = berechneLCP(alleRechtenSeiten);
                    if (lcp.isEmpty()) continue;

                    // Neues Nichtterminal für die Suffixe erzeugen
                    String neuesNichtterminal = GrammarUtils.createFreshNonterminal(nt, g);

                    // neue Produktion: nt → lcp neuesNichtterminal
                    List<String> neueRechteSeite = new ArrayList<>(lcp);
                    neueRechteSeite.add(neuesNichtterminal);
                    g.addProduktion(new Produktion(nt, neueRechteSeite));

                    // pro Gruppen-Produktion: Suffix-Produktion neuesNichtterminal → suffix
                    for (Produktion p : gruppe) {
                        // Suffix-Länge = lcp.size()
                        List<String> suffix = new ArrayList<>(
                                p.getRechts().subList(lcp.size(), p.getRechts().size()));
                        g.addProduktion(new Produktion(neuesNichtterminal, suffix)); // leere Liste = ε, korrekt
                        g.getProduktionen().remove(p); // alte Produktion entfernen
                    }

                    geaendert = true;
                }
            }
        }

        return g;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LCP-Berechnung
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Berechnet den längsten gemeinsamen Präfix (LCP) einer Liste von Symbolfolgen.
     *
     * Beispiel:
     *   [[a, b, c], [a, b, d]]  →  [a, b]
     *   [[a, b], [a, c]]        →  [a]
     *   [[x], [y]]              →  []
     */
    private static List<String> berechneLCP(List<List<String>> sequenzen) {
        if (sequenzen.isEmpty()) return Collections.emptyList();

        List<String> praefix = new ArrayList<>(sequenzen.get(0)); // 1: erste Sequenz als Start-Kandidat

        for (int i = 1; i < sequenzen.size(); i++) { // 2: mit jeder weiteren Sequenz vergleichen
            List<String> folge = sequenzen.get(i);
            int j = 0;
            while (j < praefix.size() && j < folge.size() && praefix.get(j).equals(folge.get(j))) {
                j++; // 3: so weit mitzählen, wie beide Sequenzen übereinstimmen
            }
            praefix = new ArrayList<>(praefix.subList(0, j)); // 4: praefix auf gemeinsame Länge kürzen
            if (praefix.isEmpty()) break; // 5: schon leer -> kann nicht mehr kürzer werden
        }

        return praefix;
    }
}