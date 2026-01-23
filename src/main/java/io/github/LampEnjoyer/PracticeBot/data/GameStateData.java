package io.github.LampEnjoyer.PracticeBot.data;

public class GameStateData {

    private final String fen;
    private final boolean whiteToMove;
    private final boolean inCheck;
    private final boolean checkMate;

    public GameStateData(String fen, boolean whiteToMove, boolean inCheck, boolean checkMate){
        this.fen = fen;
        this.whiteToMove = whiteToMove;
        this.inCheck = inCheck;
        this.checkMate = checkMate;
    }

    public String getFen() { return fen; }
    public boolean isWhiteToMove() { return whiteToMove ; }
    public boolean isInCheck(){return inCheck;}
    public boolean isCheckMate(){ return checkMate; }
}
