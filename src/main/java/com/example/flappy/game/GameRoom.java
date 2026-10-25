package com.example.flappy.game;

import java.util.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class GameRoom {
    static final int WIDTH = 800, HEIGHT = 500;
    static final double BIRD_X = 150, BIRD_RADIUS = 13, PIPE_WIDTH = 65, GAP = 155;
    final String code;
    final Map<String, Player> players = new LinkedHashMap<>();
    final List<Pipe> pipes = new ArrayList<>();
    final Random random = new Random();
    boolean started = false;
    long lastPipeAt = 0;

    GameRoom(String code) {
        this.code = code;
        addPipe(850);
    }

    synchronized Player addPlayer(String id, String name, WebSocketSession session) {
        if (!players.isEmpty() && players.values().stream().noneMatch(p -> p.alive)) {
            resetRound();
        }
        Player p = new Player(id, name, session);
        players.put(id, p);
        if (players.size() >= 1) started = true;
        return p;
    }

    synchronized void resetRound() {
        for (Player player : players.values()) {
            player.y = 220;
            player.velocityY = 0;
            player.score = 0;
            player.alive = true;
        }
        pipes.clear();
        addPipe(850);
        started = true;
    }

    synchronized void removePlayer(String id) {
        players.remove(id);
    }

    synchronized void flap(String id) {
        Player p = players.get(id);
        if (p != null && p.alive) p.velocityY = -7.4;
    }

    private void addPipe(double x) {
        double margin = 95;
        double gapY = margin + random.nextDouble() * (HEIGHT - 2 * margin - GAP);
        pipes.add(new Pipe(x, gapY));
    }

    synchronized void tick() {
        if (players.isEmpty()) return;
        for (Player p : players.values()) {
            if (!p.alive) continue;
            p.velocityY += 0.42;
            p.y += p.velocityY;
            if (p.y < BIRD_RADIUS || p.y + BIRD_RADIUS > HEIGHT - 20) p.alive = false;
        }

        for (Pipe pipe : pipes) {
            pipe.x -= 3.2;
            for (Player p : players.values()) {
                if (!p.alive) continue;
                boolean overlapsX = BIRD_X + BIRD_RADIUS > pipe.x &&
                        BIRD_X - BIRD_RADIUS < pipe.x + PIPE_WIDTH;
                if (overlapsX && (p.y - BIRD_RADIUS < pipe.gapY ||
                        p.y + BIRD_RADIUS > pipe.gapY + GAP)) {
                    p.alive = false;
                }
                if (!pipe.counted && pipe.x + PIPE_WIDTH < BIRD_X - BIRD_RADIUS) {
                    p.score++;
                }
            }
            if (pipe.x + PIPE_WIDTH < BIRD_X - BIRD_RADIUS) pipe.counted = true;
        }

        pipes.removeIf(pipe -> pipe.x + PIPE_WIDTH < 0);
        if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < WIDTH - 260) addPipe(WIDTH + 40);
        broadcastState();
        if (players.values().stream().noneMatch(p -> p.alive)) {
            // Keep the final scoreboard visible until players reconnect or leave.
            started = false;
        }
    }

    synchronized void broadcastState() {
        StringBuilder json = new StringBuilder();
        json.append("{\"type\":\"state\",\"room\":\"").append(escape(code))
            .append("\",\"started\":").append(started).append(",\"width\":").append(WIDTH)
            .append(",\"height\":").append(HEIGHT).append(",\"pipes\":[");
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            if (i > 0) json.append(',');
            json.append("{\"x\":").append(round(pipe.x)).append(",\"gapY\":").append(round(pipe.gapY)).append('}');
        }
        json.append("],\"players\":[");
        int i = 0;
        for (Player p : players.values()) {
            if (i++ > 0) json.append(',');
            json.append("{\"id\":\"").append(escape(p.id)).append("\",\"name\":\"").append(escape(p.name))
                .append("\",\"y\":").append(round(p.y)).append(",\"score\":").append(p.score)
                .append(",\"alive\":").append(p.alive).append('}');
        }
        json.append("]}");
        TextMessage message = new TextMessage(json.toString());
        for (Player p : players.values()) {
            try {
                if (p.session.isOpen()) p.session.sendMessage(message);
            } catch (Exception ignored) {
                // A disconnected client is removed by the WebSocket lifecycle callback.
            }
        }
    }

    private static double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
