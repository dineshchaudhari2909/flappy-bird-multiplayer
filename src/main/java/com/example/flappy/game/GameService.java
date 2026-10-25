package com.example.flappy.game;

import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public GameRoom room(String code) {
        String normalized = normalize(code);
        return rooms.computeIfAbsent(normalized, GameRoom::new);
    }

    public GameRoom existingRoom(String code) {
        return rooms.get(normalize(code));
    }

    public synchronized GameRoom joinRoom(String code, String playerId, String name, WebSocketSession session) {
        GameRoom room = room(code);
        synchronized (room) {
            boolean nameTaken = room.players.values().stream()
                    .anyMatch(player -> player.name.trim().toLowerCase(Locale.ROOT)
                            .equals(name.trim().toLowerCase(Locale.ROOT)));
            if (nameTaken) {
                throw new IllegalArgumentException("That bird name is already taken in this room.");
            }
            room.addPlayer(playerId, name, session);
        }
        return room;
    }

    public synchronized void removePlayer(String code, String playerId) {
        String normalized = normalize(code);
        GameRoom room = rooms.get(normalized);
        if (room == null) return;
        synchronized (room) {
            room.removePlayer(playerId);
            if (room.players.isEmpty()) rooms.remove(normalized, room);
        }
    }

    @Scheduled(fixedRate = 33)
    public void gameLoop() {
        rooms.values().forEach(GameRoom::tick);
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) return "LOBBY";
        return code.trim().toUpperCase().replaceAll("[^A-Z0-9_-]", "");
    }
}
