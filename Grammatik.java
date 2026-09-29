import java.util.*;

/** Eine Grammatik: Liste von Produktionen plus die Mengen ihrer Terminale und Nichtterminale. */
public class Grammatik {
    private List<Produktion> produktionen;
    private Set<String> alleNichtterminale;
    private Set<String> terminale;

    /** Explizit gespeichert, da LC/LCLR ihre Ergebnisgrammatik neu aufbauen und dabei die Reihenfolge ändern können. */
    private String startsymbol;

    public Grammatik() {
        this.produktionen = new ArrayList<>();
        this.alleNichtterminale = new LinkedHashSet<>();
        this.terminale = new LinkedHashSet<>();
    }

    public void addProduktion(Produktion produktion) {
        if (produktion.getRechts().size() == 1 && produktion.getRechts().get(0).equals("ε")) {
            // ε-Produktion: intern als leere rechte Seite geführt
            produktionen.add(new Produktion(produktion.getLinks(), Collections.emptyList()));
            alleNichtterminale.add(produktion.getLinks());
        } else {
            produktionen.add(produktion);
            alleNichtterminale.add(produktion.getLinks());
            for (String symbol : produktion.getRechts()) {
                if (Character.isUpperCase(symbol.charAt(0))) {
                    alleNichtterminale.add(symbol);
                } else {
                    terminale.add(symbol);
                }
            }
        }
    }

    public List<Produktion> getProduktionen() {
        return produktionen;
    }

    public Set<String> getNichtterminale() {
        return alleNichtterminale;
    }

    public Set<String> getTerminale() {
        return terminale;
    }

    public String getStartsymbol() {
        return startsymbol;
    }

    public void setStartsymbol(String startsymbol) {
        this.startsymbol = startsymbol;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Produktionen:\n");
        for (Produktion p : produktionen) {
            sb.append(p).append("\n");
        }
        sb.append("\nNichtterminale: ").append(alleNichtterminale);
        sb.append("\nTerminale: ").append(terminale);
        return sb.toString();
    }
}
