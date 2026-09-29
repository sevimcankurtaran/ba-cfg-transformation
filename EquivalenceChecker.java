import java.util.*;

/**
 * Beschränkte Äquivalenzprüfung zweier kontextfreier Grammatiken, Abschnitt 5.5.4:
 * Unterscheiden sich L_n(G) und L_n(G'), sind sie NICHT äquivalent (Beweis);
 * stimmen sie überein, ist das kein direkter Beweis !! (Bemerkung 5.1)
 */
public class EquivalenceChecker {

    // ─────────────────────────────────────────────────────────────────────────
    // Sprache bis Länge n
    // ─────────────────────────────────────────────────────────────────────────

    /** Berechnet L_n(S) = alle aus dem Startsymbol ableitbaren Terminalwörter mit max. n Länge */
    public static Set<List<String>> spracheBisLaenge(Grammatik g, String startsymbol, int n) {
        // 1: Start mit L_n(A) = leer, für jedes Nichtterminal
        Map<String, Set<List<String>>> sprache = new HashMap<>();
        for (String nt : g.getNichtterminale()) {
            sprache.put(nt, new HashSet<>());
        }

        boolean geaendert = true;
        while (geaendert) { // 2: bis zum Fixpunkt wiederholen - ein Durchlauf ohne Änderung heißt fertig
            geaendert = false;
            for (Produktion p : g.getProduktionen()) {
                // 3: was diese eine Produktion mit dem bisherigen Wissen erzeugen kann
                Set<List<String>> neu = woerterVonFolge(p.getRechts(), sprache, g, n);

                // 4: Menge der linken Seite holen (oder anlegen, falls noch keine da)
                Set<List<String>> bisher = sprache.get(p.getLinks());
                if (bisher == null) {
                    bisher = new HashSet<>();
                    sprache.put(p.getLinks(), bisher);
                }

                if (bisher.addAll(neu)) geaendert = true; // 5: Menge gewachsen, noch ein Durchlauf nötig
            }
        }

        // 6: nur die Sprache des gesuchten Startsymbols zurückgeben, der Rest war Zwischenergebnis
        return sprache.getOrDefault(startsymbol, Collections.emptySet());
    }

    /** Alle Terminalwörter bis Länge n, die aus X_1...Xk ableitbar sind, auf Basis der bekannten L_n(Xi). */
    private static Set<List<String>> woerterVonFolge(
            List<String> folge,
            Map<String, Set<List<String>>> sprache,
            Grammatik g,
            int n) {

        // 1: Anfangs leeres Wort    (noch kein Symbol verarbeitet)
        Set<List<String>> ergebnis = new HashSet<>();
        ergebnis.add(new ArrayList<>());

        for (String symbol : folge) { // 2: folge von links nach rechts abarbeiten

            // Was bringt dieses Symbol mit sich?
            Set<List<String>> teilwoerter;
            if (g.getNichtterminale().contains(symbol)) {
                // Nichtterminal: bisher bekannte Wörter nachschlagen
                teilwoerter = sprache.get(symbol);
                if (teilwoerter == null) {
                    teilwoerter = new HashSet<>();
                }
            } else {
                // 3: Terminal: steht für sich selbst: also Wort der Länge 1
                List<String> wort = new ArrayList<>();
                wort.add(symbol);
                teilwoerter = new HashSet<>();
                teilwoerter.add(wort);
            }

            // 4: Kreuzprodukt - jedes bisherige Teilwort mit jedem dieser Wörter verlängern
            Set<List<String>> naechste = new HashSet<>();
            for (List<String> praefix : ergebnis) {
                for (List<String> teil : teilwoerter) {
                    if (praefix.size() + teil.size() > n) {
                        continue;   // 6: wenn zu lang, verwerfen
                    }
                    List<String> kombiniert = new ArrayList<>(praefix);
                    kombiniert.addAll(teil);
                    naechste.add(kombiniert);
                }
            }

            ergebnis = naechste; // 7: neuer Stand, ein Symbol weiter
        }

        return ergebnis;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vergleich zweier Grammatiken
    // ─────────────────────────────────────────────────────────────────────────

    /**Vergleicht L_n 2er Grammatiken und liefert die Differenzmengen.*/
    public static Ergebnis vergleiche(
            Grammatik g1, String start1,
            Grammatik g2, String start2,
            int n) {

        Set<List<String>> spracheG1 = spracheBisLaenge(g1, start1, n);
        Set<List<String>> spracheG2 = spracheBisLaenge(g2, start2, n);

        Set<List<String>> nurInG1 = new HashSet<>(spracheG1);
        nurInG1.removeAll(spracheG2);
        Set<List<String>> nurInG2 = new HashSet<>(spracheG2);
        nurInG2.removeAll(spracheG1);

        return new Ergebnis(n, spracheG1, spracheG2, nurInG1, nurInG2);
    }

    /** Ergebnis eines beschränkten Vergleichs */
    public static class Ergebnis {
        public final int schranke;
        public final Set<List<String>> spracheG1;
        public final Set<List<String>> spracheG2;
        public final Set<List<String>> nurInG1;
        public final Set<List<String>> nurInG2;

        Ergebnis(int schranke,
                 Set<List<String>> spracheG1, Set<List<String>> spracheG2,
                 Set<List<String>> nurInG1, Set<List<String>> nurInG2) {
            this.schranke  = schranke;
            this.spracheG1 = spracheG1;
            this.spracheG2 = spracheG2;
            this.nurInG1   = nurInG1;
            this.nurInG2   = nurInG2;
        }

        /** true, wenn L_n übereinstimmt -> kein Beweis!! für echte Äquivalenz */
        public boolean stimmtUeberein() {
            return nurInG1.isEmpty() && nurInG2.isEmpty();
        }
    }
}