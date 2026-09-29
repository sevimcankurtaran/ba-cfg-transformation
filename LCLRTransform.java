import java.util.*;

/**
 * Left-Corner Transform für linksrekursive Nichtterminale (LCLR), Definition 4.11, Implementierung 5.4.5.
 * Wie LCTransform, aber nur retained UND linksrekursive NTs werden
 * über die Schemata 1-3 transformiert; alle anderen laufen über Schema 4 unverändert durch
 * und gelten für die Linksecken-Kette als Blatt (siehe berechneProperLC).
 *
 * Vier Schemata:
 *   Schema 1: X (Terminal oder nicht-LR-NT) echte Linksecke von retained LR-NT A, dann --> X A_LC_X
 *   Schema 2: B (LR) echte Linksecke von retained LR-NT A, B -> X β, dann --> A_LC_X -> β A_LC_B
 *   Schema 3: X echte Linksecke von retained LR-NT A, A -> X β, dann --> A_LC_X -> β
 *   Schema 4: A nicht-LR-NT, A -> β, dann --> A -> β (keine Veränderung)
 */
public class LCLRTransform {

    /** Trennzeichen für neue Nichtterminal-Namen: "A_LC_X" */
    private static final String SEP = "_LC_";

    public static Grammatik apply(Grammatik grammatik) {
        Grammatik orig = GrammarUtils.kopiereGrammatik(grammatik);

        Set<String> lrNTs = GrammarUtils.berechneLinksrekursiveNTs(orig); // linksrekursive NTs bestimmen

        String startsymbol = orig.getStartsymbol();
        Set<String> retainedNTs = berechneRetainedNTs(orig, startsymbol); // retained NTs bestimmen

        Set<String> retainedLR = new HashSet<>(lrNTs); // retained UND linksrekursiv
        retainedLR.retainAll(retainedNTs);

        Map<String, Set<String>> echteLinksecken = berechneProperLC(orig, lrNTs, retainedLR); // echte Linksecken pro retained LR-NT

        Grammatik result = new Grammatik();

        // 1: Schema 4 - Produktionen nicht-linksrekursiver NTs unverändert übernehmen
        for (Produktion p : orig.getProduktionen()) {
            if (!lrNTs.contains(p.getLinks())) {
                result.addProduktion(new Produktion(p.getLinks(), new ArrayList<>(p.getRechts())));
            }
        }

        for (String A : retainedLR) { // 2: Grammatik pro retained LR-NT aufbauen
            Set<String> echteLinkseckenVonA = echteLinksecken.get(A);

            // 3: Schema 1  (Terminal oder nicht-LR-NT X als Linksecke): A -> X A_LC_X
            for (String X : echteLinkseckenVonA) {
                if (!lrNTs.contains(X)) { // X ist Terminal oder nicht-LR-NT
                    result.addProduktion(new Produktion(A, Arrays.asList(X, lcName(A, X))));
                }
            }

            // 4: Schema 2  (LR-NT B als Linksecke, B -> X β): A_LC_X -> β A_LC_B
            for (String B : echteLinkseckenVonA) {
                if (!lrNTs.contains(B)) continue; // B muss LR sein
                for (Produktion bProduktion : GrammarUtils.getProduktionenFuer(orig, B)) {
                    if (bProduktion.getRechts().isEmpty()) continue;
                    String X    = bProduktion.getRechts().get(0);
                    List<String> beta = bProduktion.getRechts().subList(1, bProduktion.getRechts().size());
                    List<String> neueRechteSeite = new ArrayList<>(beta);
                    neueRechteSeite.add(lcName(A, B)); // A_LC_B als Fortsetzungs-NT
                    result.addProduktion(new Produktion(lcName(A, X), neueRechteSeite));
                }
            }

            // 5: Schema 3  (eigene Produktion A -> X β): A_LC_X -> β (X ist immer Linksecke von A)
            for (Produktion aProduktion : GrammarUtils.getProduktionenFuer(orig, A)) {
                if (aProduktion.getRechts().isEmpty()) continue;
                String X = aProduktion.getRechts().get(0);
                List<String> beta = new ArrayList<>(
                        aProduktion.getRechts().subList(1, aProduktion.getRechts().size()));
                result.addProduktion(new Produktion(lcName(A, X), beta));
            }
        }

        result.setStartsymbol(startsymbol);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hilfsmethoden
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retained Nichtterminale: Startsymbol + alle NTs, die in nicht-linksster Position
     * einer Produktion vorkommen (Definition 4.7, Abschnitt 4.4).
     */
    private static Set<String> berechneRetainedNTs(Grammatik g, String startsymbol) {
        Set<String> retained = new HashSet<>();
        retained.add(startsymbol); // 1: Startsymbol ist immer retained
        for (Produktion p : g.getProduktionen()) { // 2: NTs in nicht-linkster Position sammeln
            List<String> rechts = p.getRechts();
            for (int i = 1; i < rechts.size(); i++) {
                if (g.getNichtterminale().contains(rechts.get(i))) {
                    retained.add(rechts.get(i));
                }
            }
        }
        return retained;
    }

    /**
     * Berechnet die echten Linksecken (proper left corners) für jedes retained LR-NT.
     *
     * Besonderheit gegenüber der normalen Linksecken-Relation:
     * Die transitive Kette wird nur durch LR-Nichtterminale verfolgt.
     * Terminale und nicht-LR-NTs sind Blätter (werden nicht weiter expandiert).
     */
    private static Map<String, Set<String>> berechneProperLC(
            Grammatik g, Set<String> lrNTs, Set<String> retainedLR) {
        Map<String, Set<String>> result = new HashMap<>();

        for (String A : retainedLR) { // 1: für jedes retained LR-NT eine eigene BFS
            Set<String> echteLinksecken = new HashSet<>();
            Queue<String> queue = new LinkedList<>();

            for (Produktion p : g.getProduktionen()) { // 2: direkte Linksecken als Startpunkte
                if (p.getLinks().equals(A) && !p.getRechts().isEmpty()) {
                    String erstesSymbol = p.getRechts().get(0);
                    if (echteLinksecken.add(erstesSymbol)) queue.add(erstesSymbol);
                }
            }

            while (!queue.isEmpty()) { // 3: transitiv weiterverfolgen, aber NUR durch LR-NTs
                String aktuell = queue.poll();
                if (!lrNTs.contains(aktuell)) continue; // Terminal oder nicht-LR-NT: Blatt
                for (Produktion p : g.getProduktionen()) {
                    if (p.getLinks().equals(aktuell) && !p.getRechts().isEmpty()) {
                        String erstesSymbol = p.getRechts().get(0);
                        if (echteLinksecken.add(erstesSymbol)) queue.add(erstesSymbol);
                    }
                }
            }

            result.put(A, echteLinksecken);
        }
        return result;
    }

    /** Erzeugt den Namen für ein neues "A_LC_X"-Nichtterminal. */
    private static String lcName(String A, String X) {
        return A + SEP + X;
    }
}
