# Android & Kotlin Code Conventions & Design Patterns

## Executive Summary

This document prescribes code conventions, naming patterns, and architectural design practices for Kotlin-based Android projects. Adherence to these conventions ensures consistency, maintainability, and clarity across the codebase. The target audience is seasoned Android engineers familiar with SOLID principles, Kotlin idioms, and modern Android development patterns.

---

## 1. Package Organization & Naming

### 1.1 Package Structure Conventions (Multi-Modular Architecture)

**Single-module package structure within each feature module** (e.g., `:feature-authentication`):

```kotlin
com.company.product.authentication/
├── di/                              # Dependency Injection (module-level setup)
├── domain/                          # Business logic layer (feature-specific)
│   ├── model/                       # Domain entities, value objects
│   ├── repository/                  # Repository interfaces (contracts)
│   └── usecase/                     # Use case implementations
├── data/                            # Data layer (feature-specific data sources)
│   ├── datasource/                  # Concrete data source implementations
│   │   ├── local/                   # Local storage (DB, SharedPrefs)
│   │   ├── remote/                  # Remote API (HTTP, sockets)
│   │   └── cache/                   # In-memory caching
│   ├── mapper/                      # DTO ↔ Domain Entity transformations
│   └── repository/                  # Repository implementations
├── presentation/                    # Presentation layer (UI + State)
│   ├── screen/                      # Composable UI components
│   ├── viewmodel/                   # ViewModel state holders
│   ├── event/                       # UI events (sealed classes)
│   └── state/                       # UI state (data classes)
└── navigation/                      # Feature-specific navigation routing
```

**`:core` module package structure** (shared foundation):

```kotlin
com.company.product.core/
├── di/                              # Global DI configuration & shared bindings
├── base/                            # Foundation types (Result, Either, BaseViewModel, etc.)
├── ui/                              # Shared UI components & theming
│   ├── component/                   # Reusable Composables
│   ├── theme/                       # Theme, colors, typography
│   └── extension/                   # Extension functions for UI
├── util/                            # General utilities (non-domain)
│   ├── extension/                   # Kotlin extension functions
│   ├── formatter/                   # Data formatting utilities
│   └── validator/                   # Input validation logic
└── network/                         # HTTP client setup, interceptors
```

**`:app` module package structure** (application entry point):

```kotlin
com.company.product/
├── MainActivity.kt                  # Application entry point
└── navigation/                      # Global navigation graph & routing
```

### 1.2 Inter-Feature Communication: Gateway Pattern (Strategy + DI)

To prevent circular dependencies and maintain a clear, scalable structure, all communication between feature modules must occur via "gateways"—interfaces declared in the `:core` module and implemented in the owning feature module. This approach leverages the Strategy pattern and the DI engine (Koin) for runtime resolution.

**Pattern:**

1. **Declare the gateway interface in `:core`** (e.g., `com.company.product.core.gateway.UserGateway`).
2. **Feature module implements the interface** (e.g., `:feature-user` provides `UserGatewayImpl`).
3. **Register the implementation in the feature's DI module** (e.g., `single<UserGateway> { UserGatewayImpl(...) }`).
4. **Consumers depend only on the interface** (imported from `:core`), never on the implementation or feature module.
5. **Koin resolves the dependency** at runtime, wiring the correct implementation.

**Example:**

// In :core

```kotlin
// core/gateway/UserGateway.kt
interface UserGateway {
    fun getUserProfile(userId: String): UserProfile
    fun observeUserChanges(): Flow<UserProfile>
}
```

// In :feature-user

```kotlin
// feature-user/data/UserGatewayImpl.kt
class UserGatewayImpl(...) : UserGateway { ... }

// feature-user/di/FeatureUserModule.kt
val featureUserModule = module {
    single<UserGateway> { UserGatewayImpl(/* ... */) }
}
```

// In any other feature

```kotlin
class SomeViewModel(userGateway: UserGateway) : ViewModel() { ... }
```

**Rules:**

- Never import or reference code from another feature module directly.
- All cross-feature contracts must be declared in `:core` as interfaces (gateways).
- Only the owning feature provides the implementation and registers it in DI.
- Consumers depend on the interface and let DI resolve the implementation.
- This pattern ensures compile-time safety, testability, and no circular dependencies.

**Project-level module structure** (Gradle modules):

```kotlin
/
├── :app                            # Application layer (entry point)
├── :core                           # Shared foundation module
├── :feature-authentication         # Feature module (isolated)
├── :feature-payment                # Feature module (isolated)
├── :feature-location               # Feature module (isolated)
└── :feature-[name]                 # Additional features as needed
```

### 1.3 Package & Module Naming Rules

**Module-level naming (Gradle)**:

- Feature modules: `:feature-[name]` (lowercase, kebab-case)
  - `✅ :feature-authentication`, `:feature-payment`, `:feature-location`
  - `❌ :feature_authentication`, `:featureAuthentication`, `:FeatureAuthentication`
- Shared foundation: `:core` (single, reusable module)
- Application entry point: `:app`
- Feature modules should be **fully isolated** with their own `di/`, `domain/`, `data/`, and `presentation/` packages

**Package-level naming**:

- Use **reverse DNS** notation within modules: `com.company.product.authentication`
- Feature names should be **semantic** and **singular**: `authentication`, `payment`, `location` (not `authentications`, `payments`)
- Avoid single-letter or cryptic abbreviations: use `configuration` not `cfg`, use `converter` not `cvt`
- Platform-specific packages should indicate their layer: `di`, `data`, `domain`, `presentation`
- Utility packages must be **minimal and focused**: create `formatter` or `validator`, not generic `utils`
- `:core` module packages should reflect their purpose: `base`, `ui`, `util`, `network` (never mix feature-specific logic in `:core`)

### 1.4 Multi-Module Dependency Flow & Isolation

**Module dependency graph** (unidirectional):

```kotlin
:app (entry point)
  ↓
:feature-authentication, :feature-payment, :feature-location
  ↓
:core (foundation)
```

- Feature modules **must not depend on each other** (avoid circular dependencies)
- All feature modules **may depend on** `:core` (one-way dependency)
- `:core` **must not depend on any feature module** (maintain independence)
- `:app` may depend on feature modules for navigation graph assembly
- Use **inter-module communication** via shared interfaces in `:core` (e.g., event buses, navigation contracts)

**Package nesting rules within a module**:

Maximum package depth should be **4 levels**:

```kotlin
✅ com.company.product.authentication.presentation.viewmodel
❌ com.company.product.authentication.presentation.viewmodel.impl.temp.staging
```

**Cross-module package references** (anti-patterns):

```kotlin
❌ Importing from :feature-payment in :feature-authentication
❌ Referencing feature-specific models from :core
✅ Using common interfaces or sealed classes from :core
✅ Using event-driven communication between modules via :core
```

---

## 2. Naming Conventions

### 2.1 Class & Interface Naming

| Category | Convention | Example |
|----------|-----------|---------|
| **Classes** | PascalCase, descriptive nouns | `UserRepository`, `LocationProvider`, `FareCalculator` |
| **Interfaces** | PascalCase, often ending in `-able`, `-or`, `-er` | `Serializable`, `Repository`, `Listener`, `Observer` |
| **Exceptions** | PascalCase, ending with `Exception` | `NetworkException`, `ValidationException`, `CacheExpiredException` |
| **Sealed Classes** | PascalCase, suffix with domain meaning | `Result`, `UiEvent`, `DataError` |
| **Enums** | PascalCase, enumerate states/constants | `ExecutionMode`, `RideStatus`, `PaymentMethod` |

**Anti-patterns to avoid:**

```kotlin
❌ class UsersRepositoryImpl        // "Impl" suffix is redundant
❌ class IUserRepository           // Hungarian notation
❌ class user_repository           // snake_case for classes
❌ class UserRepositoryHelper      // "Helper" is too vague
```

### 2.2 Function & Property Naming

| Category | Convention | Example |
|----------|-----------|---------|
| **Functions** | camelCase, verb-first (imperative) | `calculateFare()`, `fetchTariffs()`, `validateEmail()` |
| **Boolean Functions** | `is`, `has`, `can`, `should` prefix | `isValid()`, `hasPermission()`, `canExecute()` |
| **Properties** | camelCase, noun-based | `currentFare`, `numberOfPassengers`, `isLoading` |
| **Boolean Properties** | `is`, `has`, `can`, `should` prefix | `isRideActive`, `hasError`, `canRetry` |
| **Constants** | UPPER_SNAKE_CASE | `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_MS` |
| **Private Properties** | camelCase with leading underscore | `_mutableState`, `_internalCache` |

**Anti-patterns:**

```kotlin
❌ fun getUserData()               // Ambiguous: verb doesn't indicate async/blocking
❌ fun get_user_data()             // snake_case for functions
❌ val user_name                   // snake_case for properties
❌ val IS_VALID                    // ALL_CAPS for mutable properties
❌ fun onCreate()                  // Android lifecycle methods are exceptions
```

### 2.3 File Naming

| Type | Convention | Example |
|------|-----------|---------|
| **Top-level Functions** | PascalCase matching primary export | `CalculateFareUseCase.kt` |
| **Extension Functions** | Suffixed with `Ext` (optional) | `StringExtensions.kt`, `FlowExtensions.kt` |
| **Data Classes** | PascalCase | `UserProfile.kt`, `LocationPoint.kt` |
| **Sealed Classes** | PascalCase | `Result.kt`, `NavigationEvent.kt` |
| **Mappers** | `SourceDestinationMapper` | `UserDtoMapper.kt`, `LocationToPointMapper.kt` |

**One public type per file** (with exceptions for closely related types):

```kotlin
✅ UserRepository.kt          // Single interface
✅ Result.kt                  // Sealed class + implementations (Ok, Err)
❌ User.kt                    // Too generic; use UserProfile.kt or User.kt based on domain
```

### 2.4 Visibility & Encapsulation

To improve encapsulation and reduce the public surface area of modules, prefer `internal` visibility for classes and top-level declarations by default. Make types `public` only when they are part of the module boundary or API that other modules legitimately consume.

Guidelines:

- Default: mark implementation classes, helpers, mappers, adapters, and non-shared ViewModels/Composables as `internal`.
- Public only for:
  - Module boundary contracts (interfaces in `:core` or explicit API surfaces).
  - Domain models that are intentionally shared across modules (document why they are public).
  - Types consumed by framework reflection or annotation processors where `internal` would break usage.
- Prefer `internal constructor` for data classes you need to control instantiation for, exposing factory functions when appropriate.
- For interfaces, keep the interface `public` in `:core` and implementations `internal` in the feature module.
- Tests: unit tests in the same module can access `internal` types directly; for cross-module test visibility avoid breaking encapsulation—consider `@VisibleForTesting` alternatives sparingly.

Examples:

```kotlin
// internal implementation, not visible outside the module
internal class TariffMapper internal constructor(private val config: Config) {
    fun map(dto: PriceConfigDto): Tariff = ...
}

// public API declared in core, implementation internal to feature
public interface UserGateway {
    fun getUser(id: String): Result<AppError, UserProfile>
}

internal class UserGatewayImpl(...) : UserGateway { ... }

// internal data class with controlled instantiation
data class InternalCacheEntry internal constructor(
    val key: String,
    val value: Any
)

fun createCacheEntry(key: String, value: Any): InternalCacheEntry =
    InternalCacheEntry(key, value)
```

Rationale:

- Minimizes accidental coupling between modules.
- Makes refactors safer (implementation can change without breaking dependents).
- Encourages clear module API design: public surface = intentional API.

Use this rule consistently, with documented exceptions where cross-module visibility is required.

---

## 3. Kotlin Idioms & Code Style

### 3.1 Immutability & Mutability

**Prefer immutable data structures**:

```kotlin
// ✅ Immutable by default
data class User(
    val id: Long,
    val name: String,
    val email: String
)

// ✅ Use val for properties
val users: List<User> = repository.fetchUsers()

// ❌ Mutable collections (only when absolutely necessary)
val usersMutable: MutableList<User> = mutableListOf()
```

**State management in ViewModels**:

```kotlin
// ✅ Private MutableStateFlow, public StateFlow
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// ❌ Exposing mutable state
val uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
```

### 3.2 Scope Functions & Null Safety

**Appropriate scope function usage**:

```kotlin
// ✅ apply() for object configuration
val config = Config().apply {
    timeout = 5000
    retryCount = 3
}

// ✅ let() for null checks or transformations
user?.let { currentUser ->
    displayUserProfile(currentUser)
}

// ✅ run() for complex object initialization
val result = run {
    val client = HttpClient()
    val config = client.getConfig()
    config.validate()
}

// ❌ Excessive scope function chaining
user?.let { it.name.uppercase() }?.let { println(it) }  // Use if-let instead
```

**Null-safety best practices**:

```kotlin
// ✅ Use Elvis operator for defaults
val name = user?.name ?: "Unknown"

// ✅ Use when for multiple checks
val status = when {
    user == null -> Status.NO_USER
    user.isActive -> Status.ACTIVE
    else -> Status.INACTIVE
}

// ❌ Double bangs (null pointer mines)
val name = user!!.name  // Only use when 100% certain
```

### 3.3 Lambda & Higher-Order Functions

```kotlin
// ✅ Use trailing lambda syntax
viewModel.loadData(
    onSuccess = { data ->
        updateUI(data)
    },
    onError = { error ->
        showError(error)
    }
)

// ✅ Use it for single-parameter lambdas
users.map { it.name }

// ✅ Use when for conditional flows
result.fold(
    ifError = { error -> handleError(error) },
    ifOk = { data -> processData(data) }
)

// ❌ Unnecessary intermediate variables
val names = users.map { user -> user.name }  // Use: users.map { it.name }
```

### 3.4 Extension Functions

**Use extensions for domain logic, not utility dumping grounds**:

```kotlin
// ✅ Domain-specific extension
fun LocationPoint.distanceToInKm(other: LocationPoint): Double {
    // Haversine formula
}

// ✅ UI convenience extension
fun String.toCapitalizedWords(): String = 
    this.split(" ").joinToString(" ") { it.capitalize() }

// ✅ Type-safe builder pattern
fun <T> mutableListBuilder(builder: MutableList<T>.() -> Unit): List<T> {
    return mutableListOf<T>().apply(builder)
}

// ❌ Generic utilities that belong in named functions
fun <T> Any.cast(): T = this as T  // Too generic; use explicit casting

// ❌ Poorly scoped extensions
fun String.process() = ...  // Vague; what processing?
```

### 3.5 Destructuring

```kotlin
// ✅ Destructuring in loops and lambdas
val pairs = listOf("a" to 1, "b" to 2)
pairs.forEach { (key, value) ->
    println("$key = $value")
}

// ✅ Destructuring with data classes
data class Point(val x: Int, val y: Int)
val (x, y) = Point(10, 20)

// ❌ Over-destructuring loses clarity
val (_, _, status) = complexTuple  // Unclear intent
```

### 3.6 Formatting: Trailing Commas

When declaring multiline function parameter lists, argument lists, constructors, or collection literals, prefer adding a trailing comma on the last line. Trailing commas make diffs smaller and cleaner, simplify adding/removing lines, and play well with automatic formatters.

Guidelines:

- Use a trailing comma for multiline parameter lists and argument lists:

```kotlin
fun createUser(
    id: Long,
    name: String,
    email: String,
) { /* ... */ }

callApi(
    url,
    timeout = 5_000,
    retry = true,
)
```

- Use trailing commas for multiline constructors and collection literals:

```kotlin
val users = listOf(
    user1,
    user2,
    user3,
)

data class Config(
    val host: String,
    val port: Int,
)
```

- Do not use trailing commas on single-line declarations.

Rationale:

- Minimizes noisy git diffs when adding or removing lines.
- Works well with Kotlin formatters and IDE reformatting.

Use this convention consistently; linters or formatter rules can be added later to enforce it.

---

## 4. Compose UI Conventions

### 4.1 Composable Naming & Organization

```kotlin
// ✅ Composable function names describe UI content
@Composable
fun UserProfileCard(user: User, onClick: () -> Unit) {
    // Implementation
}

// ✅ Preview function suffixed with "Preview"
@Preview
@Composable
fun UserProfileCardPreview() {
    UserProfileCard(
        user = User.mock(),
        onClick = {}
    )
}

// ✅ Group related Composables in same file
// UserProfileCard.kt
@Composable
fun UserProfileCard(user: User) { }

@Composable
private fun UserInfoSection(user: User) { }

// ❌ Generic function names
fun MyScreen() { }  // What does this screen show?

// ❌ Mixing Composables and business logic
@Composable
fun UserList(viewModel: UserViewModel) {
    LaunchedEffect(Unit) {
        viewModel.fetchUsers()  // Logic belongs in ViewModel
    }
}
```

### 4.2 State Management in Composables

```kotlin
// ✅ Local UI state with remember
@Composable
fun SearchBox(onSearch: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    
    TextField(
        value = query,
        onValueChange = { query = it },
        onSearch = { onSearch(query) }
    )
}

// ✅ Reactive state from ViewModel
@Composable
fun UserListScreen(viewModel: UserViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (val state = uiState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Success -> UserList(state.data)
        is UiState.Error -> ErrorMessage(state.error)
    }
}

// ❌ Performing side effects without LaunchedEffect
@Composable
fun UserDetail(userId: String, viewModel: UserViewModel) {
    viewModel.loadUser(userId)  // Called every recomposition!
}

// ✅ Correct pattern
@Composable
fun UserDetail(userId: String, viewModel: UserViewModel) {
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }
}
```

### 4.3 Parameter Ordering in Composables

```kotlin
// ✅ Canonical parameter order
@Composable
fun UserCard(
    user: User,                    // Data models first
    isSelected: Boolean = false,   // Configuration/flags
    modifier: Modifier = Modifier, // Layout control
    onClick: () -> Unit = {},      // Callbacks
    onDelete: (User) -> Unit = {}  // More callbacks
) {
    // Implementation
}

// ✅ All callbacks at end, Modifier second-to-last
@Composable
fun ComplexScreen(
    data: ScreenData,
    state: UiState,
    modifier: Modifier = Modifier,
    onNavigate: (Route) -> Unit,
    onRetry: () -> Unit
)

// ❌ Callbacks mixed throughout
@Composable
fun BadOrder(
    onClick: () -> Unit,
    user: User,
    modifier: Modifier = Modifier
)
```

---

## 5. Repository & Data Layer Conventions

### 5.1 Repository Pattern

```kotlin
// ✅ Domain layer repository interface
interface UserRepository {
    suspend fun getUser(id: Long): Result<DomainError, User>
    suspend fun saveUser(user: User): Result<DomainError, Unit>
    fun observeUser(id: Long): Flow<Result<DomainError, User>>
}

// ✅ Data layer implementation
class UserRepositoryImpl(
    private val remoteDataSource: RemoteUserDataSource,
    private val localDataSource: LocalUserDataSource,
    private val mapper: UserDtoMapper
) : UserRepository {
    override suspend fun getUser(id: Long): Result<DomainError, User> =
        withContext(Dispatchers.IO) {
            localDataSource.getUser(id)
                ?.let { Ok(mapper.toDomain(it)) }
                ?: remoteDataSource.getUser(id)
                    .mapCatching { mapper.toDomain(it) }
                    .map { user ->
                        localDataSource.saveUser(user)
                        user
                    }
                    .mapError { DomainError.NetworkError(it.message) }
        }
}

// ❌ Mixing domain and data concerns
class UserRepository {
    fun getUser(id: Long): User? = db.query(...)  // No error handling
}
```

### 5.2 Data Source Separation

```kotlin
// ✅ Clear data source contracts
interface RemoteUserDataSource {
    suspend fun getUser(id: Long): Result<NetworkError, UserDto>
}

interface LocalUserDataSource {
    suspend fun getUser(id: Long): UserEntity?
    suspend fun saveUser(user: UserEntity)
}

// ✅ Single Responsibility
class HttpUserDataSource(private val api: UserApi) : RemoteUserDataSource {
    override suspend fun getUser(id: Long) = try {
        Ok(api.fetchUser(id))
    } catch (e: HttpException) {
        Err(NetworkError.HttpError(e.code))
    }
}

// ❌ Data source doing multiple things
class UserDataSource {
    fun getFromHttp() { }
    fun getFromDb() { }
    fun cacheLocally() { }  // Too many responsibilities
    fun validateEmail() { }  // Not a data concern
}
```

### 5.3 Mapper Conventions

```kotlin
// ✅ Explicit, descriptive mappers
interface Mapper<FROM, TO> {
    fun map(from: FROM): TO
}

class UserDtoToDomainMapper : Mapper<UserDto, User> {
    override fun map(from: UserDto): User = User(
        id = from.userId,
        name = from.fullName,
        email = from.emailAddress
    )
}

// ✅ Batch operations
fun <FROM, TO> List<FROM>.mapWith(mapper: Mapper<FROM, TO>): List<TO> =
    this.map(mapper::map)

// ✅ Named extension functions for clarity
fun UserDto.toDomain(): User = User(
    id = this.userId,
    name = this.fullName
)

// ❌ Generic toDomain() with no context
data class UserDto {
    fun toDomain() = ...  // Unclear what domain model
}

// ❌ Mappers with business logic
class UserMapper {
    fun map(dto: UserDto): User {
        // Validates email format  ← Not mapper responsibility
        // Calculates subscription status ← Business logic
    }
}
```

---

## 6. Use Case & Domain Logic Conventions

### 6.1 Use Case Pattern

```kotlin
// ✅ Single-responsibility use cases
class CalculateFareUseCase(
    private val tariffRepository: TariffRepository
) {
    suspend operator fun invoke(
        distance: Double,
        duration: Long
    ): Result<FareError, Fare> = withContext(Dispatchers.Default) {
        tariffRepository.getTariffs()
            .flatMap { tariff ->
                calculateFare(distance, duration, tariff)
            }
    }
    
    private fun calculateFare(
        distance: Double,
        duration: Long,
        tariff: Tariff
    ): Result<FareError, Fare> = Ok(Fare(
        distance = distance * tariff.pricePerKm,
        time = duration * tariff.pricePerSecond
    ))
}

// ✅ Use cases accept data models, return domain models
class FetchUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: Long): Result<Error, User> =
        repository.getUser(userId)
}

// ❌ Generic use cases
class ExecuteUseCase { }  // What does it execute?

// ❌ Use cases with multiple responsibilities
class OrderUseCase(private val repo: OrderRepository) {
    suspend fun createOrder() { }      // Order creation
    suspend fun processPayment() { }   // Payment processing
    suspend fun sendNotification() { } // Notifications
}

// ❌ Use cases performing UI operations
class UserUseCase {
    fun getUser() {
        showLoadingDialog()  // UI responsibility
    }
}
```

### 6.2 Domain Model Design

```kotlin
// ✅ Immutable, validated domain models
@Immutable
data class User(
    val id: Long,
    val name: String,
    val email: String
) {
    init {
        require(id > 0) { "ID must be positive" }
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(email.contains("@")) { "Invalid email format" }
    }
}

// ✅ Value objects for type safety
@JvmInline
value class UserId(val value: Long) {
    init {
        require(value > 0) { "User ID must be positive" }
    }
}

// ✅ Sealed classes for domain states
sealed class PaymentStatus {
    object Pending : PaymentStatus()
    data class Processing(val transactionId: String) : PaymentStatus()
    data class Completed(val receiptId: String) : PaymentStatus()
    data class Failed(val reason: String) : PaymentStatus()
}

// ❌ Mutable domain models
class User {
    var name: String = ""  // Mutable field
    var email: String? = null  // Nullable
}

// ❌ Domain models with getters/setters
data class Order(
    private var id: Long
) {
    fun getId() = id
    fun setId(newId: Long) { id = newId }
}
```

---

## 7. ViewModel & State Management Conventions

### 7.1 ViewModel Structure

```kotlin
// ✅ Clear separation of public and private state
class UserListViewModel(
    private val fetchUsersUseCase: FetchUsersUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()
    
    init {
        loadUsers()
    }
    
    fun onRetryClicked() {
        loadUsers()
    }
    
    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchUsersUseCase()
                .fold(
                    ifError = { error ->
                        _uiState.value = UiState.Error(error.message)
                    },
                    ifOk = { users ->
                        _uiState.value = UiState.Success(users)
                    }
                )
        }
    }
}

// ✅ Sealed class for UI states
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: List<User>) : UiState()
    data class Error(val message: String) : UiState()
}

// ✅ Sealed class for UI events
sealed class UiEvent {
    data class Navigate(val route: String) : UiEvent()
    data class ShowMessage(val text: String) : UiEvent()
}

// ❌ Multiple unrelated flows
class BadViewModel : ViewModel() {
    val isLoading = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)
    val users = MutableStateFlow<List<User>>(emptyList())
    val isRefreshing = MutableStateFlow(false)
    // No unified state
}

// ❌ Exposing mutable state
class BadViewModel : ViewModel() {
    val uiState = MutableStateFlow(UiState())  // Caller can modify!
}
```

### 7.2 Lifecycle-Safe Coroutines

```kotlin
// ✅ Use viewModelScope for automatic cancellation
class UserViewModel : ViewModel() {
    fun loadUser(id: Long) {
        viewModelScope.launch {
            val user = repository.getUser(id)
            _state.value = user
        }
    }
}

// ✅ Structured concurrency with proper supervision
fun loadMultipleUsers(ids: List<Long>) {
    viewModelScope.launch {
        val users = ids.map { id ->
            async { repository.getUser(id) }
        }.awaitAll()
        _state.value = users
    }
}

// ✅ Use LaunchedEffect for reactive triggers
@Composable
fun UserScreen(userId: Long, viewModel: UserViewModel) {
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }
}

// ❌ GlobalScope (memory leak risk)
GlobalScope.launch {  // Runs indefinitely, not tied to lifecycle
    loadData()
}

// ❌ Launching without scope function
lifecycleScope.launch { }  // Wrong lifecycle scope
```

---

## 8. Error Handling Conventions

### 8.1 Either-Based Error Handling

```kotlin
// ✅ Either-based Result type
sealed interface Result<out E, out A> {
    data class Ok<A>(val value: A) : Result<Nothing, A>
    data class Err<E>(val error: E) : Result<E, Nothing>
    
    inline fun <B> map(f: (A) -> B): Result<E, B> =
        when (this) {
            is Ok -> Ok(f(value))
            is Err -> this
        }
    
    inline fun <B> flatMap(f: (A) -> Result<E, B>): Result<E, B> =
        when (this) {
            is Ok -> f(value)
            is Err -> this
        }
    
    inline fun <R> fold(
        ifErr: (E) -> R,
        ifOk: (A) -> R
    ): R = when (this) {
        is Ok -> ifOk(value)
        is Err -> ifErr(error)
    }
}

// ✅ Sealed error types
sealed class DomainError {
    object NotFound : DomainError()
    data class ValidationError(val field: String) : DomainError()
    data class NetworkError(val message: String) : DomainError()
}

// ✅ Explicit error propagation
suspend fun fetchUser(id: Long): Result<DomainError, User> =
    repository.getUser(id).flatMap { user ->
        if (user.isValid()) Ok(user) else Err(DomainError.ValidationError("user"))
    }

// ✅ Error handling in ViewModel
viewModelScope.launch {
    useCase()
        .fold(
            ifErr = { error ->
                _uiState.value = UiState.Error(error.displayMessage())
            },
            ifOk = { data ->
                _uiState.value = UiState.Success(data)
            }
        )
}

// ❌ Nullable returns (silent failures)
fun getUser(id: Long): User? = repository.find(id)

// ❌ Exception-based error handling (loses type safety)
try {
    val user = repository.getUser(id)
} catch (e: Exception) {
    // Broad exception catch
}

// ❌ Error swallowing
user.fold(
    ifErr = { },  // Silent failure
    ifOk = { processUser(it) }
)
```

### 8.2 Exception Handling Boundaries

```kotlin
// ✅ Catch exceptions at data source layer
class HttpUserDataSource(private val api: UserApi) {
    suspend fun getUser(id: Long): Result<NetworkError, UserDto> =
        try {
            Ok(api.fetchUser(id))
        } catch (e: HttpException) {
            Err(NetworkError.HttpError(e.code, e.message))
        } catch (e: IOException) {
            Err(NetworkError.ConnectionError(e.message))
        }
}

// ✅ Never throw in domain layer
class CalculateFareUseCase {
    suspend operator fun invoke(distance: Double): Result<FareError, Fare> {
        // Never throw; always return Result
    }
}

// ❌ Throwing exceptions from use cases
class BadUseCase {
    fun execute() {
        if (data == null) throw IllegalStateException("No data")  // Type-unsafe
    }
}

// ❌ Unchecked exception propagation
repository.getUser(id)  // May throw, caller unaware
```

---

## 9. Testing Conventions

### 9.1 Unit Test Structure (Given-When-Then)

```kotlin
class CalculateFareUseCaseTest {
    private lateinit var useCase: CalculateFareUseCase
    private val repository: TariffRepository = mock()
    
    @Before
    fun setup() {
        useCase = CalculateFareUseCase(repository)
    }
    
    @Test
    fun `given valid distance and tariff, when calculating fare, then returns correct amount`() {
        val distance = 10.0
        val tariff = Tariff(pricePerKm = 0.5)
        coEvery { repository.getTariffs() } returns Ok(tariff)
        
        val result = runBlocking { useCase(distance) }
        
        assertTrue(result is Ok)
        assertEquals(5.0, (result as Ok).value.amount)
    }
    
    @Test
    fun `given network failure, when fetching tariff, then returns error`() {
        coEvery { repository.getTariffs() } returns Err(NetworkError())
        
        val result = runBlocking { useCase(10.0) }
        
        assertTrue(result is Err)
        assertTrue(result is Result.Err<*>)
    }
}

// ❌ Arrange-Act-Assert comments (use blank lines)
@Test
fun testCalculateFare() {
    // Arrange
    val distance = 10.0
    
    // Act
    val result = useCase(distance)
    
    // Assert
    assertEquals(5.0, result)
}

// ❌ Poorly named tests
@Test
fun test1() { }

@Test
fun testFareCalculation() { }  // Too vague
```

### 9.2 Test Doubles: Fakes Over Mocks

#### Preferred Strategy: Use Fakes for Most Tests

Fakes are simplified, working implementations of interfaces used for testing. They should be the default choice for test doubles because they:

- Provide clearer, more readable tests
- Avoid brittle mock verification (which tests implementation details)
- Catch real bugs in business logic
- Make it easier to understand what the tested code actually does

#### Example: Fake Repository

```kotlin
// Fake (preferred)
class FakeTariffRepository : TariffRepository {
    private var tariffsToReturn: Result<Tariff> = Ok(Tariff(0.2, 0.01))
    
    fun setTariffsToReturn(result: Result<Tariff>) {
        tariffsToReturn = result
    }
    
    override suspend fun getTariffs(): Result<Tariff> = tariffsToReturn
}

// Usage in test
@Test
fun `given tariff loaded, when calculating fare, then returns correct total`() = runTest {
    val fakeRepository = FakeTariffRepository()
    fakeRepository.setTariffsToReturn(Ok(Tariff(0.2, 0.01)))
    val useCase = CalculateFareUseCase(fakeRepository)
    
    val result = useCase(routePoints, 0)
    
    assertEquals(25.0, result.value.totalPrice)
}
```

#### When to Use Mocks: Behavior Verification

Use mocks (via Mockito-kotlin) when you need to verify that a method was called with specific arguments. This is useful for:

- Verifying that a persistence operation occurred
- Ensuring side effects were triggered (e.g., logging, analytics)
- Testing integration points where the behavior of external code matters

```kotlin
// Mock (for behavior verification only)
@Test
fun `given ride completed, when finishing, then saves to database`() = runTest {
    val mockRepository = mock<RideRepository>()
    val useCase = SaveRideUseCase(mockRepository)
    
    useCase(rideData)
    
    verify(mockRepository).save(rideData)  // Behavior verification
}
```

#### Anti-Pattern: Over-Mocking

```kotlin
// ❌ Avoid: Using mocks when fakes would be clearer
@Test
fun testCalculateFare() {
    val mockTariffRepo = mock<TariffRepository>()
    val mockLocationRepo = mock<LocationRepository>()
    val mockAnalytics = mock<AnalyticsService>()
    whenever(mockTariffRepo.getTariffs()).thenReturn(Ok(Tariff(0.2, 0.01)))
    whenever(mockLocationRepo.getRoute()).thenReturn(Ok(route))
    whenever(mockAnalytics.logEvent(any())).then { }
    
    // Hard to understand what's being tested; brittle verification setup
}
```

### 9.3 Test Naming Convention

```kotlin
// ✅ Descriptive test names
fun `given empty user list, when displaying screen, then shows empty state`()
fun `given network error, when retrying, then propagates error to user`()
fun `given valid credentials, when logging in, then updates session state`()

// ✅ Parameterized tests with meaningful names
@ParameterizedTest
@CsvSource(
    "0, false",
    "1, true",
    "-1, false"
)
fun `given id parameter, when validating, then returns expected result`(
    id: Long,
    expected: Boolean
) { }

// ❌ Generic test names
fun testFare() { }
fun testUser() { }
fun testAPI() { }

// ❌ Mixed testing patterns in single class
fun userRepositoryTest() { }
fun viewModelTest() { }
fun integrationTest() { }  // Use separate test classes
```

### 9.3 Mock & Assertion Patterns

```kotlin
// ✅ Use Mockito-kotlin for behavior verification
private val repository: TariffRepository = mock()

// ✅ Specify return values with Result type
whenever(repository.getTariffs()).thenReturn(Ok(mockTariff))

// ✅ Verify behavior, not implementation details
verify(repository).getTariffs()
verify(repository, times(1)).saveUser(any())

// ✅ Use assertions from AssertJ or Truth for readability
assertThat(result).isInstanceOf(Ok::class.java)
assertThat(users).hasSize(3)

// ❌ Over-mocking (use fakes instead)
@Test
fun test() {
    val mock1 = mock<Service1>()
    val mock2 = mock<Service2>()
    val mock3 = mock<Service3>()
    // Difficult to understand what's being tested; use fakes instead
}

// ❌ Testing implementation details
verify { repository.internalCache.clear() }

// ❌ Assertions that don't clarify intent
assertTrue(result != null)
assertFalse(items.isEmpty())
```

---

## 10. Coroutines & Async Patterns

### 10.1 Structured Concurrency

```kotlin
// ✅ Use viewModelScope for automatic cancellation
class UserViewModel : ViewModel() {
    fun loadUser(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.getUser(id)
            }
        }
    }
}

// ✅ Use supervisorScope for partial failure tolerance
suspend fun loadMultipleUsers(ids: List<Long>): List<User> =
    supervisorScope {
        ids.map { id ->
            async { repository.getUser(id) }
        }.mapNotNull { it.awaitOrNull() }
    }

// ✅ Specify dispatchers explicitly
withContext(Dispatchers.IO) {
    // Network or disk I/O
}

withContext(Dispatchers.Default) {
    // CPU-intensive computation
}

// ✅ Use Flow for reactive streams
fun observeUser(id: Long): Flow<User> = flow {
    while (currentCoroutineContext().isActive) {
        emit(repository.getUser(id))
        delay(5000)
    }
}

// ❌ GlobalScope (no lifecycle management)
GlobalScope.launch { loadData() }

// ❌ CoroutineScope without lifecycle awareness
val scope = CoroutineScope(Job())

// ❌ Mixing async/await with try-catch without proper cancellation
try {
    val result = async { /* suspended */ }.await()
} catch (e: CancellationException) {
    throw e  // Must re-throw cancellation
}
```

### 10.2 Flow Patterns

```kotlin
// ✅ Cold flows for on-demand execution
fun getUsers(): Flow<List<User>> = flow {
    emit(repository.getAllUsers())
}

// ✅ Hot flows (StateFlow, SharedFlow) for shared state
private val _userUpdates = MutableSharedFlow<User>()
val userUpdates: SharedFlow<User> = _userUpdates.asSharedFlow()

// ✅ Proper error handling in Flow
fun getUserStream(id: Long): Flow<Result<Error, User>> = flow {
    emit(repository.getUser(id))
}.catch { e ->
    emit(Err(Error.NetworkError(e.message)))
}

// ✅ Use transform for complex operations
fun processUsers(users: Flow<User>): Flow<String> =
    users.transform { user ->
        emit(user.name.uppercase())
    }

// ❌ Blocking operations in Flow
fun badFlow() = flow {
    val result = Thread.sleep(1000)  // Blocks coroutine!
}

// ❌ Uncaught exceptions in Flow
fun failingFlow() = flow {
    emit(repository.getUser(1))
    throw IOException("Network error")  // Not handled
}
```

---

## 11. Anti-Patterns & Code Smells

### 11.1 Naming Anti-Patterns

| Anti-Pattern | Issue | Fix |
|--------------|-------|-----|
| `Helper`, `Util`, `Manager` | Vague responsibility | Use domain-specific names (`UserProvider`, `FareCalculator`) |
| Single-letter variables | Unclear intent | `i` → `index`, `u` → `user` |
| Boolean prefix inconsistency | Confusing API | Choose one: `is*` / `has*` / `can*` consistently |
| `Impl` suffix | Redundant | Just use the concrete class name without suffix |
| Generic acronyms | Loses meaning | `DTO` → `UserDTO`, `API` → `RestAPI` or just avoid |

### 11.2 Code Quality Anti-Patterns

| Anti-Pattern | Issue | Impact |
|--------------|-------|--------|
| **Deep nesting** | Hard to follow | Extract to separate functions |
| **Long parameter lists** | Confusing API | Use data classes or builders |
| **Mutable shared state** | Thread-safety issues | Use immutable data + StateFlow |
| **Silent failures** | Debugging nightmare | Return explicit `Result` types |
| **Leaky abstractions** | Implementation details exposed | Repository should abstract data source details |
| **Over-engineering** | Unnecessary complexity | YAGNI: You Aren't Gonna Need It |

### 11.3 Concurrency Anti-Patterns

```kotlin
// ❌ Blocking on coroutine thread
runBlocking {  // Blocks main thread!
    val user = repository.getUser(id)
}

// ❌ Launch without handling result
viewModelScope.launch {
    repository.saveUser(user)
    // Result never captured or verified
}

// ❌ Creating new scopes improperly
CoroutineScope(Job()).launch {
    // Scope not tied to lifecycle; memory leak
}

// ❌ Mixing callbacks with coroutines
fun getUser(callback: (User) -> Unit) {
    viewModelScope.launch {
        callback(repository.getUser(id))  // Awkward mix
    }
}
```

---

## 12. Documentation Conventions

### 12.1 KDoc Standards

```kotlin
// ✅ Clear KDoc for public APIs
/**
 * Calculates taxi fare based on distance and duration.
 *
 * @param distance Distance traveled in kilometers
 * @param duration Duration of ride in seconds
 * @return [Result] containing [Fare] on success or [FareError] on failure
 * @throws IllegalArgumentException if distance or duration is negative
 *
 * Example usage:
 * ```kotlin
 * val result = calculateFareUseCase(10.0, 300)
 * result.fold(
 *     ifErr = { error -> showError(error) },
 *     ifOk = { fare -> displayFare(fare) }
 * )
 * ```
 */
suspend operator fun invoke(
    distance: Double,
    duration: Long
): Result<FareError, Fare>

// ✅ Type-specific documentation
/**
 * Represents a calculated taxi fare breakdown.
 *
 * @property baseFare Initial charge
 * @property distanceFare Charge based on distance
 * @property timeFare Charge based on time
 * @property total Sum of all charges
 */
data class Fare(
    val baseFare: Double,
    val distanceFare: Double,
    val timeFare: Double,
    val total: Double
)

// ❌ Obvious documentation (noise)
/** Gets the user */
fun getUser() { }

// ❌ Outdated comments
/** TODO: Fix this bug in 2020 */  // It's 2025!
```

### 12.2 Code Comments

```kotlin
// ✅ Explain "why", not "what"
// Use Haversine formula for accurate earth-surface distance
fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    // Implementation
}

// ✅ Mark temporary solutions
// FIXME: Replace with proper caching mechanism (tracked in JIRA-123)
val cache = mutableMapOf<String, User>()

// ✅ Highlight non-obvious behavior
// Intentionally using Int.MAX_VALUE as sentinel value to avoid Optional wrapper
val timeout = Int.MAX_VALUE

// ❌ Redundant comments
val name = "John"  // Set name to John
var count = 0  // Initialize count

// ❌ Commented-out code (use version control instead)
// val oldUser = repository.getUser(id)
// val newUser = oldUser.copy(name = "Jane")
```

---

## 13. Performance Conventions

### 13.1 Memory Efficiency

```kotlin
// ✅ Use value classes for type-safe wrappers without allocation
@JvmInline
value class UserId(val value: Long)

// ✅ Reuse instances where appropriate
object EmptyUiState : UiState()

// ✅ Use sequences for large collections
users.asSequence()
    .filter { it.isActive }
    .map { it.name }
    .take(10)
    .toList()

// ❌ Creating intermediate lists
users.filter { it.isActive }.map { it.name }.take(10)  // Multiple copies

// ❌ Unnecessary object allocation in loops
for (i in list.indices) {
    val item = Item()  // New object every iteration
    process(item)
}
```

### 13.2 Computational Efficiency

```kotlin
// ✅ Offload heavy work to appropriate dispatcher
viewModelScope.launch {
    withContext(Dispatchers.Default) {
        val result = expensiveCalculation()
        withContext(Dispatchers.Main) {
            updateUI(result)
        }
    }
}

// ✅ Use lazy initialization for expensive resources
private val expensiveCache by lazy {
    buildExpensiveCache()
}

// ❌ CPU-intensive work on Main thread
viewModelScope.launch {  // Defaults to Dispatchers.Main
    val result = expensiveCalculation()  // Blocks UI!
}

// ❌ Unnecessary repeated computation
override fun equals(other: Any?): Boolean {
    val hash = this.hashCode()  // Recompute unnecessarily
    return hash == other.hashCode()
}
```

---

## 14. References & Further Reading

- **Kotlin Coding Conventions**: <https://kotlinlang.org/docs/coding-conventions.html>
- **Effective Kotlin**: Item-based best practices guide
- **Android Architecture Components**: <https://developer.android.com/guide/architecture>
- **Coroutines Best Practices**: <https://kotlinlang.org/docs/coroutines-best-practices.html>
- **Jetpack Compose Principles**: <https://developer.android.com/jetpack/compose/principles>
- **Clean Code by Robert C. Martin**: Foundational software design principles
- **SOLID Principles**: <https://en.wikipedia.org/wiki/SOLID>

---

**Document Version**: 1.0  
**Last Updated**: November 18, 2025  
**Target Kotlin Version**: 1.9.0+  
**Minimum Android API**: 24
