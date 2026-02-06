package io.github.LampEnjoyer.PracticeBot.engine;


import java.util.List;

public class Evaluator {

    static final int PAWN = 100;
    static final int KNIGHT = 300;
    static final int BISHOP = 300;
    static final int ROOK = 500;
    static final int QUEEN = 900;

    private static final int [] pieceValues = new int[]{PAWN, KNIGHT, BISHOP, ROOK, QUEEN};

    static final int CHECKMATE = 1000000;
    private static final int INF = 1000000;

    public static int positions = 0;
    public static int ttProbes = 0;
    public static int ttHits;
    public static int notFound;
    public static int cutoffs;



    private static final int MAX_QUIESCENCE_DEPTH = 10;

    private static final int captureMultiplier = 10;
    private static final int pawnPenalty = 350;

    public static MoveScore[][] pvTable = new MoveScore[10][10];



    public static int evaluateMaterial(GameState gameState, boolean isWhite){
        int shift = isWhite ? 0 : 6;
        long [] bitboard = gameState.getBoard().getBitboard();
        int material = 0;
        material += countBits(bitboard[shift]) * PAWN;
        material += countBits(bitboard[shift + 1]) * KNIGHT;
        material += countBits(bitboard[shift + 2]) * BISHOP;
        material += countBits(bitboard[shift + 3]) * ROOK;
        material += countBits(bitboard[shift + 4]) * QUEEN;
        return material;
    }

    public static int evaluateBoard(GameState gameState){
        int score = evaluateMaterial(gameState, true) - evaluateMaterial(gameState, false);
        score = gameState.getTurn() ? score : -score;
        return score;
    }


    public static int countBits(long num) {
        int count = 0;
        while (num > 0) {
            num &= (num - 1);
            count++;
        }
        return count;
    }

    public static MoveScore miniMax(GameState gameState, int depth, boolean isWhite) {
        if (depth == 0) {
            return new MoveScore(null, evaluateBoard(gameState));
        }
        Move bestMove = null;
        int bestEval = isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        List<Move> moves = gameState.getAllPossibleMoves(isWhite);

        if(moves.isEmpty()){
            if(isWhite && gameState.isGameOver()){
                return new MoveScore(null, Integer.MIN_VALUE);
            } else if(!isWhite && gameState.isGameOver()){
                return new MoveScore(null, Integer.MAX_VALUE);
            } else{
                return new MoveScore(null,0);
            }
        }
        for (Move move : moves) {
            gameState.makeMove(move);
            MoveScore res = miniMax(gameState, depth - 1, !isWhite);
            gameState.undoMove();

            if (isWhite && res.getScore() > bestEval) {
                bestEval = res.getScore();
                bestMove = res.getMove();
            } else if (!isWhite && res.getScore() < bestEval) {
                bestEval = res.getScore();
                bestMove = res.getMove();
            }
        }
        return new MoveScore(bestMove, bestEval);
    }
    public static MoveScore getBestMove(GameState gameState, int depth, int alpha, int beta, boolean isWhite, MoveScore prevBest, boolean allowNull){
        positions++;
        long hash = Zobrist.computeHash(gameState);
        ttProbes++;
        TTEntry entry = TranspositionTable.retrieve(hash);
        int originalAlpha = alpha;
        int originalBeta = beta;
        if(entry != null && entry.getDepth() >= depth && entry.getZobristHash() == hash){
            ttHits++;
            if(entry.getFlag() == 0){
                return new MoveScore(entry.getMove(), entry.getScore());
            } else if(entry.getFlag() == 1){
                alpha = Math.max(alpha, entry.getScore());
            } else if(entry.getFlag() == 2){
                beta = Math.min(beta, entry.getScore());
            } else if(alpha >= beta){
                return new MoveScore(entry.getMove(), entry.getScore());
            }
        }
        List<Move> list = gameState.getAllPossibleMoves(isWhite);
        if(list.isEmpty()){
            if(MoveValidator.isKingInCheck(gameState, !isWhite)){
                int mateScore = isWhite ? -CHECKMATE : CHECKMATE;
               // System.out.println("CHECKMATE DETECTED! isWhite=" + isWhite + " score=" + mateScore + " depth=" + depth);
                return new MoveScore(null, mateScore);
            } else{
                return new MoveScore(null, 0); //stalemate
            }
        }
        if(depth == 0){
           int score = evaluateBoard(gameState);
         //   int score = quiescenceSearch(gameState,alpha,beta,isWhite, 0);
            TranspositionTable.store(hash, 0, score, depth, null);
            return new MoveScore(null, score);
        }
        if(allowNull && depth >= 3 && !MoveValidator.isKingInCheck(gameState,isWhite) && !hasOnlyPawns(gameState,isWhite)){
            gameState.makeNullMove();
            MoveScore nullResult = getBestMove(gameState, depth - 2, alpha, beta, !isWhite, null,false);
            gameState.undoMove();
            // If null move causes cutoff, position is too good
            if(isWhite && nullResult.getScore() >= beta) {
                return new MoveScore(null, beta); // Fail-high for white
            }
            if(!isWhite && nullResult.getScore() <= alpha) {
                return new MoveScore(null, alpha); // Fail-low for black
            }
        }
        Move bestMove = null;
        if(prevBest != null){
            reorderMoves(prevBest.getMove(), list);
        }
        if(entry != null && entry.getMove() != null && entry.getDepth() >= depth - 2 && entry.getZobristHash() == hash){
            ttHits++;
            reorderMoves(entry.getMove(), list);
        }
//        if(pvTable[depth-1] != null){
//            reorderMoves(pvTable[depth-1].getMove(), list);
//        }
        int bestScore = isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : list) {
            gameState.makeMove(move);
            MoveScore result = getBestMove(gameState, depth - 1, alpha, beta, !isWhite, null, true);
            gameState.undoMove();
            if (isWhite) { // Maximizing player
                if (result.getScore() > bestScore) {
                    bestScore = result.getScore();
                    bestMove = move;
                }
                alpha = Math.max(alpha, bestScore);
            } else { // Minimizing player
                if (result.getScore() < bestScore) {
                    bestScore = result.getScore();
                    bestMove = move;
                }
                beta = Math.min(beta, bestScore);
            }

            if (alpha >= beta){
                cutoffs++;
                break; // Alpha-beta cutoff
            }
        }
        int flag;
        if(bestScore <= originalAlpha){
            flag = 2;
        } else if(bestScore >= originalBeta){
            flag = 1;
        } else{
            flag = 0;
        }
        TranspositionTable.store(hash, depth, bestScore, flag, bestMove);
        return new MoveScore(bestMove, bestScore);
    }

    private static void reorderMoves(Move move, List<Move> list)  {//Figure out why so many hash collisions
        if(!list.remove(move)) {
            notFound++;
        } else {
            list.addFirst(move);
        }
    }

    public static MoveScore iterativeSearch(GameState gameState, int depth){
        MoveScore bestMove = null;
        for(int i = 1; i<= depth; i++){
            bestMove = getBestMove(gameState, i,Integer.MIN_VALUE, Integer.MAX_VALUE, gameState.getTurn(), bestMove, true);
        }
        return bestMove;
    }

    public static int quiescenceSearch(GameState gameState, int alpha, int beta, boolean isWhite, int depth){
        int boardEval = evaluateBoard(gameState);
        // Stand-pat from current player's perspective
        int standPat = isWhite ? boardEval : -boardEval;
        if(standPat >= beta){
            return beta;
        }
        if(standPat > alpha){
            alpha = standPat;
        }

        if(depth >= MAX_QUIESCENCE_DEPTH){
            return alpha;
        }
        List<Move> captureList = gameState.getAllCaptures();
        for(Move m : captureList){
            gameState.makeMove(m);
            if(MoveValidator.isKingInCheck(gameState, isWhite)){
                gameState.undoMove();
                continue;
            }
            int score = -quiescenceSearch(gameState, -beta, -alpha, !isWhite, depth + 1);
            gameState.undoMove();
            if(score >= beta){
                return beta;
            }
            if(score > alpha){
                alpha = score;
            }
        }
        return alpha;
    }

    public static boolean hasOnlyPawns(GameState gameState, boolean isWhite){
        int shift = isWhite ? 0 : 6;
        long [] board = gameState.getBoard().getBitboard();
        for(int i = 1 + shift; i <5 + shift; i++){
            if(board[i] != 0){
                return true;
            }
        }
        return false;
    }

    public static int search(GameState gameState, int depth, int alpha, int beta) {
        long hash = gameState.getHash();
        TTEntry e = TranspositionTable.retrieve(hash);
        if(e != null && e.getZobristHash() == hash && e.getDepth() >= depth){
            if(e.getFlag() == 0){ //Exact
                return e.getScore();
            } else if(e.getFlag() == 1 && e.getScore() >= beta){ //beta is best we can do
                return e.getScore();
            }else if(e.getFlag() == 2 && e.getScore() <= alpha){ //alpha is worse we can do
                return e.getScore();
            }
        }
        positions++;
        Move bestMove = null;
        List<Move> moveList = gameState.getAllPossibleMoves(gameState.getTurn());
        orderMoves(gameState,moveList);
        if(e != null && e.getMove() != null){
            reorderMoves(e.getMove(), moveList);
        }
        int origAlpha = alpha;
        if(moveList.isEmpty()) {
            if(MoveValidator.isKingInCheck(gameState, gameState.getTurn())) {
                return -CHECKMATE + depth; // Faster mates score better
            }
            return 0; // Stalemate
        }
        if(depth <= 0) {
            return evaluateBoard(gameState);
        }
        for(Move move : moveList) {
            gameState.makeMove(move);
            int eval = -search(gameState, depth - 1, -beta, -alpha);
            gameState.undoMove();
            if(eval >= beta) {
                TranspositionTable.store(hash, depth, beta, 1, bestMove);
                cutoffs++;
                return beta; // Beta cutoff
            }
            if(eval > alpha) {
                alpha = eval;
                bestMove = move;
            }
        }
        int flag;
        if(alpha <= origAlpha){ //move ended up being worse or does nothing
             flag = 2;
        }else{
            flag = 0;
        }
        TranspositionTable.store(hash, depth, alpha, flag, bestMove);
        return alpha;
    }

    public static MoveScore findBestMove(GameState gameState, int depth) {
        positions = 0;
        cutoffs = 0;
        Move bestMove = null;
        int bestScore = -INF;
        List<Move> moves = gameState.getAllPossibleMoves(gameState.getTurn());
        orderMoves(gameState, moves);
        for(Move move : moves) {
            gameState.makeMove(move);
            int score = -search(gameState, depth - 1, -INF, INF);
            gameState.undoMove();
            if(score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return new MoveScore(bestMove, bestScore);
    }

    public static void orderMoves(GameState gameState, List<Move> list){
        int[][] positionValues = gameState.getBoard().getBoardValues();
        for(Move m : list){
            int score = 0;
            int fromIndex = m.getFromLocation();
            int toIndex = m.getToLocation();
            int movingType = -1, capturedType = -1;
            long[] pieces = gameState.getBoard().getBitboard();
            for(int i = 0; i<12; i++){
                if( (pieces[i] & (1L << fromIndex)) != 0){
                    movingType = i;
                }
                if( (pieces[i] & (1L << toIndex)) != 0){
                    capturedType= i;
                }
            }
            if(capturedType != -1){
                if(movingType % 6 != 5){
                    score += (pieceValues[capturedType%6] * captureMultiplier) - pieceValues[movingType % 6];
                    score += (positionValues[movingType%6][toIndex] - positionValues[movingType%6][fromIndex]);
                }else{
                    score += (pieceValues[capturedType%6] * captureMultiplier);
                }
            }
            m.setScore(score);
        }
        list.sort((m1, m2) -> {
            // First, prioritize captures
            int t1 = m1.getMoveType();
            int t2 = m2.getMoveType();
            if (t1 != 0 && t2 == 0) return -1; // m1 capture, m2 normal -> m1 first
            if (t1 == 0 && t2 != 0) return 1;  // m1 normal, m2 capture -> m2 first

            // If both are the same type (both captures or both normal), sort by score descending
            return Integer.compare(m2.getScore(), m1.getScore());
        });
    }

    public static MoveScore iterative(GameState gameState, int maxDepth){
        MoveScore best = null;
        for(int i = 1; i <= maxDepth; i++){
            best = findBestMove(gameState, i);
        }
        return best;
    }






}
