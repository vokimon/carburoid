package net.canvoki.carburoid.distances

import net.canvoki.carburoid.location.GeoPoint
import net.canvoki.shared.test.assertEquals
import org.junit.Test

class FranceLandMassTest {
    // MAINLAND (Core + boundary extremes)

    @Test fun `MAINLAND central Paris`() {
        val paris = GeoPoint(latitude = 48.8566, longitude = 2.3522)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(paris))
    }

    @Test fun `MAINLAND southern extreme near Perpignan`() {
        val perpignan = GeoPoint(latitude = 42.7042, longitude = 2.8967)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(perpignan))
    }

    @Test fun `MAINLAND northern extreme near Dunkirk`() {
        val dunkirk = GeoPoint(latitude = 51.0333, longitude = 2.3783)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(dunkirk))
    }

    @Test fun `MAINLAND western extreme near Pointe de Corsen`() {
        val pointeDeCorsen = GeoPoint(latitude = 48.3667, longitude = -4.7667)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(pointeDeCorsen))
    }

    @Test fun `MAINLAND eastern extreme near Lauterbourg`() {
        val lauterbourg = GeoPoint(latitude = 48.9722, longitude = 8.2167)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(lauterbourg))
    }

    @Test fun `MAINLAND with CORSICA latitude Nice`() {
        val nice = GeoPoint(latitude = 43.7102, longitude = 7.2620)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(nice))
    }

    @Test fun `MAINLAND with CORSICA longitude Genoa area`() {
        val genoaArea = GeoPoint(latitude = 44.4056, longitude = 8.9463)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(genoaArea))
    }

    // CORSICA ISLANDS (Core + boundary extremes)

    @Test fun `CORSICA central Ajaccio`() {
        val ajaccio = GeoPoint(latitude = 41.9256, longitude = 8.7372)
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(ajaccio))
    }

    @Test fun `CORSICA northern extreme Cap Corse`() {
        val capCorse = GeoPoint(latitude = 43.0278, longitude = 9.3586)
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(capCorse))
    }

    @Test fun `CORSICA southern extreme Bonifacio`() {
        val bonifacio = GeoPoint(latitude = 41.3878, longitude = 9.1580)
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(bonifacio))
    }

    @Test fun `CORSICA western extreme Île de Cavallo`() {
        val ileDeCavallo = GeoPoint(latitude = 41.3333, longitude = 9.0833)
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(ileDeCavallo))
    }

    @Test fun `CORSICA eastern extreme Alistro`() {
        val alistrio = GeoPoint(latitude = 42.2500, longitude = 9.5000)
        assertEquals(FranceLandMass.CORSICA, FranceLandMass.of(alistrio))
    }

    @Test fun `CORSICA does not overextend west mainland France near Bastia`() {
        val bastiaWest = GeoPoint(latitude = 42.7000, longitude = 8.4000)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(bastiaWest))
    }

    @Test fun `CORSICA does not overextend north mainland France near Calvi`() {
        val calviNorth = GeoPoint(latitude = 43.2000, longitude = 8.8000)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(calviNorth))
    }

    @Test fun `CORSICA does not overextend south Sardinia`() {
        val sardinia = GeoPoint(latitude = 40.5000, longitude = 9.2000)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(sardinia))
    }

    @Test fun `CORSICA does not overextend east Italy`() {
        val italy = GeoPoint(latitude = 42.0000, longitude = 10.0000)
        assertEquals(FranceLandMass.MAINLAND, FranceLandMass.of(italy))
    }
}
