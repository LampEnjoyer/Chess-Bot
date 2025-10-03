package io.github.LampEnjoyer.PracticeBot;

import java.util.Objects;

public class Move {
    private static final int MASK_FROM = 0x3F;      // 00000000 00111111 (bits 1-6)
    private static final int MASK_TO = 0xFC0;   // 00001111 11000000 (bits 7-12)
    private static final int MOVE_TYPE = 0x3 << 12; // 11 000000 000000 (bits 13-14)
    private static final int PROMOTION = 0x7 << 14; // 111 00 000000 000000 (bits 15-17)

    private int score = 0;

    // Packed move (32-bit int) but we only need first 17 :( could've used a short instead
    private final int moveValue;

    public Move(int from, int to, int moveType, int promotionPiece){
        this.moveValue = from | (to << 6) | (moveType << 12) | (promotionPiece << 14);
    }

    public Move(){
        moveValue = 0;
    }

    public int getFromLocation(){
        return moveValue & MASK_FROM;
    }

    public int getToLocation(){
        return (moveValue & MASK_TO) >> 6;
    }

    public int getMoveType(){
        int num = (moveValue & MOVE_TYPE) >> 12;
        return num; //0 = move, 1= Capture, 2 = Castle, 3 = En-Passant
    }

    public int getPromotion(){
        return (moveValue & PROMOTION) >> 14;
        //0 = no Promote, 1 = Knight, 2 = Bishop, 3 = Rook, 4 = Queen
    }

    public int getMoveValue(){
        return moveValue;
    }

    public void setScore(int score){
        this.score = score;
    }

    public int getScore(){
        return score;
    }

    @Override
    public String toString(){
        return ("From: " + getFromLocation() + " To: " + getToLocation() + " MoveType: " + getMoveType() + " Promotion: " + getPromotion()) + " Score: " + getScore();
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj){
            return true;
        }
        if(obj == null || obj.getClass() != getClass()){
            return false;
        }
        return ((Move)obj).getMoveValue()  == getMoveValue();
    }

    @Override
    public int hashCode(){
        return Objects.hash(moveValue);
    }
}
