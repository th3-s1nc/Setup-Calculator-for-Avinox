# Setup Calculator for Avinox

[Deutsche Version](README.de.md)

An unofficial setup calculator ("Setup-Rechner für Avinox") for Avinox M1, M2 and M2S e-bike drive units. From motor, weight, rider power and cadence it suggests assist level, max. watts and max. Nm for each riding mode. There is an Android app and a [web version](https://th3-s1nc.github.io/Setup-Calculator-for-Avinox/) using the same calculation.

> **Not affiliated** with Avinox or DJI. All product names and trademarks belong to their owners and are used only to describe compatibility. **No warranty.** The values are starting points and do not replace tuning by feel.

## What it does
- The four stock modes (ECO, AUTO, TRAIL, TURBO) in three flavours (all-rounder, long distance, power), or the full ladder with additional custom modes (8 steps on the M1, 9 on M2 and M2S).
- For each mode, motor power by cadence, with a note when the watt limit cannot be reached at your own cadence.
- Profiles for several riders, each with its own name and inputs. Inputs are saved explicitly; a new profile starts empty.
- Share or copy the setup as text.
- No internet access, no ads, no data collection. Your inputs stay on the device.
- User interface in English and German. The app follows the phone's language and can be switched under Info.

## Where the calculation comes from
The calculation method and the assist level percentages come from the setup guide by Bernd Hemmersbach (preliminary version of 11 May 2026) and his "Generelles Setup" sheets. Published with the author's permission. The guide itself is not part of this project. Details and deviations: [NOTICE.md](NOTICE.md) (German).

## Requirements
- Android 8.0+ (API 26)

## Install
Download the APK from the [releases](https://github.com/th3-s1nc/Setup-Calculator-for-Avinox/releases) and open it on your phone.

## Troubleshooting (installing the APK)
- **"App blocked to protect your device" (Google Play Protect):** tap *More details*, then *Install anyway*. Play Protect does not know the signing key of this open-source app yet.
- **"For your security, your phone is not allowed to install unknown apps from this source":** tap *Settings* in that message, allow the browser or file manager you opened the APK with, then go back and install.

## Build
Open the project folder in Android Studio, wait for the Gradle sync, press Run. You need Android SDK Platform 35 and JDK 17 or newer (the JDK bundled with Android Studio is enough). Tests: `./gradlew test`.

## Web version
Live at <https://th3-s1nc.github.io/Setup-Calculator-for-Avinox/>. `docs/index.html` is the calculator (English and German, with a switch) as a single file with no server and no external dependencies; GitHub Pages serves it from the `/docs` folder of `main`. The web version has no rider profiles and remembers one set of inputs in the browser.

## Layout
- `app/src/main/java/io/github/th3s1nc/setuprechner/calc/SetupCalculator.kt`: the calculation core, plain Kotlin. Targets, mode ladders and motor figures are at the top of the file.
- `.../ui/`: Jetpack Compose user interface. `SetupText.kt` builds the texts and number formats from the language files.
- `app/src/main/res/values/strings.xml` (English, default) and `values-de/strings.xml` (German): all texts.
- `app/src/test/`: tests with examples from the guide.
- `docs/index.html`: web version.

## Licenses
MIT for this project's own code (see [LICENSE](LICENSE)). Source of the calculation, third-party parts and notes: [NOTICE.md](NOTICE.md).

## Contributing
Issues and pull requests are welcome, especially feedback on how well the suggestions work in practice, and further translations. A translation is one file: copy `app/src/main/res/values/strings.xml` to `values-xx/strings.xml` and add the language to `res/xml/locales_config.xml`.
