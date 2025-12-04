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
- [ ] Products: further increase the contrast among categories and products in the dropmenu
- [ ] Products: Sort by popularity. How many gas station serve it. Now it is sorted by hand.
- [ ] Product: Get product list from Ministry API
- [ ] Product: Use short names (now that we are translating we could translate shorter)
- [ ] Language: Asturian
- [ ] Language: Aranese (Occitan)
- [ ] Modernize code
    - [ ] Migrate Activities to ViewBinding
    - [ ] Migrate uses of android package to androidx
    - [~] Compose
- [ ] Favorites
- [ ] Plot view: Buttons `[Closer]` `[Cheaper]` to go each side
- [ ] Plot view: Add Location Picker widget
- [ ] Plot view: Add AppBar and back button
- [ ] Plot view: Go to details on view
- [ ] Plot view: Animation of the summary as feedback to the swipe

## Done

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




