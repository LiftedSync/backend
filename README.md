# LiftedSync Backend

WebSocket server for synchronized video playback across YouTube, Netflix, Amazon Prime and Crunchyroll.

## Overview

This backend manages real-time synchronization between multiple users watching the same video. It handles room creation, user management, and broadcasts video state updates (play, pause, seek) to all connected clients.

## Technology Stack

- **Kotlin** - Primary language
- **Ktor 3.x** - Asynchronous web framework
- **WebSockets** - Real-time bidirectional communication
- **kotlinx.serialization** - JSON serialization
- **Netty** - Server engine
- **Koin** - Dependency injection
- **Gradle** - Build tool with Kotlin DSL

## Project Structure

```
src/main/kotlin/com/lifted/
├── Application.kt          # Entry point, module configuration
├── di/
│   └── Koin.kt             # Koin setup and dependency definitions
├── plugin/
│   ├── Auth.kt             # Admin authentication setup
│   ├── HTTP.kt             # CORS configuration
│   ├── Sockets.kt          # WebSocket configuration
│   └── Serialization.kt    # JSON serialization setup
├── endpoint/
│   ├── AdminRoutes.kt      # Admin endpoint handlers
│   ├── StatusRoutes.kt     # Health and version endpoints
│   └── RoomRoutes.kt       # WebSocket endpoint handlers
├── data/
│   ├── dto/
│   │   ├── RoomCountDto.kt # Room count (admin API)
│   │   ├── RoomDto.kt      # Serializable room representation (admin API)
│   │   └── UserDto.kt      # Serializable user representation
│   ├── model/
│   │   ├── Platform.kt     # Supported streaming platforms + matchesUrl()
│   │   ├── VideoState.kt   # Playing / paused state
│   │   ├── Room.kt         # Room domain model + toDto() mapper
│   │   └── User.kt         # User domain model + toDto() mapper
│   └── websocket/
│       └── Messages.kt     # WebSocket messages and MessageParser
└── service/
    ├── NavigateResult.kt   # Result of RoomService.navigate()
    └── RoomService.kt      # In-memory rooms + rules: create, join, leave, update, navigate

src/test/kotlin/com/lifted/  # Mirrors src/main
├── di/
│   └── KoinTest.kt         # Verifies the Koin module
├── data/
│   ├── model/
│   │   └── PlatformTest.kt # Unit tests for platform URL matching
│   └── websocket/
│       └── MessageParserTest.kt # Unit tests for message parsing
├── endpoint/
│   ├── AdminRoutesTest.kt  # Integration tests for admin endpoints
│   ├── StatusRoutesTest.kt # Integration tests for HTTP endpoints
│   └── RoomRoutesTest.kt   # Integration tests for WebSocket
└── service/
    └── RoomServiceTest.kt  # Unit tests for room rules
```

### Layers

```
endpoint  →  service
```

- **Endpoint** - Transport only: parses WebSocket messages, builds and sends/broadcasts responses, maps models to DTOs.
- **Service** (`RoomService`) - Business rules: room code generation, case-insensitive joining, deleting empty rooms, URL validation, resetting playback on navigate. Holds the rooms in memory; operations on a room are atomic. Works with models and returns models (or a result type like `NavigateResult`).

### Data Layer Conventions

- **Model** (`data/model`) - Domain classes used internally (e.g. `User` holds its `WebSocketSession`).
- **DTO** (`data/dto`) - `@Serializable` classes sent over the wire.
- One class per file.

### Dependency Injection

[Koin](https://insert-koin.io/) is installed in `Application.module()` via `configureKoin()` (`di/Koin.kt`). Services are defined in `appModule` and injected into routes:

```kotlin
fun Route.adminRoutes() {
    val roomService by inject<RoomService>()
    // ...
}
```

To add a service, create it as a class in `service/` and register it in `appModule` with `single { ... }`; constructor dependencies are resolved with `get()`.

## Development

```bash
# Run the server in development mode
./gradlew run

# The server starts at ws://localhost:8080/ws
```

## Production Build

```bash
# Build a fat JAR with all dependencies
./gradlew clean buildFatJar

# Run the production JAR
java -jar build/libs/sync-backend-all.jar
```

## Deployment

The backend is built locally and deployed as a pre-built JAR to the server via `deploy.ps1`. This avoids compiling on the resource-constrained server (1GB RAM, 1 CPU).

```powershell
# Deploy from the backend directory
.\deploy.ps1
```

The script does the following:
1. Builds the fat JAR locally (`clean buildFatJar`)
2. Stops running containers on the server
3. Cleans the remote folder and restores `.env`
4. Uploads `app.jar`, `Dockerfile`, `Caddyfile`, `docker-compose.yml`
5. Runs `docker compose up -d --build` on the server

### Server Details

- **Production URL:** `wss://liftedgang.de/api/ws`  (/api comes from Caddy reverse proxy)
- **Server path:** `/opt/sync-backend`

### Docker Commands

```bash
cd /opt/sync-backend

# Start services
docker compose up -d --build

# Stop services
docker compose down

# Restart services
docker compose restart

# Stop a specific container
docker stop <container-name>

# Remove all unused images, containers, networks and build cache
docker system prune -a

# Show resource usage of running containers (CPU, memory, etc.)
docker stats --no-stream
```

### Logs

```bash
docker compose logs -f           # All services
docker compose logs -f backend   # Backend only
docker compose logs -f caddy     # Caddy only
```

### Configuration

Edit `src/main/resources/application.yaml`:

```yaml
ktor:
    deployment:
        port: 8080
    development: false  # Set to false in production
```

## WebSocket Protocol

### Endpoint

- **Development:** `ws://localhost:8080/ws`
- **Production:** `wss://liftedgang.de/api/ws`

### Client -> Server Messages

| Type           | Payload                | Description                  |
|----------------|------------------------|------------------------------|
| `create_room`  | `userName`, `platform`, `currentTime` (optional) | Create a new room |
| `join_room`    | `roomId`, `userName`   | Join an existing room        |
| `video_update` | `state`, `currentTime` | Broadcast video state change |
| `heartbeat`    | (none)                 | Keep connection alive        |
| `leave_room`   | (none)                 | Leave the current room       |
| `navigate`     | `url`                  | Navigate the room to a new video (URL must match room platform) |

### Server -> Client Messages

| Type           | Payload                                               | Description                          |
|----------------|-------------------------------------------------------|--------------------------------------|
| `room_created` | `roomId`                                              | Room was created                     |
| `room_joined`  | `roomId`, `platform`, `state`, `currentTime`, `users` | Successfully joined room             |
| `sync_update`  | `state`, `currentTime`, `fromUserId`                  | Video state update from another user |
| `user_joined`  | `userName`, `userCount`, `users`                      | User joined the room                 |
| `user_left`    | `userName`, `userCount`, `users`                      | User left the room                   |
| `navigate_update` | `url`, `fromUserId`                                | Room navigated to a new video        |
| `error`        | `code`, `message`                                     | Error occurred                       |

### Example Messages

Create a room:
```json
{"type": "create_room", "userName": "Alice", "platform": "youtube"}
```

Join a room:
```json
{"type": "join_room", "roomId": "ABC123", "userName": "Bob"}
```

Send video update:
```json
{"type": "video_update", "state": "playing", "currentTime": 125.5}
```

## Gradle Tasks

| Task                  | Description                            |
|-----------------------|----------------------------------------|
| `./gradlew run`       | Run the server                         |
| `./gradlew build`     | Build everything                       |
| `./gradlew buildFatJar` | Build executable JAR with dependencies |
| `./gradlew test`      | Run tests                              |
