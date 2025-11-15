# Change log

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
    - Thanks to Antiono Romero for the report
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

