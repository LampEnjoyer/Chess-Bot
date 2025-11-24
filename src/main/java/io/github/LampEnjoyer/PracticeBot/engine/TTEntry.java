package io.github.LampEnjoyer.PracticeBot.engine;

public class TTEntry {
    private final long zobristHash;
    private int depth;
    private int score;
    private int flag; // 0 = Exact score, 1 = min, 2 = max
    private Move move;

    public TTEntry(long zobristHash, int depth, int score, int flag, Move move){
        this.zobristHash = zobristHash;
        this.depth = depth;
        this.score = score;
        this.flag = flag;
        this.move = move;
    }

    public long getZobristHash(){
        return zobristHash;
    }

    public int getDepth(){
        return depth;
    }

    public int getScore(){
        return score;
    }

    public int getFlag(){
        return flag;
    }

    public Move getMove(){
        return move;
    }

    public void setDepth(int depth){
        this.depth = depth;
    }

    public void setFlag(int flag){
        this.flag = flag;
    }

    public void setScore(int score){
        this.score = score;
    }

    public void setMove(Move move){
        this.move = move;
    }

}
