import java.util.List;

/** Eine Produktionsregel: linkes Nichtterminal + rechte Seite (Symbolfolge). */
public class Produktion {
    private String links;
    private List<String> rechts;

    public Produktion(String links, List<String> rechts) {
        this.links = links;
        this.rechts = rechts;
    }

    public String getLinks() {
        return links;
    }

    public List<String> getRechts() {
        return rechts;
    }

    @Override
    public String toString() {
        if (rechts.isEmpty()) {
            return links + " -> ε";
        } else {
            return links + " -> " + String.join(" ", rechts);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produktion)) return false;
        Produktion andere = (Produktion) o;
        return this.links.equals(andere.links) && this.rechts.equals(andere.rechts);
    }

    @Override
    public int hashCode() {
        return links.hashCode() + rechts.hashCode();
    }
}
