package io.github.LampEnjoyer.PracticeBot.service;
import io.github.LampEnjoyer.PracticeBot.data.GameStateData;
import io.github.LampEnjoyer.PracticeBot.engine.*;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private final GameState gameState;

    public GameService() {
        this.gameState = new GameState();
    }

    public GameService(Board board){
        this.gameState = new GameState(board);
    }

    public GameState getGameState(){
        return gameState;
    }

    public boolean makeMoveUCI(String uciMove) { //UCI format
        if(gameState.isGameOver()){
            return false;
        }
        Move move = gameState.uciToMove(uciMove);
        if(move == null){
            return false;
        }
        return MoveValidator.validateMove(gameState, move); //if true make move
    }

    public String getBestMoveUCI() {
        Move move = Evaluator.getBestMove(gameState,6,Integer.MIN_VALUE, Integer.MAX_VALUE, gameState.getTurn(),null,true ).getMove();
        return gameState.moveToUCIFormat(move);
    }
    public void resetGame() {
        gameState.resetGame();
    }



    public GameStateData getGameStateData(){
        String fen = gameState.getFenNotation();
        boolean turn = gameState.getTurn();
        boolean isCheck = MoveValidator.isKingInCheck(gameState, turn); //Test this later
        boolean isCheckMate = gameState.isGameOver();
        return new GameStateData(fen,turn,isCheck,isCheckMate);
    }

   /* public void loadFen(String s){
        game.loadFen(s);
    } */




}