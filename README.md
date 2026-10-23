# Food Expiry Tracker

Native Android app that tracks the food you buy and warns you before it expires —
built from scratch in **Kotlin** and **Jetpack Compose**.

Team project, 4 developers.

## Features

- **Inventory with expiry visibility** — products are grouped by location (fridge, freezer,
  pantry) and colour-coded by how close they are to expiring.
- **Barcode scanning** — add a product by pointing the camera at it; the barcode is resolved
  against the Open Food Facts API, bringing product name and details in automatically.
- **Shared households** — inventories are synchronised in real time between members of the
  same household, joined through invite codes.
- **Shopping list** — a separate list that survives across sessions and syncs with the household.
- **Recipe suggestions** — recipes derived from the products currently in the inventory.

## Tech stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Architecture | MVVM, single-activity Compose navigation |
| Backend | Firebase Authentication, Cloud Firestore |
| Hardware / APIs | Google ML Kit Barcode Scanning, Open Food Facts REST API, Retrofit |
| Tooling | Android Studio, Gradle Kotlin DSL, Git/GitHub, Trello |

## Architecture notes

- UI is fully declarative (Compose), with `ViewModel`s exposing state consumed by screens.
- Data layer is split into `data/` (Retrofit client and response models for Open Food Facts),
  `firebaseSync/` (authentication and Firestore access) and a `ProductDataStore` that the UI
  observes.
- Shared-household state is the interesting part: several users write to the same inventory,
  so concurrent reads and writes have to stay consistent when someone adds, consumes or
  deletes an item.

## Process

- Team of 4, weekly development cycles tracked in Trello.
- Pull-request based workflow with **peer code review**.
- User research: interviews with 6 target users to build personas, then low-fidelity and
  high-fidelity prototypes iterated against their feedback (e.g. slide-to-delete, traffic-light
  colour coding for expiry).

## Running this code

The repository intentionally omits `app/google-services.json`, which contains the Firebase
project configuration. To build locally:

1. Create a Firebase project and an Android app with package name `com.gal101.foodexpirytracker`.
2. Download `google-services.json` and place it in `app/`.
3. Enable Email/Password (and Google, if you want the login screen) in Firebase Authentication,
   and create a Cloud Firestore database.
4. `./gradlew assembleDebug`

Without that file Gradle will fail at the `google-services` plugin step — that is expected.
