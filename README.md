# LiftedSync Backend

WebSocket server for synchronized video playback across YouTube and Crunchyroll.

## Overview

This backend manages real-time synchronization between multiple users watching the same video. It handles room creation, user management, and broadcasts video state updates (play, pause, seek) to all connected clients.

## Technology Stack

- **Kotlin** - Primary language
- **Ktor 3.x** - Asynchronous web framework
- **WebSockets** - Real-time bidirectional communication
- **kotlinx.serialization** - JSON serialization
- **Netty** - Server engine
- **Gradle** - Build tool with Kotlin DSL

## Project Structure

```
src/main/kotlin/com/lifted/
├── Application.kt          # Entry point, module configuration
├── plugins/
│   ├── HTTP.kt             # CORS configuration
│   ├── Sockets.kt          # WebSocket configuration
│   └── Serialization.kt    # JSON serialization setup
├── routes/
│   ├── StatusRoutes.kt     # Health and version endpoints
│   └── RoomRoutes.kt       # WebSocket endpoint handlers
├── models/
│   ├── Enums.kt            # Platform, VideoState enums
│   ├── Room.kt             # Room data class
│   └── User.kt             # User data class
├── dto/
│   └── Messages.kt         # WebSocket message DTOs
└── services/
    └── RoomManager.kt      # In-memory room management

src/test/kotlin/com/lifted/
├── services/
│   └── RoomManagerTest.kt  # Unit tests for room management
├── dto/
│   └── MessageParserTest.kt # Unit tests for message parsing
└── routes/
    ├── StatusRoutesTest.kt # Integration tests for HTTP endpoints
    └── RoomRoutesTest.kt   # Integration tests for WebSocket
```

## Development

```bash
# Run the server in development mode
./gradlew run

# The server starts at ws://localhost:8080/ws
```

## Production Build

```bash
# Build a fat JAR with all dependencies
./gradlew shadowJar

# Run the production JAR
java -jar build/libs/sync-backend-all.jar

# Or set a custom port
PORT=9090 java -jar build/libs/sync-backend-all.jar
```

## Deployment

### Server Details

- **Production URL:** `wss://liftedgang.de/api/ws`
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

# Rebuild and start
docker compose up -d --build
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
        port: ${PORT:8080}
    development: false  # Set to false in production
```

## WebSocket Protocol

### Endpoint

- **Development:** `ws://localhost:8080/ws`
- **Production:** `wss://liftedgang.de/api/ws`

### Client -> Server Messages

| Type           | Payload                | Description                  |
|----------------|------------------------|------------------------------|
| `create_room`  | `userName`, `platform` | Create a new room            |
| `join_room`    | `roomId`, `userName`   | Join an existing room        |
| `video_update` | `state`, `currentTime` | Broadcast video state change |
| `heartbeat`    | (none)                 | Keep connection alive        |
| `leave_room`   | (none)                 | Leave the current room       |

### Server -> Client Messages

| Type           | Payload                                               | Description                          |
|----------------|-------------------------------------------------------|--------------------------------------|
| `room_created` | `roomId`                                              | Room was created                     |
| `room_joined`  | `roomId`, `platform`, `state`, `currentTime`, `users` | Successfully joined room             |
| `sync_update`  | `state`, `currentTime`, `fromUserId`                  | Video state update from another user |
| `user_joined`  | `userName`, `userCount`                               | User joined the room                 |
| `user_left`    | `userName`, `userCount`                               | User left the room                   |
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
| `./gradlew shadowJar` | Build executable JAR with dependencies |
| `./gradlew test`      | Run tests                              |
