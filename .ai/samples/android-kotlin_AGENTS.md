# Store it! - Clean Architecture Specification & Tech Stack

## Executive Summary

This document prescribes the architectural blueprint, dependency stack, and engineering practices for building "Store it!", a KMP application using Clean Architecture principles. The target audience is seasoned Android engineers who understand SOLID principles, reactive programming paradigms, and modern Android development practices.

The codebase follows strict layer separation with testability-first design, adhering to MVVM + Clean Architecture principles on Jetpack Compose.

---

## 1. Architectural Layers & Responsibility Boundaries

### 1.1 Layer Architecture Diagram

```UML
┌─────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                         │
│  ┌──────────────────┐                  ┌──────────────────────┐ │
│  │  Compose UI      │◄─────MutableFlow─┤  ViewModel           │ │
│  │  (TaximeterUI)   │                  │  (TaximeterViewModel)│ │
│  └──────────────────┘                  └──────────────────────┘ │
│                            ▲                       ▲            │
│                            │                       │            │
└────────────────────────────┼───────────────────────┼────────────┘
                             │                       │
                     (Dependency Injection)  (use cases)
                             │                       │
┌────────────────────────────┼───────────────────────┼────────────┐
│                    DOMAIN LAYER (Use Cases)        │            │
│  ┌─────────────────────────────────────────────────┴─────────┐  │
│  │                 UseCase Interfaces                        │  │
│  ├──────────────────┬──────────────────┬─────────────────────┤  │
│  │ CalculateFareUC  │ FetchTariffsUC   │ TrackLocationUC     │  │
│  │ (In: points,     │ (In: none)       │ (Out: LocationFlow) │  │
│  │  Out: Fare)      │ (Out: Tariffs)   │                     │  │
│  └──────────────────┴──────────────────┴─────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Domain Entities & Value Objects             │   │
│  │  ┌──────────┬──────────┬────────────┬──────────────────┐ │   │
│  │  │Fare      │Tariff    │LocationPt  │RouteSegment      │ │   │
│  │  │(total,   │(perKm,   │(lat,long,  │(distance,        │ │   │
│  │  │components)perSec)   │timestamp)  │timeElapsed)      │ │   │
│  │  └──────────┴──────────┴────────────┴──────────────────┘ │   │
│  │                                                          │   │
│  │  ┌──────────────────────────────────────────────────┐    │   │
│  │  │        Repository Interfaces (Contracts)         │    │   │
│  │  │  ┌────────────────┬──────────────────────────┐   │    │   │
│  │  │  │ TariffRepository  │ LocationRepository    │   │    │   │
│  │  │  │ • getTariffs()    │ • getRouteStream()    │   │    │   │
│  │  │  └────────────────┴──────────────────────────┘   │    │   │
│  │  └──────────────────────────────────────────────────┘    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                             ▲
                             │
                     (Repository Pattern)
                             │
┌─────────────────────────────────────────────────────────────────┐
│                     DATA LAYER (Repositories)                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │            TariffRepository Implementation                │  │
│  │  ┌──────────────────┬──────────────────────────────────┐  │  │
│  │  │ RemoteTariffDS   │ CachedTariffDataSource           │  │  │
│  │  │ • fetch via HTTP │ • in-memory cache with TTL       │  │  │
│  │  │ • KotlinX Serial │ • fallback defaults              │  │  │
│  │  └──────────────────┴──────────────────────────────────┘  │  │
│  │                                                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │         LocationRepository Implementation                 │  │
│  │  ┌──────────────────┬──────────────────────────────────┐  │  │
│  │  │ GPSLocationDS    │ LocationProvider Adapter         │  │  │
│  │  │ • Wraps JAR      │ • Flow<LocationPoint> adapter    │  │  │
│  │  │ • Route/Config   │ • Timestamp-based filtering      │  │  │
│  │  │   management     │                                  │  │  │
│  │  └──────────────────┴──────────────────────────────────┘  │  │
│  │                                                           │  │
│  │  ┌──────────────────────────────────────────────────────┐ │  │
│  │  │          External Data Sources                       │ │  │
│  │  │  ┌──────────────┬──────────────────────────────────┤ │ │  │
│  │  │  │ HTTP Client  │ locationProvider.jar (LocationDS)│ │ │  │
│  │  │  └──────────────┴──────────────────────────────────┘ │ │  │
│  │  └──────────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Layer Responsibilities

#### **Presentation Layer** (`presentation/`)

- **Components**: `TaximeterScreen` (Compose), `TaximeterViewModel`
- **Responsibilities**:
  - State management via `MutableStateFlow<UiState>`
  - UI event handling and delegation to use cases
  - Lifecycle-aware coroutine collection (`viewModelScope`)
  - Formatting/presentation logic only (no business rules)
- **Boundaries**: Never directly instantiate repositories; always receive via DI
- **Anti-patterns to avoid**:
  - Direct API calls from ViewModel
  - Business logic in `@Composable` functions
  - `GlobalScope.launch` or unmanaged coroutines

#### **Domain Layer** (`domain/`)

- **Components**: Use cases, domain entities, repository interfaces
- **Responsibilities**:
  - Business logic orchestration (pure, testable)
  - Use case implementation with clear input/output contracts
  - Domain entity definitions (immutable data classes)
  - Repository interface contracts (no implementation)
- **Key Invariant**: Zero external dependencies (no Android Framework, no Compose, no HTTP libraries)
- **Example Use Case Structure**:

  ```kotlin
  class CalculateFareUseCase(
      private val tariffRepository: TariffRepository,
      private val locationRepository: LocationRepository
  ) {
      suspend operator fun invoke(
          routePoints: List<LocationPoint>,
          luggageCount: Int
      ): Result<Fare> = withContext(Dispatchers.Default) {
          val tariffs = tariffRepository.getTariffs()
          val fare = calculateInternalFare(routePoints, tariffs, luggageCount)
          Result.success(fare)
      }
  }
  ```

#### **Data Layer** (`data/`)

- **Components**: Repository implementations, data sources (remote, local, cached)
- **Responsibilities**:
  - Fetching data from external sources (HTTP, LocationProvider JAR)
  - Data transformation (DTO ↔ Domain Entity mapping)
  - Caching strategies and fallback mechanisms
  - Error handling and retry logic
- **Sub-layers**:
  - **Data Sources**: Specialized adapters for each external source
  - **Repositories**: Orchestrate multiple data sources with business rules (e.g., cache-first with fallback)
- **Dependency Direction**: Only depends on domain repository interfaces

---

## 2. Directory Structure

```UML
app/src/main/
├── java/com/cabify/codechallenge/
│   ├── di/                                    # Dependency Injection (Koin modules)
│   │   ├── dataModule.kt                     # Koin module for repositories & data sources
│   │   ├── domainModule.kt                   # Koin module for use cases
│   │   └── presentationModule.kt             # Koin module for ViewModels
│   │
│   ├── domain/                               # DOMAIN LAYER (business rules)
│   │   ├── model/
│   │   │   ├── Fare.kt                       # Immutable data class: total, distance, time, luggage components
│   │   │   ├── Tariff.kt                     # Immutable: perKm, perSecond (domain entity, not DTO)
│   │   │   ├── LocationPoint.kt              # Immutable: lat, long, timestamp (derived from provider)
│   │   ├── Route.kt                      # Sealed class for Route1, Route2, Route3
│   │   ├── ExecutionConfig.kt            # Sealed class for Default, Fast (simulations)
│   │   └── Result.kt                     # Either-based error handling: Ok<A>, Err<E>
│   │   │
│   │   ├── repository/
│   │   │   ├── TariffRepository.kt           # Interface: getTariffs(): Result<Tariff>
│   │   │   └── LocationRepository.kt         # Interface: getRouteFlow(route, config): Flow<LocationPoint>
│   │   │
│   │   └── usecase/
│   │       ├── CalculateFareUseCase.kt       # Operator: invoke(points, luggage) -> Result<Fare>
│   │       ├── FetchTariffsUseCase.kt        # Operator: invoke() -> Result<Tariff>
│   │       └── TrackLocationUseCase.kt       # Operator: invoke(route, config) -> Flow<LocationPoint>
│   │
│   ├── data/                                 # DATA LAYER (external sources)
│   │   ├── datasource/
│   │   │   ├── remote/
│   │   │   │   ├── HttpPriceConfigDataSource.kt  # HTTP client (Retrofit/OkHttp)
│   │   │   │   └── PriceConfigDto.kt             # DTO: maps JSON {"price_per_km", "price_per_second"}
│   │   │   │
│   │   │   ├── cache/
│   │   │   │   └── TariffCacheDataSource.kt      # In-memory cache with TTL
│   │   │   │
│   │   │   └── location/
│   │   │       ├── LocationProviderAdapter.kt    # Wraps locationProvider.jar
│   │   │       └── LocationPointDto.kt           # DTO if needed (maps from provider)
│   │   │
│   │   ├── mapper/
│   │   │   ├── TariffMapper.kt                   # DTO -> Domain entity: mapToDomain(PriceConfigDto): Tariff
│   │   │   └── LocationMapper.kt                 # DTO -> Domain entity: mapToDomain(providerPoint): LocationPoint
│   │   │
│   │   └── repository/
│   │       ├── TariffRepositoryImpl.kt            # Implements domain.repository.TariffRepository
│   │       │                                     # Composes: remote + cache data sources
│   │       └── LocationRepositoryImpl.kt          # Implements domain.repository.LocationRepository
│   │                                             # Wraps LocationProviderAdapter
│   │
│   ├── presentation/                        # PRESENTATION LAYER (UI + State)
│   │   ├── taximeter/
│   │   │   ├── TaximeterViewModel.kt         # StateHolder: collects use case outputs
│   │   │   ├── TaximeterScreen.kt            # Compose UI
│   │   │   ├── TaximeterUiState.kt           # Data class: currentFare, isRiding, etc.
│   │   │   └── TaximeterUiEvent.kt           # Sealed class: UserClickedStart, AddedLuggage
│   │   │
│   │   └── common/
│   │       └── UiState.kt                    # Generic sealed class: Loading, Success<T>, Error(Exception)
│   │
│   ├── ui/
│   │   └── theme/
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   │
│   └── MainActivity.kt                       # Activity host
│
└── res/                                      # Android resources
    └── ...
```

---

## 3. Tech Stack & Dependencies

### 3.1 Preferred Dependencies for Clean Architecture

| Dependency | Purpose | Rationale |
|-----------|---------|-----------|
| **androidx.compose.bom** | UI framework | Declarative, state-reactive UI paradigm |
| **androidx.lifecycle:lifecycle-runtime-ktx** | Lifecycle awareness | `viewModelScope`, Flow collection, coroutine lifecycle safety |
| **androidx.activity:activity-compose** | Activity-Compose integration | Host for Compose application |
| **androidx.compose.material3** | Material Design 3 | Modern UI components |
| **org.jetbrains.kotlinx:kotlinx-serialization-json** | Kotlin-native serialization | Type-safe, compile-time JSON with compile-time code generation (preferred approach) |
| **io.insert-koin:koin-android** | Lightweight DI framework | Annotation-free, simple, Kotlin-idiomatic dependency injection |
| **io.insert-koin:koin-compose** | Koin Compose integration | ViewModel injection in Compose |
| **com.squareup.okhttp3:okhttp** | HTTP client | Interceptors, retry logic, built-in caching |
| **com.squareup.retrofit2:retrofit** | REST abstraction | Type-safe HTTP client |
| **com.squareup.retrofit2:converter-kotlinx-serialization** | Retrofit-KotlinX integration | Serialization adapter for Retrofit |
| **org.jetbrains.kotlinx:kotlinx-coroutines-android** | Android coroutine support | Main dispatcher, lifecycle-aware launch |
| **junit:junit** | Unit testing | Local VM tests |
| **org.jetbrains.kotlinx:kotlinx-coroutines-test** | Coroutine testing | Test dispatchers, `runTest {}` |
| **org.mockito.kotlin:mockito-kotlin** | Mocking framework | Mockito with Kotlin extensions for behavior verification |
| **androidx.test.ext:junit** | Android test extensions | Instrumented test support |
| **androidx.test.espresso:espresso-core** | UI testing framework | Compose integration testing |
| **androidx.compose.ui:ui-test-junit4** | Compose test utilities | Compose assertion and interaction APIs |

### 3.2 Dependency Injection Strategy (Koin)

Using **Koin**, a lightweight, Kotlin-idiomatic DI framework that eliminates annotation processing overhead.

**Koin Modules** (in `di/` package):

```kotlin
// di/dataModule.kt
val dataModule = module {
    single<TariffRepository> {
        TariffRepositoryImpl(
            remoteDataSource = get(),
            cacheDataSource = get(),
            mapper = get()
        )
    }

    single<LocationRepository> {
        LocationRepositoryImpl(adapter = get())
    }

    single { HttpPriceConfigDataSource() }
    single { TariffCacheDataSource() }
    single { TariffMapper() }
    single { LocationProviderAdapter() }
}

// di/domainModule.kt
val domainModule = module {
    factory {
        CalculateFareUseCase(
            tariffRepository = get(),
            locationRepository = get()
        )
    }

    factory {
        FetchTariffsUseCase(tariffRepository = get())
    }

    factory {
        TrackLocationUseCase(locationRepository = get())
    }
}

// di/presentationModule.kt
val presentationModule = module {
    viewModel {
        TaximeterViewModel(
            calculateFareUseCase = get(),
            fetchTariffsUseCase = get(),
            trackLocationUseCase = get()
        )
    }
}

// In MainActivity.kt or Application class:
startKoin {
    modules(
        dataModule,
        domainModule,
        presentationModule
    )
}
```

### 3.3 Build Logic & Version Catalog

Use **Gradle build-logic** and **version catalog** for centralized dependency and plugin management.

**gradle/libs.versions.toml** (Version Catalog):

```toml
[versions]
kotlin = "1.9.0"
compose-bom = "2023.08.00"
lifecycle = "2.6.1"
koin = "3.5.0"
okhttp = "4.11.0"
retrofit = "2.9.0"
coroutines = "1.7.1"
junit = "4.13.2"
mockito = "5.5.0"
mockito-kotlin = "5.1.0"

[libraries]
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui", version = "" }
androidx-lifecycle-runtime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
mockito = { module = "org.mockito:mockito-core", version.ref = "mockito" }
mockito-kotlin = { module = "org.mockito.kotlin:mockito-kotlin", version.ref = "mockito-kotlin" }

[bundles]
androidx-compose = ["androidx-compose-ui", "androidx-compose-material3"]
koin = ["koin-android", "koin-compose"]
testing = ["junit", "mockito", "mockito-kotlin"]

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

**build-logic/build.gradle.kts**:

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
}
```

**build-logic/src/main/kotlin/android-library.gradle.kts**:

```kotlin
import org.gradle.api.JavaVersion

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

**app/build.gradle.kts** (using conventions):

```kotlin
plugins {
    id("android-application")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.cabify.codechallenge"
}

dependencies {
    implementation(libs.bundles.androidx.compose)
    implementation(libs.bundles.koin)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    
    testImplementation(libs.bundles.testing)
}
```

---

## 4. Core Patterns & Implementations

### 4.1 Domain Model Design

All domain models are **immutable, serializable data classes** with explicit value semantics:

```kotlin
// domain/model/Fare.kt
@Immutable
data class Fare(
    val totalPrice: Double,
    val distancePrice: Double,
    val timePrice: Double,
    val luggagePrice: Double,
    val currency: String = "EUR"
) {
    init {
        require(totalPrice >= 0) { "Total price must be non-negative" }
        require(distancePrice >= 0) { "Distance price must be non-negative" }
        // Validation rules enforce invariants
    }
}

// domain/model/Tariff.kt
@Immutable
data class Tariff(
    val pricePerKilometer: Double,
    val pricePerSecond: Double,
    val currency: String = "EUR"
) {
    init {
        require(pricePerKilometer >= 0) { "Price per km must be non-negative" }
        require(pricePerSecond >= 0) { "Price per second must be non-negative" }
    }
}

// domain/model/LocationPoint.kt
@Immutable
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude out of bounds" }
        require(longitude in -180.0..180.0) { "Longitude out of bounds" }
        require(timestampMillis > 0) { "Timestamp must be positive" }
    }
}
```

### 4.2 Use Case Pattern (Operator Invocation)

All use cases follow a **single-responsibility, testable pattern** using `operator fun invoke`:

#### **4.2.1 Use Cases with Business Logic (Classes)**

Complex use cases that contain business logic, validation, or orchestration are implemented as **regular classes**:

```kotlin
// domain/usecase/CalculateFareUseCase.kt
class CalculateFareUseCase(
    private val tariffRepository: TariffRepository,
    private val locationRepository: LocationRepository,
    private val dispatchers: CoroutineDispatchers = CoroutineDispatchers.Default
) {
    suspend operator fun invoke(
        routePoints: List<LocationPoint>,
        luggageCount: Int
    ): Result<Fare> = withContext(dispatchers.computation) {
        runCatching {
            require(routePoints.isNotEmpty()) { "Route must contain at least one point" }
            require(luggageCount >= 0) { "Luggage count must be non-negative" }

            val tariff = tariffRepository.getTariffs()
                .getOrThrow() // Propagate errors

            val distanceKm = calculateTotalDistance(routePoints)
            val durationSeconds = calculateDuration(routePoints)
            val luggageSupplements = luggageCount * LUGGAGE_UNIT_PRICE

            Fare(
                distancePrice = distanceKm * tariff.pricePerKilometer,
                timePrice = durationSeconds * tariff.pricePerSecond,
                luggagePrice = luggageSupplements,
                totalPrice = distancePrice + timePrice + luggageSupplements
            )
        }
            .fold(
                onSuccess = { fare -> Ok(fare) },
                onFailure = { error -> Err(error) },
            )
    }

    private fun calculateTotalDistance(points: List<LocationPoint>): Double {
        return points.zipWithNext()
            .sumOf { (start, end) -> haversineDistance(start, end) }
    }

    private fun haversineDistance(start: LocationPoint, end: LocationPoint): Double {
        // Haversine formula implementation
        val latDiff = Math.toRadians(end.latitude - start.latitude)
        val lonDiff = Math.toRadians(end.longitude - start.longitude)
        val a = sin(latDiff / 2).pow(2) +
                cos(Math.toRadians(start.latitude)) *
                cos(Math.toRadians(end.latitude)) *
                sin(lonDiff / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }

    companion object {
        private const val LUGGAGE_UNIT_PRICE = 5.0
        private const val EARTH_RADIUS_KM = 6371.0
    }
}
```

#### **4.2.2 Use Cases Without Business Logic (Functional Interfaces)**

Simple use cases that **merely delegate to a single repository function** with no additional logic, validation, or transformation should be declared as **functional interfaces** to reduce boilerplate:

```kotlin
// domain/usecase/FetchTariffsUseCase.kt
internal fun interface FetchTariffsUseCase {
    suspend operator fun invoke(): Result<Tariff>
}

// Or, if more semantically explicit:
typealias FetchTariffsUseCase = suspend () -> Result<Tariff>

// DI wiring in di/domainModule.kt:
val domainModule = module {
    factory<FetchTariffsUseCase> {
        FetchTariffsUseCase { tariffRepository.getTariffs() }
    }
}
```

**Rationale**:

- **Reduces ceremony**: Eliminates class boilerplate when no composition or transformation is needed
- **Intent clarity**: Signals to readers that this is a direct pass-through to a repository
- **Testability**: Still mockable and injectable via Koin
- **Consistency**: Use `fun interface` for symmetric signatures with repository methods

**When to use functional interface**:

✓ Use case simply delegates to one repository method
✓ No input transformation or validation
✓ No output transformation or mapping
✓ No orchestration between multiple repositories

**When to use regular class**:

✓ Multiple business logic steps
✓ Input validation or error handling
✓ Output transformation or enrichment
✓ Orchestration across multiple repositories or use cases

### 4.3 Repository Pattern (Repository Implementations)

Repositories orchestrate data sources with cache-first strategies:

```kotlin
// data/repository/TariffRepositoryImpl.kt
class TariffRepositoryImpl(
    private val remoteDataSource: HttpPriceConfigDataSource,
    private val cacheDataSource: TariffCacheDataSource,
    private val mapper: TariffMapper,
    private val dispatchers: CoroutineDispatchers = CoroutineDispatchers.Default
) : TariffRepository {

    override suspend fun getTariffs(): Result<Tariff> = 
        withContext(dispatchers.io) {
            // Try cache first
            cacheDataSource.getTariffs()?.let {
                return@withContext Result.success(it)
            }

            // Fetch from remote with retry
            return@withContext runCatching {
                val dto = remoteDataSource.fetchPriceConfig()
                mapper.mapToDomain(dto).also {
                    cacheDataSource.cacheTariffs(it, ttlMinutes = 60)
                }
            }.onFailure { error ->
                // Log error, optionally emit fallback
                Log.e("TariffRepo", "Failed to fetch tariffs", error)
            }
        }
}
```

### 4.3a Data Serialization (Kotlin Serialization for DTOs)

All **Data Transfer Objects (DTOs)** are serialized using **KotlinX Serialization** for compile-time type-safe JSON parsing. DTOs are exclusively used in the data layer for mapping between external APIs and domain entities.

```kotlin
// data/datasource/remote/PriceConfigDto.kt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PriceConfigDto(
    @SerialName("price_per_km")
    val pricePerKm: Double,
    
    @SerialName("price_per_second")
    val pricePerSecond: Double,
    
    val currency: String = "EUR"
)

// data/datasource/remote/HttpPriceConfigDataSource.kt
class HttpPriceConfigDataSource(
    private val api: PriceApi,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : RemotePriceDataSource {
    
    override suspend fun fetchPriceConfig(): Result<NetworkError, PriceConfigDto> =
        runCatching {
            api.getPriceConfig()
        }.mapCatching { response ->
            json.decodeFromString<PriceConfigDto>(response.body().string())
        }.onFailure { error ->
            // Log or handle deserialization errors
        }
}

// Using Retrofit with KotlinX Serialization converter
interface PriceApi {
    @GET("config/prices")
    suspend fun getPriceConfig(): Response<ResponseBody>
}

// In build.gradle.kts:
dependencies {
    implementation(libs.retrofit)
    implementation(libs.bundles.kotlinx.serialization)
}
```

**Advantages of KotlinX Serialization over Gson**:

- **Compile-time code generation**: Serializers are generated at compile-time, not runtime reflection
- **Type-safe**: Compile-time verification of serialization contracts
- **Multiplatform**: Works on Kotlin/JS, Kotlin/Native, and Kotlin/JVM
- **No reflection overhead**: Faster deserialization, smaller APK
- **Kotlin-idiomatic**: First-class coroutine support, sealed class serialization
- **Format flexibility**: JSON, Protobuf, CBOR, and custom formats via plugins

### 4.4 Error Handling (Either-Based Result Type)

The project uses a custom **Either-based `Result<E, A>` type** (inspired by Arrow library's `Either` type), providing explicit error handling with right-biased semantics.

```kotlin
// base/Result.kt
sealed interface Result<out E, out A> {
    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    // Functional combinators
    inline fun <B> map(f: (A) -> B): Result<E, B>
    inline fun <B> flatMap(f: (A) -> Result<E, B>): Result<E, B>
    inline fun <EE> mapLeft(f: (E) -> EE): Result<EE, A>
    
    inline fun <R> fold(
        ifErr: (E) -> R,
        ifOk: (A) -> R
    ): R

    fun getOrNull(): A?
    fun leftOrNull(): E?
}

data class Ok<out A>(val value: A) : Result<Nothing, A>
data class Err<out E>(val error: E) : Result<E, Nothing>
```

**Usage in Use Cases**:

```kotlin
class CalculateFareUseCase(
    private val tariffRepository: TariffRepository
) {
    suspend operator fun invoke(
        routePoints: List<LocationPoint>,
        luggageCount: Int
    ): Result<FareError, Fare> = withContext(Dispatchers.Default) {
        tariffRepository.getTariffs()
            .flatMap { tariff ->
                val distanceKm = calculateTotalDistance(routePoints)
                val durationSeconds = calculateDuration(routePoints)
                
                Ok(Fare(
                    distancePrice = distanceKm * tariff.pricePerKilometer,
                    timePrice = durationSeconds * tariff.pricePerSecond,
                    luggagePrice = luggageCount * LUGGAGE_UNIT_PRICE,
                    totalPrice = /* sum */
                ))
            }
    }
}

// In ViewModel:
viewModelScope.launch {
    calculateFareUseCase(routePoints, luggage)
        .fold(
            ifErr = { error ->
                _uiState.update { it.copy(error = error.message) }
            },
            ifOk = { fare ->
                _uiState.update { it.copy(currentFare = fare.totalPrice) }
            }
        )
}
```

### 4.5 ViewModel with UiState

ViewModels use a **single UiState data class** to represent all UI state:

```kotlin
// presentation/taximeter/TaximeterUiState.kt
@Immutable
data class TaximeterUiState(
    val currentFare: Double = 0.0,
    val isRideInProgress: Boolean = false,
    val numberOfLuggages: Int = 0,
    val elapsedSeconds: Long = 0,
    val distanceKm: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

// presentation/taximeter/TaximeterViewModel.kt
@HiltViewModel
class TaximeterViewModel(
    private val calculateFareUseCase: CalculateFareUseCase,
    private val fetchTariffsUseCase: FetchTariffsUseCase,
    private val trackLocationUseCase: TrackLocationUseCase,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaximeterUiState())
    val uiState: StateFlow<TaximeterUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<TaximeterUiEvent>()
    val uiEvents: SharedFlow<TaximeterUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadTariffs()
    }

    fun onStartRideClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRideInProgress = true, error = null) }
            
            trackLocationUseCase(Route.Route1, ExecutionConfig.Default)
                .onEach { point ->
                    // Update UI with live pricing
                    calculateFareUseCase(_currentRoutePoints, _uiState.value.numberOfLuggages)
                        .onSuccess { fare ->
                            _uiState.update { it.copy(currentFare = fare.totalPrice) }
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(error = error.message) }
                        }
                }
                .catch { error ->
                    _uiState.update { it.copy(isRideInProgress = false, error = error.message) }
                }
                .collect { point ->
                    _currentRoutePoints.add(point)
                }
        }
    }

    fun onAddLuggageClicked() {
        _uiState.update { it.copy(numberOfLuggages = it.numberOfLuggages + 1) }
    }

    private fun loadTariffs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchTariffsUseCase()
                .onSuccess { _uiState.update { state -> state.copy(isLoading = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    companion object {
        private val _currentRoutePoints = mutableListOf<LocationPoint>()
    }
}
```

---

## 5. Testing Strategy

### 5.1 Unit Testing (Domain & Use Cases)

Domain logic is 100% testable with no Android dependencies. Tests follow **Given-When-Then** structure with no block separators, only blank lines.

**Test Double Strategy**: Prefer **fakes** (simplified implementations) over mocks when possible. Use mocks selectively for behavior verification (e.g., ensuring a save operation was called). For most use cases, fakes provide better clarity and maintainability.

```kotlin
// app/src/test/java/com/cabify/codechallenge/domain/usecase/CalculateFareUseCaseTest.kt
class CalculateFareUseCaseTest {
    private lateinit var useCase: CalculateFareUseCase
    private val tariffRepository: TariffRepository = mock()
    private val locationRepository: LocationRepository = mock()

    @Before
    fun setup() {
        useCase = CalculateFareUseCase(tariffRepository, locationRepository)
    }

    @Test
    fun `given valid route and tariff, when calculating fare, then returns correct total`() = runTest {
        val route = listOf(
            LocationPoint(0.0, 0.0, 1000),
            LocationPoint(0.0, 0.1, 2000)
        )
        val tariff = Tariff(pricePerKilometer = 0.2, pricePerSecond = 0.01)
        coEvery { tariffRepository.getTariffs() } returns Ok(tariff)

        val result = useCase(route, luggageCount = 1)

        assertTrue(result is Ok)
        val fare = (result as Ok).value
        assertEquals(25.2, fare.totalPrice, 0.01)
    }

    @Test
    fun `given empty route, when calculating fare, then returns error`() = runTest {
        val result = useCase(emptyList(), 0)

        assertTrue(result is Err)
    }
}
```

### 5.2 Integration Testing (Repositories + Data Sources)

Integration tests verify repository composition with mocked external sources.

```kotlin
// app/src/test/java/com/cabify/codechallenge/data/repository/TariffRepositoryImplTest.kt
class TariffRepositoryImplTest {
    private lateinit var repository: TariffRepository
    private val remoteDataSource: HttpPriceConfigDataSource = mock()
    private val cacheDataSource: TariffCacheDataSource = mock()
    private val mapper = TariffMapper()

    @Before
    fun setup() {
        repository = TariffRepositoryImpl(remoteDataSource, cacheDataSource, mapper)
    }

    @Test
    fun `given cached tariffs, when fetching, then returns cache without remote call`() = runTest {
        val cachedTariff = Tariff(0.2, 0.01)
        coEvery { cacheDataSource.getTariffs() } returns cachedTariff

        val result = repository.getTariffs()

        assertTrue(result is Ok)
        verify(remoteDataSource, never()).fetchPriceConfig()  // Or use a fake with call counter
    }

    @Test
    fun `given cache miss, when fetching, then fetches from remote and caches result`() = runTest {
        val dto = PriceConfigDto(price_per_km = 0.2, price_per_second = 0.01)
        coEvery { cacheDataSource.getTariffs() } returns null
        coEvery { remoteDataSource.fetchPriceConfig() } returns dto
        coEvery { cacheDataSource.cacheTariffs(any(), any()) } just runs

        val result = repository.getTariffs()

        assertTrue(result is Ok)
        coVerify { remoteDataSource.fetchPriceConfig() }
        verify(cacheDataSource).cacheTariffs(any(), eq(60))  // Or verify via fake state
    }
}
```

### 5.3 UI Testing (Compose)

ViewModel state flows are testable with `runTest` and verify reactive behavior.

```kotlin
// app/src/androidTest/java/com/cabify/codechallenge/presentation/TaximeterViewModelTest.kt
@RunWith(AndroidJUnit4::class)
class TaximeterViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: TaximeterViewModel
    private val calculateFareUseCase: CalculateFareUseCase = mock()
    private val fetchTariffsUseCase: FetchTariffsUseCase = mock()
    private val trackLocationUseCase: TrackLocationUseCase = mock()

    @Before
    fun setup() {
        viewModel = TaximeterViewModel(calculateFareUseCase, fetchTariffsUseCase, trackLocationUseCase)
    }

    @Test
    fun `given start button clicked, when ride not in progress, then updates state to in progress`() = runTest {
        viewModel.onStartRideClicked()

        assertEquals(true, viewModel.uiState.value.isRideInProgress)
    }

    @Test
    fun `given add luggage clicked, when luggage count is zero, then increments to one`() = runTest {
        assertEquals(0, viewModel.uiState.value.numberOfLuggages)

        viewModel.onAddLuggageClicked()

        assertEquals(1, viewModel.uiState.value.numberOfLuggages)
    }
}
```

---

## 6. Build Configuration

### 6.1 Using Version Catalog & Build Logic (Already Covered in Section 3.3)

---

## 7. Execution Flow (State Transitions)

### 7.1 Ride Start Flow

```UML
User clicks "Start Ride"
    ↓
ViewModel.onStartRideClicked()
    ↓
ViewModel: isRideInProgress = true
    ↓
TrackLocationUseCase.invoke(Route.Route1)
    ↓
LocationRepository.getRouteFlow()
    ↓
LocationProviderAdapter → locationProvider.jar
    ↓
Flow<LocationPoint> emitted every N ms
    ↓
For each LocationPoint:
    - Add to _currentRoutePoints
    - Call CalculateFareUseCase(points, luggage)
    - Update uiState.currentFare
    - UI re-composes with new fare
    ↓
User clicks "Stop Ride" (or stream completes)
    ↓
isRideInProgress = false
    ↓
Clear _currentRoutePoints for next ride
```

---

## 8. Production Readiness Checklist

- [ ] **Proguard/R8 Minification**: Enable in release builds; add keep rules for Hilt, data models
- [ ] **Error Handling**: All use cases return `Result<T>`; ViewModel displays errors to UI
- [ ] **Offline Resilience**: Tariff cache with TTL; LocationRepository handles provider failures
- [ ] **Logging**: Structured logging (Timber recommended) in data sources; no logs in domain
- [ ] **Testing**: ≥80% code coverage; all use cases unit tested
- [ ] **Performance**: Fare calculation on `Dispatchers.Default` (not Main thread)
- [ ] **Security**: API endpoints use HTTPS; consider certificate pinning (OkHttp)
- [ ] **Analytics**: Track ride completion, errors, and tariff mismatches (optional Firebase Analytics integration)
- [ ] **CI/CD**: Gradle tasks: `./gradlew test connectedAndroidTest assembleRelease`

---

## 8. References & Further Reading

- **Uncle Bob's Clean Architecture**: <https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html>

- **Android Architecture Guide**: <https://developer.android.com/guide/architecture>
- **MVVM on Android**: <https://developer.android.com/topic/architecture/ui-layer/stateholders>
- **Hilt Documentation**: <https://developer.android.com/training/dependency-injection/hilt-android>
- **Kotlin Coroutines Best Practices**: <https://kotlinlang.org/docs/coroutines-basics.html>
- **Flow & StateFlow**: <https://kotlinlang.org/docs/flow.html>
- **Testing in Android**: <https://developer.android.com/training/testing>

---

## 9. Glossary

| Term | Definition |
|------|-----------|
| **Use Case** | Single, testable business logic unit; named after the action it performs |
| **Repository** | Abstraction over data sources; provides unified interface to domain layer |
| **Data Source** | Concrete implementation fetching from external (HTTP, JAR, local) sources |
| **DTO** | Data Transfer Object; model matching external API structure (e.g., JSON) |
| **Domain Entity** | Pure, immutable model representing business concept (e.g., `Fare`, `Tariff`) |
| **StateFlow** | Reactive stream for observable state; replaces `LiveData` in Compose |
| **Dispatchers** | Coroutine context determining execution thread (Main, Default, IO, Unconfined) |
| **Result<E, A>** | Either-based sealed type returning `Ok(A)` or `Err(E)` for explicit error handling |
| **Koin** | Lightweight, annotation-free DI framework; Kotlin-idiomatic alternative to Dagger/Hilt |
| **Proguard/R8** | Code obfuscation & shrinking for release builds |
| **Given-When-Then** | Test structure pattern: Given (setup), When (action), Then (verify) without block comments |

---

**Document Version**: 1.0  
**Last Updated**: November 18, 2025  
**Target Kotlin Version**: 1.9.0  
**Min SDK**: 24 | **Target SDK**: 34
