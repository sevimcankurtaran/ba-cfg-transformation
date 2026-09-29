import java.util.*;

/** Berechnet First- (fi) und Follow-Mengen (fo) einer CFG, Definition 2.11 / 2.12, Abschnitt 2.3 / 5.5.1. */
public class FirstFollowSets {

    /** Epsilon für ε in den Mengen */
    public static final String EPSILON = "ε";
    /** und EOF Eingabeende $ in Follow-Mengen */
    public static final String EOF     = "$";

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /** Berechnet fi(A) für alle Nichtterminale A, iterativ bis zum Ende. */
    public static Map<String, Set<String>> berechneFirstSets(Grammatik g) {
        Map<String, Set<String>> first = new HashMap<>();

        // 1: Start - leere Menge für jedes Nichtterminal
        for (String nt : g.getNichtterminale()) {
            first.put(nt, new HashSet<>());
        }

        boolean geaendert = true;
        while (geaendert) { // 2: bis zum letzten Punkt wiederholen - ein Durchlauf ohne Änderung heißt fertig
            geaendert = false;
            for (Produktion p : g.getProduktionen()) { // 3: jede Produktion A -> α einmal anwenden
                String A     = p.getLinks();
                Set<String> fiA = first.get(A);
                // 4: fi(α) für die rechte Seite dieser Produktion berechnen
                Set<String> fiAlpha = berechneFirstVonFolge(p.getRechts(), first, g);
                if (fiA.addAll(fiAlpha)) geaendert = true; // 5: fi(A) gewachsen -> noch ein Durchlauf nötig
            }
        }
        return first;
    }

    /** Berechnet fo(A) für alle Nichtterminale A nach Lemma 7.1, iterativ bis zum Fixpunkt. */
    public static Map<String, Set<String>> berechneFollowSets(
            Grammatik g, Map<String, Set<String>> firstSets) {

        // 1: Start - leere Menge für jedes Nichtterminal
        Map<String, Set<String>> follow = new HashMap<>();
        for (String nt : g.getNichtterminale()) {
            follow.put(nt, new HashSet<>());
        }

        // 2: Regel (1): Startsymbol bekommt $ (EOF)
        // Explizit über g.getStartsymbol() statt über NT-Reihenfolge, LC / LCLR kann Reihenfolge verändern

        String start = g.getStartsymbol();
        follow.get(start).add(EOF);

        boolean geaendert = true;
        while (geaendert) { // 3: bis zum letzten Punkt wiederholen - ein Durchlauf ohne Änderung heißt fertig
            geaendert = false;
            for (Produktion p : g.getProduktionen()) { // 4: jede Produktion A → X1...Xk durchgehen
                String A = p.getLinks();
                List<String> rechts = p.getRechts();

                for (int i = 0; i < rechts.size(); i++) { // 5: jede Position i mit Nichtterminal B = Xi prüfen
                    String B = rechts.get(i);
                    if (!g.getNichtterminale().contains(B)) continue; // nur NTs

                    // 6: β = alles nach B in dieser Produktion, fi(β) berechnen
                    List<String> beta = rechts.subList(i + 1, rechts.size());
                    Set<String> fiBeta = berechneFirstVonFolge(beta, firstSets, g);

                    // 7: Regel (2): fi(β) \ {ε} ⊆ fo(B)
                    for (String terminal : fiBeta) {
                        if (!terminal.equals(EPSILON)) {
                            if (follow.get(B).add(terminal)) geaendert = true;
                        }
                    }

                    // 8: Regeln (3) und (4): falls ε ∈ fi(β) → fo(A) ⊆ fo(B)
                    if (fiBeta.contains(EPSILON)) {
                        if (follow.get(B).addAll(follow.get(A))) geaendert = true;
                    }
                }
            }
        }
        return follow;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HILFSMETHODE: fi einer Symbolfolge
    // ─────────────────────────────────────────────────────────────────────────

    /** Berechnet fi(α) für eine Symbolfolge α = X1 X2 ... Xn. */
    public static Set<String> berechneFirstVonFolge(
            List<String> folge,
            Map<String, Set<String>> firstSets,
            Grammatik g) {

        Set<String> ergebnis = new HashSet<>();

        if (folge.isEmpty()) {
            // leere Folge = ε
            ergebnis.add(EPSILON);
            return ergebnis;
        }

        boolean alleKoennenEpsilon = true; // 1: optimistisch starten - bis ein Symbol das Gegenteil beweist

        for (String symbol : folge) { // 2: Symbol für Symbol, von links
            Set<String> fiSymbol = new HashSet<>();

            if (g.getNichtterminale().contains(symbol)) {
                // 3: Nichtterminal: fi aus der bereits berechneten Map holen
                fiSymbol.addAll(firstSets.get(symbol));
            } else {
                // 4: Terminal (inkl. ε): fi = {symbol}, ein Terminal ist immer nur es selbst
                fiSymbol.add(symbol);
            }

            // 5: alles außer ε direkt ins Ergebnis übernehmen - ε heben wir uns bis zum Schluss auf
            for (String terminal : fiSymbol) {
                if (!terminal.equals(EPSILON)) ergebnis.add(terminal);
            }

            // 6: wenn ε nicht enthalten → alles danach in der Folge ist irrelevant-> Kette bricht hier ab
            if (!fiSymbol.contains(EPSILON)) {
                alleKoennenEpsilon = false;
                break;
            }
        }

        // 7: ε nur hinzufügen, wenn wir die ganze Folge durchlaufen haben, ohne bei Schritt 6 abzubrechen
        if (alleKoennenEpsilon) ergebnis.add(EPSILON);

        return ergebnis;
    }
}
