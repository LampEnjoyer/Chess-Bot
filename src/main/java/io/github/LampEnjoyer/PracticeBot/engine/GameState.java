package io.github.LampEnjoyer.PracticeBot.engine;

import java.util.*;
import java.util.Scanner;

public class GameState {

    private boolean isWhiteTurn = true;
    private boolean hasWon = false;
    private final Board board;
    private final MoveGenerator moveGenerator = new MoveGenerator();
    private Stack<MoveState> moveHistory = new Stack<>();
    private int moveCounter = 1;
    private int halfMoveClock = 0;
    private int castlingRights = 15;
    private int currentEnPassantSquare = -1; //this will hold the place where a pawn can move to enpassant
    private long hash;
    private Scanner scan;



    public GameState(){
        this.board = new Board();
        MoveValidator.generateMagicNumbers();
        hash = Zobrist.computeHash(this);
    }

    public GameState(Board board){
        this.board = board;
        MoveValidator.generateMagicNumbers();
        updateCastlingRights();
        hash = Zobrist.computeHash(this);
    }

    public GameState(String s){
        this.board = new Board(s.substring(0,s.indexOf(' '))); //Setting Boards
        s = s.substring(s.indexOf(' ') + 1);
        String[] arr = s.split(" ");
        changeTurn(arr[0].equals("w"));
        decipherFENCastle(arr[1]);
        if(arr[2].equals("-")){
            currentEnPassantSquare = -1;
        } else{
             currentEnPassantSquare = arr[2].charAt(0) - 'a' + (arr[2].charAt(1) - '0' ) * 8;
        }
        halfMoveClock = Integer.parseInt(arr[3]);
        moveCounter = Integer.parseInt(arr[4]);
        MoveValidator.generateMagicNumbers();
        hash = Zobrist.computeHash(this);
    }

    public void loadFen(String s){
        board.updateBoard((s.substring(0,s.indexOf(' ')))); //Setting Boards
        s = s.substring(s.indexOf(' ') + 1);
        String[] arr = s.split(" ");
        changeTurn(arr[0].equals("w"));
        decipherFENCastle(arr[1]);
        if(arr[2].equals("-")){
            currentEnPassantSquare = -1;
        } else{
            currentEnPassantSquare = arr[2].charAt(0) - 'a' + (arr[2].charAt(1) - '0' ) * 8;
        }
        halfMoveClock = Integer.parseInt(arr[3]);
        moveCounter = Integer.parseInt(arr[4]);
        MoveValidator.generateMagicNumbers();
        hash = Zobrist.computeHash(this);
    }



    public Board getBoard(){
        return board;
    }

    public MoveGenerator getMoveGenerator(){ return moveGenerator;}

    public void decipherFENCastle(String s){
        if(s.equals("-")){
            castlingRights = 0;
        } else{
            castlingRights = 0; //Temp set to 0, because we are going to add up, easier to add up than subtract down
            for(int i = 0; i<s.length(); i++){
                switch (s.charAt(i)){
                    case 'K':
                        castlingRights += 8;
                        break;
                    case 'Q':
                        castlingRights += 4;
                        break;
                    case 'k':
                        castlingRights += 2;
                        break;
                    case 'q':
                        castlingRights += 1;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void play(){
        scan = new Scanner(System.in);
        while(!hasWon){
            System.out.println("Enter a starting index: ");
            String from = scan.nextLine();
            System.out.println("Enter another index: ");
            String to = scan.nextLine();
            Move move = creatingMove(from,to);
            if(move != null){
                makeMove(move);
            }

        }
    }

    public long getWhiteBoard(){
        long [] arr = board.getBitboard();
        long b = arr[0];
        for(int i = 1; i<6; i++){
            b |= arr[i];
        }
        return b;
    }

    public long getPieceBoard(){
        return getBlackBoard() | getWhiteBoard();
    }

    public long getBlackBoard(){
        long [] arr = board.getBitboard();
        long b = arr[6];
        for(int i = 7; i<12; i++){
            b |= arr[i];
        }
        return b;
    }

    public long getEmptySquareBoard(){
        return ~(getWhiteBoard() | getBlackBoard());
    }

    public boolean getTurn(){
        return isWhiteTurn; // 0 = black, 1 = white
    }

    public long getHash(){
        return hash;
    }

    public void setHash(long hash){
        this.hash = hash;
    }

    public void makeMove(Move move){
        int fromIndex = move.getFromLocation();
        int toIndex = move.getToLocation();
        int isWhite = isWhiteTurn ? 0 : 1;
        int movingPiece = -1, capturedPiece = -1;
        long [] boards = board.getBitboard();
        for(int i = 0; i<12; i++){
            long l = boards[i];
            if( (1L << fromIndex & l) != 0){
                movingPiece = i;
            }
            if( (1L << toIndex & l ) != 0){
                capturedPiece = i;
            }
        }
        if(movingPiece == -1) {
            System.err.println("FAILED to find piece for move: " + move);
            System.out.println(board.isCollision());
            throw new RuntimeException("Piece not found!");
        }
        int oldCastling = castlingRights, oldHalfMoveClock = halfMoveClock, oldEnPassant = -1;
        if(move.getMoveType() == 2){ //Castle Logic
            capturedPiece = isWhiteTurn ? 3 : 9; //We're going to make the rook the "captured Piece" to reduce edge case undoing
            boards[movingPiece] |= (1L << toIndex);
            int rookTo, rookFrom;
            boolean kingSide = toIndex % 8 == 6; //Checking if we are doing kingside or queen sidecastle
            if(isWhiteTurn && kingSide) {
                rookTo = 5;
                rookFrom = 7;
            }else if(isWhiteTurn && !kingSide){
                rookFrom = 0;
                rookTo = 3;
            } else if(!isWhiteTurn && kingSide){
                rookFrom = 63;
                rookTo = 61;
            } else{
                rookFrom = 56;
                rookTo = 59;
            }
            //Removing rook from its spot and putting it back in to its new castled spot
            boards[capturedPiece] &= ~(1L << rookFrom);
            hash ^= Zobrist.pieceHash[capturedPiece][rookFrom];
            boards[capturedPiece] |= (1L << rookTo);
            hash ^= Zobrist.pieceHash[capturedPiece][rookTo];
        }
        boards[movingPiece] &= ~(1L << fromIndex); //Removing from original index
        hash ^= Zobrist.pieceHash[movingPiece][fromIndex]; //Initial hash to remove piece from original spot
        if(move.getPromotion() != 0){ //Handling Promotions
            int promoType = move.getPromotion() + isWhite * 6;
            boards[move.getPromotion() + isWhite * 6] |= (1L << toIndex); //basically just jumping boards here
            hash ^= Zobrist.pieceHash[promoType][toIndex];
        }else{
            boards[movingPiece] |= (1L << toIndex); //Adding the new index
            hash ^= Zobrist.pieceHash[movingPiece][toIndex];
        }
        if(capturedPiece != -1 && move.getMoveType() == 1){ //Making sure its not enPassant cause that has different logic
            boards[capturedPiece] &= ~(1L << toIndex); //Removing the captured Piece
            hash ^= Zobrist.pieceHash[capturedPiece][toIndex];
        }
        if(movingPiece % 6 == 0 && Math.abs(toIndex - fromIndex) == 16){ //Setting en Passants
            int diff = (toIndex - fromIndex)/2;
            if(currentEnPassantSquare != -1){ //If we override another passant square
                hash ^= Zobrist.enPassantHash[currentEnPassantSquare % 8];
            }
            oldEnPassant = currentEnPassantSquare;
            setCurrentEnPassantSquare(fromIndex + diff);
            hash ^= Zobrist.enPassantHash[currentEnPassantSquare % 8];
        } else if(move.getMoveType() == 3) { //Handling EnPassant
            capturedPiece = movingPiece == 0 ? 6 : 0; //for EnPassants the capturedPiece has to be the opposite pawn
            hash ^= Zobrist.enPassantHash[currentEnPassantSquare % 8]; //Hash out the old enPassant index
            oldEnPassant = currentEnPassantSquare;
            currentEnPassantSquare = -1;
            int passantIndex = getEnPassantSquare(fromIndex,toIndex);
            boards[capturedPiece] &= ~(1L << passantIndex); //Removing the pawn that got enPassanted
            hash ^= Zobrist.pieceHash[capturedPiece][passantIndex];
        } else{  //we are not setting an enpassant and checking if didnt make an enpassant move
            if(currentEnPassantSquare != -1){
                hash ^= Zobrist.enPassantHash[currentEnPassantSquare % 8];
                oldEnPassant = currentEnPassantSquare;
            }
            currentEnPassantSquare = -1;
        }
        updateCastlingRights(movingPiece, fromIndex, toIndex);
        if(castlingRights != oldCastling){
            hash ^= Zobrist.castleHash[oldCastling];
            hash ^= Zobrist.castleHash[castlingRights];
        }
        if( (movingPiece != 0 && movingPiece != 6) && (move.getMoveType() == 0 || move.getMoveType() == 2)){
            halfMoveClock++;
        } else{
            halfMoveClock = 0;
        }
        capturedPiece %= 6; //Making this color-independent and adjusting for our movestate (sry)
        capturedPiece++;
        MoveState moveState = new MoveState(move, capturedPiece, oldCastling , oldHalfMoveClock , oldEnPassant , isWhite);
        moveHistory.push(moveState);
        isWhiteTurn = !isWhiteTurn;
        hash ^= Zobrist.turnHash;
        moveCounter++;
    }

    public void undoMove(){ //Revert everything back
        MoveState moveState = moveHistory.pop();
        Move move = moveState.getMove();
        if(move == null){
            isWhiteTurn = !isWhiteTurn;
            hash ^= Zobrist.turnHash;
            if(moveState.getOldEnPassantSquare() != -1){
                hash ^= Zobrist.enPassantHash[ moveState.getOldEnPassantSquare() % 8];
            }
            currentEnPassantSquare = moveState.getOldEnPassantSquare();
            moveCounter--;
            halfMoveClock = moveState.getOldMoveCounter();
            castlingRights = moveState.getOldCastlingRights();
            return; //early return statement
        }
        int fromIndex = move.getFromLocation();
        int toIndex = move.getToLocation();
        int movingPiece = 0, capturedPiece = -1;
        int moveType = move.getMoveType();
        int promo = move.getPromotion();
        int isWhite = (getWhiteBoard() & (1L << toIndex) ) != 0 ? 1 : 0;

        long [] boards = board.getBitboard();
        for(int i = 0; i<boards.length; i++){
            if( ((1L << toIndex) & boards[i]) != 0){
                movingPiece = i;
            }
        }

        boards[movingPiece] &= ~(1L << toIndex); //Removing the piece from its new position
        hash ^= Zobrist.pieceHash[movingPiece][toIndex]; //Hashing out the new spot
        if(promo > 0){
            int pawn =  (isWhite^1) * 6;
            boards[pawn] |= (1L << fromIndex); //Flipping here because of poor design choices sighhhhh
            hash ^= Zobrist.pieceHash[pawn][fromIndex]; //Hashing pawn back into its spot
        } else{
            boards[movingPiece] |= (1L << fromIndex); //Bringing the piece back to its original spot
            hash ^= Zobrist.pieceHash[movingPiece][fromIndex]; //Hashing non promo piece back in
        }
        int oldEnPassant = moveState.getOldEnPassantSquare();
        if(moveState.getCapturedPiece() != 0 ) { //making sure that there is a captured piece
            capturedPiece = moveState.getCapturedPiece()-1 + ( (moveState.wasWhiteTurn()^1) * 6); //Finding the type of piece that got captured
            if(moveType == 2){ //Castling
                int rookFrom, rookTo, rookType;
                if(movingPiece == 5 && toIndex == 2){ //White king, Queen side castle
                    rookType = 3;
                    rookFrom = 0;
                    rookTo = 3;
                } else if(movingPiece == 5 && toIndex == 6) { //White KingSide Castle
                   rookType = 3;
                   rookFrom = 7;
                   rookTo = 5;

                } else if(movingPiece == 11 && toIndex == 58){ //Black QueenSide Castle
                    rookType = 9;
                    rookFrom = 56;
                    rookTo = 59;
                } else { //Black KingSide Castle
                    rookType = 9;
                    rookFrom = 63;
                    rookTo = 61;
                }
                boards[rookType] |= (1L << rookFrom);
                hash ^= Zobrist.pieceHash[rookType][rookFrom];
                boards[rookType] &= ~(1L << rookTo);
                hash ^= Zobrist.pieceHash[rookType][rookTo];
            } else if(moveType == 3) {
                hash ^= Zobrist.enPassantHash[oldEnPassant % 8]; //Hash back in the old enPassant
                int enPassantIndex = !isWhiteTurn ? oldEnPassant - 8 : oldEnPassant + 8; //negated cause right now its the opposite sides turn
                boards[capturedPiece] |= (1L << enPassantIndex); //checking if it got enPassanted
                hash ^= Zobrist.pieceHash[capturedPiece][enPassantIndex]; //hash back in the enpassanted piece
            } else{
                boards[capturedPiece] |= (1L << toIndex); //returning it back
                hash ^= Zobrist.pieceHash[capturedPiece][toIndex];
            }
        }
        if(capturedPiece == 5 || capturedPiece == 11){
            System.err.println("King taken");
        }
        if (currentEnPassantSquare != -1 && moveType != 3) { //If we set a new one by pushing a pawn twice
            hash ^= Zobrist.enPassantHash[currentEnPassantSquare % 8];
        }
        currentEnPassantSquare = moveState.getOldEnPassantSquare();
        if (currentEnPassantSquare != -1 && moveType != 3) { //If we chose not to enpassant
            hash ^= Zobrist.enPassantHash[currentEnPassantSquare % 8];
        }

        halfMoveClock = moveState.getOldMoveCounter();
        if(castlingRights != moveState.getOldCastlingRights()){
            hash ^= Zobrist.castleHash[castlingRights]; //hash out the new one
            castlingRights = moveState.getOldCastlingRights();
            hash ^= Zobrist.castleHash[castlingRights]; //Hash back in the old one
        }
        isWhiteTurn = !isWhiteTurn;
        hash ^= Zobrist.turnHash;
    }

    public void updateCastlingRights(int movingPiece, int fromIndex, int toIndex){
        if(movingPiece == 11){ //Black king
            castlingRights &= 0b1100;
        } else if(movingPiece == 5) { //WhiteKing
            castlingRights &= 0b0011;
        } else if(movingPiece == 3){//White Rooks
            if(fromIndex == 7){ //King Side
                castlingRights &= 0b0111;
            }
            if(fromIndex == 0){ //Queen Side
                castlingRights &= 0b1011;
            }
        } else if(movingPiece == 9){ //Black Rooks
            if(fromIndex == 63){ //King Side
                castlingRights &= 0b1101;
            }
            if(fromIndex == 56){  //Queen Side
                castlingRights &= 0b1110;
            }
        }
        //If they capture a rook that can still castle
        if(toIndex == 7) castlingRights &= 0b0111;
        if(toIndex == 0) castlingRights &= 0b1011;
        if(toIndex == 63) castlingRights &= 0b1101;
        if(toIndex == 56) castlingRights &= 0b1110;
    }


    public void updateCastlingRights(){
        long [] arr = board.getBitboard();
        if( ( arr[3] & (1L) ) == 0){ //bottom left corner
            castlingRights &= 0b1011;
        }
        if( (arr[3] & (1L << 7)) == 0){ //bottom right
            castlingRights &= 0b0111;
        }
        if( (arr[9] & (1L << 56)) == 0){ //upper left
            castlingRights &= 0b1110;
        }
        if( (arr[9] & (1L << 63)) == 0){
            castlingRights &= 0b1101;
        }
    }

    public int getEnPassantSquare(int fromIndex, int toIndex){
        int diff = toIndex - fromIndex;
        if(diff == 9 || diff == - 7){ //EnPassant on the right from white and black
            return fromIndex +1;
        }else if(diff == 7 || diff == -9){ //EnPassant on the left from White and black
            return fromIndex - 1;
        }
        return -1; //In case something goes wrong prob won't
    }

    public PieceType getPieceAt(int index){
        for(int i = 0; i<12; i++){
            long l = board.getBitboard()[i];
            if( ((1L << index) & l) != 0){
                return PieceType.values()[i % 6];
            }
        }
        return null;
    }

    public Move creatingMove(String from, String to){
        long [] boards = board.getBitboard();
        int fromIndex = from.charAt(0) - 'a' + (from.charAt(1) - '0' * 8);
        int toIndex = to.charAt(0) - 'a' + (to.charAt(1) - '0' * 8);
        if(fromIndex >= 64 || fromIndex < 0 || toIndex >= 64 || toIndex < 0){
            return null;
        }
        int movingPiece = -1;
        int capturedPiece = -1;
        for(int i = 0; i<12; i++){
            long l = boards[i];
            if( (1L << fromIndex & l) != 0){
                movingPiece = i;
            }
            if( (1L << toIndex & l ) != 0){
                capturedPiece = i;
            }
        }
        int moveType = 0;
        if(capturedPiece != -1){
            moveType = 1;
        } else if( (movingPiece == 5 || movingPiece == 11) && Math.abs(fromIndex - toIndex) == 2){
            moveType = 2;
        } else if(toIndex == getCurrentEnPassantSquare()){
            moveType = 3;
        }
        int promoPiece = 0;
        if( (movingPiece == 0 && toIndex / 8 == 7) || (movingPiece == 6 && toIndex / 8 == 0)){
            int input = scan.nextInt();
            if(input > 0 && input < 4){
                promoPiece = input;
            } else{
                return null;
            }
        }
        return new Move(fromIndex, toIndex, moveType, promoPiece);
    }

    public int getMoveCounter(){
        return moveCounter;
    }

    public int getCastlingRights(){
        return castlingRights;
    }

    public void setWhiteTurn(){
        isWhiteTurn = !isWhiteTurn;
    }

    public void setCurrentEnPassantSquare(int num){
        currentEnPassantSquare = num;
    }

    public int getCurrentEnPassantSquare(){
        return currentEnPassantSquare;
    }

    public Stack<MoveState> getMoveHistory(){
        return moveHistory;
    }

    public void changeTurn(boolean b){
        isWhiteTurn = b;
    }

    public void checkMated(){
        hasWon = true;
    }

    public boolean isGameOver(){
        return hasWon;
    }

    public List<Move> getAllPossibleMoves(boolean isWhiteTurn){
        List<Move> list = new ArrayList<>();
        for(PieceType pieceType : PieceType.values()){
            for(Move m : pieceType.generateMoves(this,moveGenerator.getPieceMoveMasks(), isWhiteTurn)){
                if(MoveValidator.validateMove(this,m)){
//                    System.out.println("--Starting--");
//                    board.printBoard();
//                    System.out.println(m);
//                    System.out.println("---Undoing------");
                    undoMove();
//                    board.printBoard();
                    list.add(m);
                }
            }
        }
        sortMoves(list);
        return list;
    }

    public String getFenNotation(){
        StringBuilder sb = new StringBuilder(board.getFenNotation() + " ");
        if(isWhiteTurn){
            sb.append("w ");
        } else{
            sb.append("b ");
        }
        if(castlingRights == 0){
            sb.append('-');
        }
        char[] arr = {'q', 'k', 'Q', 'K'};
        for(int i = 3; i>= 0; i--){
            if( (castlingRights & (1 << i)) != 0){
                sb.append(arr[i]);
            }
        }
        sb.append(' ');
        if(currentEnPassantSquare != -1){
            char file = (char) ('a' + (currentEnPassantSquare%8));
            int rank = (currentEnPassantSquare/8);
            sb.append(file).append(rank).append(" ");
        } else{
            sb.append("- ");
        }
        sb.append(halfMoveClock).append(" ");
        sb.append(moveCounter);
        return sb.toString();
    }

    public void incrementMoveCounter(){
        moveCounter++;
    }

    public long getAttackMap(boolean isWhiteTurn){
        long l = 0L;
        MoveGenerator gen = new MoveGenerator();
        for(PieceType pieceType : PieceType.values()){
            l |= pieceType.getAttackMask(this, gen.getPieceMoveMasks(), isWhiteTurn);
        }
        return l;
    }

    public void sortMoves(List<Move> list){
        list.sort((m1, m2) -> Integer.compare(m2.getScore(), m1.getScore()));
    }

    public void assignScore(Move m){
        int [][] boardValues = board.getBoardValues();
        int score = 0;
        int fromIndex = m.getFromLocation();
        int toIndex = m.getToLocation();
        int movingType = -1, capturedType = -1; //temp variables but will get set to non negative
        for (int i = 0; i < 12 && (movingType == -1 || capturedType == -1) ; i++) {
            long l = board.getBitboard()[i];
            if ((l & (1L << fromIndex)) != 0) {
                movingType = i;
            }
            if ((l & (1L << toIndex)) != 0) {
                capturedType = i;
            }
        }
        if (movingType == -1) {
            System.err.println("Warning: No piece found at from square " + fromIndex);
            m.setScore(0);
            return;
        }
        int[] pieceValues = {100, 300, 300, 500, 900, 1500}; // P, N, B, R, Q, K
        if(m.getMoveType() == 1) {
            int victimValue = pieceValues[capturedType % 6];
            int attackerValue = pieceValues[movingType % 6];

            // MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
            score = victimValue * 10 + (600 - attackerValue);
        }
        score += (m.getPromotion()) * 100; //Valuing promotions
        if(movingType >= 6){
            score += boardValues[movingType-6][toIndex] - boardValues[movingType-6][fromIndex];
        } else{
            score += boardValues[movingType][toIndex ^ 56] - boardValues[movingType][fromIndex ^ 56]; //flip because array is from black's pov when I generated
        }


        if(m.getMoveType() == 2){
            score += 20; //Castling is good
        }
        m.setScore(score);
    }

    public List<Move> getAllCaptures(){
        List<Move> captureList = new ArrayList<>();
        for(PieceType pieceType : PieceType.values()) {
            for (Move m : pieceType.generateMoves(this, moveGenerator.getPieceMoveMasks(), isWhiteTurn)) {
                if (MoveValidator.validateMove(this, m)) {
                    undoMove();
                    if(m.getMoveType() == 1){
                        assignScore(m);
                        captureList.add(m);
                    }
                }
            }
        }
        sortMoves(captureList);
        return captureList;
    }

    public void makeNullMove(){
        MoveState nullMoveState = new MoveState(
                null,                          // no actual move
                -1,                            // no captured piece
                castlingRights,                // current castling rights
                halfMoveClock,                 // store BEFORE modifying
                currentEnPassantSquare,        // store BEFORE modifying
                isWhiteTurn ? 1 : 0            // whose turn it was
        );
        moveHistory.push(nullMoveState);
        isWhiteTurn = !isWhiteTurn;
        moveCounter++;
        hash ^= Zobrist.turnHash;
        if(currentEnPassantSquare != -1){
            hash ^= Zobrist.enPassantHash[currentEnPassantSquare % 8];
        }
        currentEnPassantSquare = -1;
    }

    private int chessNotationToIndex(String str) {
        int file = str.charAt(0) - 'a';
        int rank = str.charAt(1) - '1';
        return rank * 8 + file;
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

    public Move uciToMove(String move) {
        if (move.length() < 4) {
            return null;
        }
        long[] boards = getBoard().getBitboard();
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
        if (to == getCurrentEnPassantSquare() && movingPiece == (getTurn() ? 0 : 6)) { //En Passant
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

    public boolean peek(){
        return moveHistory.peek() != null;
    }

    public void resetGame(){
        isWhiteTurn = true;
        hasWon = false;
        moveCounter = 1;
        halfMoveClock = 0;
        castlingRights = 15;
        currentEnPassantSquare = -1;
        board.reset();
        hash = Zobrist.computeHash(this);
    }



}
