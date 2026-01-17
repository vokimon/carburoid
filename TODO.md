# TODO

Do you want to help? This is my roadmap.

- [x] Fix: In Landscape, empty List does not expands as cool as Loading list does.
- [ ] Fix: If resolved location description is the same, the icon keeps loading
- [ ] The Data loader pill and the snackbar compete vor the lower part of screeen
    - Benchmark: ~28xx ms en el FP4
- [ ] Data loading performance: Use kotlin serialization instead of gson
    - [x] Split interface and gson implementation
    - [x] Create a Kotlin Serialization version to GSon -> Works but slower
    - [x] Optimize Kotlin Serialization version -> Now 20% faster than GSon
- [ ] Support other countries
    - [ ] Split GasStation, the interface, and SpanishGasStation the implementation with deserialization logic
    - [ ] Support for different ProductCatalogs
    - [ ] Support for different LandMass sets
- [ ] DeepLinks: Notify the user when a sharing did'nt work (not able to parse)
- [ ] DeepLinks: Google started to use opaque urls like https://maps.app.goo.gl/jweCgQbzCw9PretY6
    - [ ] We could fetch the url, and take the coords from the 304 redirect
    - [ ] We could explain the user how to do it (share with a browser, from browser share the url to Carburoid)
- [ ] Location Search: When empty, autocomplete show later searches
- [ ] Location Search: Prioritize near matches to the current position
- [ ] Location Search: Feedback when doing a query
- [ ] Refactor: Invert Paretto: Sort by price, filter by distance, to ease distance recomputation
- [ ] Real road distance
- [ ] Route deviation distance
    - [ ] How to set a route from LocationPicker
    - [ ] How to display the route
    - [ ] Accept routes as deep links
        - https://www.google.com/maps/dir/Start/Waypoint1/Waypoint2/.../Destination
        - xml+gpx
            ```
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />

                <!-- Para abrir GPX desde almacenamiento o compartido -->
                <data android:mimeType="application/gpx+xml" />
                
                <!-- Opcional: permitir abrir desde URI file:// o content:// -->
                <data android:scheme="file" android:host="*" android:pathPattern=".*\\.gpx" />
                <data android:scheme="content" android:host="*" android:pathPattern=".*\\.gpx" />
            </intent-filter>
            ```
    - [ ] material icon 'route'
- [x] Products: further increase the contrast among categories and products in the dropmenu
- [ ] Products: Update product list from: https://energia.serviciosmin.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/Listados/ProductosPetroliferos/
- [ ] Products: Sort by popularity. How many gas station serve it. Now it is sorted by hand.
- [ ] Products: Disable or remove unavailable products.
- [ ] Products: Use short names (now that we are translating we could translate shorter)
- [ ] Products: What to do with official short names: G95E85 GOA GOA+ BGNL... 
- [ ] Language: Asturian
- [ ] Language: Aranese (Occitan)
- [~] Modernize code: Compose
    - [x] Move empty message inside StationList
    - [x] Move reloading message inside StationList
    - [x] Turn MainActivity AppCompatActivity -> ComponentActivity
    - [x] Intent while running: Activity does not change
    - [x] Intent, app not running: Coords are updated but description is not computed
    - [x] Intent, app not running: Coords are updated but station list is not refiltered
    - [x] Changing the style in settings has no effect in main activity, it does on the rest of view open after, but not the main it gets the theme only if i change the language
- [ ] Favorites
- [ ] Plot view: Add Location Selector widget
- [ ] Plot view: Add AppBar and back button
- [ ] Plot view: Go to details on view
- [ ] Plot view: Animation of the summary as feedback to the swipe left-right
- [ ] Plot view: Disable cheaper/nearer buttons when on extremes.
- [ ] Plot view: Rename "Cheaper" -> "Farther" when irrelevant filter is disabled.
- [ ] Station List: Add company icon
- [ ] Discounts: https://geoportalgasolineras.es/geoportal-instalaciones/PlanesDescuento (portal, csv)
    - [ ] https://geoportalgasolineras.es/geoportal/rest/discountPlans
    - [ ] https://geoportalgasolineras.es/geoportal/rest/9999/planesDescuentoEstacion
    - [ ] https://sede.serviciosmin.gob.es/en-US/datosabiertos/catalogo/precios-carburantes
- [ ] Report price incoherences: https://geoportalgasolineras.es/geoportal-instalaciones/IncoherenciaPrecios (form)
- [ ] Other Countries:
    - Listing (most outdated but gives clues): https://geoportalgasolineras.es/geoportal-instalaciones/GeoportalesEuropeos
    - Portugal (portal, not api): https://precoscombustiveis.dgeg.gov.pt/


## Done

- [x] Plot view: Buttons `[Closer]` `[Cheaper]` to go each side
- [x] PlotView: Single point plot crashes
- [x] Location Search: Always accept on back, get ride of the dialog nonsense mentality.
- [x] Details view: Back button on AppBar
- [x] Migrate state to ModelView
- [x] List: Different style to discriminate stations that appear because we disabled a filter
- [x] Filters: Hide stations beyond the sea (Peninsule, Canary, Balearic, Ceuta, Melilla)
- [x] Products: Recent products on top
- [x] Products: Categorize
- [x] Product: Translate literals in CategorizedProductSelector
- [x] Product translation (now Api provides an spanish name as id, monolingual and fragile)
- [x] Translate PositionPicker navigation button tooltips
- [x] When dragging the marker, update the description
- [x] Quick search text clear
- [x] Bug: after setting gps position, picker keeps previous position
- [x] Language: Arabic
- [x] Edit current position
- [x] Limit searchs to spain
- [x] See options while writing (autocomplete)
- [x] Select options while writing
- [x] Fdroid
- [x] Filter out private stations
    - [x] Tipo de venta: P Publico R Restringida A Asociados (si tiene las dos, dos entradas distintas cada una con sus precios)
- [x] Fix: Set initial product from GasStation.currentProduct
- Settings:
    - [x] Launch setting page
    - [x] Darkmode settings
    - [x] Hide furhter and expensive
    - [x] Show only public stations
    - [x] Show onpy open stations
- [x] Save last product selection
- [x] Station details
- [x] Theme MainActivity
- [x] Extract status formatting from adapter so that the detail view can use it
- [x] Warning on non public prices in details
- [x] Why MainActivity doubles sometimes with different data
- [x] About to close stations do not lower the cutoff price
- [x] Configurable language
- [x] Link to weblate
- [x] Language: Aragonese
- [x] Language: Basque
- [x] Language: Galician




