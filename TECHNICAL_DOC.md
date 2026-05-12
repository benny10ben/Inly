# Inly Technical Specification

Inly is a local-first personal knowledge management (PKM) system built with **Kotlin** and **Jetpack Compose**. It utilizes a modular, block-based architecture to provide a unified interface for notes, tasks, and structured data.

## 1. Core Architecture

The application follows the **MVVM** (Model-View-ViewModel) pattern and is built for robustness and privacy.

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose with Material 3.
* **Dependency Injection:** Hilt (Dagger).
* **Database:** Room (for note metadata and folder structures).
* **Storage:** JSON-based persistence for note content to support complex, nested block structures.
* **Visual Identity:** Glassmorphism effects implemented via the **Haze** library for toolbars and interaction pills.

## 2. The Block System (`NoteBlock`)

The heart of Inly is a polymorphic list of `NoteBlock` types. This design allows for a single `EditorScreen` to dynamically render diverse content types.

### Block Hierarchy
* **Textual:** `TextBlock`, `HeadingBlock` (H1, H2), `BulletedListBlock`, `NumberedListBlock`, `ToggleBlock`.
* **Actionable:** `CheckboxBlock` with integrated `reminderTimestamp`.
* **Structured:** `DatabaseBlock` supporting `ColumnType` definitions (Text, Number, Date, Checkbox, Formula).
* **Multimedia:** `ImageBlock`, `DocumentBlock`, `BookmarkBlock` (with metadata fetching), `VoiceBlock`.

## 3. Specialized Logic Engines

### Unified Editor Logic
A `BaseEditorViewModel` centralizes CRUD operations for blocks, indentation management, and formatting. This allows the `DailyEditorViewModel` and `StandaloneEditorViewModel` to share a robust core while implementing specialized features like date-based navigation or folder categorization.

### The Rollover System
The Daily Notes system implements an automated rollover logic. At the start of a new day, the system scans the previous day's JSON content, identifies unfinished `CheckboxBlock` items, and prepends them to the current day's block list, ensuring task continuity.

### Formula Engine
A lightweight expression evaluator is used within `DatabaseBlock` to process spreadsheet-like formulas (e.g., `prop("Quantity") * prop("Price")`), enabling structured data analysis without an external calculation engine.

## 4. Hardware & System Integration

### Reminders (`AlarmManager`)
Task notifications utilize the system `AlarmManager`.
* **Exact Alarms:** The `ReminderScheduler` includes a fallback mechanism for Android 12+ (API 31). If the `SCHEDULE_EXACT_ALARM` permission is denied, it degrades gracefully to inexact alarms to maintain app stability.
* **Broadcasts:** A `ReminderReceiver` handles the transition from system alarm to user-facing `NotificationChannel`.

### Media Handling
To maintain local-first integrity, `MediaStorageHelper` intercepts system URIs from file/image pickers, copies the binary data into the application's internal `filesDir`, and stores only the local relative path. This protects note content from broken links if the source file is moved.

## 5. Development Roadmap

I am are actively seeking contributions in the following areas:
* **End-to-End Encrypted Sync:** Implementation of a backend architecture that allows multi-device synchronization while maintaining zero-knowledge privacy.
* **Multi-platform Expansion:** Ports for desktop environments using Compose Multiplatform or similar technologies.
