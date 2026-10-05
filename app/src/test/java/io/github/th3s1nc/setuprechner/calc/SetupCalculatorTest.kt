package io.github.th3s1nc.setuprechner.calc

import org.junit.Assert.assertEquals
import org.junit.Test

/** Vergleicht den Rechner mit Beispielen aus dem Setup-Guide und den Blättern "Generelles Setup". */
class SetupCalculatorTest {

    private fun setup(
        motor: Motor, bike: Double, rider: Double, power: Double, cadence: Int,
        scope: Scope = Scope.FOUR, profile: Profile = Profile.ALLROUND
    ): List<String> =
        SetupCalculator.compute(SetupInput(motor, bike, rider, power, cadence, scope, profile)).modes.map {
            val al = if (it.isRange) "${it.alLo}-${it.alHi}" else "${it.alLo}"
            "${it.name} $al/${it.watt}/${it.nm}"
        }

    @Test
    fun guideStepByStep() {
        // Guide 2.5: 110 kg, 75 U/min, 200 W
        assertEquals(
            listOf("ECO 3/150/20", "AUTO 4-6/300/40", "TRAIL 6-9/600/80", "TURBO 11/850/105"),
            setup(Motor.M1, 23.0, 87.0, 200.0, 75)
        )
    }

    @Test
    fun guideLowAndHighCadence() {
        // Guide 3.2: 60 und 90 U/min
        assertEquals(
            listOf("ECO 3/150/25", "AUTO 4-6/300/50", "TRAIL 6-9/600/100", "TURBO 11/850/105"),
            setup(Motor.M1, 23.0, 87.0, 200.0, 60)
        )
        assertEquals(
            listOf("ECO 3/150/20", "AUTO 4-6/300/35", "TRAIL 6-9/600/65", "TURBO 11/850/95"),
            setup(Motor.M1, 23.0, 87.0, 200.0, 90)
        )
    }

    @Test
    fun guideWeakAndStrongRider() {
        // Guide 3.3: 150 und 300 W Eigenleistung
        assertEquals(
            listOf("ECO 4/150/20", "AUTO 5-8/300/40", "TRAIL 8-11/600/80", "TURBO 13/850/105"),
            setup(Motor.M1, 23.0, 87.0, 150.0, 75)
        )
        assertEquals(
            listOf("ECO 2/150/20", "AUTO 3-4/300/40", "TRAIL 6-8/600/80", "TURBO 9/850/105"),
            setup(Motor.M1, 23.0, 87.0, 300.0, 75)
        )
    }

    @Test
    fun guideLongDistance() {
        // Guide 3.5
        assertEquals(
            listOf("ECO 2/100/15", "AUTO 3-4/200/30", "TRAIL 6-7/300/40", "TURBO 8/450/60"),
            setup(Motor.M1, 23.0, 87.0, 200.0, 75, profile = Profile.LONG)
        )
    }

    @Test
    fun sheetM2() {
        // Blatt "Generelles SET-UP M2": 112 kg, 75 U/min, 200 W.
        // TURBO steht im Blatt mit 75 Nm, dort von Hand um 5 Nm gesenkt.
        assertEquals(
            listOf(
                "LOW 1/100/10", "FLAT 2/150/20", "ECO 4/200/30",
                "ROAD 5/250/35", "AUTO 5-7/350/45", "TRAIL 7-8/450/60",
                "TURBO 9/600/80", "POWER 11/800/95", "BEAST 13/1000/110"
            ),
            setup(Motor.M2, 22.0, 90.0, 200.0, 75, scope = Scope.ALL)
        )
    }

    @Test
    fun turboPowerAtCadence() {
        // Guide S. 9: TURBO liefert bei 75 U/min 825 W, die vollen 850 W ab 78 U/min
        val turbo = SetupCalculator.compute(
            SetupInput(Motor.M1, 23.0, 87.0, 200.0, 75, Scope.FOUR, Profile.ALLROUND)
        ).modes.last()
        assertEquals(825.0, turbo.climb, 0.5)
        assertEquals(78, turbo.rpmForMax)
    }

    @Test
    fun batteryOfTheM2S() {
        // DJI: im Boost 1500 W mit FP700/RS800, 1300 W mit FS800/FS600. Die Modi bleiben bei 1300 W.
        fun boost(battery: Battery?) = SetupCalculator.compute(
            SetupInput(Motor.M2S, 22.0, 90.0, 300.0, 95, Scope.ALL, Profile.ALLROUND, battery)
        )
        assertEquals(1500, boost(Battery.FP).boost.watt)
        assertEquals(1300, boost(Battery.FS).boost.watt)
        assertEquals(1500, boost(null).boost.watt)
        assertEquals(boost(Battery.FP).modes.map { it.signature }, boost(Battery.FS).modes.map { it.signature })
        assertEquals(1000, SetupCalculator.compute(SetupInput(Motor.M1, 23.0, 87.0, 200.0, 75, Scope.FOUR, Profile.ALLROUND, Battery.FS)).boost.watt)
    }

    @Test
    fun adjustByHand() {
        fun auto(adjust: Map<String, Adjust>) = SetupCalculator.compute(
            SetupInput(Motor.M1, 23.0, 87.0, 200.0, 75, Scope.FOUR, Profile.ALLROUND, null, adjust)
        ).modes[1]
        val plain = auto(emptyMap())
        assertEquals("4-6/300/40", plain.signature)
        assertEquals(false, plain.adjusted)
        val changed = auto(mapOf("AUTO" to Adjust(alHi = 1, nm = -5)))
        assertEquals("4-7/300/35", changed.signature)
        assertEquals(true, changed.adjusted)
        assertEquals(40, changed.calcNm)
        assertEquals(-5, changed.dNm)
        // Der Einstellbereich des Modus begrenzt die Anpassung: AUTO geht bis Level 11 und 105 Nm.
        val capped = auto(mapOf("AUTO" to Adjust(alHi = 9, nm = 500, watt = -5000)))
        assertEquals("4-11/200/105", capped.signature)
    }

    @Test
    fun gradeEstimate() {
        // 110 kg, 200 W Fahrer + 600 W Motor bei 15 km/h: rund 15 %
        assertEquals(15.4, SetupCalculator.grade(800.0, 110.0, 15.0), 0.3)
        // langsamer ist steiler, mehr Gewicht ist flacher
        assertEquals(true, SetupCalculator.grade(800.0, 110.0, 13.0) > SetupCalculator.grade(800.0, 110.0, 15.0))
        assertEquals(true, SetupCalculator.grade(800.0, 140.0, 15.0) < SetupCalculator.grade(800.0, 110.0, 15.0))
    }
}
