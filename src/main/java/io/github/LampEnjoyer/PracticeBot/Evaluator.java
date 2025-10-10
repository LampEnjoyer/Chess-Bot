package io.github.LampEnjoyer.PracticeBot;


import java.io.IOException;
import java.util.List;

public class Evaluator {

    static final int PAWN = 100;
    static final int KNIGHT = 300;
    static final int BISHOP = 300;
    static final int ROOK = 500;
    static final int QUEEN = 900;

    static final int CHECKMATE = 1000000;

    public static int positions = 0;
    public static int ttProbes = 0;
    public static int ttHits;
    public static int notFound;

    public static MoveScore [][] pvTable = new MoveScore[10][10];



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
        return evaluateMaterial(gameState, true) - evaluateMaterial(gameState, false);
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
    public static MoveScore getBestMove(GameState gameState, int depth, int alpha, int beta, boolean isWhite, MoveScore prevBest){
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
            TranspositionTable.store(hash, 0, score, depth, null);
            return new MoveScore(null, score);
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
            MoveScore result = getBestMove(gameState, depth - 1, alpha, beta, !isWhite, null);
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

            if (alpha >= beta) break; // Alpha-beta cutoff
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
            bestMove = getBestMove(gameState, i,Integer.MIN_VALUE, Integer.MAX_VALUE, gameState.getTurn(), bestMove);
        }
        return bestMove;
    }

}
