package net.canvoki.carburoid.distances

import org.junit.Test
import kotlin.test.assertEquals

class SpainLandMassTest {
    // MAINLAND (Core + boundary extremes + Balearic-overlaps)

    @Test fun `MAINLAND central madrid`() {
        val madrid = 40.4168 to -3.7038
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(madrid.first, madrid.second))
    }

    @Test fun `MAINLAND southern extreme Tarifa`() {
        val tarifa = 36.0 to -5.6103
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(tarifa.first, tarifa.second))
    }

    @Test fun `MAINLAND northern extreme Estaca de Bares`() {
        val estacaDeBares = 43.7937 to -7.6800
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(estacaDeBares.first, estacaDeBares.second))
    }

    @Test fun `MAINLAND western extreme Cabo Touriñan`() {
        val caboTouriñan = 43.045 to -9.3105
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(caboTouriñan.first, caboTouriñan.second))
    }

    @Test fun `MAINLAND eastern extreme Cap de Creus`() {
        val capDeCreus = 42.3180 to 3.3320
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(capDeCreus.first, capDeCreus.second))
    }

    @Test fun `MAINLAND with BALEARIC latitude Girona`() {
        val girona = 41.9794 to 2.8214
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(girona.first, girona.second))
    }

    @Test fun `MAINLAND with BALEARIC longitude Valencia`() {
        val valencia = 39.4699 to -0.3763
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(valencia.first, valencia.second))
    }

    // CANARY ISLANDS (Core + boundary extremes)

    @Test fun `CANARY central Las Palmas`() {
        val lasPalmas = 28.1235 to -15.4363
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(lasPalmas.first, lasPalmas.second))
    }

    @Test fun `CANARY northern extreme Alegranza`() {
        val alegranza = 29.418047 to -13.5130967
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(alegranza.first, alegranza.second))
    }

    @Test fun `CANARY southern extreme La Restinga at El Hierro`() {
        val laRestinga = 27.63662 to -17.98389
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(laRestinga.first, laRestinga.second))
    }

    @Test fun `CANARY eastern extreme at Lanzarote`() {
        val lanzarote = 29.20168 to -13.41956
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(lanzarote.first, lanzarote.second))
    }

    @Test fun `CANARY western extreme at El Hierro`() {
        val hierro = 27.71452 to -18.16145
        assertEquals(SpainLandMass.CANARY, SpainLandMass.of(hierro.first, hierro.second))
    }

    // AUTONOMOUS CITIES

    @Test fun `AUTONOMOUS_CITIES Ceuta`() {
        val ceuta = 35.8894 to -5.3213
        assertEquals(SpainLandMass.AUTONOMOUS_CITIES, SpainLandMass.of(ceuta.first, ceuta.second))
    }

    @Test fun `AUTONOMOUS_CITIES Melilla`() {
        val melilla = 35.2923 to -2.9382
        assertEquals(SpainLandMass.AUTONOMOUS_CITIES, SpainLandMass.of(melilla.first, melilla.second))
    }

    @Test fun `AUTONOMOUS_CITIES north of Ceuta`() {
        val ceutaEdge = 35.92189 to -5.365865
        assertEquals(SpainLandMass.AUTONOMOUS_CITIES, SpainLandMass.of(ceutaEdge.first, ceutaEdge.second))
    }

    // BALEARIC ISLANDS

    @Test fun `BALEARIC central Palma de Mallorca`() {
        val palma = 39.5696 to 2.6502
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(palma.first, palma.second))
    }

    @Test fun `BALEARIC western extreme Far Bleda Plana a les Pitiuses`() {
        val ibiza = 38.97991 to 1.157889
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(ibiza.first, ibiza.second))
    }

    @Test fun `BALEARIC eastern extreme Fortalasa de Isabel II a Maó Menorca`() {
        val mao = 39.876975 to 4.327804
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(mao.first, mao.second))
    }

    @Test fun `BALEARIC northern extreme Far de Cavalleria a Menorca`() {
        val farDeCavalleria = 40.08930 to 4.092188
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(farDeCavalleria.first, farDeCavalleria.second))
    }

    @Test fun `BALEARIC southern extreme Formentera`() {
        val formentera = 38.6964 to 1.4531
        assertEquals(SpainLandMass.BALEARIC, SpainLandMass.of(formentera.first, formentera.second))
    }

    @Test fun `BALEARIC does not overextends west Cap de la Nau`() {
        val capDeLaNau = 38.787 to 0.327
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(capDeLaNau.first, capDeLaNau.second))
    }

    @Test fun `BALEARIC does not overextends north west Castell de Peniscola`() {
        val castellDePeniscola = 40.359 to 0.400
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(castellDePeniscola.first, castellDePeniscola.second))
    }

    @Test fun `BALEARIC does not overextends north west Far del Cap de Tortosa`() {
        val farDelCapDeTortosa = 40.714 to 0.946
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(farDelCapDeTortosa.first, farDelCapDeTortosa.second))
    }

    @Test fun `BALEARIC does not overextends north west safe point at sea`() {
        val castellDePeniscola = 40.359 to 0.400
        val farDelCapDeTortosa = 40.714 to 0.946
        // Safe point: 40.359, 0.946
        assertEquals(SpainLandMass.MAINLAND, SpainLandMass.of(castellDePeniscola.first, farDelCapDeTortosa.second))
    }
}

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
