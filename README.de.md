# Setup-Rechner für Avinox

[English version](README.md)

Ein inoffizieller Rechner für E-Bike-Antriebe Avinox M1, M2 und M2S. Aus Motor, Gewicht, Eigenleistung und Trittfrequenz entstehen Einstellungsvorschläge für Assist Level, max. Watt und max. Nm je Modus. Es gibt eine Android-App und eine [Web-Version](https://th3-s1nc.github.io/Setup-Calculator-for-Avinox/) mit demselben Rechenweg.

> **Keine Verbindung** zu Avinox oder DJI. Produktnamen und Marken gehören ihren Inhabern und dienen nur der Beschreibung. **Keine Gewähr.** Die Werte sind Startpunkte und ersetzen nicht das Abstimmen nach Fahrgefühl.

## Was der Rechner kann
- Vier Werksmodi (ECO, AUTO, TRAIL, TURBO) in den Ausrichtungen Alleskönner, Langstrecke und Power, oder alle Stufen mit Zusatzmodi (8 beim M1, 9 bei M2 und M2S).
- Je Modus die Motorleistung nach Trittfrequenz, mit Hinweis, wenn die Wattgrenze bei der eigenen Trittfrequenz nicht erreicht wird.
- Profile für mehrere Fahrer, jedes mit eigenem Namen und eigenen Eingaben. Eingaben werden ausdrücklich gespeichert, ein neues Profil beginnt leer.
- Setup als Text teilen oder kopieren.
- Kein Internetzugriff, keine Werbung, keine Datensammlung. Die Eingaben bleiben auf dem Gerät.
- Oberfläche auf Deutsch und Englisch. Die App folgt der Sprache des Handys und lässt sich unter Info umstellen.

## Quelle des Rechenwegs
Rechenweg und Assist-Level-Werte stammen aus dem Setup-Guide von Bernd Hemmersbach (Vorabfassung vom 11.05.2026) und seinen Blättern "Generelles Setup". Veröffentlicht mit Zustimmung des Autors. Der Guide selbst ist nicht Teil dieses Projekts. Einzelheiten und Abweichungen: [NOTICE.md](NOTICE.md).

## Voraussetzungen
- Android 8.0+ (API 26)

## Bauen
Projektordner in Android Studio öffnen, Gradle-Sync abwarten, Run drücken. Nötig sind Android SDK Platform 35 und JDK 17 oder neuer (das in Android Studio mitgelieferte JDK reicht). Tests: `./gradlew test`.

## Web-Version
Erreichbar unter <https://th3-s1nc.github.io/Setup-Calculator-for-Avinox/>. `docs/index.html` ist der Rechner (Deutsch und Englisch, mit Umschalter) als einzelne Datei ohne Server und ohne externe Abhängigkeiten; GitHub Pages liefert sie aus dem Ordner `/docs` von `main`. Die Web-Version hat keine Fahrerprofile und merkt sich einen Satz Eingaben im Browser.

## Aufbau
- `app/src/main/java/io/github/th3s1nc/setuprechner/calc/SetupCalculator.kt`: der Rechenkern, reines Kotlin. Zielwerte, Modus-Leitern und Motor-Eckdaten stehen oben in der Datei.
- `.../ui/`: Oberfläche in Jetpack Compose. `SetupText.kt` baut Texte und Zahlenformate aus den Sprachdateien. `App.kt` hält die untere Leiste, `SetupTab.kt` die Modus-Liste, `InputTab.kt` die Eingaben, `InfoTab.kt` Rechenweg und Quelle, `Components.kt` die Bausteine.
- `app/src/main/res/values/strings.xml` (Englisch, Standard) und `values-de/strings.xml` (Deutsch): alle Texte.
- `app/src/test/`: Tests mit Beispielen aus dem Guide.
- `docs/index.html`: Web-Version.

## Lizenzen
MIT für den eigenen Code ([LICENSE](LICENSE)). Quelle des Rechenwegs, fremde Bestandteile und Hinweise: [NOTICE.md](NOTICE.md).

## Mitmachen
Issues und Pull Requests sind willkommen, vor allem Rückmeldungen, wie gut die Vorschläge in der Praxis passen, und weitere Übersetzungen. Eine Übersetzung ist eine Datei: `app/src/main/res/values/strings.xml` nach `values-xx/strings.xml` kopieren und die Sprache in `res/xml/locales_config.xml` eintragen.
