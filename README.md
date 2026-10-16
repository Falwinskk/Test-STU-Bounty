# Bounty

Kopfgeld Plugin für Paper 26.2 (Java 25). Geld läuft über Vault.

## Befehle

- `/bounty` - Übersicht aller Kopfgelder (GUI, blätterbar)
- `/bounty add <spieler> <betrag>` - Kopfgeld aussetzen oder erhöhen

Wer einen Spieler mit Kopfgeld tötet bekommt den Betrag gutgeschrieben. Alle Änderungen werden im Chat angekündigt.

## Permissions

- `bounty.use` - Übersicht öffnen (Standard: alle)
- `bounty.add` - Kopfgeld aussetzen (Standard: alle)

## Config

```yaml
min-amount: 100      # Mindestbetrag pro /bounty add
max-amount: 1000000  # Obergrenze für das gesamte Kopfgeld auf einen Spieler
```

## Bauen

```
mvn package
```

Die Jar liegt danach in `target/Bounty.jar`. Auf dem Server braucht man Vault + ein Economy Plugin (z.B. EssentialsX).

## Getestet mit

- Paper 26.2 Build 134, Java 25 (Zulu 25.0.4)
- Vault 1.7.3, EssentialsX 2.22.0

Beim Build laufen die Tests mit, GitHub Actions baut zusätzlich bei jedem Push.

Getestet ist die Geldlogik (`Geld`) und die Speicherung (`BountyStore`) inklusive der Fehlerfälle, in denen die Datei nicht geschrieben werden kann. Command, GUI und Kill-Ablauf hab ich auf dem Testserver von Hand durchgespielt, dafür müsste man sonst den halben Bukkit Server mocken, das lohnt sich bei der Größe nicht.

## Speicherfehler

Jede Änderung an der bounties.yml wird sofort geschrieben. Klappt das nicht (Platte voll, Rechte, was auch immer), wird die Änderung im Speicher zurückgenommen und der Spieler bekommt eine Meldung. Beim Kopfgeld setzen wird deshalb erst gespeichert und dann abgebucht, beim Kill erst vorgemerkt und dann ausgezahlt. Kann nicht gespeichert werden, fließt kein Geld.

## Bekannte Einschränkung

Beim Kill wird das Kopfgeld erst in der bounties.yml als "offen" markiert, dann über Vault ausgezahlt und danach als erledigt gespeichert. Stürzt der Server genau zwischen Auszahlung und dem letzten Speichern ab, wird beim nächsten Start nochmal ausgezahlt. Das Fenster ist winzig, ganz weg bekommt man es aber nicht, weil Vault und die YAML Datei zwei getrennte Systeme sind und es keine gemeinsame Transaktion gibt. Dafür müsste man das Economy System selbst mit in eine Datenbank legen, das wäre für das Plugin zu viel. Ich hab mich bewusst für diese Reihenfolge entschieden, weil eine doppelte Auszahlung im Log sichtbar ist und ein Admin sie korrigieren kann, ein verlorenes Kopfgeld dagegen nicht.
