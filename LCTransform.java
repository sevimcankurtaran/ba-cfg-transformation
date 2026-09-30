import java.util.*;

/**
 * Left-Corner Transform (LC), Definition 4.7 / 4.8, Abschnitt 4.4 / Implementierung 5.4.4
 * Transformiert alle retained Nichtterminale (nicht nur linksrekursive wie LCLR) über
 * drei Schemata
 *   Schema 1: a Terminal, echte Linksecke von retained A,dann   --> A -> a A-a
 *   Schema 2: B echte Linksecke von retained A und  B -> X β, dann      --> A-X -> β A-B
 *             ( wenn B = A nur wenn A linksrekursiv, sonst übernimmt Schema 3)
 *   Schema 3: X echte Linksecke von retained A, A -> X β:      --> A-X -> β
 */
public class LCTransform {

    private static final String SEP = "_LC_";

    public static Grammatik apply(Grammatik grammatik) {
        Grammatik orig = GrammarUtils.kopiereGrammatik(grammatik);

        String startsymbol = orig.getStartsymbol();
        Set<String> retainedNTs = berechneRetainedNTs(orig, startsymbol); // retained NTs bestimmen

        Set<String> lrNTs = GrammarUtils.berechneLinksrekursiveNTs(orig); // für Schema-2-Sonderfall

        Map<String, Set<String>> echteLinksecken = berechneProperLC(orig, retainedNTs); // echte Linksecken pro retained NT

        Grammatik result = new Grammatik();

        for (String A : retainedNTs) { // 1: Grammatik pro retained NT aufbauen
            Set<String> echteLinkseckenVonA = echteLinksecken.get(A);

            // 2: Schema 1     A -> a Y  -->  A -> a A-a
            for (String a : echteLinkseckenVonA) {
                if (orig.getTerminale().contains(a)) {
                    result.addProduktion(new Produktion(A, Arrays.asList(a, lcName(A, a))));
                }
            }

            // 3: Schema 2 A->B Y und B -> X β  -->   A-X -> β A-B    (B=A nur wenn A linksrekursiv)
            for (String B : echteLinkseckenVonA) {
                if (!orig.getNichtterminale().contains(B)) continue; // nur Nichtterminale
                if (B.equals(A) && !lrNTs.contains(A)) continue;    //  B=A

                for (Produktion bProduktion : GrammarUtils.getProduktionenFuer(orig, B)) {
                    if (bProduktion.getRechts().isEmpty()) continue;
                    String X         = bProduktion.getRechts().get(0);
                    List<String> beta = bProduktion.getRechts().subList(1, bProduktion.getRechts().size());
                    List<String> neueRechteSeite = new ArrayList<>(beta);
                    neueRechteSeite.add(lcName(A, B));
                    result.addProduktion(new Produktion(lcName(A, X), neueRechteSeite));
                }
            }

            // 4: Schema 3  A -> X β  -->  A-X -> β (X ist immer Linksecke von A, siehe berechneProperLC)
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
     * Retained Nichtterminale: Startsymbol plus alle NTs, die (mindestens einmal) an nicht-linkester Position vorkommen (Definition 4.7, Abschnitt 4.4).
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
     * Echte Linksecken (proper left corners) für retained NTs.
     *
     * VOLLE transitive Verfolgung: alle Nichtterminale (nicht nur LR-NTs).
     * Das ist der zentrale Unterschied zu LCLR, wo nur durch LR-NTs verfolgt wird.
     */
    private static Map<String, Set<String>> berechneProperLC(
            Grammatik g, Set<String> retainedNTs) {
        Map<String, Set<String>> result = new HashMap<>();

        for (String A : retainedNTs) { // 1: für jedes retained NT eine eigene BFS
            Set<String> echteLinksecken = new HashSet<>();
            Queue<String> queue = new LinkedList<>();

            for (Produktion p : g.getProduktionen()) { // 2: direkte Linksecken als Startpunkte
                if (p.getLinks().equals(A) && !p.getRechts().isEmpty()) {
                    String X = p.getRechts().get(0);
                    if (echteLinksecken.add(X)) queue.add(X);
                }
            }

            while (!queue.isEmpty()) { // 3: transitiv weiterverfolgen, über ALLE Nichtterminale
                String aktuell = queue.poll();
                if (!g.getNichtterminale().contains(aktuell)) continue; // Terminal: Blatt
                for (Produktion p : g.getProduktionen()) {
                    if (p.getLinks().equals(aktuell) && !p.getRechts().isEmpty()) {
                        String X = p.getRechts().get(0);
                        if (echteLinksecken.add(X)) queue.add(X);
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
