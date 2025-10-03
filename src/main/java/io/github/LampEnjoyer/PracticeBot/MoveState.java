package io.github.LampEnjoyer.PracticeBot;

//This class is an extension of the Move class and is designed to help us undoMoves during our search tree/checking if moves are legal
public class MoveState {
    private Move move;
//    int capturedPieces; //identity of the captured piece, 1 = Pawn, 2 = Knight, 3 = Bishop, 4 = Rook, 5 = Queen, 0 = 0
//    int wasWhiteTurn; 0 =  black, 1 = white, we use int here so we can shift it instead of casting it later
//    int oldCastlingRights; // 4 bits, White KingSide, White QueenSide, Black KingSide, Black QueenSide (Left to right)
//    int oldEnPassantSquare; //the index of the pawn that got enPassant (6 bits)
//    int oldMoveCounter; //100-move rule 7 bit

    private int moveValue; //Bits 1-3 = Capture Piece, Bits 4-7 = Castle State, 8-14 = Move Counter, 15-21 = EnPassant Index, Bit 22 = Whose turn (Bit 24
    private final int CAPTURED_MASK = 0x7; //111
    private final int CASTLE_MASK = 0xF << 3; //11110000
    private final int MOVE_MASK = 0x7F << 8; //111111100000000000
    private final int ENPASSANT_MASK = 0x7F << 14;  //Holds the index of the pawn that can be enpassanted
    private final int COLOR_MASK = 1 << 21;

    public MoveState(Move move, int capturedPieces, int oldCastlingRights, int oldMoveCounter, int oldEnPassantSquare, int wasWhiteTurn){
        this.move = move;
        if(oldEnPassantSquare == -1){
            oldEnPassantSquare = 127;
        }
        this.moveValue = capturedPieces | (oldCastlingRights << 3) | (oldMoveCounter << 7) | (oldEnPassantSquare << 14) | (wasWhiteTurn << 21);
    }

    public int getCapturedPiece(){
        return moveValue & CAPTURED_MASK;
    }

    public int getOldCastlingRights(){return (moveValue & CASTLE_MASK) >> 3;
    }

    public int getOldMoveCounter(){
        return (moveValue & MOVE_MASK) >> 7;
    }

    public int getOldEnPassantSquare(){
        int enPassant = (moveValue & ENPASSANT_MASK)  >> 14;
        if(enPassant == 127){
            return -1;
        }
        return (moveValue & ENPASSANT_MASK)  >> 14;
    }

    public int wasWhiteTurn(){
        return (moveValue & COLOR_MASK) >> 21;
    }

    public Move getMove(){
        return move;
    }

    public int getMoveValue(){return moveValue;}

    @Override
    public String toString(){
        return (getMove() + " Captured Piece: " + getCapturedPiece());
    }








}
