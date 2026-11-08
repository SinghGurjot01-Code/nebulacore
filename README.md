# Nebula for Android

Native Android frontend for **Nebula**, a cloud gaming platform. This is phase 1: the complete app UI and every
interaction that can work without a backend. Supabase and real cloud streaming come in later phases.

> **Build status:** the project was written without access to the Android SDK or Gradle, so it has **not been compiled
> yet**. The GitHub Actions workflow in `.github/workflows/android.yml` is the first real build. Expect to fix a small
> number of compile errors on the first run, then it should stay green.

## What works today (completed)

| Area | What you can do |
|---|---|
| Auth | Splash, welcome, register, log in, **Continue with Google**, forgot and reset password, email verification state, log out, persistent login. Local demo, or Supabase when configured. |
| Home | Greeting, notification badge, session status, upcoming booking, featured games, recently played, announcements. |
| Games | 55-game demo catalogue with artwork, search, category filters, favourites, game details. |
| Bookings | Choose a plan, date, time slot and optional game, review, confirm, view upcoming and history, cancel (1 hour notice). Simulated. |
| Support | Create tickets, list with status, ticket conversation, add replies. Simulated. |
| Profile | Edit name, favourites, gaming history, notification preferences and inbox, app info, privacy and terms placeholders. |
| Launch flow | Game launch screen, pre-session checks (real network check), unavailable/error state, return to Nebula. |

## What is planned (not done)

- Push notifications, a staff app for support replies.
- Moonlight streaming client, Sunshine-compatible hosts and machine assignment.
- Dark mode (light only by design in this release), wallet and admin features (out of scope).

In local demo mode, simulated items are labelled. With Supabase on, nothing claims a stream exists: cloud streaming is not connected.

## Technology

Kotlin, Jetpack Compose, Material 3 (customised), MVVM, Coroutines and Flow, Navigation Compose, DataStore
(Preferences), Coil, AndroidX core-splashscreen. Gradle Kotlin DSL, AGP 8.7.3, Kotlin 2.0.21, minSdk 26, target 35.
No Hilt or Room: a small manual container is enough, and Room is not needed yet.

## Project structure

```
app/src/main/java/com/nebula/app/
  MainActivity.kt, NebulaApp.kt       entry point, scaffold, bottom bar
  di/AppContainer.kt                  wires repository implementations
  core/common|navigation              Loadable state, ViewModel helper, formatting, routes, NavHost
  design/theme|components             colours, type, shapes, reusable components
  domain/models|repositories          models, repository interfaces, validation, game filtering
  data/local|repositories             demo data, DataStore wrapper, Local* repository implementations
  features/auth|home|games|bookings|support|profile|streaming   screens and ViewModels
app/src/test/                         unit tests
```

UI talks only to ViewModels, ViewModels only to repository **interfaces** in `domain/repositories`.

## Android Studio setup

1. Install Android Studio (Ladybug or newer) with JDK 17+ and Android SDK 35.
2. Open this folder. Studio reads `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9) and syncs.
3. Optional: run `gradle wrapper` once to generate `gradlew` scripts.
4. Run the `app` configuration on a device or emulator (Android 8.0+).

## Build from the command line

```
gradle :app:assembleDebug            # APK: app/build/outputs/apk/debug/
gradle :app:testDebugUnitTest        # unit tests
```

GitHub Actions does the same on every push and uploads the debug APK as the `nebula-debug-apk` artifact. No signing
secrets are needed.

## Demo account and data

- Log in with `demo@nebula.app` / `Nebula123`, or register a new local account.
- Demo mode only: accounts and password hashes (SHA-256) live in DataStore, bookings and tickets in memory.
- Game titles and Steam app ids are used only to load public store artwork. Artwork needs internet access.
- Slot availability is generated from the date, not from a server.

## Backend: Supabase

The app uses the **Nebula** Supabase project when `nebula.supabaseUrl` and `nebula.supabaseAnonKey` are set in
`gradle.properties` (they are). Clear both values to run in local demo mode instead.

| Feature | Backend |
|---|---|
| Sign up, log in, log out, reset password, change name | Supabase Auth (email and password) |
| Continue with Google | Supabase Auth Google provider, PKCE flow in a Custom Tab, returns to `nebula://auth/callback` |
| Games, booking plans, announcements | Tables `games`, `booking_options`, `announcements` (public read) |
| Favourites | Table `favourites`, one row per user and game |
| Bookings | Table `bookings`, written only by the `create_booking` and `cancel_booking` functions |
| Slot availability | Function `get_booking_load` plus `app_settings.host_capacity` (3 concurrent sessions) |
| Support tickets and replies | Tables `support_tickets`, `ticket_messages`, written through `create_ticket` and RLS |
| Gaming history | Derived from completed bookings that have a game |

Every table has row level security. Users can only read their own favourites, bookings, tickets and messages.
The server rejects bookings in the past, more than 7 days ahead, overlapping your own bookings, or beyond host
capacity, and rejects cancellations with under 60 minutes notice.

Still local on the device: notification preferences and the sample notification inbox.

The anon key in `gradle.properties` is a public client key by design. Never add the service-role key to the app.
Tokens are kept in DataStore; move them to the Android Keystore before a Play Store release.

### Dashboard settings you must do once

1. Authentication, URL Configuration, Redirect URLs: add `nebula://auth/*`. Without it, confirmation and
   reset emails open the site URL instead of the app.
2. Optional while testing: turn off "Confirm email" so sign-up returns a session straight away.
3. Google sign-in: follow `GOOGLE_SIGN_IN_SETUP.md` (Google Cloud web client plus the Supabase Google provider).

### Support replies

Replies from the Nebula team are written in the Supabase table editor (insert into `ticket_messages` with
`author = 'support'`, and update the ticket `status`). There is no staff app yet.

## Future Moonlight integration

The boundary is `StreamingClient` and `StreamingRepository` in `domain/repositories`, with `StreamSession`
(host address and session token) in `domain/models/Streaming.kt`. `UnavailableStreamingClient` never streams.

1. Backend validates the user, assigns a machine and returns a `StreamSession`.
2. Integrate moonlight-android (GPLv3, check licence fit) as a module or library without modifying its streaming code,
   and implement `StreamingClient` on top of it.
3. When the client reports `StreamingState.Streaming`, `StreamingScreen` already switches to immersive landscape
   through `SessionDisplayMode` and restores portrait afterwards.
4. Sunshine-compatible hosts, pairing and networking are infrastructure work outside this app.

## Tests

`app/src/test` covers validation, game filtering, favourites, auth, booking creation, overlap and cancellation,
support tickets, the login, register and ticket ViewModels, and the Supabase parsing helpers. The Supabase
repositories themselves are not unit tested (they need a network).

## App icon

`Nebula_logo.svg` is converted to `res/drawable/nebula_logo.xml` (used in the app, tinted) and to the adaptive launcher icon: black logo on a white background, with a monochrome layer for Android 13+ themed icons.
