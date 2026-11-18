# Claude Instructions for sync-backend

## Testing Requirements

When implementing new features that need tests or modifying existing code:

1. **Create new tests** for any new functionality
2. **Review existing tests** for necessary adjustments when modifying code
3. **Run `./gradlew test`** to verify all tests pass before completing

## Architecture

Package layout under `com.lifted` (see README "Project Structure"):

- `endpoint` - Ktor routes; transport only (parse, build messages, send/broadcast, map to DTOs)
- `service` - Business rules and in-memory state (e.g. `RoomService` holds the rooms); works with models
- `plugin` - Ktor plugin configuration
- `di` - Koin setup (`appModule`); inject in routes via `val x by inject<X>()`
- `data/model` - Domain models
- `data/dto` - `@Serializable` DTOs
- `data/websocket` - WebSocket messages and parser

Conventions:

- One class per file.
- The test tree mirrors the main tree.
- Call direction is `endpoint → service`. No repository layer: the project is small and state lives in memory, so services own their storage. Introduce `data/repository` only if storage becomes external (e.g. Redis, a database).
- Service operations that can fail in more than one way return a sealed result (e.g. `NavigateResult`) instead of throwing; WebSocket handlers map it to an `ErrorMessage`.
- `RoomService` changes a room's users inside `rooms.computeIfPresent(...)` so join and delete-if-empty cannot interleave.

## Dependency Injection (Koin)

- Register new services as `single { ... }` in `appModule` (`di/Koin.kt`); constructor dependencies via `get()`.
- Services are classes, not `object`s, so every test can create a fresh instance.
- Unit tests instantiate classes directly (e.g. `RoomService()`); no Koin needed.
- Integration tests that need to seed state take the instance from Koin and start the app before the first request:
  ```kotlin
  lateinit var roomService: RoomService
  application { module(); roomService = get() }
  startApplication()
  ```

## Test Structure

- `service` - Unit tests for services
- `data/model` - Unit tests for model logic (e.g. `Platform.matchesUrl`)
- `data/websocket` - Unit tests for WebSocket message parsing
- `endpoint` - Integration tests for endpoints
- `di` - Koin module tests

## Test Frameworks

- **Kotlin Test** - Assertions and test annotations
- **Ktor Test Host** - HTTP and WebSocket integration testing
- **MockK** - Mocking dependencies
