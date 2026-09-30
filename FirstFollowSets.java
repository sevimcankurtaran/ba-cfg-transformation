import java.util.*;

/** Berechnet First- (fi) und Follow-Mengen (fo) einer CFG, Definition 2.11 / 2.12, Abschnitt 2.3 / 5.5.1. */
public class FirstFollowSets {

    /** Repräsentiert ε in den First/Follow-Mengen. */
    public static final String EPSILON = "ε";
    /** Repräsentiert das Eingabeende $ in Follow-Mengen. */
    public static final String EOF     = "$";

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /** Berechnet fi(A) für alle Nichtterminale A, iterativ bis zum Fixpunkt. */
    public static Map<String, Set<String>> berechneFirstSets(Grammatik g) {
        Map<String, Set<String>> first = new HashMap<>();
        for (String nt : g.getNichtterminale()) {
            first.put(nt, new HashSet<>());
        }

        boolean geaendert = true;
        while (geaendert) { // Fixpunkt: ein Durchlauf ohne Änderung heißt fertig
            geaendert = false;
            for (Produktion p : g.getProduktionen()) {
                String A     = p.getLinks();
                Set<String> fiA = first.get(A);
                Set<String> fiAlpha = berechneFirstVonFolge(p.getRechts(), first, g);
                if (fiA.addAll(fiAlpha)) geaendert = true;
            }
        }
        return first;
    }

    /** Berechnet fo(A) für alle Nichtterminale A nach Lemma 7.1, iterativ bis zum Fixpunkt. */
    public static Map<String, Set<String>> berechneFollowSets(
            Grammatik g, Map<String, Set<String>> firstSets) {

        Map<String, Set<String>> follow = new HashMap<>();
        for (String nt : g.getNichtterminale()) {
            follow.put(nt, new HashSet<>());
        }

        // Regel (1): Startsymbol bekommt $. Explizit über g.getStartsymbol(),
        // da LC / LCLR die NT-Reihenfolge verändern können.
        String start = g.getStartsymbol();
        follow.get(start).add(EOF);

        boolean geaendert = true;
        while (geaendert) { // Fixpunkt: ein Durchlauf ohne Änderung heißt fertig
            geaendert = false;
            for (Produktion p : g.getProduktionen()) {
                String A = p.getLinks();
                List<String> rechts = p.getRechts();

                for (int i = 0; i < rechts.size(); i++) {
                    String B = rechts.get(i);
                    if (!g.getNichtterminale().contains(B)) continue;

                    List<String> beta = rechts.subList(i + 1, rechts.size());
                    Set<String> fiBeta = berechneFirstVonFolge(beta, firstSets, g);

                    // Regel (2): fi(β) \ {ε} ⊆ fo(B)
                    for (String terminal : fiBeta) {
                        if (!terminal.equals(EPSILON)) {
                            if (follow.get(B).add(terminal)) geaendert = true;
                        }
                    }

                    // Regeln (3) und (4): falls ε ∈ fi(β), dann fo(A) ⊆ fo(B)
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
            ergebnis.add(EPSILON); // leere Folge = ε
            return ergebnis;
        }

        boolean alleKoennenEpsilon = true; // bis ein Symbol das Gegenteil beweist

        for (String symbol : folge) {
            Set<String> fiSymbol = new HashSet<>();

            if (g.getNichtterminale().contains(symbol)) {
                fiSymbol.addAll(firstSets.get(symbol));
            } else {
                fiSymbol.add(symbol); // Terminal ist immer nur es selbst
            }

            for (String terminal : fiSymbol) {
                if (!terminal.equals(EPSILON)) ergebnis.add(terminal);
            }

            if (!fiSymbol.contains(EPSILON)) { // Symbol kann nicht verschwinden -> Kette bricht ab
                alleKoennenEpsilon = false;
                break;
            }
        }

        if (alleKoennenEpsilon) ergebnis.add(EPSILON); // nur wenn die ganze Folge durchlaufen wurde
        return ergebnis;
    }
}
