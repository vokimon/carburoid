package net.canvoki.carburoid.distances

import net.canvoki.carburoid.location.GeoPoint
import net.canvoki.shared.test.assertEquals
import org.junit.Test

class SpainLandMassTest {
    // MAINLAND (Core + boundary extremes + Balearic-overlaps)

    @Test fun `MAINLAND central madrid`() {
        val madrid = GeoPoint(latitude = 40.4168, longitude = -3.7038)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(madrid))
    }

    @Test fun `MAINLAND southern extreme Tarifa`() {
        val tarifa = GeoPoint(latitude = 36.0, longitude = -5.6103)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(tarifa))
    }

    @Test fun `MAINLAND northern extreme Estaca de Bares`() {
        val estacaDeBares = GeoPoint(latitude = 43.7937, longitude = -7.6800)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(estacaDeBares))
    }

    @Test fun `MAINLAND western extreme Cabo Touriñan`() {
        val caboTouriñan = GeoPoint(latitude = 43.045, longitude = -9.3105)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(caboTouriñan))
    }

    @Test fun `MAINLAND eastern extreme Cap de Creus`() {
        val capDeCreus = GeoPoint(latitude = 42.3180, longitude = 3.3320)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(capDeCreus))
    }

    @Test fun `MAINLAND with BALEARIC latitude Girona`() {
        val girona = GeoPoint(latitude = 41.9794, longitude = 2.8214)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(girona))
    }

    @Test fun `MAINLAND with BALEARIC longitude Valencia`() {
        val valencia = GeoPoint(latitude = 39.4699, longitude = -0.3763)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(valencia))
    }

    // CANARY ISLANDS (Core + boundary extremes)

    @Test fun `CANARY central Las Palmas`() {
        val lasPalmas = GeoPoint(latitude = 28.1235, longitude = -15.4363)
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(lasPalmas))
    }

    @Test fun `CANARY northern extreme Alegranza`() {
        val alegranza = GeoPoint(latitude = 29.418047, longitude = -13.5130967)
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(alegranza))
    }

    @Test fun `CANARY southern extreme La Restinga at El Hierro`() {
        val laRestinga = GeoPoint(latitude = 27.63662, longitude = -17.98389)
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(laRestinga))
    }

    @Test fun `CANARY eastern extreme at Lanzarote`() {
        val lanzarote = GeoPoint(latitude = 29.20168, longitude = -13.41956)
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(lanzarote))
    }

    @Test fun `CANARY western extreme at El Hierro`() {
        val hierro = GeoPoint(latitude = 27.71452, longitude = -18.16145)
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(hierro))
    }

    // AUTONOMOUS CITIES

    @Test fun `AUTONOMOUS_CITIES Ceuta`() {
        val ceuta = GeoPoint(latitude = 35.8894, longitude = -5.3213)
        assertEquals(SpainLandMass.AUTONOMOUS_CITIES, SpainLandMass.of(ceuta))
    }

    @Test fun `AUTONOMOUS_CITIES Melilla`() {
        val melilla = GeoPoint(latitude = 35.2923, longitude = -2.9382)
        assertEquals(SpainLandMass.AUTONOMOUS_CITIES, SpainLandMass.of(melilla))
    }

    @Test fun `AUTONOMOUS_CITIES north of Ceuta`() {
        val ceutaEdge = GeoPoint(latitude = 35.92189, longitude = -5.365865)
        assertEquals(SpainLandMass.AUTONOMOUS_CITIES, SpainLandMass.of(ceutaEdge))
    }

    // BALEARIC ISLANDS

    @Test fun `BALEARIC central Palma de Mallorca`() {
        val palma = GeoPoint(latitude = 39.5696, longitude = 2.6502)
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(palma))
    }

    @Test fun `BALEARIC western extreme Far Bleda Plana a les Pitiuses`() {
        val ibiza = GeoPoint(latitude = 38.97991, longitude = 1.157889)
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(ibiza))
    }

    @Test fun `BALEARIC eastern extreme Fortalasa de Isabel II a Maó Menorca`() {
        val mao = GeoPoint(latitude = 39.876975, longitude = 4.327804)
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(mao))
    }

    @Test fun `BALEARIC northern extreme Far de Cavalleria a Menorca`() {
        val farDeCavalleria = GeoPoint(latitude = 40.08930, longitude = 4.092188)
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(farDeCavalleria))
    }

    @Test fun `BALEARIC southern extreme Formentera`() {
        val formentera = GeoPoint(latitude = 38.6964, longitude = 1.4531)
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(formentera))
    }

    @Test fun `BALEARIC does not overextends west Cap de la Nau`() {
        val capDeLaNau = GeoPoint(latitude = 38.787, longitude = 0.327)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(capDeLaNau))
    }

    @Test fun `BALEARIC does not overextends north west Castell de Peniscola`() {
        val castellDePeniscola = GeoPoint(latitude = 40.359, longitude = 0.400)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(castellDePeniscola))
    }

    @Test fun `BALEARIC does not overextends north west Far del Cap de Tortosa`() {
        val farDelCapDeTortosa = GeoPoint(latitude = 40.714, longitude = 0.946)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(farDelCapDeTortosa))
    }

    @Test fun `BALEARIC does not overextends north west safe point at sea`() {
        val castellDePeniscola = GeoPoint(latitude = 40.359, longitude = 0.400)
        val farDelCapDeTortosa = GeoPoint(latitude = 40.714, longitude = 0.946)
        // Safe point: mix latitude from one, longitude from another -> 40.359, 0.946
        val safePoint = GeoPoint(latitude = castellDePeniscola.latitude, longitude = farDelCapDeTortosa.longitude)
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(safePoint))
    }
}
