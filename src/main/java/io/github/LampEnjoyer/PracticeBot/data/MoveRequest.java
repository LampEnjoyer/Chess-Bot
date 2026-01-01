package io.github.LampEnjoyer.PracticeBot.data;

import io.github.LampEnjoyer.PracticeBot.engine.Move;

public class MoveRequest {
    private String move;

    public MoveRequest(String move) {
        this.move = move;
    }

    public String getMove() {
        return move;
    }

    public void setMove(String move) {
        this.move = move;
    }
}