package io.github.th3s1nc.setuprechner.ui

import io.github.th3s1nc.setuprechner.calc.Adjust
import io.github.th3s1nc.setuprechner.calc.Battery
import io.github.th3s1nc.setuprechner.calc.Motor
import io.github.th3s1nc.setuprechner.calc.Profile
import io.github.th3s1nc.setuprechner.calc.Scope
import org.junit.Assert.assertEquals
import org.junit.Test

/** Sicherung der Profile und Link zum Teilen, beides im Format der Web-Version. */
class ExchangeTest {

    private val franz = BackupProfile(
        name = "Franz",
        values = ProfileValues("Franz", Motor.M2S, Scope.ALL, Profile.ALLROUND, "22", "90,5", "300", "95", Battery.FS),
        adjust = mapOf(Scope.ALL to mapOf("ALL IN" to Adjust(al = -1, watt = -100))),
        checks = mapOf(Scope.ALL to mapOf("LOW" to "1-1/100/10"))
    )

    @Test
    fun backupRoundTrip() {
        val back = Exchange.decode(Exchange.encode(listOf(franz), "2026-10-05T10:00:00Z"))
        // Zahlen stehen in der Datei mit Punkt, wie im Browser
        assertEquals(listOf(franz.copy(values = franz.values.copy(rider = "90.5"))), back)
    }

    @Test
    fun foreignFilesAreRejected() {
        assertEquals(null, Exchange.decode("{\"foo\": 1}"))
        assertEquals(null, Exchange.decode("kein JSON"))
    }

    @Test
    fun shareLink() {
        assertEquals(
            Exchange.SITE + "#m=M2S&a=fs&b=22&r=90.5&p=300&c=95&s=all&x=ALL+IN.al.-1%2CALL+IN.watt.-100",
            Exchange.link(franz.values, franz.adjust[Scope.ALL] ?: emptyMap())
        )
        assertEquals(
            Exchange.SITE + "#m=M1&b=23&r=87&p=200&c=75&s=four&f=allround",
            Exchange.link(ProfileValues("", Motor.M1, Scope.FOUR, Profile.ALLROUND, "23", "87", "200", "75"), emptyMap())
        )
    }
}
