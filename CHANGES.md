# Change log

## Unreleased

New location picker and ongoing French translation

- ✨ Location picker: Dark theme map in dark mode
- ✨ Location picker: Long press set a route (disabled)
- ♻️ Location picker: using maplibre-compose
- 🐛 Location picker: Takes properly location when comming from GPS
- 🌐 FR translation 53%
- ♻️ Station details migrated to compose

## 1.4.3 (2026-02-16)

F-Droid build fixes

- 🏗️ Build android-yaml-string as submodule to reenable f-droid
- 🏗️ Acknoledge to FDroid non-free data source data.economie.gouv.fr
- ♻️ Extracted WeblateLink to shared

## 1.4.2 (2026-02-16)

Internal reorganization and supporting incomplete translations

- 🏗️ Upgraded dependencies to current BOM versions
- 🏗️ Extracted general code as shared library, internal by now
- 🏗️ Extracted yaml translation plugin as https://github.com/vokimon/android-yaml-strings
- 🔧 Do not fail on WIP translations
- 🚸 Warn about WIP translations

## 1.4.1 (2026-02-12)

Production ready France support and Plot navigator

- ✨ France land masses: do not show corsica if in mainland and viceversa
- ✨ France names and brands: use a static database as workarround
- ✨ France opening hours
- ✨ Plot navigator: No more a experimental view
- ✨ Plot navigator: Landscape layout
- ✨ Plot navigator: Header, backbutton and location/product selectors
- 🧹 Plot navigator: Removed koalaplot extensions already integrated in koalaplot 0.11.0
- 🌐 Plot navigator: Texts translations

## 1.4.0 (2026-02-08)

French gas stations and Compose/Material3 Settings rewrite

- ✨ Data source option: Enables support for countries other than Spain
- ✨ French gas stations (with limitations)
- 🚸 One time warning when disabling expensive filter
- 💄 Settings page rewritten on Compose/Material 3
- ⚡️ Using a faster data loading library (Gson -> Kotlin Serialization) 20% speed up
- ♻️  API format abstraction. A step towards supporting for other countries

## 1.3.3 (2026-01-15)

Main screen migrated to Compose plus visual and UX improvements

- 🚸 On landscape, side by side product and location selectors gives more space to the list
- 🚸 Avoid blocking the UI while refiltering stations
- 🚸 Show a placeholder message while refiltering stations
- 💥 Not a change from us: Google Maps now shares imparseable opaque urls.
     Workaround if you do not care about your privacy:
     - First Share the location from GMaps to a browser
     - Then share the url from the Browser to Carburoid
- 📝 README: Describe what the application really does
- 📝 Added a FAQ document with common questions
- 💄 Loading placeholder with watermark icon and bigger font
- 💄 PlotNav: Reduce float precission axis labels
- 💄 Icon for weblate link in settings
- ♻️ Station list screen fully migrated to Compose
- ♻️ UserMessageBus: Decoupled Compose based Snackbar messages

## 1.3.2 (2025-12-24)

Working remote product setting

- 🐛 Remote product setting mistakenly used lowlevel interface
     skiping safeguards of high level interface and failed
     to change the product on some conditions.

## 1.3.1 (2025-12-24)

Gas Station List rewritten with Compose

- ✨ Application Intent to externally change the product (Partially addresses #5)
- 🐛 Plot Nav: Fix phantom station list when changing filters (Fixes #15)
- 🌐 Filter settings: "Irreleval stations" instead "expensive and far", shorter and emphasizes intent
- 🚸 Gas Station List: Scrollbar indicating how far we are from the closer - cheaper.
- 💄 Gas Station List: Background image on empty search
- 💄 Gas Station List: Product and Location selectors are more compact.
- ♻️ Gas Station List: Migrated to Compose

## 1.3.0 (2025-12-10)

Product selector rewrite, many ui fixes and a new experimental plot navigator

- ✨ Product categories for easy search
- ✨ Recent products on top
- ✨ Hide stations on a distinct land mass: Peninsule, Canary, Balearic or Autonomous Cities. (Fixes #
- 🚸 Location picker just accept on exit (instead accept or cancel)
- 🌐 Product names and categories translated
- 💄 Detail view with the staion name in the AppBar
- 🐛 Android 16 ui still slipped under the appbar and system bar (Fixes #2)
- 🐛 Product label and name overlaped (Fixes #9)
- 🐛 Osm links were not processed
- 🐛 Filter on about to close/open did not apply inmediatelly
- 🐛 Location and product texts non-editable to prevent weird state
- ♻️ Using ModelView for filtering GasStations
- ♻️ Some classic Views migrated to Compose
- 🚧 Experimental plot navigator

## 1.2.3 (2025-11-15)

- 💄 Less anoying data spinner for data refreshing
- 🌐 Spanish contained untranslated Catalan words (Fixes #4)
    - Thanks to wuniversales for the report
- 🏗️ agp upgraded back. FDroid server got an upgrade.

## 1.2.2 (2025-11-14)

- 🏗️ agp downgraded 8.13.0 -> 8.11.0 because fdroid build server (Fixes #1)

## 1.2.1 (2025-11-14)

Layout fixes on Android 15 and issues on language/rotation changes

- 💄 Icons for some preferences
- 🌐 Arabic added (via ChatGpt, sorry)
- 🐛 Clearer text for weblate link
- 🐛 Android 15 slided the product selector under the AppBar
    - Thanks to Antonio Romero for the report
- 🐛 Memory leaks when stoping or pausing Location Picker
- 🐛 Location picker lost position when rotating the device
- 🐛 Language changes flickered and not fully applied
- 🐛 Current position was lost on rotation or language change

## 1.2.0 (2025-10-28)

Edit your current position

- ✨ By clicking the edit icon, enter a edit position screen
- ✨ Edit position with interactive map
- ✨ Edit position with utocompletion location search
- ✨ GPS reposition by clicking on concentric target icon
- 💥 Swipe does not update current positon, just reload gas stations.
- 💄 Avoid flicker when updating the description

## 1.1.0 (2025-10-26)

Bidirectional connection with mapping apps

- ✨ Carburoid as share target for locations
- ✨ Carburoid as handler for geo uri
- ✨ Carburoid as handler for http OSD links
- ✨ Carburoid as handler for http Google Maps links

## 1.0.3 (2025-10-20)

Even more FDroid tweaks:

- 🏗️ Fdroid: Removed privative dependency info only readable by Google
- 🏗️ Cleaned warnings on optin use of experimental api's on tests

## 1.0.2 (2025-10-19)

FDroid tweaks:

- 🏗️ Signed packages and related automations
- 🏗️ Informative apk names: appid-flavor-version-build.apk
- 🏗️ bump to gradle 8.14

## 1.0.1 (2025-10-15)

First version on FDroid, featuring:

- 🏗️ floss and nonfree flavors, using or not privative GPlay location services
- 🌐 Galician translation. Thanks Beatriz Plana
- 🌐 Basque translation. Thanks Javier Calzado Sànchez (javikalsan)
- 🐛 After changing language, product list disappeared
- 🐛 Settings title was untranslated
- 💄 Icons, colors and pill bg for open/closed status
- 💄 Translate preference moved down
- 💄 Better Material support
- ♻️  Station "open status" is more modular

## 1.0.0 (2025-10-13)

First public release featuring:

- ✨ Sort stations by distance to your current position
- ✨ Filter out stations without your product
- ✨ Filter out stations more expensive than nearest ones
- ✨ Filter out closed stations unless they open soon
- ✨ Configuration to disable any of the previous settings
- ✨ Gas station details page
- ✨ Open a gas station in your chosen map application
- 🌐 Translations to Spanish, Catalan, Aragonese and more to come
- 💄 Dark and Light themes

