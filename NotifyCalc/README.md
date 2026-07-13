# NotifyCalc

A production-quality Android application combining two fully independent features:

1. **Calculator** — a modern Material Design 3 calculator (addition, subtraction, multiplication, division, percentage, negative numbers, decimals, delete, clear, live result preview, calculation history, portrait & landscape, light & dark theme).
2. **Notification Backup (optional)** — an opt-in feature built on Android's official `NotificationListenerService`. Newly delivered notifications are converted into `NotificationData` snapshots and handed to `NotificationRepository.uploadNotification(...)`, a placeholder designed so cloud synchronization can be added later. Notifications are **never** stored locally, and the calculator works fully whether the feature is enabled or not.

## Tech stack

- Kotlin, MVVM, Repository pattern, `StateFlow`, coroutines, lifecycle components
- Material Design 3 (Material Components 1.12, dynamic color on Android 12+)
- Gradle Kotlin DSL, AGP 8.7, Gradle 8.9
- `minSdk` 29 (Android 10), `targetSdk`/`compileSdk` 35

## Project structure

```
app/src/main/java/com/notifycalc/app/
├── NotifyCalcApp.kt                     # Application class (dynamic colors)
├── data/
│   ├── model/        NotificationData, HistoryEntry
│   ├── preferences/  AppPreferences (welcome flag, opt-in, history)
│   └── repository/   NotificationRepository (cloud-ready placeholder)
├── domain/           CalculatorEngine (BigDecimal expression evaluator)
├── service/          NotificationBackupService (NotificationListenerService)
├── util/             NotificationAccessUtil
└── ui/
    ├── welcome/      WelcomeActivity (first launch only)
    ├── main/         MainActivity (calculator)
    ├── calculator/   CalculatorViewModel + UI state
    ├── history/      HistoryAdapter
    └── settings/     SettingsActivity + SettingsViewModel
```

## Building

Open the `NotifyCalc` folder in Android Studio (latest stable), let Gradle sync, and run the `app` configuration — or build from the command line:

```
./gradlew assembleDebug
```

## Permissions

The app requests **no** runtime permissions. Notification Backup relies solely on the system *Notification access* setting, which the user grants manually; the listener service is protected by the platform `BIND_NOTIFICATION_LISTENER_SERVICE` permission.
