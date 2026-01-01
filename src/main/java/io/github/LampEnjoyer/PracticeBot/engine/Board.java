package io.github.LampEnjoyer.PracticeBot.engine;

public class Board {
    private long bitboard[] = new long[12];

    private final int [][] boardValues = {
        // Pawn
        {
            0,  0,  0,  0,  0,  0,  0,  0,
                50, 50, 50, 50, 50, 50, 50, 50,
                    10, 10, 20, 30, 30, 20, 10, 10,
                    5,  5, 10, 25, 25, 10,  5,  5,
                    0,  0,  0, 20, 20,  0,  0,  0,
                    5, -5,-10,  0,  0,-10, -5,  5,
                    5, 10, 10,-20,-20, 10, 10,  5,
                    0,  0,  0,  0,  0,  0,  0,  0
        },
        // Knight
        {
            -50,-40,-30,-30,-30,-30,-40,-50,
                    -40,-20,  0,  5,  5,  0,-20,-40,
                    -30,  5, 10, 15, 15, 10,  5,-30,
                    -30,  0, 15, 20, 20, 15,  0,-30,
                    -30,  5, 15, 20, 20, 15,  5,-30,
                    -30,  0, 10, 15, 15, 10,  0,-30,
                    -40,-20,  0,  0,  0,  0,-20,-40,
                    -50,-40,-30,-30,-30,-30,-40,-50
        },
        // Bishop
        {
            -20,-10,-10,-10,-10,-10,-10,-20,
                    -10,  0,  0,  0,  0,  0,  0,-10,
                    -10,  0,  5, 10, 10,  5,  0,-10,
                    -10,  5,  5, 10, 10,  5,  5,-10,
                    -10,  0, 10, 10, 10, 10,  0,-10,
                    -10, 10, 10, 10, 10, 10, 10,-10,
                    -10,  5,  0,  0,  0,  0,  5,-10,
                    -20,-10,-10,-10,-10,-10,-10,-20
        },
        // Rook
        {
            0,  0,  0,  0,  0,  0,  0,  0,
                    5, 10, 10, 10, 10, 10, 10,  5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    0,  0,  0,  5,  5,  0,  0,  0
        },
        // Queen
        {
            -20,-10,-10, -5, -5,-10,-10,-20,
                    -10,  0,  0,  0,  0,  5,  0,-10,
                    -10,  0,  5,  5,  5,  5,  5,-10,
                    -5,  0,  5,  5,  5,  5,  0, -5,
                    0,  0,  5,  5,  5,  5,  0, -5,
                    -10,  5,  5,  5,  5,  5,  0,-10,
                    -10,  0,  5,  0,  0,  0,  0,-10,
                    -20,-10,-10, -5, -5,-10,-10,-20
        },
        // King (middle game PST)
        {
            -30,-40,-40,-50,-50,-40,-40,-30,
                    -30,-40,-40,-50,-50,-40,-40,-30,
                    -30,-40,-40,-50,-50,-40,-40,-30,
                    -30,-40,-40,-50,-50,-40,-40,-30,
                    -20,-30,-30,-40,-40,-30,-30,-20,
                    -10,-20,-20,-20,-20,-20,-20,-10,
                    20, 20,  0,  0,  0,  0, 20, 20,
                    20, 30, 10,  0,  0, 10, 30, 20
        }
    };
    public Board(){
        createBoard();
    }

    public Board(long [] bitboard){ //Future stuff if people want to analyze a certain position
        for(int i = 0; i<12; i++){
            this.bitboard[i] = bitboard[i];
        }
    }

    public Board(String s){ //fern notation
        int index = 56;
        for(Character c : s.toCharArray()){
            int piece = pieceFromChar(c);
            if(piece != -1){
                bitboard[piece] |= (1L << index);
            } else if(c == '/'){
                index -= 16;
                continue;
            } else{
                while(c > '0'){
                    c--;
                    index++;
                }
                continue;
            }
            index++;
        }
    }

    public void updateBoard(String s){
        int index = 56;
        for(Character c : s.toCharArray()){
            int piece = pieceFromChar(c);
            if(piece != -1){
                bitboard[piece] |= (1L << index);
            } else if(c == '/'){
                index -= 16;
                continue;
            } else{
                while(c > '0'){
                    c--;
                    index++;
                }
                continue;
            }
            index++;
        }
    }
    private void createBoard(){
        // White pieces
        bitboard[0]  = 0x000000000000FF00L;  // White pawns
        bitboard[1]  = 0x0000000000000042L;  // White knights
        bitboard[2]  = 0x0000000000000024L;  // White bishops
        bitboard[3]  = 0x0000000000000081L;  // White rooks
        bitboard[4]  = 0x0000000000000008L;  // White queen
        bitboard[5]  = 0x0000000000000010L;  // White king

        // Black pieces
        bitboard[6]  = 0x00FF000000000000L;  // Black pawns
        bitboard[7]  = 0x4200000000000000L;  // Black knights
        bitboard[8]  = 0x2400000000000000L;  // Black bishops
        bitboard[9]  = 0x8100000000000000L;  // Black rooks
        bitboard[10] = 0x0800000000000000L;  // Black queen
        bitboard[11] = 0x1000000000000000L;  // Black king
    }

    public void printBoard(){
        char[] pieceSymbols = {'P', 'N', 'B', 'R', 'Q', 'K','p', 'n', 'b', 'r', 'q', 'k'}; //White is capital, black is lowercase
        for(int rank = 7; rank >= 0; rank--){
            for(int col  = 0; col < 8; col++){
                int index = rank * 8 + col;
                char piece = '.';
                for(int i = 0; i<12; i++){
                    if( ((1L << index) & bitboard[i]) != 0){ // != 0 because of the edge case of shifting 1 63 times to the left will cause overflow
                        piece = pieceSymbols[i];
                        break;
                    }
                }
                System.out.print(piece + " ");
            }
            System.out.println();
        }
    }

    public long [] getBitboard(){
        return bitboard;
    }
    public void printBoard(long board){
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

    public void updateBoard(Move move, int pieceType){
        //Extracting data from the move class
        int fromIndex = move.getFromLocation();
        int toIndex = move.getToLocation();
        int moveType = move.getMoveType();

        int capturedType = -1;//Default cause otherwise it would be 0 can mess up stuff
        if(moveType > 0){
            for(int i = 0; i<12; i++){
                if( (bitboard[i] & (1L << toIndex) )!= 0){
                    capturedType = i;
                    bitboard[capturedType] ^= (1L << toIndex);
                    break;
                }
            }
        }
        bitboard[pieceType] ^= (1L << fromIndex);
        bitboard[pieceType] |= (1L << toIndex);
    }

    public boolean isCollision(){
        long num = 0L;
        for(long l : bitboard){
            if((l & num) == 0){
                num |= l;
            }else{
                return true;
            }
        }
        return false;
    }

    public int pieceFromChar(char c){
        return switch (c) {
            case 'P' -> 0;
            case 'N' -> 1;
            case 'B' -> 2;
            case 'R' -> 3;
            case 'Q' -> 4;
            case 'K' -> 5;
            case 'p' -> 6;
            case 'n' -> 7;
            case 'b' -> 8;
            case 'r' -> 9;
            case 'q' -> 10;
            case 'k' -> 11;
            default -> -1;
        };
    }

    public String getFenNotation() {
        char[] pieceSymbols = {'P', 'N', 'B', 'R', 'Q', 'K', 'p', 'n', 'b', 'r', 'q', 'k'};
        StringBuilder sb = new StringBuilder();
        int index = 56;
        int counter = 0;
        boolean found = false;
        while (index >= 0) {
            for (int i = index; i < index + 8; i++) { //Looping through each row
                for (int j = 0; j < 12; j++) { //Looping through each board to find if a piece matches
                    if ((bitboard[j] & (1L << i)) != 0) {
                        if (counter != 0) {
                            sb.append(counter);
                        }
                        sb.append(pieceSymbols[j]);
                        counter = 0;
                        found = true;
                    }
                }
                if(!found){
                    counter++;
                }
                found = false;
            }
            if(counter != 0){
                sb.append(counter);
                counter = 0;
            }
            if(index != 0){
                sb.append('/');
            }
            index -= 8;
        }
        return sb.toString();
    }

    public int [][] getBoardValues(){
        return boardValues;
    }
}
