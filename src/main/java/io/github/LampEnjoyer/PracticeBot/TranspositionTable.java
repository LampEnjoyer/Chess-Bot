package io.github.LampEnjoyer.PracticeBot;

public class TranspositionTable {

    private static TTEntry [] table = new TTEntry[1 << 20];

    public static void store(long zobristHash, int depth, int score, int flag, Move bestMove) {
        int index = (int)(Math.abs(zobristHash % (table.length)));
        TTEntry existing = table[index];
        if(existing == null){
            existing = new TTEntry(zobristHash,depth,score,flag,bestMove);
            table[index] = existing;
        } else if(existing.getZobristHash() == zobristHash){
            table[index] = new TTEntry(zobristHash,depth,score,flag,bestMove);
        } else if(existing.getScore() <= score){
            table[index] = new TTEntry(zobristHash,depth,score,flag,bestMove);
        } else if(depth > existing.getDepth()){
            table[index] = new TTEntry(zobristHash,depth,score,flag,bestMove);
        }
    }

    public static TTEntry retrieve(long zobristHash) {
        int index = (int)(Math.abs(zobristHash % (table.length)));

        TTEntry entry = table[index];
        if (entry != null && entry.getZobristHash() == zobristHash) {
            return entry;
        }
        return null; // not found or collision
    }
}
