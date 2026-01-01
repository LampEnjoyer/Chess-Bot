package io.github.LampEnjoyer.PracticeBot.engine;

import java.io.IOException;
import java.util.Scanner;

public class Game { //OLD FILE USED FOR COMMAND LINE INPUT
    private GameState gameState;
    private Scanner scan;
    private MoveGenerator moveGenerator = new MoveGenerator();
    private MoveValidator.OpeningBook book;

    public Game() {
        this.gameState = new GameState();
        this.scan = new Scanner(System.in);
    }

    public Game(Board board) {
        this.gameState = new GameState(board);
        this.scan = new Scanner(System.in);
    }

    public Game(String fen){
        this.gameState = new GameState(fen);
    }

    public void loadFen(String fen){
        this.gameState = new GameState(fen);
    }

    public void play() {
        while (true) {
            gameState.getBoard().printBoard();
            System.out.println((gameState.getTurn() ? "White" : "Black") + "'s turn");
            if (MoveValidator.isKingInCheck(gameState, !gameState.getTurn())) {
                System.out.println((gameState.getTurn() ? "White" : "Black") + " is in check");
                if (MoveValidator.isCheckMate(gameState)) {
                    System.out.println((gameState.getTurn() ? "White" : "Black") + " is in checkmate");
                    break;
                }
            }
            System.out.println("Enter a move (e.g. : e2e4)");
            String input = scan.nextLine();
            Move move = getPlayerMove(input);
            System.out.println(move);
            if (move == null || !MoveValidator.validateMove(gameState, move)) {
                System.out.println("Invalid choice");
                continue;
            } else {
                System.out.println("Good move");
                System.out.println("Collision: " + gameState.getBoard().isCollision());
                if (gameState.getTurn()) {
                    gameState.incrementMoveCounter();
                }
                System.out.println(gameState.getFenNotation());
            }
        }
    }

    public void playWithBot() {
        while (true) {
            gameState.getBoard().printBoard();
            System.out.println((gameState.getTurn() ? "White" : "Black") + "'s turn");
            if (MoveValidator.isKingInCheck(gameState, !gameState.getTurn())) {
                System.out.println((gameState.getTurn() ? "White" : "Black") + " is in check");
                if (MoveValidator.isCheckMate(gameState)) {
                    System.out.println((gameState.getTurn() ? "White" : "Black") + " is in checkmate");
                    break;
                }
            }
            if (gameState.getTurn()) {
                System.out.println("Enter a move (e.g. : e2e4)");
                String input = scan.nextLine();
                Move move = getPlayerMove(input);
                System.out.println(move);
                if (move == null || !MoveValidator.validateMove(gameState, move)) {
                    System.out.println("Invalid choice");
                    continue;
                } else {
                    System.out.println("Good move");
                    System.out.println("Collision: " + gameState.getBoard().isCollision());
                    System.out.println(gameState.getFenNotation());
                }
            } else {
                Move move;
                if (book.isBookPosition(gameState.getFenNotation())) {
                    System.out.println("here");
                    move = getPlayerMove(book.getMove(gameState.getFenNotation()));
                } else {
                    move = Evaluator.getBestMove(gameState, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, false, null, true).getMove();
                }
                gameState.makeMove(move);
                System.out.println("Good move");
                System.out.println("Collision: " + gameState.getBoard().isCollision());
                System.out.println(gameState.getFenNotation());
                gameState.incrementMoveCounter();
            }
        }
    }

    public Move getPlayerMove(String input) {
        if (input.length() != 4) {
            return null;
        }
        long[] boards = gameState.getBoard().getBitboard();
        int from = chessNotationToIndex(input.substring(0, 2));
        int to = chessNotationToIndex(input.substring(2, 4));
        System.out.println(from + " " + to);
        if (from < 0 || from > 63 || to < 0 || to > 63) {
            return null;
        }
        int movingPiece = -1;
        int capturedPiece = -1;
        for (int i = 0; i < 12; i++) {
            long l = boards[i];
            if ((1L << from & l) != 0) {
                movingPiece = i;
            }
            if ((1L << to & l) != 0) {
                capturedPiece = i;
            }
        }
        System.out.println("Moving piece: " + movingPiece);
        int moveType = 0, promo = 0;
        if (capturedPiece != -1) {
            moveType = 1;
        } else if ((movingPiece == 5 || movingPiece == 11) && Math.abs(from - to) == 2) {
            moveType = 2;
        }
        if (to == gameState.getCurrentEnPassantSquare() && movingPiece == (gameState.getTurn() ? 0 : 6)) {
            moveType = 3;
        }
        if ((to / 8 == 7 && movingPiece == 0) || (to / 8 == 0 && movingPiece == 6)) {
            while (promo <= 0 || promo > 5) {
                System.out.println("Enter promotion piece: ");
                promo = scan.nextInt();
                scan.nextLine();
            }
        }
        return new Move(from, to, moveType, promo);
    }

    private int chessNotationToIndex(String str) {
        int file = str.charAt(0) - 'a';
        int rank = str.charAt(1) - '1';
        return rank * 8 + file;
    }

    public GameState getGameState() {
        return gameState;
    }

    public Move getPlayerMoveUCI(String move) {
        if (move.length() < 4) {
            return null;
        }
        long[] boards = gameState.getBoard().getBitboard();
        int from = chessNotationToIndex(move.substring(0, 2));
        int to = chessNotationToIndex(move.substring(2, 4));
        System.out.println(from + " " + to);
        if (from < 0 || from > 63 || to < 0 || to > 63) {
            return null;
        }
        int movingPiece = -1;
        int capturedPiece = -1;
        for (int i = 0; i < 12; i++) {
            long l = boards[i];
            if ((1L << from & l) != 0) {
                movingPiece = i;
            }
            if ((1L << to & l) != 0) {
                capturedPiece = i;
            }
        }
        int moveType = 0, promo = 0;
        if (capturedPiece != -1) { //Capture
            moveType = 1;
        } else if ((movingPiece == 5 || movingPiece == 11) && Math.abs(from - to) == 2) { //Castle
            moveType = 2;
        }
        if (to == gameState.getCurrentEnPassantSquare() && movingPiece == (gameState.getTurn() ? 0 : 6)) { //En Passant
            moveType = 3;
        }
        if (move.length() == 5) {
            char c = move.charAt(4);
            promo = switch (c) {
                case 'n' -> 1;
                case 'b' -> 2;
                case 'r' -> 3;
                case 'q' -> 4;
                default -> promo;
            };
        }
        return new Move(from, to, moveType, promo);
    }

    public void makeMove(Move move) {
        gameState.makeMove(move);
    }

    public void undoMove() {
        gameState.undoMove();
    }

    public boolean getTurn() {
        return gameState.getTurn();
    }

    public String moveToUCIFormat(Move move) {
        StringBuilder sb = new StringBuilder();
        int from = move.getFromLocation();
        int to = move.getToLocation();
        int promo = move.getPromotion();

        sb.append((char) ('a' + from % 8));       // file
        sb.append((char) ('1' + from / 8));       // rank

        sb.append((char) ('a' + to % 8));
        sb.append((char) ('1' + to / 8));
        if (promo != 0) {
            char p = switch (promo) {
                case 1 -> 'n';
                case 2 -> 'b';
                case 3 -> 'r';
                case 4 -> 'q';
                default -> 0;
            };
            sb.append(p);
        }
        return sb.toString();
    }

    public boolean validFen(String fen){
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

    public void resetGame(){
        gameState = new GameState();
    }

    public void resetGame(Board board){
        gameState = new GameState(board);
    }


}
