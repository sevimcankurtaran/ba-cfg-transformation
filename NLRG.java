import java.util.*;

/**
 * Non-Left-Recursion Grouping (NLRG), Moore, Definition 4.4, Abschnitt 4.2 / 5.4.2.
 * Fasst je linksrekursivem Nichtterminal A dessen nicht-linksrekursive Expansionen
 * unter einem neuen A' zusammen, damit A' in Paulls Algorithmus nie substituiert wird.
 */
public class NLRG {

    public static Grammatik apply(Grammatik grammatik) {
        Grammatik g = GrammarUtils.kopiereGrammatik(grammatik);

        // Linksrekursive Nichtterminale berechnen
        Set<String> lrNTs = GrammarUtils.berechneLinksrekursiveNTs(g);

        // Jedes linksrekursive Nichtterminal A einzeln behandeln
        for (String A : new ArrayList<>(lrNTs)) {
            List<Produktion> aProduktionen = GrammarUtils.getProduktionenFuer(g, A);

            // Nicht-linksrekursive Expansionen von A herausfiltern:
            //   - Leere Expansion (ε) zählt immer als nicht-LR
            //   - Expansion beginnt mit Terminal oder nicht-LR-Nichtterminal
            List<Produktion> nichtLRProduktionen = new ArrayList<>();
            for (Produktion p : aProduktionen) {
                if (p.getRechts().isEmpty() || !lrNTs.contains(p.getRechts().get(0))) {
                    nichtLRProduktionen.add(p);
                }
            }

            // Nur sinnvoll, wenn mehr als eine nicht-LR Expansion existiert
            if (nichtLRProduktionen.size() <= 1) continue;

            // Neues Nichtterminal A' anlegen
            String aStrich = GrammarUtils.createFreshNonterminal(A, g);

            // Nicht-LR Produktionen aus A entfernen
            g.getProduktionen().removeAll(nichtLRProduktionen);

            // A → A' ersetzt alle bisherigen nicht-LR Expansionen von A
            g.addProduktion(new Produktion(A, Collections.singletonList(aStrich)));

            // Jede ehemalige nicht-LR Expansion wird zu einer A'-Produktion
            for (Produktion p : nichtLRProduktionen) {
                g.addProduktion(new Produktion(aStrich, new ArrayList<>(p.getRechts())));
            }
        }

        return g;
    }
}