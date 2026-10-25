package com.example.flappy.game;

import org.springframework.web.socket.WebSocketSession;

class Player {
    final String id;
    final String name;
    final WebSocketSession session;
    double y = 220;
    double velocityY = 0;
    int score = 0;
    boolean alive = true;
    boolean passedCurrentPipe = false;

    Player(String id, String name, WebSocketSession session) {
        this.id = id;
        this.name = name;
        this.session = session;
    }
}
