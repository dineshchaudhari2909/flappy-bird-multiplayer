package com.example.flappy.game;

class Pipe {
    double x;
    final double gapY;
    boolean counted = false;

    Pipe(double x, double gapY) {
        this.x = x;
        this.gapY = gapY;
    }
}
