import java.util.*;

/** Paulls Algorithmus (PA), Moore, Abschnitt 4.3 / Implementierung 5.4.3, Abbildung 4.1. */
public class PaullAlgorithm {

    public static Grammatik removeLeftRecursion(Grammatik grammatik) {
        Grammatik g = GrammarUtils.kopiereGrammatik(grammatik);

        List<String> reihenfolge = berechneBesteReihenfolge(g);

        // eliminiereDirekteLinksrekursion muss INNERHALB der äußeren Schleife stehen:
        // die Substitution von Aj liefert nur dann eine nicht-linksrekursive rechte
        // Seite, wenn Aj zuvor schon selbst bereinigt wurde.
        for (int i = 0; i < reihenfolge.size(); i++) {
            String Ai = reihenfolge.get(i);

            // Innere Schleife: Aj als direkte Linksecke von Ai ersetzen
            for (int j = 0; j < i; j++) {
                String Aj = reihenfolge.get(j);
                substituiere(g, Ai, Aj);
            }

            // Direkte Linksrekursion für Ai eliminieren (gehört in die äußere Schleife!)
            eliminiereDirekteLinksrekursion(g, Ai);
        }

        return g;
    }

    /** Wie {@link #removeLeftRecursion}, aber mit Moores ε-freier Elimination (Abschnitt 5.4.3). */
    public static Grammatik removeLeftRecursionMoore(Grammatik grammatik) {
        Grammatik g = GrammarUtils.kopiereGrammatik(grammatik);

        List<String> reihenfolge = berechneBesteReihenfolge(g);

        for (int i = 0; i < reihenfolge.size(); i++) {
            String Ai = reihenfolge.get(i);

            for (int j = 0; j < i; j++) {
                String Aj = reihenfolge.get(j);
                substituiere(g, Ai, Aj);
            }

            eliminiereDirekteLinksrekursionMoore(g, Ai); // Unterschied zu removeLeftRecursion: Moore- statt ε-Variante
        }

        return g;
    }

    /** NT absteigend nach Anzahl ihrer Linksecken sortiert; bei Gleichstand entscheidet die Lesereihenfolge (stabile Sortierung). */
    private static List<String> berechneBesteReihenfolge(Grammatik g) {
        Map<String, Set<String>> linksecken = GrammarUtils.berechneLinkseckenRelation(g);
        List<String> nts = new ArrayList<>(g.getNichtterminale());
        nts.sort((a, b) ->
                linksecken.getOrDefault(b, Collections.emptySet()).size() -
                        linksecken.getOrDefault(a, Collections.emptySet()).size()
        );
        return nts;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Kern-Operationen
    // ─────────────────────────────────────────────────────────────────────────

    /** Innere Schleife: Ai → Aj α wird zu Ai → β α für jede Aj-Produktion Aj → β. */
    private static void substituiere(Grammatik g, String Ai, String Aj) {
        List<Produktion> aiProduktionen = new ArrayList<>(GrammarUtils.getProduktionenFuer(g, Ai)); // 1: Ai-Produktionen holen
        List<Produktion> ajProduktionen = GrammarUtils.getProduktionenFuer(g, Aj);
        List<Produktion> neueAiProduktionen = new ArrayList<>();
        boolean geaendert = false;

        for (Produktion p : aiProduktionen) { // 2: jede Ai-Produktion einzeln prüfen
            List<String> rechts = p.getRechts();
            if (!rechts.isEmpty() && rechts.get(0).equals(Aj)) {
                // 3: beginnt mit Aj -> durch jede Aj-Alternative ersetzen
                geaendert = true;
                List<String> alpha = rechts.subList(1, rechts.size());
                for (Produktion ajProduktion : ajProduktionen) {
                    List<String> neueRechteSeite = new ArrayList<>(ajProduktion.getRechts());
                    neueRechteSeite.addAll(alpha);
                    neueAiProduktionen.add(new Produktion(Ai, neueRechteSeite));
                }
                // Falls Aj keine Produktionen hat (leere Sprache), wird Ai → Aj α entfernt - korrekt.
            } else {
                neueAiProduktionen.add(p); // 4: sonst unverändert übernehmen
            }
        }

        if (geaendert) { // 5: alte Ai-Produktionen durch die neuen ersetzen
            g.getProduktionen().removeIf(p -> p.getLinks().equals(Ai));
            g.getProduktionen().addAll(neueAiProduktionen);
        }
    }

    /** Eliminiert direkte Linksrekursion von A nach Definition 3.8 (ε-Version, Aho). */
    private static void eliminiereDirekteLinksrekursion(Grammatik g, String A) {
        List<Produktion> aProduktionen = GrammarUtils.getProduktionenFuer(g, A); // 1: alle A-Produktionen holen

        List<List<String>> alphaListe = new ArrayList<>(); // A → A α  (linksrekursiv)
        List<List<String>> betaListe  = new ArrayList<>(); // A → β   (nicht-LR, inkl. ε)

        for (Produktion p : aProduktionen) { // 2: in linksrekursiv (alpha) und nicht (beta) aufteilen
            List<String> rechts = p.getRechts();
            if (!rechts.isEmpty() && rechts.get(0).equals(A)) {
                // Direkt linksrekursiv: α ist der Rest nach A
                alphaListe.add(new ArrayList<>(rechts.subList(1, rechts.size())));
            } else {
                // Nicht linksrekursiv; leere Liste steht für ε - korrekt behandelt
                betaListe.add(new ArrayList<>(rechts));
            }
        }

        if (alphaListe.isEmpty()) return; // 3: keine direkte Linksrekursion -> nichts zu tun

        String aStrich = GrammarUtils.createFreshNonterminal(A, g); // 4: neues Nichtterminal A' anlegen

        // 5: neue A-Produktionen: A → βi A'
        List<Produktion> neueAProduktionen = new ArrayList<>();
        for (List<String> beta : betaListe) {
            List<String> neueRechteSeite = new ArrayList<>(beta);
            neueRechteSeite.add(aStrich);
            neueAProduktionen.add(new Produktion(A, neueRechteSeite));
        }

        // 6: neue A'-Produktionen: A' → αi A', plus A' → ε
        List<Produktion> neueAStrichProduktionen = new ArrayList<>();
        for (List<String> alpha : alphaListe) {
            List<String> neueRechteSeite = new ArrayList<>(alpha);
            neueRechteSeite.add(aStrich);
            neueAStrichProduktionen.add(new Produktion(aStrich, neueRechteSeite));
        }
        neueAStrichProduktionen.add(new Produktion(aStrich, Collections.emptyList())); // A' → ε

        g.getProduktionen().removeIf(p -> p.getLinks().equals(A)); // 7: alte A-Produktionen ersetzen
        g.getProduktionen().addAll(neueAProduktionen);
        g.getProduktionen().addAll(neueAStrichProduktionen);
    }

    /**
     * Eliminiert direkte Linksrekursion von A, ε-frei nach [Moo00, S.250]/[HU79, S.96]:
     * jede Alternative doppelt, einmal mit und einmal ohne A', statt A' → ε.
     */
    private static void eliminiereDirekteLinksrekursionMoore(Grammatik g, String A) {
        // 1-4: wie eliminiereDirekteLinksrekursion (Produktionen aufteilen, A' anlegen)
        List<Produktion> aProduktionen = GrammarUtils.getProduktionenFuer(g, A);

        List<List<String>> alphaListe = new ArrayList<>(); // A → A α  (linksrekursiv)
        List<List<String>> betaListe  = new ArrayList<>(); // A → β    (nicht-LR)

        for (Produktion p : aProduktionen) {
            List<String> rechts = p.getRechts();
            if (!rechts.isEmpty() && rechts.get(0).equals(A)) {
                alphaListe.add(new ArrayList<>(rechts.subList(1, rechts.size())));
            } else {
                betaListe.add(new ArrayList<>(rechts));
            }
        }

        if (alphaListe.isEmpty()) return; // Keine direkte Linksrekursion → nichts zu tun

        String aStrich = GrammarUtils.createFreshNonterminal(A, g);

        // 5: neue A-Produktionen - anders als bei der ε-Version: jedes βi doppelt, mit UND ohne A'
        List<Produktion> neueAProduktionen = new ArrayList<>();
        for (List<String> beta : betaListe) {
            neueAProduktionen.add(new Produktion(A, new ArrayList<>(beta)));
            List<String> mitAStrich = new ArrayList<>(beta);
            mitAStrich.add(aStrich);
            neueAProduktionen.add(new Produktion(A, mitAStrich));
        }

        // 6: neue A'-Produktionen - genauso doppelt, KEIN A' → ε (Unterschied zur ε-Version)
        List<Produktion> neueAStrichProduktionen = new ArrayList<>();
        for (List<String> alpha : alphaListe) {
            neueAStrichProduktionen.add(new Produktion(aStrich, new ArrayList<>(alpha)));
            List<String> mitAStrich = new ArrayList<>(alpha);
            mitAStrich.add(aStrich);
            neueAStrichProduktionen.add(new Produktion(aStrich, mitAStrich));
        }

        // 7: alte A-Produktionen ersetzen
        g.getProduktionen().removeIf(p -> p.getLinks().equals(A));
        g.getProduktionen().addAll(neueAProduktionen);
        g.getProduktionen().addAll(neueAStrichProduktionen);
    }
}