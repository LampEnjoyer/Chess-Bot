package io.github.LampEnjoyer.PracticeBot.controller;

import io.github.LampEnjoyer.PracticeBot.data.GameStateData;
import io.github.LampEnjoyer.PracticeBot.data.MoveRequest;
import io.github.LampEnjoyer.PracticeBot.engine.Game;
import io.github.LampEnjoyer.PracticeBot.engine.GameState;
import io.github.LampEnjoyer.PracticeBot.engine.Move;
import io.github.LampEnjoyer.PracticeBot.service.GameService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService){
        this.gameService = gameService;
    }

    @GetMapping("/test-engine")
    public String testEngine() {
        GameState g = new GameState(); // create a new game
        return "Engine initialized! Fen: " + g.getFenNotation();
    }

    @GetMapping("/ping")
    public String ping() {
        return "Server OK!";
    }

    @GetMapping("/status")
    public ResponseEntity<GameStateData> getStatus(){
        return ResponseEntity.ok(gameService.getGameStateData());
    }

    @GetMapping
    public ResponseEntity<GameStateData> getGameState() {
        return ResponseEntity.ok(gameService.getGameStateData());
    }

    @GetMapping("/legal-moves")
    public ResponseEntity<List<MoveRequest>> getLegalMoves(){
        List<MoveRequest> moveRequestList  = new ArrayList<>();
        boolean isWhiteTurn = gameService.getGameState().getTurn();
        for(Move move: gameService.getGameState().getAllPossibleMoves(isWhiteTurn)){
            moveRequestList.add(new MoveRequest(gameService.getGameState().moveToUCIFormat(move)));
        }
        return ResponseEntity.ok(moveRequestList);
    }

    @PostMapping("/new")
    public ResponseEntity<GameStateData> newGame(){
        gameService.resetGame();
        return ResponseEntity.ok(gameService.getGameStateData());
    }

    @PostMapping("/move")
    public ResponseEntity<GameStateData> makeMove(@RequestBody MoveRequest move){
        boolean valid = gameService.makeMoveUCI(move.getMove());
        if(!valid){
            return ResponseEntity.badRequest().build();
        }else{
            return ResponseEntity.ok(gameService.getGameStateData());
        }
    }

    @PostMapping("/ai-move")
    public ResponseEntity<GameStateData> aiMove(){
        if(gameService.getGameStateData().isCheckMate()){
            return ResponseEntity.badRequest().build();
        }
        String uci = gameService.getBestMoveUCI();
        if(gameService.makeMoveUCI(uci)){
            return ResponseEntity.ok(gameService.getGameStateData());
        }else{
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/undo")
    public ResponseEntity<GameStateData> undoMove(){
        if(!gameService.getGameState().peek()){
            return ResponseEntity.badRequest().build();
        }else{
            return ResponseEntity.ok(gameService.getGameStateData());
        }
    }

    @PostMapping("/load-fen")
    public ResponseEntity<GameStateData> loadFen(@RequestBody String fen){
        if(!validFen(fen)){
            return ResponseEntity.badRequest().build();
        }else{
            gameService.getGameState().loadFen(fen);
            return ResponseEntity.ok(gameService.getGameStateData());
        }
    }

    @PostConstruct
    public void init() {
        System.out.println("GameController loaded");
    }

    private boolean validFen(String fen){
        boolean blackKing = false;
        boolean whiteKing = false;
        String[] arr = fen.split(" ");
        if (arr.length == 0) {
            return false; //Empty
        }
        String board = arr[0];
        int numRows = 1;
        int numCols = 0;
        for(Character s : board.toCharArray()){
            if(s == '/'){
                numRows++;
                if(numCols != 8){
                    return false;
                }
                numCols = 0;
            }else if(Character.isAlphabetic(s)){
                if ("PNBRQKpnbrqk".indexOf(s) == -1) { //Only valid pieces
                    return false;
                }
                if(s == 'K'){
                    if(whiteKing){ //2 white Kings
                        return false;
                    } else{
                        whiteKing = true;
                    }
                } else if(s == 'k'){
                    if(blackKing){ //2 black Kings
                        return false;
                    }else{
                        blackKing = true;
                    }
                }
                numCols++;
            }else{
                if (s < '1' || s > '8') return false;
                numCols += (s - '0');
            }
            if (numCols > 8){ //too many columns
                return false;
            }
        }
        return numRows == 8 && numCols == 8 && whiteKing && blackKing;
    }



    
}