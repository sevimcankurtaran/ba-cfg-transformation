import java.util.*;

/**
 * Gemeinsame Hilfsmethoden für alle Algorithmen.
 * Enthält die Linksecken-Berechnung (Abschnitt 5.3), die von PA, NLRG und LCLR verwendet wird.
 */
public class GrammarUtils {

    // ─────────────────────────────────────────────────────────────────────────
    // Grundlegende Hilfsmethoden
    // ─────────────────────────────────────────────────────────────────────────

    /** Alle Produktionen mit linker Seite {@code nt}, als neue Liste. - PA, LF, NLRG, LC, LCLR */
    public static List<Produktion> getProduktionenFuer(Grammatik g, String nt) {
        List<Produktion> ergebnis = new ArrayList<>();
        for (Produktion p : g.getProduktionen()) {
            if (p.getLinks().equals(nt)) {
                ergebnis.add(p);
            }
        }
        return ergebnis;
    }

    /** Tiefe Kopie der Grammatik - Transformationen arbeiten darauf, das Original bleibt unangetastet. - PA, LF, NLRG, LC, LCLR */
    public static Grammatik kopiereGrammatik(Grammatik original) {
        Grammatik copy = new Grammatik();
        for (Produktion p : original.getProduktionen()) { // 1: jede Produktion neu anlegen
            copy.addProduktion(new Produktion(p.getLinks(), new ArrayList<>(p.getRechts())));
        }
        copy.setStartsymbol(original.getStartsymbol()); // 2: Startsymbol mitkopieren
        return copy;
    }

    /** Erzeugt und registriert ein noch freies Nichtterminal ({@code A'1}, {@code A'2}, …). - PA, LF, NLRG */
    public static String createFreshNonterminal(String basis, Grammatik g) {
        int nummer = 1;
        String kandidat = basis + "'" + nummer;
        while (g.getNichtterminale().contains(kandidat)) { // 1: bis ein freier Name gefunden ist
            nummer++;
            kandidat = basis + "'" + nummer;
        }
        g.getNichtterminale().add(kandidat); // 2: sofort registrieren
        return kandidat;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Linksecken-Relation
    // ─────────────────────────────────────────────────────────────────────────

    /** Map: Nichtterminal A → Menge seiner direkten Linksecken (erstes Symbol jeder A-Produktion). - intern */
    private static Map<String, Set<String>> berechneDirekteLinksecken(Grammatik g) {
        Map<String, Set<String>> direkteLinksecken = new HashMap<>();
        for (String nt : g.getNichtterminale()) { // 1: leere Menge pro NT
            direkteLinksecken.put(nt, new HashSet<>());
        }
        for (Produktion p : g.getProduktionen()) { // 2: erstes Symbol jeder Produktion eintragen
            if (!p.getRechts().isEmpty()) {
                direkteLinksecken.computeIfAbsent(p.getLinks(), k -> new HashSet<>())
                        .add(p.getRechts().get(0));
            }
        }
        return direkteLinksecken;
    }

    /** lc(A) für jedes A: reflexiv-transitive Hülle - A selbst plus alles, was über Ketten A → X … erreichbar ist. - PA, intern */
    public static Map<String, Set<String>> berechneLinkseckenRelation(Grammatik g) {
        // 1: direkte Linksecken holen
        Map<String, Set<String>> direkteLinksecken = berechneDirekteLinksecken(g);

        Map<String, Set<String>> linksecken = new HashMap<>();
        for (String nt : g.getNichtterminale()) { // 2: BFS pro NT für die transitive Hülle
            Set<String> erreichbar = new HashSet<>();
            erreichbar.add(nt); // reflexiv: A ist immer Linksecke von sich selbst
            Queue<String> queue = new LinkedList<>();
            for (String direkteLinksecke : direkteLinksecken.getOrDefault(nt, Collections.emptySet())) {
                if (erreichbar.add(direkteLinksecke)) queue.add(direkteLinksecke);
            }
            while (!queue.isEmpty()) { // 3: weiter verfolgen, bis nichts Neues mehr dazukommt
                String aktuell = queue.poll();
                for (String naechstes : direkteLinksecken.getOrDefault(aktuell, Collections.emptySet())) {
                    if (erreichbar.add(naechstes)) queue.add(naechstes);
                }
            }
            linksecken.put(nt, erreichbar);
        }
        return linksecken;
    }

    /** Menge der linksrekursiven Nichtterminale (Definition 3.1, Abschnitt 3.1 / 5.3): A, das in ≥ 1 Schritt sich selbst als Linksecke erreicht. - NLRG, LC, LCLR */
    public static Set<String> berechneLinksrekursiveNTs(Grammatik g) {
        Map<String, Set<String>> linksecken = berechneLinkseckenRelation(g); // 1: lc(X) für alle X
        Map<String, Set<String>> direkteLinksecken = berechneDirekteLinksecken(g);

        Set<String> linksrekursive = new HashSet<>();
        for (String nt : g.getNichtterminale()) { // 2: pro NT prüfen
            // 3: echte Linksecken = Union der Linksecken-Mengen aller direkten Linksecken
            Set<String> echteLinksecken = new HashSet<>();
            for (String direkteLinksecke : direkteLinksecken.getOrDefault(nt, Collections.emptySet())) {
                echteLinksecken.addAll(linksecken.getOrDefault(direkteLinksecke, Collections.emptySet()));
            }
            if (echteLinksecken.contains(nt)) { // 4: nt in seiner eigenen Menge → linksrekursiv
                linksrekursive.add(nt);
            }
        }
        return linksrekursive;
    }
}