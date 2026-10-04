# Hinweise (Setup-Rechner für Avinox)

Inoffizielles Hobbyprojekt. Keine Verbindung zu Avinox oder DJI.
Genannte Namen und Marken gehören ihren Inhabern. "Avinox" dient nur der Beschreibung, für welche Antriebe der Rechner gedacht ist.

## Haftung
Keine Gewähr. Die berechneten Werte sind Vorschläge und können unpassend sein. Einstellungen am Antrieb nimmt jeder auf eigene Verantwortung vor. Siehe auch Haftungsausschluss in der MIT-Lizenz (`LICENSE`).

## Eigene Teile (MIT, `LICENSE`)
Quellcode der App und der Web-Version, Oberfläche, App-Symbol (eigene Zeichnung), Dokumentation. Urheber: th3_s1nc.
Es sind keine Programmteile, Grafiken, Schriften oder Texte der Hersteller-App enthalten.

## Quelle des Rechenwegs
Rechenweg und Assist-Level-Werte stammen aus dem Setup-Guide von Bernd Hemmersbach (Vorabfassung vom 11.05.2026) und seinen Blättern "Generelles Setup" für M1, M2 und M2S.

- Rechner und Web-Version sind mit Zustimmung des Autors veröffentlicht.
- Der Guide und die Blätter selbst sind nicht enthalten und nicht von der MIT-Lizenz dieses Projekts erfasst. Die Rechte daran liegen beim Autor.
- Übernommen sind die Rechenschritte, die Zielwerte in Watt je kg Gesamtgewicht, die Namen und Farbcodes der Zusatzmodi und die Tabelle "Assist Level und Unterstützung". Diese Tabelle beruht auf Testfahrten des Autors mit dem M1 und ist von Avinox nicht bestätigt.
- Die Zielwerte für die Zusatzmodi sind aus den Beispielen der Blätter abgeleitet. An einzelnen Stellen weicht der Rechner um eine Stufe von Werten ab, die dort von Hand gesetzt sind.
- Die Eckdaten der Motoren (Nm und Watt in den Modi und im Boost) stammen aus den Blättern und aus öffentlichen Angaben. Beim M2 rechnet der Rechner im Boost mit 125 Nm, im Blatt stehen 130 Nm.

## Fremde Bestandteile

| Teil | Quelle / Lizenz | Hinweis |
|---|---|---|
| Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) | Gradle, Apache License 2.0 | Liegt im Repository, damit das Projekt ohne installiertes Gradle baut |
| AndroidX, Jetpack Compose, Material 3 | Apache License 2.0 | Abhängigkeiten, werden beim Bauen geladen |
| Kotlin | Apache License 2.0 | Abhängigkeit, wird beim Bauen geladen |
| JUnit 4 | Eclipse Public License 1.0 | Nur für die Tests |

Die Web-Version (`docs/index.html`) lädt keine fremden Dateien nach, auch keine Schriften.

## Nennung der Produktnamen
"Avinox" wird nur beschreibend genannt, um zu sagen, für welche Antriebe der Rechner gedacht ist (vgl. § 23 Abs. 1 Nr. 3 MarkenG, "Bestimmungshinweis"). Der Name steht im App-Namen nur in dieser beschreibenden Form ("Setup-Rechner für Avinox") und nicht im Paketnamen, es werden keine Logos oder Bildmarken verwendet, und es soll nicht der Eindruck einer Verbindung zum Hersteller entstehen. Das dunkle Farbschema und die Farben der vier Werksmodi sind an die Hersteller-App angelehnt, damit man die Modi wiedererkennt; Aufbau und Symbole sind eigene. Wer heute welche Rechte an den Marken hält, wurde nicht geprüft.

## Datenschutz
Die App hat keine Internet-Berechtigung und sammelt nichts. Die Eingaben liegen nur auf dem Gerät und sind vom Cloud-Backup ausgenommen. Die Web-Version speichert die Eingaben nur im Browser.
