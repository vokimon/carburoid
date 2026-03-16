# Dynamic Gas Station Sorting

## Purpose

This document compiles design criteria for acommodating current 2026-02 sorting algorithm
to use road distances versus geodesical (haversine) distances.

## Current algorithm

- **Compute geodesical distances** to the origin point for every stations (O(n))
- **Sort** stations by geodesical distance O(nlog(n))
- In that order **discard** any station more expensive than a previous one (O(n))

This algorithm implements Pareto optimal stations to minimize distance and price.

We have to repeat the process whenever:

- The station list is updated
    - compute distance - sort by distance - filter by price
- The product changes (and thus prices)
    - sort by distance - filter by price
    - Ideally just: filter by price
- The origin point is changed
    - compute distance - sort by distance - filter by price

## Introducing trip deviation adaptation

- Now users can define points origin and destiny (A, B).
- In that scenario, we consider as distance the deviation from the straight line route (A -> S + S -> B - A -> B)
- The algorithm here can be kept the same, but considering the deviation as distance
- Changes to origin A and target B can trigger a distance recomputation

This has been already implemented.

Problem: geodesical distances are not as good approximation for trip deviation as it was for plain distance to the station A -> S.
For plain distance to the station, heaviside and road distances tend to be covariant:
a further heaviside, often is also a further road distance.
But with route deviations, the algorithm considers 0 deviations stations stations that might be quite far from the actual road,
hiding stations besides the road that users might be interested in.

So introducing trip deviation urges to properly solve the road distance as well.

## Introducing road distance

We want to consider road distances, not geodesical, which takes longer to compute and it is not advisable
to compute them for each one of, for instance, the 9k stations in Spain.
So we want to design an algorithm for partial computation:

- Temporaly rely on heaviside distance while we compute the road distance
- Criteria to avoid computing as many road distances as possible, since they take more time
- An algorithm to progressively incorporate those distances to get the relevant station list

## Analysis

### Implementations

How to obtain the road distance:

- Graphhopper
    - Enables offline use
    - Blobs ~1Gb per country
    - Hellish configuration (at least i was unable to setup data and use it from kotlin)
- OSRM
    - Online
    - Simple API
    - Entry point for table origin-destination to make block

### Oportunities

- Geodesical distance is faster to compute and can be used as lower bound for the road distance.
    - Every station with a longer geodesical distance it will also have a longer road distance.
- If the origin A changes (because the device moves) we can keep the S -> B distance
- Because every station share A and B, an algorithm could reuse path explored in other stations from A or from B
- We can have the same list result inverting attributes:
    - **Sorting by price ascending**  (instead of distance)
    - **Filtering by distance**: From cheapest to most expensive, if a previous station is further than previous ones remove it
    - **Invert** the resulting list
- If prices are stable, this attribution inversion enables keeping the sorting O(nlogn) and just apply the filtering O(n)
- Also sets an order of computation for distances so that many distances computations could be avoided

### Requirements

- Hide any gas station if any other exist that is better in both distance and price
- Gas Stations should appear ordered by distance (or deviation distance) and inversely ordered by price
- User may set and change origin and target position, target is optional
- User may change the product (and thus the prices)
- User may update gas station list from the api, the result arrives asynchronously
- Road distance is computed and updated asyncronously
- Geodesical distance is used until road distance is available

### Challenges

- Many more stations are now at the same distance, how to sort them?
    - Sort secondarily by distance to A
        - How to display both in a compact and userfriendly way
        - What deviation difference to have to start taking A -> S distance into account?
 
## Design

### Events

- New data loaded
- Destiny B changed
- Origin A changed
- Prices get updated (product changes)
- Road distance of some station is  async computed

### Cacheable computations

- Price order (or distance order)
- Station road distance to A
- Station road distance to B
- Station geodesical distance to A
- Station geodesical distance to B
- Distance A to B
- Filtering by distance

### When new api arrives

- Reset stations road and geodesic distance cache (set to null)
- Compute A-B geodesic
- Launch A-B road computation
- Sort by price
- From cheapest:
    - Compute geodesic distances to A
    - If B geodesic defined compute distance

### A new road distance arrives:

- The road distance will be (equal or) greater than the previous geodesic distance
- If stations are sorted by price, the order is the same
    - Caution: Prices are quantized, there are many stations with the same price
- Cheaper stations are not affected same or greater price are


## Simplification ignore origin (A) and destination (B) independence

Simplification: Consider A-B? points a single unity, not partial A or B changes.

- Compute crow distance for all stations
- Sort stations by distance
- Filter stations (by price and other criteria)
- If all resulting stations has road station end
- Compute road distances for remaing stations in result
- Go back to "Sort stations by distance"

Entry points:

- New full data from API: from distances
- Route changed: from distances
- Road distances arrived: from Sort, reuse existing distances
- New config: From filter, reuse sorting
- Product changed: From filter, reuse sorting



