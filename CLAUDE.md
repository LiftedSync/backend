# Claude Instructions for sync-backend

## Testing Requirements

When implementing new features that need tests or modifying existing code:

1. **Create new tests** for any new functionality
2. **Review existing tests** for necessary adjustments when modifying code
3. **Run `./gradlew test`** to verify all tests pass before completing

## Test Structure

- `src/test/kotlin/com/lifted/services/` - Unit tests for services
- `src/test/kotlin/com/lifted/dto/` - Unit tests for DTOs and parsing
- `src/test/kotlin/com/lifted/routes/` - Integration tests for endpoints

## Test Frameworks

- **Kotlin Test** - Assertions and test annotations
- **Ktor Test Host** - HTTP and WebSocket integration testing
- **MockK** - Mocking dependencies
