package net.canvoki.carburoid.distances

import net.canvoki.shared.test.assertEquals
import org.junit.Test

class FranceLandMassTest {
    // MAINLAND (Core + boundary extremes)

    @Test fun `MAINLAND central Paris`() {
        val paris = 48.8566 to 2.3522
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(paris.first, paris.second))
    }

    @Test fun `MAINLAND southern extreme near Perpignan`() {
        val perpignan = 42.7042 to 2.8967
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(perpignan.first, perpignan.second))
    }

    @Test fun `MAINLAND northern extreme near Dunkirk`() {
        val dunkirk = 51.0333 to 2.3783
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(dunkirk.first, dunkirk.second))
    }

    @Test fun `MAINLAND western extreme near Pointe de Corsen`() {
        val pointeDeCorsen = 48.3667 to -4.7667
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(pointeDeCorsen.first, pointeDeCorsen.second))
    }

    @Test fun `MAINLAND eastern extreme near Lauterbourg`() {
        val lauterbourg = 48.9722 to 8.2167
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(lauterbourg.first, lauterbourg.second))
    }

    @Test fun `MAINLAND with CORSICA latitude Nice`() {
        val nice = 43.7102 to 7.2620
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(nice.first, nice.second))
    }

    @Test fun `MAINLAND with CORSICA longitude Genoa area`() {
        val genoaArea = 44.4056 to 8.9463
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(genoaArea.first, genoaArea.second))
    }

    // CORSICA ISLANDS (Core + boundary extremes)

    @Test fun `CORSICA central Ajaccio`() {
        val ajaccio = 41.9256 to 8.7372
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(ajaccio.first, ajaccio.second))
    }

    @Test fun `CORSICA northern extreme Cap Corse`() {
        val capCorse = 43.0278 to 9.3586
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(capCorse.first, capCorse.second))
    }

    @Test fun `CORSICA southern extreme Bonifacio`() {
        val bonifacio = 41.3878 to 9.1580
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(bonifacio.first, bonifacio.second))
    }

    @Test fun `CORSICA western extreme Île de Cavallo`() {
        val ileDeCavallo = 41.3333 to 9.0833
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(ileDeCavallo.first, ileDeCavallo.second))
    }

    @Test fun `CORSICA eastern extreme Alistro`() {
        val alistrio = 42.2500 to 9.5000
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(alistrio.first, alistrio.second))
    }

    @Test fun `CORSICA does not overextend west mainland France near Bastia`() {
        val bastiaWest = 42.7000 to 8.4000
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(bastiaWest.first, bastiaWest.second))
    }

    @Test fun `CORSICA does not overextend north mainland France near Calvi`() {
        val calviNorth = 43.2000 to 8.8000
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(calviNorth.first, calviNorth.second))
    }

    @Test fun `CORSICA does not overextend south Sardinia`() {
        val sardinia = 40.5000 to 9.2000
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(sardinia.first, sardinia.second))
    }

    @Test fun `CORSICA does not overextend east Italy`() {
        val italy = 42.0000 to 10.0000
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(italy.first, italy.second))
    }
}
