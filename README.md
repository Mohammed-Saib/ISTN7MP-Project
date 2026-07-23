# Academic Weapon - Student Productivity App

A comprehensive Android productivity application designed for university students to manage their academic life. Built with Java using Clean Architecture and MVVM pattern.

**Developed by:** Aisha Abba Omar, Firdous Khan, and Mohammed Saib as part of the ISTN7MP module.

## Project History

Academic Weapon was originally developed as a university module project with full cloud integration. The app used **Firebase Authentication** for user accounts, **Firebase Firestore** for cloud metadata synchronisation across devices, and **Firebase Storage** for uploaded files (module notes, personal attachments, and research papers). The architecture was designed as an offline-first hybrid system where Room served as the local cache and Firestore as the cloud source of truth.

Following the end of the module, the Firebase and Google Cloud services backing this project were disabled. To ensure the app remains functional, all cloud dependencies have been removed and the application has been converted to run **entirely locally** on-device. Key changes made during this cleanup include:

- **Firebase Auth** replaced with local Room-based authentication (passwords hashed with SHA-256, session managed via SharedPreferences)
- **Firebase Firestore** removed from all 10 repository implementations — data operations now go directly to Room
- **Firebase Storage** replaced with Android internal file storage (`context.getFilesDir()`)
- **All cloud sync logic** (`syncToFirestore` / `syncFromFirestore` methods) stripped from every repository
- **Google Services plugin** and `google-services.json` deleted
- All Firebase dependencies removed from Gradle build files
- `INTERNET` permission removed from the manifest

The app now compiles and runs with zero external service dependencies.

## Features

### Module Management
- Create, edit, and archive university modules
- Track module details: lecturer info, office hours, module codes
- Colour-code modules for easy identification
- Semester-based organisation

### Assessment Tracking
- Add assessments to modules with weighting, due dates, and scores
- Track score modes (points-based or percentage)
- Link assessments to calendar events automatically
- Supports multiple assessment types per module

### Calendar
- Full calendar view with monthly grid
- Create, edit, and delete events
- Recurring events (daily, weekly, monthly, yearly)
- Events linked to modules and assessments
- Push events to the device calendar

### Task Management (To-Do)
- Create tasks with priority levels (High, Medium, Low)
- Due date tracking with overdue highlighting
- Recurring tasks with automatic next-occurrence scheduling
- Filter by priority, completion status, or module

### Notes
- Module-specific file notes (PDF, images, documents)
- Personal notes with rich text content
- Folder organisation for personal notes
- Note attachments with local file storage
- Pin important notes

### Research Papers
- Track research papers with metadata (authors, year, category)
- Reading progress tracking (Unread, Reading, Read)
- Important paper flagging
- Full-text search across title, authors, category, and summary
- Local file storage for paper documents

### Pomodoro Timer
- Customisable study timer with work/break intervals
- Session tracking
- Notification reminders for study sessions
- Recommended tasks based on priority and due date

### Additional Features
- AI Chatbot for quick queries
- Dark mode / Light mode theme support
- Local notification system with daily study reminders
- Motivation reminders
- User registration and login (local authentication)

## Architecture

```
com.example.mpproject/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs (10 interfaces)
│   │   ├── entity/       # Room Entities (10 classes)
│   │   ├── AppDatabase   # Room database (version 9)
│   │   ├── DatabaseMigrations  # Schema migrations
│   │   ├── LocalSessionManager # User session via SharedPreferences
│   │   └── PomodoroPreferences # Timer settings
│   └── repository/       # Repository implementations (11 classes)
├── domain/
│   ├── model/            # Domain models (10 classes)
│   └── repository/       # Repository interfaces (11 interfaces)
└── presentation/
    ├── adapter/          # RecyclerView adapters (10 classes)
    ├── model/            # UI state models
    ├── notification/     # Notification helpers & schedulers
    ├── view/             # Fragments & views
    │   ├── auth/         # Login & Register fragments
    │   ├── calendar/     # Calendar day items & event sheets
    │   ├── modules/      # Module detail & bottom sheets
    │   └── todo/         # Todo bottom sheet
    └── viewmodel/        # ViewModels & Factories (20 classes)
```

### Design Patterns
- **Clean Architecture**: Clear separation between data, domain, and presentation layers
- **MVVM**: ViewModels expose LiveData to fragments via observable patterns
- **Repository Pattern**: Each data entity has a repository interface (domain) and implementation (data)
- **Single-Activity**: Jetpack Navigation Component with one MainActivity hosting all fragments

## Technical Details

| Property | Value |
|----------|-------|
| Language | Java 11 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 |
| Compile SDK | 36 |
| Database | Room (version 2.6.1) |
| Navigation | Jetpack Navigation 2.8.5 |
| Architecture | Clean Architecture + MVVM |
| DI | Manual (ViewModelFactory) |

### Key Libraries
- **Room** - Local SQLite database with compile-time query verification
- **LiveData + ViewModel** - Lifecycle-aware observable data holders
- **Navigation Component** - Single-activity navigation with bottom nav
- **Material Design 3** - UI components (BottomSheet, Chips, TextInputLayout, etc.)
- **View Binding** - Type-safe view references

## Data Storage

All data is stored locally on the device:
- **Room Database** (`notesapp_database`): All structured data (modules, assessments, tasks, calendar events, notes, user profiles)
- **SharedPreferences** (`app_prefs`): User session, app settings (dark mode, remember me, Pomodoro settings)
- **Internal Storage**: Uploaded files (module notes, personal attachments, research papers)

### Database Schema (v9)
| Table | Description |
|-------|-------------|
| `users` | User profiles with hashed passwords |
| `modules` | University modules |
| `module_notes` | File-based notes attached to modules |
| `personal_notes` | Free-form personal notes |
| `personal_note_attachments` | Files attached to personal notes |
| `todos` | Tasks and to-do items |
| `calendar_events` | Calendar events with recurrence support |
| `assessments` | Module assessments and grades |
| `note_folders` | Folders for organising personal notes |
| `research_papers` | Research paper metadata and tracking |

## Building

1. Clone the repository
2. Open in Android Studio (Ladybug or later)
3. Sync Gradle files
4. Run on an emulator or device (API 26+)

No API keys or external service configuration required - the app runs entirely offline.

## Possible Improvements

### Security
- **Encrypted SharedPreferences** for sensitive data (session tokens, passwords) using AndroidX Security library
- **Biometric authentication** (fingerprint/face unlock) as an alternative to password login
- **Encrypted database** using Room's `SupportFactory` with SQLCipher

### Data & Storage
- **Backup and restore** via Android's built-in backup or export/import to local file
- **Cloud sync option** (re-integrate Firebase or use an alternative like Supabase) for users who want multi-device access
- **Data export** to CSV or PDF for module summaries, assessment reports, or task lists

### User Experience
- **Widgets** for home screen — quick view of today's tasks, upcoming deadlines, or timer controls
- **Tablet / landscape layouts** for larger screens
- **Search across all features** — a unified global search that covers modules, notes, tasks, events, and research papers
- **Drag-and-drop reordering** for tasks and notes
- **Batch operations** — select and delete/archive multiple items at once

### Productivity Features
- **Grade calculator** — predict required marks to hit target grades based on assessment weightings
- **Study analytics** — track time spent in Pomodoro sessions, tasks completed per day, and module study distribution
- **Dashboard customisation** — let users choose which widgets appear on the home screen and in what order
- **Smart task scheduling** — suggest optimal study times based on due dates and available time slots
- **Deadline warnings** — proactive alerts when assessment deadlines are approaching or when workload is unusually high

### Notes & Files
- **Markdown support** in personal notes for formatted text, lists, and code blocks
- **Handwritten notes / drawing** support using a canvas view
- **Document scanning** using the device camera with ML Kit text recognition
- **Tagging system** for notes — cross-cutting labels independent of folders and modules

### Notifications & Reminders
- **Customisable notification schedules** — let users set quiet hours and preferred reminder times
- **Location-based reminders** — trigger reminders when arriving at a specific location (e.g., campus)
- **Notification grouping** — bundle multiple upcoming deadlines into a single summary notification

### Testing & Quality
- **Unit tests** for ViewModels, repositories, and DAOs using JUnit and Room testing
- **UI tests** with Espresso for critical user flows (login, create task, add event)
- **CI/CD pipeline** using GitHub Actions to run tests on every push
- **Code coverage reporting** with JaCoCo

### Accessibility
- **Screen reader optimisation** — ensure all interactive elements have proper content descriptions
- **Larger text support** — respect system font scale across all screens
- **High contrast mode** for users with visual impairments
- **Keyboard navigation** support for external keyboards and switch controls

### Architecture & Code Quality
- **Dependency injection** with Hilt/Dagger instead of manual ViewModelFactory construction
- **Kotlin migration** — convert from Java to Kotlin for null safety, coroutines, and modern Android idioms
- **Kotlin Coroutines** for async operations instead of ExecutorService threads
- **Modularisation** — split into feature modules (`:feature:calendar`, `:feature:notes`, etc.) for faster builds and better separation
- **Code coverage** targets and linting rules enforced in CI

## Project Structure

```
Academic Weapon/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/mpproject/
│       └── res/
├── build.gradle
├── gradle/
│   └── libs.versions.toml
├── gradle.properties
├── settings.gradle
└── README.md
```

## Contributors

### Aisha Abba Omar

**Primary areas:** Pomodoro Timer, To-Do List, UI Development, Database Design, Documentation, Testing

Aisha was responsible for building the Pomodoro timer feature and the task management (to-do) system, two of the core productivity tools in the app. She also contributed to the overall UI design, database schema design, documentation, and testing.

**Pomodoro Timer** (`presentation/view/TimerFragment.java`, `presentation/view/TimerSettingFragment.java`, `presentation/viewmodel/PomodoroViewModel.java`, `data/repository/PomodoroRepositoryImpl.java`, `data/local/PomodoroPreferences.java`, `data/local/entity/PomodoroEntity.java`, `data/local/dao/PomodoroDao.java`, `domain/repository/PomodoroRepository.java`, `domain/model/PomodoroSettings.java`, `presentation/adapter/SessionTaskAdapter.java`, `presentation/model/PomodoroUiState.java`):
- Implemented the full Pomodoro timer with configurable focus duration, short break, long break, and sessions before long break
- Built the timer mode switching logic (focus → short break → focus → long break cycle)
- Created the session management system that lets users select tasks to work on during timer sessions
- Designed the visual timer UI with animated progress indicators and themed backgrounds (default, forest, ocean, sunset themes)
- Implemented `PomodoroPreferences` for persisting timer settings using SharedPreferences
- Built the timer settings bottom sheet for customising durations and themes
- Implemented recommended tasks panel that surfaces high-priority and upcoming tasks

**To-Do List** (`presentation/view/TasksFragment.java`, `presentation/view/todo/TodoBottomSheetFragment.java`, `presentation/viewmodel/TaskViewModel.java`, `presentation/viewmodel/TaskViewModelFactory.java`, `data/repository/TodoRepositoryImpl.java`, `data/local/entity/TodoEntity.java`, `data/local/dao/TodoDao.java`, `domain/repository/TodoRepository.java`, `domain/model/Todo.java`, `presentation/adapter/TaskAdapter.java`, `presentation/adapter/MyTaskAdapter.java`):
- Designed and implemented the task creation bottom sheet with title, description, priority, due date, module linking, and recurrence options
- Built the task filtering system (All, High Priority, Medium, Low, Completed) with search functionality
- Implemented `TaskViewModel` with filtered task lists, task completion toggling, and deletion
- Created the recurring task system that automatically generates the next occurrence when a task is completed
- Designed task adapters for displaying tasks with priority indicators and due date formatting

**Database Design:**
- Co-designed the database schema with the team, contributing to the table structure for todos, calendar events, and the Pomodoro preferences
- Participated in defining entity relationships and DAO query patterns

**UI Development:**
- Contributed to the overall app UI design and layout consistency

**Documentation & Testing:**
- Contributed to project documentation
- Participated in testing across the app's features

---

### Firdous Khan

**Primary areas:** Notes System, UI/UX Design, Research Paper Organiser, File Uploads & Management, Database Design, Documentation, Testing

Firdous was responsible for the entire notes system (module notes, personal notes, folders, and attachments), the research paper organiser, and the file upload/management infrastructure. She also led UI/UX design decisions and contributed to database design, documentation, and testing.

**Notes System** (`presentation/view/NotesFragment.java`, `presentation/viewmodel/NotesViewModel.java`, `data/repository/ModuleNoteRepositoryImpl.java`, `data/repository/PersonalNoteRepositoryImpl.java`, `data/repository/PersonalNoteAttachmentRepositoryImpl.java`, `data/repository/NoteFolderRepositoryImpl.java`, `data/local/entity/ModuleNoteEntity.java`, `data/local/entity/PersonalNoteEntity.java`, `data/local/entity/PersonalNoteAttachmentEntity.java`, `data/local/entity/NoteFolderEntity.java`, `data/local/dao/ModuleNoteDao.java`, `data/local/dao/PersonalNoteDao.java`, `data/local/dao/PersonalNoteAttachmentDao.java`, `data/local/dao/NoteFolderDao.java`, `domain/repository/ModuleNoteRepository.java`, `domain/repository/PersonalNoteRepository.java`, `domain/repository/PersonalNoteAttachmentRepository.java`, `domain/repository/NoteFolderRepository.java`, `domain/model/ModuleNote.java`, `domain/model/PersonalNote.java`, `domain/model/PersonalNoteAttachment.java`, `domain/model/NoteFolder.java`, `presentation/adapter/NotesAdapter.java`, `presentation/model/NoteListItem.java`):
- Built the entire notes system from the ground up, including three distinct note types: module file notes, personal notes, and note attachments
- Implemented folder organisation for personal notes, allowing users to create, rename, and delete folders to categorise their notes
- Designed the unified notes list that merges all note types into a single scrollable view with type indicators and search
- Built the module filter system so users can view notes for a specific module
- Implemented note pinning to keep important notes at the top of the list
- Created the search functionality across all note types (title, content, filename)
- Designed all four note-related DAOs with complex queries including joins, aggregations, and full-text search patterns

**Research Paper Organiser** (`presentation/view/ResearchFragment.java`, `presentation/viewmodel/ResearchViewModel.java`, `data/repository/ResearchPaperRepositoryImpl.java`, `data/local/entity/ResearchPaperEntity.java`, `data/local/dao/ResearchPaperDao.java`, `domain/repository/ResearchPaperRepository.java`, `presentation/adapter/ResearchPaperAdapter.java`):
- Designed and implemented the research paper tracking system
- Built the paper upload flow with metadata entry (title, authors, year, category, summary, key findings, methodology, relevance)
- Implemented reading progress tracking (Unread → Reading → Read) with status badges
- Created the important paper flagging system
- Built the search functionality across all paper fields
- Designed the paper list adapter with visual indicators for status and importance

**File Upload & Management Infrastructure:**
- Designed the file upload pipeline that copies files from the system file picker to the app's internal storage (`context.getFilesDir()`)
- Implemented file type detection and handling for PDFs, images (JPG, PNG, WEBP), and documents (DOC, DOCX, PPT, PPTX)
- Built the in-app file viewer using WebView for documents and ImageView for images
- Created the file metadata tracking system (filename, type, size, path, upload date)
- Implemented the PDF viewer integration using ACTION_VIEW intents

**UI/UX Design:**
- Led UI/UX design decisions across the app, establishing visual patterns for cards, bottom sheets, and navigation
- Designed the visual language for note type indicators, folder icons, and status badges
- Contributed to the overall layout and user flow of the application

**Database Design:**
- Co-designed the database schema, contributing to the structure of note-related tables and the research papers table
- Participated in defining entity relationships and query patterns for the notes system

**Documentation & Testing:**
- Contributed to project documentation and README content
- Participated in testing across the app's features, particularly the notes and file management systems

---

### Mohammed Saib

**Primary areas:** Project Setup, Data Layer Design, Database Implementation, Firebase Setup, Calendar, Modules, Assessment Tracker, End-of-Life Conversion, Documentation, Testing

Mohammed was responsible for the foundational project setup, the entire data layer (Room database, all entities, DAOs, and repository implementations), the calendar system, module management, assessment tracker, and the Firebase-to-local conversion. He also contributed to project documentation and testing.

**Project Setup:**
- Configured the Android project with Gradle build system, dependency management via version catalog (`gradle/libs.versions.toml`)
- Set up the project architecture with Clean Architecture + MVVM pattern
- Configured compileSdk 36, minSdk 26, targetSdk 36
- Established the single-activity architecture with Jetpack Navigation Component
- Set up view binding, Material Design 3, and all project dependencies

**Data Layer Design & Database Implementation** (`data/local/AppDatabase.java`, `data/local/DatabaseMigrations.java`, `data/local/entity/UserEntity.java`, `data/local/entity/ModuleEntity.java`, `data/local/entity/CalendarEventEntity.java`, `data/local/entity/AssessmentEntity.java`, `data/local/dao/UserDao.java`, `data/local/dao/ModuleDao.java`, `data/local/dao/CalendarEventDao.java`, `data/local/dao/AssessmentDao.java`):
- Implemented the Room database (`notesapp_database`) with 10 tables
- Created all 10 entity classes defining the database schema
- Implemented all 10 DAO interfaces with compile-time verified SQL queries
- Built `DatabaseMigrations` with versioned migrations (v1→v2 through v8→v9) to handle schema evolution without data loss
- Designed the user authentication entity with password hashing support
- Implemented the background thread pool (`databaseWriteExecutor`) for non-blocking database operations
- Created all domain models and repository interfaces for the Clean Architecture data flow

**Firebase Setup (Pre-Conversion):**
- Initially configured Firebase Authentication, Firestore, and Storage
- Designed the cloud sync architecture with offline-first hybrid pattern
- Set up Firestore collection paths and document structures
- Configured Firebase Storage for file uploads

**Calendar System** (`presentation/view/CalendarFragment.java`, `presentation/view/calendar/CalendarDayItem.java`, `presentation/view/calendar/AgendaItem.java`, `presentation/view/calendar/EventBottomSheetFragment.java`, `presentation/viewmodel/CalendarViewModel.java`, `presentation/viewmodel/CalendarViewModelFactory.java`, `data/repository/CalendarEventRepositoryImpl.java`, `data/local/entity/CalendarEventEntity.java`, `data/local/dao/CalendarEventDao.java`, `domain/repository/CalendarEventRepository.java`, `domain/model/CalendarEvent.java`, `presentation/adapter/CalendarGridAdapter.java`, `presentation/adapter/AgendaAdapter.java`):
- Built the full calendar view with monthly grid rendering and day selection
- Implemented event creation, editing, and deletion via bottom sheets
- Designed the recurring event system with four recurrence patterns (daily, weekly, monthly, yearly)
- Built the agenda section showing upcoming events and tasks
- Implemented event-module linking for academic event tracking

**Module Management** (`presentation/view/ModulesFragment.java`, `presentation/view/modules/ModuleDetailFragment.java`, `presentation/view/modules/ModuleBottomSheetFragment.java`, `presentation/viewmodel/ModuleViewModel.java`, `presentation/viewmodel/ModuleViewModelFactory.java`, `data/repository/ModuleRepositoryImpl.java`, `data/local/entity/ModuleEntity.java`, `data/local/dao/ModuleDao.java`, `domain/repository/ModuleRepository.java`, `domain/model/Module.java`, `presentation/adapter/ModuleAdapter.java`):
- Implemented the modules list with active/archived tabs
- Built module creation and editing bottom sheets with colour coding
- Designed module detail screen showing assessments, stats, and linked notes
- Implemented module archiving and deletion with confirmation dialogs
- Created module-adaptive UI elements (coloured headers, accent colours)

**Assessment Tracker** (`presentation/view/modules/AssessmentBottomSheetFragment.java`, `presentation/viewmodel/AssessmentViewModel.java`, `presentation/viewmodel/AssessmentViewModelFactory.java`, `data/repository/AssessmentRepositoryImpl.java`, `data/local/entity/AssessmentEntity.java`, `data/local/dao/AssessmentDao.java`, `domain/repository/AssessmentRepository.java`, `domain/model/Assessment.java`, `presentation/adapter/AssessmentAdapter.java`):
- Built the assessment creation and editing system linked to modules
- Implemented weighting validation (total must equal 100%)
- Designed score tracking with points-based and percentage modes
- Created assessment cards with colour-coded pass/fail indicators
- Implemented assessment summary statistics (total weighting, average score)

**End-of-Life Conversion (Firebase Removal):**
- Led the removal of all Firebase/Google Cloud dependencies from the codebase
- Replaced Firebase Auth with local Room-based authentication using SHA-256 password hashing
- Stripped Firestore sync logic from all 10 repository implementations
- Replaced Firebase Storage with Android internal file storage
- Removed Google Services plugin and `google-services.json`
- Created `LocalSessionManager` for SharedPreferences-based session management
- Bumped database version from 8 to 9 with destructive migration
- Removed `INTERNET` permission from AndroidManifest.xml
- Verified the build compiles successfully with zero errors

**Core App Infrastructure:**
- Implemented `MainActivity` with bottom navigation, session checking, and fragment management
- Built the custom tab selection logic for complex navigation flows (Modules → Module Details → Notes)
- Implemented the Home Fragment dashboard with module preview, today's tasks, and upcoming events
- Created the `AuthViewModel` and `SettingsFragment` for profile management
- Built the notification system (`NotificationHelper`, `ReminderScheduler`, `NotificationReceiver`)
- Implemented the AI chatbot bottom sheet with Botpress WebView integration

**Documentation & Testing:**
- Created the comprehensive README.md with project history, architecture, features, and technical details
- Documented the Firebase-to-local conversion process
- Contributed to testing across all app features
