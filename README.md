# Flappy Squad — Java Multiplayer MVP

A small real-time multiplayer Flappy Bird-style game built with Java 21, Spring Boot, WebSockets, and a plain HTML Canvas frontend. No database or frontend build tooling is required.

## MVP features
- Room codes: players using the same room code share one game world.
- Real-time state broadcast over WebSockets.
- Server-side gravity, flap input, pipe movement, collision detection, and scoring.
- Live player list and scores.
- Restart when every player is out.
- Browser controls: Space, Up Arrow, click, or tap.

## Requirements
- JDK 21+
- Maven 3.9+ (or use your IDE's Maven integration)

## Run locally
From this folder:

```bash
mvn spring-boot:run
```

Open http://localhost:8080. Enter a name and room code, then click **Join room**. Open another browser tab at the same URL and use the same room code to test multiplayer.

## Package as a JAR

```bash
mvn clean package
java -jar target/flappy-bird-multiplayer-0.0.1-SNAPSHOT.jar
```

## Project structure

```text
src/main/java/com/example/flappy/
  FlappyApplication.java
  config/WebSocketConfig.java
  game/
    GameService.java
    GameRoom.java
    GameWebSocketHandler.java
    Player.java
    Pipe.java
    SchedulerConfig.java
src/main/resources/
  application.properties
  static/index.html
```

## WebSocket protocol

Endpoint: `ws://localhost:8080/game?name=Player&room=BIRDS`

Client messages:
```json
{"type":"flap"}
{"type":"restart"}
```

Server messages are `state` snapshots with `players` and `pipes`.

## Important MVP limitations
- In-memory rooms: a server restart clears rooms; this is a single-server prototype.
- No login, matchmaking, persistent leaderboard, rate limiting, or anti-cheat protections.
- WebSocket origin is open for local development; restrict allowed origins before public deployment.
- For production, add validation/rate limits, automated tests, room lifecycle cleanup, TLS, and persistence (e.g. PostgreSQL/Redis).
