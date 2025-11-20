# MedSupplyDrones – Informatics Large Practical 2025

This repository contains my implementation for the **Informatics Large Practical (ILP)** at the **School of Informatics, University of Edinburgh (2025)**.

The coursework centres on designing and implementing a **REST-based microservice** for a drone-based medication delivery system, including **route planning**, **drone allocation**, and **JSON-based communication**, all running inside **Docker**.

---

## Course & Project Overview

**Informatics Large Practical (ILP)** is a **20-credit**, Level 9, **individual** software engineering course for **3rd-year Informatics students**.

The project simulates a system called **MedSupplyDrones**, whose purpose is to:

- Deliver prepared medication orders via drones from **pharmacy dispatch points (pick-up)**  
  to **patients (drop-off)**.
- Work within constraints of:
    - Drone capabilities (capacity, cooling/heating, max moves, cost per move, etc.)
    - Limited battery/flight distance.
    - **Restricted** and **no-fly zones**.
- Run as a **REST microservice** inside a **Docker container**, consuming data from a central **ILP REST service**.

There is **no written exam** for ILP; the final mark is based entirely on **coursework and an oral examination**.

---

## Domain Model & Problem Setting

### High-Level Scenario

We assume an emergency medication delivery system where:

- Drones start at **fixed service points**.
- Each **flight**:
    1. Starts at a **central service point**.
    2. Flies to a **pick-up** location (pharmacy/dispatch).
    3. Visits one or more **drop-off** locations.
    4. Returns to the **same service point**.
- Drones may fail or become unavailable during the day; **availability must be checked** before using any drone.
- Orders are assumed **valid**, but not always **feasible** if no suitable drone exists (e.g. no cooling capability).

The **main challenge** is to:

- Select appropriate drones.
- Group and assign orders efficiently (minimising drones, flights, distance, and cost).
- Compute feasible 2D flight paths that respect constraints.

---

## Coordinates, Distances & Movement

### Coordinate System

- Locations use **longitude/latitude** in degrees:
    - Longitude ≈ −3 (Edinburgh, West of prime meridian)
    - Latitude ≈ +56 (Edinburgh, North of equator)
- For the purposes of ILP:
    - The Earth is treated as a **plane**, not a sphere.
    - Distances use the **Pythagorean distance**:
      \[
      d = \sqrt{(x_1 - x_2)^2 + (y_1 - y_2)^2}
      \]
- **Tolerance / “hit” test**:
    - A location ℓ₁ is “close” to ℓ₂ if distance < **0.00015°**.
    - This tolerance is crucial for **auto-marking** and must be used for hit detection.

### Drone Movement Rules

- Moves are of two types:
    - **Horizontal**: changes longitude/latitude.
    - **Vertical**: changes altitude only (altitude is not considered in path planning; energy use is identical).
- Every **horizontal move** is a straight line of length **0.00015°** (±1e−12 tolerance due to floating point).
- Drones can only move in **16 compass directions**:
    - Primary: N, E, S, W
    - Secondary: NE, NW, SE, SW
    - Tertiary: NNE, ENE, etc., spaced by **22.5°**.
- Angle convention:
    - `0`   → East
    - `90`  → North
    - `180` → West
    - `270` → South
    - Others are intermediate.

---

## No-Fly Zones, Restricted Areas & Costs

### Restricted / No-Fly Zones

- Zones are always **rectangular polygons**, defined by 4+ vertices (last point closes the polygon).
- You **must not**:
    - Enter a no-fly zone.
    - Fly across corners of a restricted area.
- Some zones have **limits** (e.g. altitude bands); if `limits` is missing, the zone is treated as a **no-fly zone** (fully forbidden).

### Flight Cost Model

Each drone has:

- `costPerMove`
- `costInitial` (take-off)
- `costFinal` (landing)

Total flight cost:

```text
Total cost = costInitial + costFinal + (numberOfMoves * costPerMove)
