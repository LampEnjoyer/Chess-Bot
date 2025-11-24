package io.github.LampEnjoyer.PracticeBot.engine;

public class MoveGenerator {

    private final long[][] pieceMoveMasks = new long[8][64]; //Only 8 because only king and pawn moves are color dependent

    public MoveGenerator(){
        setWhitePawnMoveMasks(); //arr[0]
        setKnightMoveMasks(); //arr[1]
        setBishopMoveMasks(); //arr[2]
        setRookMoveMasks(); //arr[3]
        setQueenMoveMasks(); //arr[4]
        setWhiteKingMoveMasks(); //arr[5]
        setBlackPawnMoveMasks(); //arr[6]
        setBlackKingMoveMasks(); //arr[7]
    }

    public void setWhitePawnMoveMasks(){
        long [] arr = new long[64];
        for(int rank = 1; rank <7; rank++){
            for(int col = 0; col<8; col++){
                long n = 0L;
                int index = rank * 8 + col;
                if(col != 0){
                    n |= (1L << (index + 7)); //Capturing right side
                }
                if(col != 7){
                    n |= (1L << (index + 9)); //Capturing left side
                }
                if(rank == 1){
                    n |= (1L << (index + 16)); //Pawn can only move forward twice, if it hasn't moved yet (Staying at the starting rank)
                }
                n |= (1L << (index+8)); //Moving up one
                arr[index] = n;
            }
        }
        pieceMoveMasks[0] = arr;
    }

    public void setBlackPawnMoveMasks(){
        long [] arr = new long[64];
        for(int rank = 1; rank <7; rank++){
            for(int col = 0; col<8; col++){
                long n = 0L;
                int index = rank * 8 + col;
                if(col != 0){
                    n |= (1L << (index - 9)); //Capturing right side
                }
                if(col != 7){
                    n |= (1L << (index - 7)); //Capturing left side
                }
                if(rank == 6){
                    n |= (1L << (index - 16)); //Pawn can only move forward twice, if it hasn't moved yet (Staying at the starting rank)
                }
                n |= (1L << (index-8)); //Moving down one
                arr[index] = n;
            }
        }
        pieceMoveMasks[6] = arr;
    }

    public void setBishopMoveMasks(){
        long [] arr = new long[64];
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for(int i = 0; i<64; i++){
            int rank = i / 8;
            int col = i % 8;
            long n = 0L;
            for(int[] d : directions){
                int newRank = rank;
                int newCol = col;
                while(true){
                    newRank += d[0];
                    newCol += d[1];
                    int index = newRank * 8 + newCol;
                    if(newRank < 0 || newRank > 7 || newCol < 0 || newCol > 7){
                        break;
                    }
                    n |= (1L << index);
                }
            }
            arr[i] = n;
        }
        pieceMoveMasks[2] = arr;

    }

    public void setRookMoveMasks(){ //Color independent of moves
        long [] arr = new long[64];
        for(int rank = 0; rank<8; rank++){
            for(int col = 0; col <8; col++){
                long n = 0L;
                int index = rank * 8 + col;
                for(int i = 0; i<8; i++){ //Traversing up and down
                    if(i != rank){
                        n |= (1L << (i * 8 + col));
                    }
                }
                for(int i = 0; i<8; i++){ //Traversing left and right Castling is already included here
                    if(i != col){
                        n |= (1L << (rank * 8 + i));
                    }
                }
                arr[index] = n;
            }
        }
        pieceMoveMasks[3] = arr;
    }

    public void setKnightMoveMasks(){
         long [] arr = new long[64];
         int [][] directions = { {-2,1}, {-2,-1} , {2,1}, {2,-1}, {1,2}, {1,-2}, {-1,2}, {-1,-2}};
         for(int i = 0; i<64; i++){
             int rank = i/8;
             int col = i % 8;
             long n = 0L;
             for(int [] d : directions){
                 int newRank = rank + d[0];
                 int newCol = col + d[1];
                 if(newRank < 8 && newRank >= 0 && newCol < 8 && newCol >= 0){
                     int index = newRank * 8 + newCol;
                     n |= (1L << index);
                 }
             }
             arr[i] = n;
         }
         pieceMoveMasks[1] = arr;
    }

    public void setQueenMoveMasks(){
        long [] arr = new long [64];
        for(int i = 0; i<64; i++){
            arr[i] = pieceMoveMasks[2][i] | pieceMoveMasks[3][i]; //Queen is just bishop + rook
        }
        pieceMoveMasks[4] = arr;
    }

    public void setWhiteKingMoveMasks(){
        long [] arr = new long [64];
        int [][] directions =  { {1,1} , {1,-1} , {-1,1} , {-1,-1}, {1,0}, {-1,0}, {0,1}, {0,-1} };
        for(int i = 0; i<64; i++){
            long n = 0L;
            int rank = i / 8;
            int col = i % 8;
            for(int [] d : directions){
                int newRank = rank + d[0];
                int newCol = col + d[1];
                if(newRank < 8 && newRank >= 0 && newCol < 8 && newCol >= 0){
                    int index = newRank * 8 + newCol;
                    n |= (1L << index);
                }
            }
            arr[i] = n;
        }
        arr[4] |= (1L << 2) | (1L << 6); //Castling
        pieceMoveMasks[5] = arr;
    }

    public void setBlackKingMoveMasks(){
        long [] arr = new long [64];
        int [][] directions =  { {1,1} , {1,-1} , {-1,1} , {-1,-1}, {1,0}, {-1,0}, {0,1}, {0,-1} };
        for(int i = 0; i<64; i++){
            long n = 0L;
            int rank = i / 8;
            int col = i % 8;
            for(int [] d : directions){
                int newRank = rank + d[0];
                int newCol = col + d[1];
                if(newRank < 8 && newRank >= 0 && newCol < 8 && newCol >= 0){
                    int index = newRank * 8 + newCol;
                    n |= (1L << index);
                }
            }
            arr[i] = n;
        }
        arr[60] |= (1L << 58) | (1L << 62); //Castling
        pieceMoveMasks[7] = arr;
    }

    public long[][] getPieceMoveMasks(){
        return pieceMoveMasks;
    }

}
