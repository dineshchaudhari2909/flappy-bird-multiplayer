# Flappy Squad — Java Multiplayer MVP

A small real-time multiplayer Flappy Bird-style game built with Java 17, Spring Boot, WebSockets, and a plain HTML Canvas frontend. No database or frontend build tooling is required.

## MVP features
- Room codes: players using the same room code share one game world.
- Real-time state broadcast over WebSockets.
- Server-side gravity, flap input, pipe movement, collision detection, and scoring.
- Live player list and scores.
- Restart when every player is out.
- Browser controls: Space, Up Arrow, click, or tap.

## Requirements
- JDK 17+
- Maven 3.9+ (or use your IDE's Maven integration)

## Run locally
From this folder:

```bash
mvn spring-boot:run
```

Open http://localhost:8080. Enter a name and room code, then click **Join room**. Open another browser tab at the same URL and use the same room code to test multiplayer.

## Play from a phone on the same Wi-Fi

1. Start the server on your computer with `mvn spring-boot:run` and leave it running.
2. Find your computer's IPv4 address (`ipconfig` on Windows; use the active Wi-Fi adapter address).
3. Connect your phone to the same Wi-Fi network and open `http://<computer-ip>:8080` in its browser, replacing `<computer-ip>` with that IPv4 address (for example, `http://192.168.1.25:8080`).
4. If the page does not load, allow Java or port 8080 through the computer's firewall on the private network.

The game uses the page's host for its WebSocket connection, so the phone connects back to the computer automatically. The on-screen **FLAP** button and tapping the game canvas work on touch screens. This LAN setup is for devices on the same trusted network; use HTTPS and suitable network security before exposing the server to the internet.

## Deploy on Render

This repository includes a `Dockerfile` and `render.yaml` for a free Render web service in Singapore. In the Render Dashboard, choose **New > Blueprint**, connect this GitHub repository, and apply the Blueprint. Render builds the Java app from the Dockerfile and uses its assigned `PORT`; WebSocket traffic shares the web service port. Free services can spin down after 15 minutes without HTTP requests or WebSocket messages and may take about a minute to wake up.

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
