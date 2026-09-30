import java.util.*;

/** LL(1)-Prüfung und Grammatikgröße-Messung, Definition 2.14 / 2.16, Abschnitt 2.4 / 5.5.2 / 5.5.3. */
public class LL1Checker {

    // ─────────────────────────────────────────────────────────────────────────
    // LOOKAHEAD-MENGEN
    // ─────────────────────────────────────────────────────────────────────────

    /** Berechnet la(A → α) = fi(α) \ {ε}, plus fo(A) falls ε ∈ fi(α), für alle Produktionen. */
    public static Map<Produktion, Set<String>> berechneLookaheadSets(
            Grammatik g,
            Map<String, Set<String>> firstSets,
            Map<String, Set<String>> followSets) {

        Map<Produktion, Set<String>> lookahead = new LinkedHashMap<>();

        for (Produktion p : g.getProduktionen()) {
            String A = p.getLinks();

            // 1: fi(α) berechnen
            Set<String> fiAlpha = FirstFollowSets.berechneFirstVonFolge(
                    p.getRechts(), firstSets, g);

            Set<String> lookaheadMenge = new HashSet<>();

            // 2: fi(α) \ {ε} hinzufügen
            for (String symbol : fiAlpha) {
                if (!symbol.equals(FirstFollowSets.EPSILON)) lookaheadMenge.add(symbol);
            }

            // 3: falls ε ∈ fi(α): fo(A) zusätzlich hinzufügen
            if (fiAlpha.contains(FirstFollowSets.EPSILON)) {
                lookaheadMenge.addAll(followSets.getOrDefault(A, Collections.emptySet()));
            }

            lookahead.put(p, lookaheadMenge);
        }
        return lookahead;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LL(1)-CHECK
    // ─────────────────────────────────────────────────────────────────────────

    /** G ∈ LL(1) ⟺ keine zwei Produktionen desselben NT haben überschneidende Lookahead-Mengen. */
    public static boolean istLL1(
            Grammatik g,
            Map<Produktion, Set<String>> lookaheadSets) {

        return !hatKonflikt(g, lookaheadSets);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GRAMMATIKGRÖẞE
    // ─────────────────────────────────────────────────────────────────────────

    /** Grammatikgröße nach Definition 2.3: je NT ein Symbol für die linke Seite plus alle rechten Seiten. */
    public static int berechneGrammatikGroesse(Grammatik g) {
        int groesse = 0;

        // Produktionen nach NT gruppieren
        Map<String, List<Produktion>> proNTProduktionen = new LinkedHashMap<>();
        for (String nt : g.getNichtterminale()) proNTProduktionen.put(nt, new ArrayList<>());
        for (Produktion p : g.getProduktionen()) {
            proNTProduktionen.computeIfAbsent(p.getLinks(), k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<String, List<Produktion>> eintrag : proNTProduktionen.entrySet()) {
            List<Produktion> produktionen = eintrag.getValue();
            if (produktionen.isEmpty()) continue; // NT ohne Produktionen zählt nicht mit

            groesse += 1;
            for (Produktion p : produktionen) {
                groesse += p.getRechts().size();
                // ε-Produktion hat getRechts().size() == 0 → zählt 0 Symbole (korrekt)
            }
        }
        return groesse;
    }

    /** true, wenn zwei Produktionen desselben NT überschneidende Lookahead-Mengen haben. */
    private static boolean hatKonflikt(
            Grammatik g,
            Map<Produktion, Set<String>> lookaheadSets) {

        // Produktionen nach NT gruppieren
        Map<String, List<Produktion>> proNTProduktionen = new LinkedHashMap<>();
        for (String nt : g.getNichtterminale()) proNTProduktionen.put(nt, new ArrayList<>());
        for (Produktion p : g.getProduktionen()) {
            proNTProduktionen.computeIfAbsent(p.getLinks(), k -> new ArrayList<>()).add(p);
        }

        for (List<Produktion> produktionen : proNTProduktionen.values()) {
            // jedes Produktionspaar desselben NT genau einmal vergleichen
            for (int i = 0; i < produktionen.size(); i++) {
                for (int j = i + 1; j < produktionen.size(); j++) {
                    Set<String> schnittmenge = new HashSet<>(lookaheadSets.get(produktionen.get(i)));
                    schnittmenge.retainAll(lookaheadSets.get(produktionen.get(j)));
                    if (!schnittmenge.isEmpty()) return true;
                }
            }
        }
        return false;
    }
}
