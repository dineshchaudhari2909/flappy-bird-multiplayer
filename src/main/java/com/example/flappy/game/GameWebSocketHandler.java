package com.example.flappy.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final GameService gameService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    public GameWebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> query = queryParams(session.getUri());
        String roomCode = query.getOrDefault("room", "LOBBY");
        String name = query.getOrDefault("name", "Bird").trim();
        if (name.isBlank()) name = "Bird";
        if (name.length() > 18) name = name.substring(0, 18);
        String id = UUID.randomUUID().toString().substring(0, 8);
        GameRoom room;
        try {
            room = gameService.joinRoom(roomCode, id, name, session);
        } catch (IllegalArgumentException e) {
            session.close(new CloseStatus(4001, "Bird name already taken in this room"));
            return;
        }
        connections.put(session.getId(), new Connection(room.code, id));
        synchronized (room) {
            room.broadcastState();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Connection c = connections.get(session.getId());
        if (c == null) return;
        JsonNode data = mapper.readTree(message.getPayload());
        if ("flap".equals(data.path("type").asText())) {
            GameRoom room = gameService.existingRoom(c.roomCode);
            if (room != null) room.flap(c.playerId);
        } else if ("restart".equals(data.path("type").asText())) {
            GameRoom room = gameService.existingRoom(c.roomCode);
            if (room == null) return;
            synchronized (room) {
                Player p = room.players.get(c.playerId);
                if (p != null && !p.alive && room.players.values().stream().noneMatch(other -> other.alive)) {
                    room.resetRound();
                    room.broadcastState();
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Connection c = connections.remove(session.getId());
        if (c != null) {
            gameService.removePlayer(c.roomCode, c.playerId);
        }
    }

    private Map<String, String> queryParams(URI uri) {
        Map<String, String> result = new java.util.HashMap<>();
        if (uri == null || uri.getQuery() == null) return result;
        for (String pair : uri.getQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                result.put(java.net.URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8),
                           java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    private record Connection(String roomCode, String playerId) {}
}
