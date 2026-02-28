# Dynamic Gas Station Sorting

## Purpose

This document compiles design criteria for acommodating current 2026-02 sorting algorithm
to having road path versus having just the geodesical distance.

## Current algorithm

- **Compute geodesical distances** to the origin point for every stations (O(n))
- **Sort** stations by geodesical distance O(nlog(n))
- In that order **discard** any station more expensive than a previous one (O(n))

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

Problem: geodesical distance here are now not that good approximation.
It can diverge a lot with gas station between both A and B.
so we urge for the need to consider road distances.

## Introducing road distance

- We want to consider road distances, not geodesical, which takes longer to compute and it is not advisable
to compute them for each one of, for instance, the 9k stations in Spain

## Oportunities

- Geodesical distance can still be used as lower bound for the road distance.
    - This means that if we have a road distance,
        we can consider that every station with a longer geodesical distance it will also have a longer road distance.
- If the origin A change (because the device moves) we can keep the S -> B distance
- Because every station share A and B, an algorithm could reuse path explored in other stations from A or from B

## Requirements and constraints







