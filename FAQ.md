# Frequently Asked Questions

## How is Carburoid different from other similar apps?

First, Carburoid is Free Libre Open Source Software (FLOSS).
Sure, there are a few free/no-cost apps around,
but FLOSS is not about price, but freedom... and security, and more.
See below on why you want this app and others you use to be FLOSS.

Second, it has a unique noiseless way of presenting gas stations.
This project started out of the frustration with existing apps
showing many gas stations that are not even worth to check
because they were neither cheaper nor closer than others already listed.

## How Carburoid eases choosing a Gas Station?

What you see is a filtered list of gas stations sorted by distance
but skipping those that do not provide value because they 
are more expensive than nearest ones.

So the list starts with the nearest gas station whichever the price.
The next one is the next in distance that lowers or matches the first price, and so on.

As you scroll you go up in distance and down in price.
**You can stop scrolling wherever the saving is not worth the distance.**

## Why do I want an application to be FLOSS?

Anyone can check the source code of a FLOSS app for evil doing.
If authors start doping some paying brand up in the list, community could tell.
If authors start tracking you, comunity could tell.
If community doesn't like what they find,
they can take the source code,
fork it and from there remove the evil part.

And thas why i won't put any evil code...
in a clear and intelligible way... just joking }:-)
Please, don't fork, i'll be nice :-)

## Why downloading from FDroid is good?

As maintainers of the project we could release
binaries that have unreleased malicious code.
F-Droid is a third party that ensures the application file
you download comes from unaltered published source code.
So it is even more safe than downloading it from authors page.

They also check for anti-features.

## Why FDroid says Carburoid has anti-features?

Carburoid takes data from an API offered by the
Ministry of Industry of Spain.
That API is not FLOSS.
It should since it is founded by Spanish people taxes.
But it is not.
Yet it is a safe source for you to get that information.

That's all.

If the app would use a less benign API, FDroid will tell.
Authors didn't tell them about Ministry API, they found out.

## Why is the application not showing a given gas station?

You might have skipped the previous question:
This app does not show gas stations that are not cheaper or nearer than others shown.
So if the station is not shown, it is likely that cheaper or closer stations exist.

Other filtering could be affecting:

- Not having the selected product
- Being currently closed
- Having prices only for members
- Being across sea from the selected location.

All those filters can be disabled on the preferences to see the full list.

Also sometimes gas stations sent bad information,
so if they are not in the unfiltered list, you could tip them.


## Why does Carburoid list so few gas stations?

You migth be lucky to be close to a really cheap gas station.
It could be the selected product not being popular.

The expected result for a common product like "Gasoleo A"
is a list of between 10 and 20 gas stations.

## Are you charging for the app?

We may charge for downloads from **proprietary platforms**
like the Apple App Store or Google Play if we ever publish Carburoid there.
It will always be free if you install it via F-Droid, GitHub, or any other
open-source repository.

You’d be paying for the convenience of not having to install F-Droid first.

Remember: in FLOSS, “free” refers to freedom, not price.
Developing and maintaining software has real costs,
and authors are entitled to charge for copies—
but never to restrict what you do with them, as long as you comply with the license.

You can also make **donations** to either Carburoid or F-Droid.
If you’re saving money with Carburoid, it’s only fair
to share a portion of those savings to help us keep going.
Don’t you think so?

## What should I do if the prices don’t match what’s shown in the app?

Fuel prices are updated by the Ministry every 30 minutes.
First, make sure you have the latest data by **pulling down to refresh** the station list.

If the price still appears incorrect after refreshing,
you can report the discrepancy directly to Spain’s Ministry of Industry
using their official form:

[Report inaccurate fuel prices](https://geoportalgasolineras.es/geoportal-instalaciones/IncoherenciaPrecios)

## Why Carburoid ignores shared locations from Google Maps? How can I get them? {#gmaps-opaque-links}

Google Maps has been descending into a privatization spiral.
Originally, they shared standard `geo` links,
Later, they switched to links to its website,
which were still parseable to extract coordinates.
However, recent versions (2025) they now share opaque links.
Their content cannot be inspected and may include private data beyond the coordinates.

At the cost of your privacy,
if you still want to share a Google Maps location,
the workaround is to share the link via a web browser first,
then share the resulting (parseable) URL from the browser with Carburoid.

## Can this app work for my country?

Right now, only **Spain and France** are supported.
The system is designed to be extensible up to some point.
Community contributions adding new countries are very welcome.

You can help by providing information about your country’s data sources,
contributing code, or improving the common code to support more cases.

To add support for a country, we need a **reliable public API** that provides, for every gas station:

- GPS position
- Name
- Brand
- Address (ideally split into street, province, and city)
- Prices by product

We also need:

- A mapping between **product names/IDs and standardized products** (currently based on Spanish ones)
- A way to **group stations by regions or landmasses** (e.g. Italy: Peninsula, Sicily, Sardinia)

Often APIs are documented in the native language,
so explaining us how the API works is also very helpful.

The current common code expects a **single API call returning JSON** with all stations in the country and prices for all the products.
If your data source fits there, you can follow existing implementations by looking at files with **“France/French”** or **“Spain/Spanish”** in their names.

If the API requires multiple queries, does not return JSON, or lacks some of the required data, the common code will need to be extended.
In that case, it’s best to **contact the maintainers first** to agree on the approach before starting implementation.

## Can you change the license to MIT or something similar?

No. Carburoid’s license is a deliberate choice.
GNU AGPLv3+ grants strong rights to end users
and prevents leaches from taking our open work,
and profiting from it while stripping away those freedoms granted to users.
