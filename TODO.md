# TODO

Do you want to help? This is my roadmap.

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
- [ ] List: Different style for stations that appear because we disabled a filter
- [ ] Filters: Hide stations beyond the sea (Peninsule, Canary, Balearic, Ceuta, Melilla)
- [x] Products: Recent products on top
- [x] Products: Categorize
- [ ] Products: Sort by popularity (how many gas station serve it)
- [x] Product: Translate literals in CategorizedProductSelector
- [ ] Product: Get list from Ministry API
- [x] Product translation (now Api provides an spanish name as id, monolingual and fragile)
- [ ] Language: Asturian
- [ ] Language: Aranese (Occitan)
- [ ] Modernize code
    - [ ] Migrate Activities to ViewBinding
    - [ ] Migrate uses of android package to androidx
    - [~] Migrate state to ModelView
    - [~] Compose
- [ ] Favorites

## Done

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




