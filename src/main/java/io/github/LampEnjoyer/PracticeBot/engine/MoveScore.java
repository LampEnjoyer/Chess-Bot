package io.github.LampEnjoyer.PracticeBot.engine;

public class MoveScore {

    private Move move;
    private int score;

    public MoveScore(Move move, int score){
        this.move = move;
        this.score = score;
    }

    public Move getMove(){
        return move;
    }

    public int getScore(){
        return score;
    }

    public void setMove(Move move){
        this.move = move;
    }

    public void setScore(int score){
        this.score = score;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj){
            return true;
        }
        if(obj == null || (getClass() != obj.getClass())){
            return false;
        }
        return ((MoveScore)obj).getMove().equals(getMove());
    }

    @Override
    public String toString(){
        return move.toString() + " Evaluation: " + getScore();
    }

    @Override
    public int hashCode(){
        return getMove().hashCode();
    }
}
