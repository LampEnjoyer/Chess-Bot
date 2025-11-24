package io.github.LampEnjoyer.PracticeBot.engine;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class MoveValidator {


    private final static long [] rookMagicNumbers = new long[64];
    private final static long [] bishopMagicNumbers = new long[64];

    private final static long [][] rookAttackTable = new long[64][];
    private final static long [][] bishopAttackTable = new long [64][];
    private final static MoveGenerator moveGenerator = new MoveGenerator();


    private MoveValidator(){
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean validateMove(GameState gameState, Move move){
        int fromIndex = move.getFromLocation();
        int toIndex = move.getToLocation();

        if(fromIndex == toIndex){return false;}

        long blackBoard = gameState.getBlackBoard();
        long whiteBoard = gameState.getWhiteBoard();
        long occupiedBoard = blackBoard | whiteBoard;
        long[][] pieceMoveMasks = moveGenerator.getPieceMoveMasks();

        int moveType = move.getMoveType();
        //Determine piece Color and checking player turn
        boolean isWhite = ( (1L << fromIndex) & whiteBoard) != 0; //Checking to see if you're moving your own piece

        if(gameState.getTurn() != isWhite){
            return false;
        }
        long currBoard = isWhite ? whiteBoard : blackBoard;
        if((currBoard & (1L << toIndex)) != 0){//Trying to move your own piece onto your own piece
            return false;
        }

        int adjustedPieceType = adjustPiece(findPiece(gameState,fromIndex)); //Getting pieceType then adjusting cause of move masks
        //Check if move is possible and follows chess rules I.E rook cannot move diagonally
        if(adjustedPieceType == -1 || (pieceMoveMasks[adjustedPieceType][fromIndex] & (1L << toIndex)) == 0){
            return false;
        }
        PieceType pieceType = gameState.getPieceAt(fromIndex);
        if (!pieceType.isValidMove(gameState, move, pieceMoveMasks)){
            return false;}
        return isKingSafe(gameState, move);
    }

    public static boolean isKingSafe(GameState gameState, Move move){
        gameState.makeMove(move);
        if(!isKingInCheck(gameState, gameState.getTurn())){
            return true;
        }else{
            gameState.undoMove();   
            return false;
        }
    }



    public static boolean isKingInCheck(GameState gameState, boolean isWhite){ //Checking if the other pieces can reach the king
        MoveGenerator moveGenerator = gameState.getMoveGenerator();
        long[][] pieceMoveMasks = moveGenerator.getPieceMoveMasks();
        for (PieceType piece : PieceType.values()) {
            if (piece.attacksKing(gameState, pieceMoveMasks, isWhite)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCheckMate(GameState gameState){
        long[][] pieceMoveMasks = moveGenerator.getPieceMoveMasks();
        if(numAttackers(gameState) >= 2){ //Can only check King moves
            for(Move move : PieceType.KING.generateMoves(gameState,pieceMoveMasks, gameState.getTurn())){
                if(MoveValidator.validateMove(gameState,move)){
                    gameState.undoMove();
                    return false;
                }
            }
            return true;
        } else{
            for(PieceType pieceType : PieceType.values()){
                for(Move move : pieceType.generateMoves(gameState,pieceMoveMasks,gameState.getTurn())){
                    if(MoveValidator.validateMove(gameState,move)){
                        gameState.undoMove();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isStalemate(GameState gameState){
        long[][] pieceMoveMasks = moveGenerator.getPieceMoveMasks();
        return numAttackers(gameState) == 0 && gameState.getAllPossibleMoves(gameState.getTurn()).isEmpty();
    }

    public static boolean canKingEscape(GameState gameState){
        long[][] pieceMoveMasks = moveGenerator.getPieceMoveMasks();
        long [] boards = gameState.getBoard().getBitboard();
        return false;

    }

    public static int numAttackers(GameState gameState){
        int count = 0;
        long[][] pieceMoveMasks = moveGenerator.getPieceMoveMasks();
        for(PieceType pieceType : PieceType.values()){
            count += pieceType.numAttackers(gameState,pieceMoveMasks);
        }
        return count;
    }
    public static boolean canCastle(GameState gameState, Move move, int movingPiece){
        int castlingRights = gameState.getCastlingRights();
        int toIndex = move.getToLocation();

        if(toIndex == 2  && movingPiece == 5){ //Queen Side White Castle
            long rookBlocker = getRookBlockerBoard(gameState, 0);
            rookBlocker &= 0b1111; //Getting blockers between the rook and the king
            return rookBlocker == 0 && ((castlingRights & 0b0100) != 0);
        }
        if(toIndex == 6 && movingPiece == 5){ //King Side White Castle
            long rookBlocker = getRookBlockerBoard(gameState, 7);
            rookBlocker = (rookBlocker >> 5) & 0b111;
            return rookBlocker == 0 && ((castlingRights & 0b1000) != 0);
        }
        if(toIndex == 58 && movingPiece == 11){ //Queen Side Black Castle
            long rookBlocker = getRookBlockerBoard(gameState, 56);
            rookBlocker = (rookBlocker >> 56) & 0b1111;
            return rookBlocker == 0 && ((castlingRights & 0b0001) != 0);
        }
        if(toIndex == 62 && movingPiece == 11){ //King Side Black Castle
            long rookBlocker = getRookBlockerBoard(gameState, 63);
            rookBlocker = (rookBlocker >> 60) & 0b10;
            return rookBlocker == 0 && ((castlingRights & 0b0010) != 0);
        }
        return false;
    }


    public static long getRookBlockerBoard(GameState state, int index){
        long board = state.getPieceBoard();
        return board & getRookBlockerMask(index);

    }

    public static long getBishopBlockerBoard(GameState state, int index){
        long board = state.getPieceBoard();
        return board & getBishopBlockerMask(index);
    }

    public static long getQueenBlockerBoard(GameState state, int index){
        return getBishopBlockerBoard(state, index) | getRookBlockerBoard(state,index);
    }

    public static int findPiece(GameState gameState, int fromIndex){
        long [] bitboards = gameState.getBoard().getBitboard();
        int num = - 1;
        for(int i = 0; i<bitboards.length; i++){
            if( ((1L << fromIndex) & bitboards[i]) != 0){
                num = i;
            }
        }
        return num; //default case -1 won't ever happen i hope
    }
    // 0 = White Pawn, 1 = Knight, 2 = Bishop, 3 = Rook, 4 = Queen, 5 = White King, 6 = Black Pawn, 11 = Black
    public static int adjustPiece(int pieceType){ //this is an adjustment because our moveMasks aren't 1-1 with the bitboards due to some pieces not being color-dependent
        if(pieceType >= 7 && pieceType <= 10){
            return pieceType-6;
        } else if(pieceType == 11){
            return 4;
        }
        return pieceType;
    }

   public static void generateMagicNumbers(){
        try{
            DataInputStream in = new DataInputStream(new FileInputStream("rook_magic_numbers.bin"));
            DataInputStream in2 = new DataInputStream(new FileInputStream("bishop_magic_numbers.bin"));
            for(int i = 0; i<64; i++){
                rookMagicNumbers[i] = in.readLong();
                bishopMagicNumbers[i] = in2.readLong();
            }
        }catch (IOException e){
            System.out.println("File not found");
        }
        System.out.println("Generating .....");
        readRookAttackTable();
        readBishopAttackTable();
   }

   public static void setBishopAttackTable(){
        try(DataOutputStream out = new DataOutputStream(new FileOutputStream("bishop_attack_table.bin"))) { //Ran once offline to compute, same with the bishop
            for (int i = 0; i < 64; i++) {
                long magicNum = bishopMagicNumbers[i];
                int numBits = countBits(getBishopBlockerMask(i));
                int numIndices = 1 << numBits;
                int rank = i / 8;
                int col = i % 8;
                bishopAttackTable[i] = new long[numIndices];
                for (long blocker : generateBlockers(i, false)) {
                    long move = 0L;
                    int index = (int) ((magicNum * blocker) >>> (64 - numBits));
                    int[][] dir = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
                    for (int[] d : dir) {
                        int newRank = rank + d[0];
                        int newCol = col + d[1];
                        while (newRank >= 0 && newRank < 8 && newCol < 8 && newCol >= 0) {
                            int idx = newRank * 8 + newCol;
                            move |= 1L << idx;
                            if ((blocker & (1L << idx)) != 0) {
                                break;
                            }
                            newRank += d[0];
                            newCol += d[1];
                        }
                    }
                    if (i == 54 && index == 0) {
                        System.out.println("WRITING to [54][0]:");
                        System.out.println("  move: " + Long.toBinaryString(move));
                        System.out.println("  blocker: " + Long.toBinaryString(blocker)); // Add this line
                        System.out.println("  magic hash result: " + index);
                        new Exception("Write location").printStackTrace();
                    }
                    bishopAttackTable[i][index] = move;
                }
            }
            for(int i = 0; i<64; i++){
                for(long l : bishopAttackTable[i]){
                    out.writeLong(l);
                }
            }
            System.out.println("Generated");
        } catch (IOException ignored){
            System.out.println("Error");
        }
   }

   public static void readBishopAttackTable(){
        try(DataInputStream in = new DataInputStream(new FileInputStream("bishop_attack_table.bin"))) {
            for (int i = 0; i < 64; i++) {
                int numBits = countBits(getBishopBlockerMask(i));
                int numIndices = 1 << numBits;
                bishopAttackTable[i] = new long[numIndices];
                for (int j = 0; j < numIndices; j++) {
                    bishopAttackTable[i][j] = in.readLong();
                }
            }
        }catch (IOException e){
            System.out.println(e.getStackTrace());
        }
   }

   public static void setRookAttackTable(){
        System.out.println("Starting");
        try(DataOutputStream out = new DataOutputStream(new FileOutputStream("rook_attack_table.bin"))) {
            for (int i = 0; i < 64; i++) {
                long magicNum = rookMagicNumbers[i];
                int numBits = countBits(getRookBlockerMask(i));
                int numIndices = 1 << numBits;
                int rank = i / 8;
                int col = i % 8;
                rookAttackTable[i] = new long[numIndices];
                for (long blocker : generateBlockers(i, true)) {
                    long move = 0L;
                    int index = (int) ((magicNum * blocker) >>> (64 - numBits));
                    int[][] dir = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}};
                    for (int[] d : dir) {
                        int newRank = rank + d[0];
                        int newCol = col + d[1];
                        while (newRank >= 0 && newRank < 8 && newCol < 8 && newCol >= 0) {
                            int idx = newRank * 8 + newCol;
                            move |= 1L << idx;
                            if ((blocker & (1L << idx)) != 0) {
                                break;
                            }
                            newRank += d[0];
                            newCol += d[1];
                        }
                    }
                    rookAttackTable[i][index] = move;
                }
                for(long l : rookAttackTable[i]){
                    out.writeLong(l);
                }
            }
        }catch (IOException e){
            System.out.println("Error");
        }
   }

   public static void readRookAttackTable(){
        try(DataInputStream in = new DataInputStream(new FileInputStream("rook_attack_table.bin"))){
            for(int i = 0; i<64; i++){
                int numBits = countBits(getRookBlockerMask(i));
                rookAttackTable[i] = new long[1 << numBits];
                for(int j = 0; j < 1<<numBits; j++){
                    rookAttackTable[i][j] = in.readLong();
                }
            }
        }catch (IOException e){
            System.out.println("Error");
        }
   }

   public static long[] getRookMagicNumbers(){
        return rookMagicNumbers;
   }

   public static long[] getBishopMagicNumbers(){
        return bishopMagicNumbers;
   }

    public static long[][] getBishopAttackTable(){
        return bishopAttackTable;
    }

    public static long[][] getRookAttackTable(){
        return rookAttackTable;
    }

    public static ArrayList<Long> generateBlockers(int index, boolean isRook){ //Generates all possible combinations of blockers given an index
        long mask = isRook ? getRookBlockerMask(index) : getBishopBlockerMask(index);
        ArrayList<Integer> bits = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (((mask >> i) & 1) != 0) bits.add(i);
        }

        int n = bits.size();
        int permutations = 1 << n;
        ArrayList<Long> blockers = new ArrayList<>();

        for (int i = 0; i < permutations; i++) {
            long blocker = 0L;
            for (int j = 0; j < n; j++) {
                if (((i >> j) & 1) != 0) {
                    blocker |= 1L << bits.get(j);
                }
            }
            blockers.add(blocker);
        }
        return blockers;
    }
    public static long getRookBlockerMask(int square) { //Creates a board blocking every possible path
        long mask = 0L;
        int rank = square / 8;
        int file = square % 8;

        for (int f = file + 1; f < 7; f++) mask |= 1L << (rank * 8 + f);
        for (int f = file - 1; f > 0; f--) mask |= 1L << (rank * 8 + f);
        for (int r = rank + 1; r < 7; r++) mask |= 1L << (r * 8 + file);
        for (int r = rank - 1; r > 0; r--) mask |= 1L << (r * 8 + file);
        return mask;
    }


    public static long getBishopBlockerMask(int square){
        long mask = 0L;
        int rank = square / 8;
        int file = square % 8;

        int [][] dir = { {1,1}, {1,-1}, {-1,1}, {-1,-1} };
        for(int [] d : dir){
            int tempRank = rank + d[0];
            int tempFile = file + d[1];
            while(tempRank < 7 && tempRank > 0 && tempFile < 7 && tempFile > 0){
                mask |= 1L << ( (tempRank * 8) + tempFile);
                tempRank += d[0];
                tempFile += d[1];
            }
        }
        return mask;
    }

    public static int countBits(long num) {
        int count = 0;
        while (num > 0) {
            num &= (num - 1);
            count++;
        }
        return count;
    }

    public static void printBoard(long board){
        for(int i = 7; i>=0; i--){
            for(int j = 0; j<8; j++){
                int index = i * 8 + j;
                if( (1L << index & board) != 0){
                    System.out.print("1 ");
                } else{
                    System.out.print("0 ");
                }
            }
            System.out.println();
        }
    }

    public static long generateRandom(){
        Random rand = new Random();
        return rand.nextLong() & rand.nextLong() & rand.nextLong() & rand.nextLong(); //more sparse bits better random
    }

    public static void createBishopMagicNumFile(){
        for(int i = 0; i<64; i++){
            boolean found = false;
            while(!found){
                long magic = generateRandom();
                Set<Integer> usedIndices = new HashSet<>();
                boolean hasCollision = false;
                int numBits = countBits(getBishopBlockerMask(i));
                for(long l : generateBlockers(i,false)){
                    int index = (int) ( (magic * l) >>> (64 - numBits));
                    if(usedIndices.contains(index)){
                        hasCollision = true;
                        break;
                    }
                    usedIndices.add(index);
                }
                if(!hasCollision){
                    found = true;
                    bishopMagicNumbers[i] = magic;
                    System.out.println(magic);
                }

            }
        }
        try(DataOutputStream out = new DataOutputStream(new FileOutputStream("bishop_magic_numbers.bin"))){
            for(long l : bishopMagicNumbers){
                out.writeLong(l);
            }
        } catch (Exception e) {
             e.printStackTrace();
        }
    }

    public static void verifyRookMagicNumbers(){
        for(int square = 0; square < 64; square++){
            System.out.println("Verifying square " + square);

            Set<Integer> usedIndices = new HashSet<>();
            boolean hasCollision = false;

            // Test all possible blockers for this square
            for(long blocker : generateBlockers(square, true)){ // true for rook
                int numBits = countBits(getRookBlockerMask(square));
                int index = (int) ((rookMagicNumbers[square] * blocker) >>> (64 - numBits));

                if(usedIndices.contains(index)){
                    System.out.println("MAGIC COLLISION at square " + square + ", index " + index);
                    hasCollision = true;
                    break;
                }
                usedIndices.add(index);
            }

        }
    }


    public static class OpeningBook {
        private Map<String, List<String>> map;

        public OpeningBook () throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            this.map = mapper.readValue(new File("src/main/java/io/github/LampEnjoyer/PracticeBot/book.json"), new TypeReference<Map<String, List<String>>>(){});
        }

        public boolean isBookPosition(String str){
            return map.containsKey(str);
        }

        public String getMove(String str){
            List<String> list = map.get(str);
            Random random = new Random();
            return list.get(random.nextInt(list.size()));
        }

        public Map<String, List<String>> getMap(){
            return map;
        }




    }
}




