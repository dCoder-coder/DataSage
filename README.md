# DataSage Android App

DataSage is a Kotlin/Jetpack Compose Android client for the RetailIQ platform. It is designed for **retail operations with unstable connectivity**, where billing must continue offline and sync later. The app uses a layered architecture with Hilt DI, Retrofit networking, Room local persistence, and WorkManager background sync.

---

## 1) What this app does (end-to-end)

At a high level, DataSage supports:

- **Authentication lifecycle**: register, OTP verification, login, forgot/reset password, token refresh, logout.
- **Role-aware navigation**: owner and staff see different tabs.
- **Daily operations**: sales creation, inventory browsing/search, dashboard and analytics views.
- **Hardware Integrations**: ZXing barcode scanning for fast checkout, and Bluetooth thermal receipt printing via background polling.
- **Rich Data Visualizations**: Custom Jetpack Compose wrappers around MPAndroidChart for dashboard analytics and demand forecasting.
- **Offline-first billing**: sales are stored locally first, then synced in batches when online.
- **Operational visibility**: pending/failed sync counters and retry from Settings.

End-to-end runtime flow:

1. App launches (`RetailIQApp`) and schedules periodic sync work.
2. `MainActivity` loads auth state (token present or not).
3. Unauthenticated users go through auth navigation; authenticated users enter main navigation.
4. Feature screens call ViewModels, which call repositories.
5. Repositories use Retrofit APIs for online data and Room for offline transaction queueing.
6. `SyncTransactionsWorker` uploads queued transactions and updates local statuses.
7. Connectivity observer updates UI with online/offline state.

---

## 2) Technology stack

- **Language & runtime**: Kotlin, Java 17
- **UI**: Jetpack Compose + Material 3
- **Navigation**: Navigation Compose
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp + Gson
- **Storage**: Room (SQLite) + EncryptedSharedPreferences
- **Background jobs**: WorkManager + Hilt workers
- **Logging**: Timber
- **Build system**: Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`)

---

## 3) Project structure and modules

```text
app/src/main/java/com/retailiq/datasage/
├── core/           # cross-cutting concerns (auth storage, connectivity, auth events)
├── data/
│   ├── api/        # Retrofit services + request/response models
│   ├── local/      # Room entities, DAO, database
│   └── repository/ # domain-facing data orchestration
├── di/             # Hilt modules (network, database, auth bindings)
├── ui/
│   ├── auth/       # auth flow screens + viewmodel + validation
│   ├── navigation/ # bottom tabs + role-based routing
│   ├── dashboard/
│   ├── sales/
│   ├── inventory/
│   ├── analytics/
│   ├── alerts/
│   ├── customers/
│   ├── settings/
│   ├── worker/     # sync status UI/viewmodel
│   └── common/     # shared UI widgets (offline banner)
├── worker/         # background sync worker implementation
├── MainActivity.kt
└── RetailIQApp.kt
```

### Responsibilities by layer

- **UI layer**: renders state and emits intents (button clicks, searches, submit sale).
- **ViewModel layer**: transforms UI intents into business calls and exposes `StateFlow` UI state.
- **Repository layer**: central place for API invocation, error mapping, caching, and offline queue orchestration.
- **Data sources**:
  - Remote: Retrofit interfaces in `data/api`
  - Local: Room DB + DAO (`pending_transactions`) and encrypted token storage.

---

## 4) Detailed architecture: how modules connect and why

### 4.1 App bootstrap

- `RetailIQApp` is the application root (`@HiltAndroidApp`), initializes logging and enqueues periodic transaction sync.
- It also provides WorkManager configuration so Hilt can inject worker dependencies.

**Why**: this guarantees sync continues independently from UI sessions and supports eventual consistency.

### 4.2 Authentication & session management

- `AuthManager` implements `TokenStore` using `EncryptedSharedPreferences`.
- Access token + refresh token + role are saved on login **and on OTP verification** (auto-login after signup).
- Network layer injects Authorization header from `TokenStore`.
- **Session persistence across app restarts**: On startup, `SplashScreen` proactively calls `validateSession()` which attempts a token refresh via `/auth/refresh`. This ensures expired access tokens are renewed before any API call, preventing 401 errors after restarts.
- A refresh interceptor handles `401` responses:
  - attempts refresh via `/auth/refresh`
  - saves new tokens when successful
  - emits `SessionExpired` via `AuthEventBus` when refresh fails
  - **does not clear tokens** on transient network errors (only on explicit logout or invalid refresh token)
- **Auto-login after signup**: After OTP verification, the backend returns auth tokens. The app saves them immediately and navigates to setup/dashboard — no manual re-login required.
- UI can react to session expiry globally.

**Why**: central token handling prevents duplicated auth logic across feature modules. Proactive session validation ensures seamless restarts.

### 4.3 Networking module (`di/NetworkModule`)

Provides:

- `Gson`
- `OkHttpClient` with:
  - auth header interceptor
  - refresh/retry-on-401 interceptor
  - debug logging interceptor
- `Retrofit` bound to `BuildConfig.API_BASE_URL`
- Service singletons for all backend domains.

**Why**: shared client configuration guarantees consistent auth, logging, and timeout behavior.

### 4.4 Local persistence + offline queue & analytics caching

- **Transaction Queue**: Room table `pending_transactions` queues new sales offline, using `SyncTransactionsWorker` to upload them sequentially.
- **Analytics Snapshot**: Room table `analytics_snapshot` caches the latest dashboard/analytics metadata (via `SnapshotDto`). `SnapshotSyncWorker` fetches new data periodically (every 6 hours).
- **Offline Logic**: Pure Kotlin class `LocalKpiEngine` handles metric recalculation (like week-over-week trends) from cached objects. Failed transactions can be explicitly retried from Settings.

**Why**: decoupled network conditions reduce operational downtime risk and provide instantaneous startup rendering.

### 4.5 Navigation + role gating

- `MainNavigation` uses bottom tabs from `tabsForRole(UserRole)`.
- Owners get: Home, Sales, Inventory, Analytics, More.
- Staff gets: Home, Sales, Inventory.
- Connectivity state is shown via `OfflineBanner`.

**Why**: role-appropriate UI keeps operational focus and avoids accidental access to owner-only flows.

### 4.6 Hardware Integrations (Camera & Bluetooth)

- **Barcode Scanning**: Uses ZXing's `IntentIntegrator` via `ScanContract`. The data layer (`ReceiptsRepository.lookupBarcode`) maps the scan payload directly to the cart.
- **Bluetooth Printing**: Uses a `ModalBottomSheet` to discover `BluetoothAdapter.bondedDevices`. The UI submits a print job via API and actively polls `/receipts/print/{jobId}` until `COMPLETED` or `FAILED`.
- Settings screen stores default customizable templates.

### 4.7 Charting & Data Visualizations (MPAndroidChart)

- Wraps `MPAndroidChart` native Views inside Compose's `AndroidView` within the `ui/components/ChartComponents.kt` library.
- Safely handles lifecycle cleanup and list recycling using `onReset` blocks to clear old datasets.
- Maps domain-specific models like `CategoryBreakdown` into `PieEntry` datasets locally to keep UI declarative.
- **Real-time data**: `DashboardRepository` fetches category breakdown (`GET /api/v1/analytics/category-breakdown`) and payment modes (`GET /api/v1/analytics/payment-modes`) from the backend. Both `DashboardViewModel` and `AnalyticsViewModel` expose these as `StateFlow` for reactive UI binding.

---

## 5) Feature modules (functional view)

### Auth (`ui/auth`, `data/repository/AuthRepository`)

- Register + OTP verification (auto-login: tokens saved on OTP success)
- Login
- Forgot/reset password
- Setup wizard completion flag
- Token-backed session state (`hasToken`, `validateSession`, `role`, `logout`)
- Proactive session refresh on app startup via `SplashScreen`

### Dashboard (`ui/dashboard`, `data/repository/DashboardRepository`)

- Fetches daily summary + dashboard analytics payload (`DashboardPayload`).
- Uses `ConnectivityObserver` to seamlessly fall back to an internal Room snapshot, displaying a yellow offline capability banner and using `LocalKpiEngine` algorithms for metrics.
- Integrates `RevenueLineChart`, `PaymentModeBarChart`, and `CategoryPieChart` for rich analytical overviews.
- Shows pending/failed local sync counters for awareness

### Sales (`ui/sales`, `data/repository/TransactionRepository`)

- Product selection/cart from cached inventory
- ZXing Barcode Scanning integration for 1-tap fast cart additions
- `PrintReceiptBottomSheet` triggered after an offline sale is saved, invoking remote Bluetooth thermal print jobs
- Builds transaction payload and saves offline immediately
- Sync runs asynchronously in background worker

### Inventory (`ui/inventory`, `data/repository/InventoryRepository`)

- Loads products from inventory API
- In-memory cache for faster subsequent filtering/search

### Analytics (`ui/analytics`)

- Uses dashboard analytics data for chart/summary style screens. Features `ContributionBarChart` & `CategoryPieChart` breakdowns.
- Shares the Room `analytics_snapshot` table as a fallback, utilizing `LocalKpiEngine`'s logic to render trends dynamically, even seamlessly substituting complex network calls.

### Forecast (`ui/forecast`, `data/repository/ForecastRepository`)

- Projects demand for items and stores utilizing a dual-dataset `ForecastLineChart` featuring historical actuals alongside predicted ranges and confidence bounds.

### Alerts (`ui/alerts`)

- Pulls inventory alerts from backend and maps generic payload fields to UI model
- Provides direct "Create PO" call-to-action for critical items, navigating to the pre-filled PO creation flow

### Suppliers & Purchase Orders (`ui/supplier`, `ui/purchaseorder`, `data/repository/SupplierRepository`)

- Displays comprehensive Supplier directory including analytical insights (lead time, fill rate).
- Supplier Profile UI detailing associated products, contact info, and recent PO history timeline.
- End-to-end Purchase Order creation flow supporting line item entry from inventory and draft/send capabilities.
- Optimistic UI updates during PO sending.
- Goods Receipt UI for reconciling delivered quantities against expected inventory, finalizing the inbound supply chain loop.

### Staff Management & Session Tracking (`ui/staff`, `data/repository/StaffRepository`)

- Introduces UX routing based on JWT `role` claims: `OWNER`, `STAFF`, and `VIEWER`.
- **Staff Session Banner**: Appears above the Dashboard for active staff, allowing session initialization and tracking formatted elapsed time via background coroutine polling. Summarizes session on end (total tx/revenue).
- **Staff Performance Dashboard**: An OWNER-only insight screen detailing per-employee metrics (target tracking, transaction volume, revenue, discount limits). Uses bottom-sheet forms to dispatch daily operational goals (`DailyTargetRequest`).

### Settings + Worker status (`ui/settings`, `ui/worker`)

- Displays pending and failed sync counts
- Triggers retry for failed items
- Customizable "Receipt & Printer" card for modifying header/footer templates
- Logout action with unsynced warning

---

## 6) API domains available in the app

The app already has typed Retrofit services for:

- Auth
- Store profile/categories/tax config
- Transactions (single + batch + daily summary + listing)
- Inventory (CRUD + stock + audit + history)
- Customers
- Analytics
- Forecasting
- Alerts
- Recommendations
- NLP query

> Note: not every available service is fully surfaced by UI screens yet; some are infrastructure-ready for future expansion.

---

## 7) Build and run instructions

## Prerequisites

- JDK 17
- Android Studio (latest stable) with Android SDK 35
- Android emulator/device (min SDK 23)

## Clone and open

```bash
git clone <your-repo-url>
cd DataSage
```

Open in Android Studio and let Gradle sync.

## Configure backend URL

Default emulator URL:

- `http://10.0.2.2:5000/`

Override at build time:

```bash
./gradlew assembleDebug -PAPI_BASE_URL=https://your-retailiq-domain/
```

Persist in Gradle properties (`~/.gradle/gradle.properties` or project `gradle.properties`):

```properties
API_BASE_URL=https://your-retailiq-domain/
```

## Run

```bash
./gradlew installDebug
```

Then launch **DataSage** on emulator/device.

---

## 8) Testing and quality checks

Common commands:

```bash
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
```

What exists today:

- Unit tests for auth repository/validation/viewmodels
- Navigation/role guard tests
- Auth event bus tests
- Android integration tests for auth manager and DAO

---

## 9) How to modify the app safely

When implementing changes, follow this flow:

1. **API contract first**
   - Add request/response models in `data/api`.
   - Add/extend Retrofit method in the relevant `*ApiService`.
2. **Repository integration**
   - Add use-case style function in repository.
   - Use `NetworkResult` and map API errors with `toUserMessage()`.
3. **ViewModel state**
   - Add/extend sealed UI state and `StateFlow` fields.
   - Keep business logic in ViewModel/repository, not composables.
4. **Compose screen wiring**
   - Bind to ViewModel state with `collectAsState`.
   - Keep composables declarative and side-effect-light.
5. **Navigation update (if needed)**
   - Add route + tab mapping in navigation package.
6. **Tests**
   - Add/adjust unit tests for new behavior.

---

## 10) How to extend the app (new module blueprint)

Use this template to add a new feature module (example: `Returns`):

1. **Create API interface**
   - `data/api/ReturnsApiService.kt`
2. **Create repository**
   - `data/repository/ReturnsRepository.kt`
3. **Create ViewModel + state**
   - `ui/returns/ReturnsViewModel.kt`
4. **Create screen**
   - `ui/returns/ReturnsScreen.kt`
5. **Register DI provider**
   - Add `@Provides` in `NetworkModule` for new API service.
6. **Wire navigation**
   - Add composable route in `MainNavigation`.
   - Add bottom tab entry (optionally role-limited).
7. **Add tests**
   - repository + viewmodel unit tests

Design recommendations for extension:

- Reuse `NetworkResult` for consistency.
- Prefer repository as single source of truth for data access.
- If the feature must work offline, create a Room entity + DAO + worker strategy similar to transactions.
- Keep role decisions in navigation/model layer, not scattered inside screens.

---

## 11) Operational behavior details

### Offline/online behavior

- Connectivity observer continuously emits internet capability state to ViewModels (`DashboardViewModel`, `AnalyticsViewModel`).
- Real-time yellow offline banner indicates degraded connectivity and timestamps showing cache staleness for dashboards.
- Dashboards fall back instantly to locally-cached Room snapshots parsed via `LocalKpiEngine`.
- Sales continue offline and are sync-queued via WorkManager.

### Sync behavior

- Periodic sync: every 15 minutes when connected.
- Ad-hoc sync: triggered on sale creation and retry actions.
- Batch size is fixed in worker (`BATCH_SIZE = 500`).
- `409` is treated as success during batch sync (idempotency/conflict-tolerant behavior).

### Failure strategy

- Each failed transaction increments retry count.
- After max retries (`MAX_RETRIES = 5`), item is marked failed.
- Failed items are user-retriable from Settings.

---

## 12) Security and data notes

- Tokens are stored in encrypted preferences (`EncryptedSharedPreferences` with AES-256).
- Role is decoded from JWT payload and cached for UI routing.
- Authorization headers are auto-attached by interceptor.
- Token refresh is transparent: the interceptor retries on 401, and `SplashScreen` proactively refreshes on startup.
- Transient network errors during refresh do not destroy the session — tokens are preserved and refresh is retried on next request.

Potential hardening opportunities:

- Add certificate pinning in OkHttp for production.
- Add SQLCipher (if at-rest DB encryption becomes mandatory).

---

## 13) Known current limitations

- Some APIs are integrated at service layer but not surfaced in UI yet.
- `AppViewModel` auth event reaction exists, but app-level redirection for session expiry can be expanded.
- Inventory cache is in-memory only; app restart requires refetch.

---

## 14) Quick reference: key classes

- App bootstrap: `RetailIQApp`, `MainActivity`
- DI: `NetworkModule`, `DatabaseModule`, `AuthModule`
- Core: `AuthManager`, `ConnectivityObserver`, `AuthEventBus`
- Offline queue: `PendingTransaction`, `PendingTransactionDao`, `SyncTransactionsWorker`
- Repositories: `AuthRepository`, `DashboardRepository`, `InventoryRepository`, `TransactionRepository`
- Navigation: `MainNavigation`, `tabsForRole`, `UserRole`

---

## 15) Contribution tips

- Keep feature code grouped by package (`ui/<feature>`, repository/api support in data layer).
- Avoid placing networking code directly in screens.
- Prefer incremental PRs:
  1. API + repository
  2. ViewModel + screen
  3. navigation + tests

This keeps review quality high and reduces regressions.
