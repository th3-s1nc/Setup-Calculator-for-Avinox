# Changelog

(Deutsch.)

## Web-Version, 05.10.2026
Nur `docs/index.html`, die App bleibt bei v1.9.1.
- Mit „Erweitert“ steht die Tabelle nach Trittfrequenz jetzt wie in der App unter der aufgeklappten Stufe, neben dem Nachstellen von Hand. So zeigt sich jede Änderung sofort in der Tabelle. Auch BOOST lässt sich aufklappen.
- Die Karten mit allen Tabellen untereinander gibt es weiter im Standardrechner.

## v1.9.1
- Mit „Erweitert“ steht die Tabelle nach Trittfrequenz wieder direkt unter der aufgeklappten Stufe. Das Nachstellen von Hand folgt darunter, vorher stand es davor und schob die Tabelle aus dem Bild.

## v1.9
App und Web-Version (`docs/index.html`) haben jetzt dieselben Funktionen.
- Neu: Akku-Auswahl beim M2S. Im Boost 1.500 W mit FP700/RS800, 1.300 W mit FS800/FS600 (Angaben von DJI). Profile aus älteren Versionen rechnen weiter mit 1.500 W, bis ein Akku gewählt ist.
- Neu: Schalter „Erweitert“. Dahinter eine Checkliste beim Übertragen in die Avinox Ride App, das Nachstellen der Werte von Hand, die geschätzte Steigung statt W/kg und der Vergleich zweier Setups. Ist der Schalter aus, bleibt es beim bisherigen Rechner.
- Neu: Profile als Datei sichern und laden. Das Format ist in App und Web-Version dasselbe.
- Neu: Link teilen. Die Eingaben stecken im Link, er öffnet die Web-Version.
- Neu: Drucken als Setup-Karte mit Kästchen zum Abhaken.
- Neu: helles Design, in der App unter Info.
- Web-Version: Profile im Browser, mit dem Hinweis, dass sie beim Löschen der Browserdaten verloren gehen.

## v1.8
- Erste öffentliche Version auf GitHub.
- Hinweis ergänzt: Veröffentlichung mit Zustimmung von Bernd Hemmersbach.

## v1.7.2
- Neues App-Symbol: drei Schieberegler in den Farben von ECO, TRAIL und TURBO, dazu eine einfarbige Fassung für thematische Symbole.

## v1.7.1
- Web-Version: keine Vorgabewerte mehr, Hinweis statt Ergebnis bei leeren Feldern, neuer Knopf "Zurücksetzen". Die App selbst ist unverändert.

## v1.7
- Neu: Speichern-Knopf. Eingaben sind zunächst ein Entwurf; erst "Speichern" schreibt sie ins Profil, "Verwerfen" setzt das Formular zurück. Das Setup rechnet immer mit dem gespeicherten Stand.
- Keine Vorgabewerte mehr: Der erste Start und jedes neue Profil beginnen leer, der Motor muss gewählt werden.
- Neu: kleines Dreieck rechts unter jeder Stufe als Zeichen, dass sie sich aufklappen lässt.

## v1.6.1
- App heißt auf dem Gerät jetzt "Setup-Rechner für Avinox" (englisch "Setup Calculator for Avinox"), wie die Web-Version.

## v1.6
- Neu: Profile für mehrere Fahrer. Jedes Profil hat einen Namen und eigene Eingaben; anlegen, umbenennen und löschen unter Eingaben, wechseln auch direkt auf der Setup-Seite. Bisherige Eingaben werden als erstes Profil übernommen.
- Beim Teilen und Kopieren steht der Profilname über dem Setup.

## v1.5
- Neu: Sprachwahl in der App unter Info (System, Deutsch, English). Die Wahl bleibt gespeichert.

## v1.4
- Neu: Englisch. Alle Texte liegen in Sprachdateien (`values/strings.xml` Englisch, `values-de/strings.xml` Deutsch). Die App folgt der Sprache des Handys, Zahlenformat passend zur Sprache.
- Ab Android 13 ist die Sprache pro App in den Systemeinstellungen wählbar.
- Web-Version auf Deutsch und Englisch, mit Umschalter.

## v1.3.2
- Lizenz auf th3_s1nc, Paket `io.github.th3s1nc.setuprechner` (wie beim OSMAnd-HUD-Projekt).
- Neu: `NOTICE.md` mit Quelle, fremden Bestandteilen und Markenhinweis. README auf Englisch und Deutsch.

## v1.3.1
- Urheberzeile der Lizenz ohne Personennamen.

## v1.3
- Umbenannt in "Setup-Rechner für Avinox".
- MIT-Lizenz, Quellen-, Marken- und Datenschutzhinweis ergänzt.
- Cloud-Backup der Eingaben abgeschaltet.
- Web-Version unter `docs/` für GitHub Pages.

## v1.2
- Dunkles Farbschema mit orangem Akzent.

## v1.1
- Drei Bereiche mit unterer Leiste: Setup, Eingaben, Info.
- Plus/Minus-Tasten für Eigenleistung und Trittfrequenz.

## v1.0
- Erste Version: Rechner für M1, M2 und M2S.
