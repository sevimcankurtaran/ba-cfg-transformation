# BAV2

Implementierung zur Bachelorarbeit *Transformation kontextfreier Grammatiken für die
Top-Down-Syntaxanalyse* (RWTH Aachen, Sevim Aybüke Cankurtaran).

Enthält Left Factoring, NLRG, Paulls Algorithmus, Left-Corner-Transformation (LC) und
LCLR, sowie First/Follow-Mengen, LL(1)-Prüfung und eine beschränkte Äquivalenzprüfung.

## Ausführen

```
cd src
javac *.java
java Main
```

`Main` wertet alle sechs Grammatiken in `src/` aus und gibt für jede eine Tabelle mit
Grammatikgröße, LL(1)-Eigenschaft und Äquivalenzergebnis aus.

## Grammatiken

Entsprechen den Testgrammatiken aus Kapitel 6.2 der Arbeit:

| Datei | Grammatik |
|---|---|
| `g1_v3` | G1 |
| `g1_v4` | G1' |
| `g2_v3` | G2 |
| `g2_v4` | G2' |
| `g3_v3` | G3 |
| `grammarOG` | G_g (Beispiel 3.3) |
