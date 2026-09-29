import java.util.*;

/**
 * Auswertungslauf für die Algorithmenkombinationen aus Moore, Tabellen 3 und 4.
 *
 * Für jede Eingabegrammatik wird eine Tabelle mit einer Zeile je Kombination
 * ausgegeben: Grammatikgröße, Größenänderung, LL(1)-Eigenschaft und das
 * Ergebnis der beschränkten Äquivalenzprüfung gegen die Ausgangsgrammatik.
 */
public class Main {

    /** Verzeichnis der Grammatikdateien */
    private static final String VERZEICHNIS = "";

    /** Obere Schranke für Wortlänge in der Äquivalenzprüfung. */
    private static final int MAX_WORTLAENGE = 8;

    /** Spaltenformat für Kopf- und Datenzeilen der Ergebnistabelle. */
    private static final String ZEILENFORMAT = "%-20s %7s %7s %8s %10s%n";

    /** Spaltenformat für die Zusatztabelle (PA ε vs. Moore). */
    private static final String GROESSENFORMAT = "%-20s %12s %14s%n";

    public static void main(String[] args) throws Exception {
        // Über die Kommandozeile übergebene Dateien haben Vorrang.
        String[] dateien = args.length > 0
                ? args
                : new String[]{
                "g1_v3.txt", "g1_v4.txt",
                "g2_v3.txt", "g2_v4.txt",
                "g3_v3.txt",
                "grammarOG.txt"};

        for (String datei : dateien) {
            werteAus(datei.contains("/") ? datei : VERZEICHNIS + datei);
        }
    }

    private static void werteAus(String pfad) throws Exception {
        Grammatik ausgangsgrammatik = GrammarReader.leseGrammatik(pfad);

        // Startsymbol der Ausgangsgrammatik. Es wird explizit festgehalten und
        // von Grammatik.getStartsymbol() geliefert
        String startsymbol = ausgangsgrammatik.getStartsymbol();

        int ausgangsgroesse = LL1Checker.berechneGrammatikGroesse(ausgangsgrammatik);

        // Zwischenergebnisse, die von mehreren Kombinationen benötigt werden
        Grammatik lf   = LeftFactoring.applyLeftFactoring(ausgangsgrammatik);
        Grammatik nlrg = NLRG.apply(lf);

        // Die Kombinationen aus Moore, Tabellen 3 und 4, + LF und PA einzeln
        Map<String, Grammatik> kombinationen = new LinkedHashMap<>();
        kombinationen.put("LF",               lf);
        kombinationen.put("PA",               PaullAlgorithm.removeLeftRecursion(ausgangsgrammatik));
        kombinationen.put("LF + PA",          PaullAlgorithm.removeLeftRecursion(lf));
        kombinationen.put("LF + NLRG + PA",   PaullAlgorithm.removeLeftRecursion(nlrg));
        kombinationen.put("LC",               LCTransform.apply(ausgangsgrammatik));
        kombinationen.put("LCLR",             LCLRTransform.apply(ausgangsgrammatik));
        kombinationen.put("LF + LCLR",        LCLRTransform.apply(lf));
        kombinationen.put("LF + NLRG + LCLR", LCLRTransform.apply(nlrg));

        System.out.println();
        System.out.println("=".repeat(64));
        System.out.printf("  %s   (Startsymbol %s, Ausgangsgröße %d)%n",
                pfad, startsymbol, ausgangsgroesse);
        System.out.println("=".repeat(64));
        druckeTabelle(kombinationen, ausgangsgrammatik, startsymbol, ausgangsgroesse);

        // Zusätzlicher Vergleich der Grammatikgrößen: mit ε-Version (Definition 3.8) gegen Moores
        // ε-freie Variante, jeweils
        Map<String, Grammatik> epsilon = new LinkedHashMap<>();
        epsilon.put("PA",             PaullAlgorithm.removeLeftRecursion(ausgangsgrammatik));
        epsilon.put("LF + PA",        PaullAlgorithm.removeLeftRecursion(lf));
        epsilon.put("LF + NLRG + PA", PaullAlgorithm.removeLeftRecursion(nlrg));

        Map<String, Grammatik> moore = new LinkedHashMap<>();
        moore.put("PA",             PaullAlgorithm.removeLeftRecursionMoore(ausgangsgrammatik));
        moore.put("LF + PA",        PaullAlgorithm.removeLeftRecursionMoore(lf));
        moore.put("LF + NLRG + PA", PaullAlgorithm.removeLeftRecursionMoore(nlrg));

        System.out.println();
        System.out.println("-".repeat(64));
        System.out.println("  Zusatz: Grammatikgröße mit PA (ε-Version) vs. PA (Moore, ε-frei)");
        System.out.println("-".repeat(64));
        System.out.printf(GROESSENFORMAT, "Verfahren", "Größe (ε)", "Größe (Moore)");
        for (String verfahren : epsilon.keySet()) {
            System.out.printf(GROESSENFORMAT,
                    verfahren,
                    LL1Checker.berechneGrammatikGroesse(epsilon.get(verfahren)),
                    LL1Checker.berechneGrammatikGroesse(moore.get(verfahren)));
        }
    }

    private static void druckeTabelle(
            Map<String, Grammatik> kombinationen,
            Grammatik ausgangsgrammatik, String startsymbol, int ausgangsgroesse) {

        System.out.printf(ZEILENFORMAT, "Verfahren", "Größe", "Diff", "LL(1)", "äquiv.");

        for (Map.Entry<String, Grammatik> eintrag : kombinationen.entrySet()) {
            String    verfahren = eintrag.getKey();
            Grammatik ergebnis  = eintrag.getValue();

            int     groesse     = LL1Checker.berechneGrammatikGroesse(ergebnis);
            int     differenz   = groesse - ausgangsgroesse;
            boolean aequivalent = EquivalenceChecker
                    .vergleiche(ausgangsgrammatik, startsymbol, ergebnis, startsymbol, MAX_WORTLAENGE)
                    .stimmtUeberein();

            System.out.printf(ZEILENFORMAT,
                    verfahren,
                    groesse,
                    String.format("%+d", differenz),
                    istLL1(ergebnis) ? "ja" : "nein",
                    aequivalent ? "ja" : "NEIN");
        }
    }

    /** Prüft die LL(1)-Eigenschaft einer Grammatik. */
    private static boolean istLL1(Grammatik grammatik) {
        Map<String, Set<String>> first  = FirstFollowSets.berechneFirstSets(grammatik);
        Map<String, Set<String>> follow = FirstFollowSets.berechneFollowSets(grammatik, first);
        return LL1Checker.istLL1(grammatik, LL1Checker.berechneLookaheadSets(grammatik, first, follow));
    }
}