package io.github.LampEnjoyer.PracticeBot.engine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Opening {
    private List<String> games;
    private Map<Long , Map<Move, Integer>> openingBook;
    private final String fileName = "Games.txt";

    public Opening () throws IOException {
        openingBook = new HashMap<>();
        games = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        while (true) {
            String curr = br.readLine();
            if (curr != null) {
                games.add(curr);
            } else {
                br.close();
                br = new BufferedReader(new FileReader(fileName));
                break;
            }
        }
    }

    public List<String> getGames() {
        return games;
    }

    public void initialize(){
        GameState gameState = new GameState();
        for(String s : games){
            String[] moveList = s.split(" ");
            System.out.println(s);
            gameState = new GameState();
            for(int i = 0; i < Math.min(10, moveList.length); i++){
                String move = moveList[i];
                Move m = findMove(move, gameState);
                long hash = gameState.getHash();

                openingBook.computeIfAbsent(hash, k -> new HashMap<>());
                Map<Move, Integer> map = openingBook.get(hash);

                map.put(m, map.getOrDefault(m, 0) + 1);

                gameState.makeMove(m);
            }

        }
    }

    public Move getMove(Long hash){
        return null;
    }

    public Move findMove(String notation, GameState gameState){
        List<Move> list = gameState.getAllPossibleMoves(gameState.getTurn());
        int [] parsedMove = parseAlgebraic(notation, gameState.getTurn());
        int [] pieceBoard = gameState.getBoard().getPieceBoard();

        for(Move m : list){
          //  System.out.println(m);
            int toIndex = m.getToLocation();
            int fromIndex = m.getFromLocation();
            if(toIndex != parsedMove[3]){
                continue;
            }
            if( parsedMove[4] != -1 && m.getPromotion() != parsedMove[4]){
                continue;
            }
            if(pieceBoard[fromIndex] != parsedMove[0]){
                continue;
            }
            if(parsedMove[5] != -1 && (m.getMoveType() != 1 && m.getMoveType() != 3) ){
                continue;
            }
            //Disambiguation file (if present)
            if (parsedMove[1] != -1) {
                int fromFile = fromIndex % 8;
                if (fromFile != parsedMove[1]) {
                    continue;
                }
            }
            //Disambiguation rank (if present)
            if (parsedMove[2] != -1) {
                int fromRank = fromIndex / 8;
                if (fromRank != parsedMove[2]) {
                    continue;
                }
            }
            return m;
//            if(isRightMove(m, parsedMove, pieceBoard)){
//                return m;
//            }
        }
        System.out.println(gameState.getFenNotation());
        throw new IllegalArgumentException("Invalid move: " + notation + " Parsed Move: " + Arrays.toString(parsedMove));
    }

    public int[] parseAlgebraic(String notation, boolean isWhite){
        int [] arr = new int[6]; // piece, disambigFile, disambigRank, toIndex ,promoPiece, capture
        Arrays.fill(arr, -1);
        notation = notation.replaceAll("[+#]", "");
        if(notation.equals("O-O-O")){ //Queen side castle
            arr[0] = isWhite ? 5 : 11;
            arr[3] = isWhite ? 2 : 58;
            return arr;
        }else if(notation.equals("O-O")){ //King side castle
            arr[0] = isWhite ? 5 : 11;
            arr[3] = isWhite ? 6 : 62;
            return arr;
        }
        if(notation.contains("x")){ //Capture flag
            arr[5] = 1;
            notation = notation.replace("x", "");
        }
        if(notation.contains("=")){ //Promotion
            arr[0] = isWhite ? 0 : 6;
            int piece = getPiece(notation.charAt(notation.length()-1));
            arr[4] = isWhite ? piece : piece + 6;
            notation = notation.replace("=", "");
        }
        int len = notation.length();
        char firstChar = notation.charAt(0); //Piece type
        if(Character.isUpperCase(firstChar)) {
            int piece = isWhite ? getPiece(firstChar) : getPiece(firstChar) + 6;
            arr[0] = piece;
            notation = notation.substring(1);
            len = notation.length();
        }else{
            arr[0] = isWhite ? 0 : 6;
        }

        if(len > 2){
            char disambigChar = notation.charAt(0);
            if (Character.isDigit(disambigChar)) { // rank
                arr[2] = disambigChar - '1';
            } else { // file
                arr[1] = disambigChar - 'a';
            }
            notation = notation.substring(1);
            len = notation.length();
        }

        //destination
        int file = notation.charAt(len - 2) - 'a';
        int rank = notation.charAt(len - 1) - '1';
        int index = rank * 8 + file;
        arr[3] = index;

        return arr;
    }

    private int getPiece(char c){
        c = Character.toUpperCase(c);
        return switch (c) {
            case 'P' -> 0;
            case 'N' -> 1;
            case 'B' -> 2;
            case 'R' -> 3;
            case 'Q' -> 4;
            case 'K' -> 5;
            default -> -1;
        };
    }

    public boolean isRightMove(Move m, int[]parsedMove, int[] pieceBoard){ //Another layer of abstraction just for testing purposes
        int toIndex = m.getToLocation();
        int fromIndex = m.getFromLocation();
        if(toIndex != parsedMove[3]){
            System.out.println("Wrong Index");
            return false;
        }
        if(parsedMove[4] != -1 && m.getPromotion() != parsedMove[4]){
            System.out.println("Wrong promo");
            return false;

        }
        if(pieceBoard[fromIndex] != parsedMove[0]){
            System.out.println("Wrong piece");
            return false;
        }
        if(parsedMove[5] != -1 && ( m.getMoveType() != 1 && m.getMoveType() != 3) ){
            System.out.println("Wrong capture type");
            return false;
        }
        //Disambiguation file (if present)
        if (parsedMove[1] != -1) {
            int fromFile = fromIndex % 8;
            if (fromFile != parsedMove[1]) {
                System.out.println("Wrong disambig file");
                return false;
            }
        }
        //Disambiguation rank (if present)
        if (parsedMove[2] != -1) {
            int fromRank = fromIndex / 8;
            if (fromRank != parsedMove[2]) {
                System.out.println("Wrong disambig rank");
                return false;
            }
        }
        return true;
    }

    public void testGame(String s){
        GameState gameState = new GameState();
        String [] moveList = s.split(" ");
        for(String move : moveList){
            int [] pieceBoard = gameState.getBoard().getPieceBoard();
            try {
                System.out.println(move);
                for(Move w : gameState.getAllPossibleMoves(gameState.getTurn())){
                    System.out.println(w);
                }
                gameState.getBoard().printBoard();
                System.out.println("---------------");

                Move m = findMove(move, gameState);
                gameState.makeMove(m);
            }  catch (IllegalArgumentException e){
                System.out.println(gameState.getFenNotation() + " " + move);
                return;
            }
        }
    }


}
