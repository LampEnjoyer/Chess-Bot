package io.github.LampEnjoyer.PracticeBot.engine;


import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public enum PieceType {

    PAWN(0){
        @Override
        public boolean isValidMove(GameState gameState, Move move, long [][] moveMasks){
            boolean isWhite = gameState.getTurn();
            int shift = isWhite ? 0 : 6;
            int fromIndex = move.getFromLocation();
            int toIndex = move.getToLocation();
            long friendlyPieces = gameState.getTurn() ? gameState.getWhiteBoard() : gameState.getBlackBoard();
            long enemyPieces = gameState.getTurn() ? gameState.getBlackBoard() : gameState.getWhiteBoard();
            long occupied = friendlyPieces | enemyPieces;
            long pawnMask = moveMasks[shift][fromIndex];

            int dir = isWhite ? 8 : - 8;
            int doubleDir = dir * 2;

            if( ((1L << toIndex) & pawnMask) == 0){
                return false;
            }
            if(toIndex == (fromIndex + dir) && ((1L << toIndex) & occupied) == 0){
                return true;
            }
            if(toIndex == (fromIndex + doubleDir) && ((1L << toIndex) & occupied) == 0
            && (((1L << (fromIndex + dir))) & occupied) == 0) { //double move
                return true;
            }
            boolean isStraight = (Math.abs(toIndex-fromIndex) % 8) == 0;
            if(!isStraight && ((1L << toIndex) & enemyPieces) != 0){
                return true;
            } else return !isStraight && toIndex == gameState.getCurrentEnPassantSquare();
        }

        @Override
        public boolean attacksKing(GameState gameState, long[][] moveMasks, boolean isWhite){
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long kingIndex = isWhite ? boards[11]: boards[5]; //Finding the kingBoard depending on color
            long pawnBoard = boards[shift]; //Board of pawns
            long FILE_A = 0x0101010101010101L; //Edge pieces
            long FILE_H = 0x8080808080808080L;
            long pawnAttacks = 0L;
            if(shift == 0){
                pawnAttacks |= ((kingIndex >>> 7) & ~FILE_A )| ((kingIndex >>> 9) & ~FILE_H);
            } else{
                pawnAttacks = ((kingIndex << 7) & ~FILE_H) | ((kingIndex << 9) & ~FILE_A);
            }
            return (pawnAttacks & pawnBoard) != 0;
        }

        @Override
        public int numAttackers(GameState gameState, long[][] pieceMoveMasks) {
            boolean isWhite = gameState.getTurn();
            long [] boards = gameState.getBoard().getBitboard();
            long kingBoard = !isWhite ? boards[5] : boards[11];
            long pawnBoard = isWhite ? boards[0] : boards[6];
            int kingIndex = Long.numberOfTrailingZeros(kingBoard);

            int diff= isWhite ? 8 : -8;
            if(kingIndex % 8 != 0){
                int newIndex = kingIndex + diff - 1; //left
                if(((1L << newIndex) & pawnBoard) != 0){return  1;}
            }
            if(kingIndex % 8 != 7){
                int newIndex = kingIndex + diff + 1; //Right
                if((((1L << newIndex)) & pawnBoard) != 0){ return 1;}
            }
            return 0;
        }


        @Override
        public List<Move> generateMoves(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            List<Move> list =  new ArrayList<>();
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long pawnBoard = boards[shift];
            long enemyPieces = gameState.getTurn() ? gameState.getBlackBoard() : gameState.getWhiteBoard();
            long friendlyPieces = gameState.getTurn() ? gameState.getWhiteBoard() : gameState.getBlackBoard();
            long occupied = enemyPieces | friendlyPieces;
            long [] pawnMoveMasks = pieceMoveMasks[shift];
            Stack<MoveState> moveHistory = gameState.getMoveHistory();
            while(pawnBoard != 0){
                int pawnIndex = Long.numberOfTrailingZeros(pawnBoard);
                long pawnMask = pawnMoveMasks[pawnIndex] & ~friendlyPieces;
                while(pawnMask != 0){
                    int toIndex = Long.numberOfTrailingZeros(pawnMask);
                    int moveType = 0;
                    boolean isStraight = Math.abs(pawnIndex - toIndex) % 8 == 0;
                    boolean possibleCapture = ((1L << toIndex ) & enemyPieces) != 0;
                    if (isStraight) {
                        int diff = isWhite ? 8 : -8;// Handle double pawn move
                        if (toIndex == pawnIndex + 2 * diff) {
                            int intermediate = pawnIndex + diff;
                            if (((1L << intermediate) & (friendlyPieces | enemyPieces)) != 0 ||
                                    ((1L << toIndex) & (friendlyPieces | enemyPieces)) != 0) {
                                pawnMask &= ~(1L << toIndex);
                                continue;
                            }
                        }// No straight captures
                        if (possibleCapture) {
                            pawnMask &= ~(1L << toIndex);
                            continue;
                        }
                    } else if( (enemyPieces & (1L << toIndex)) != 0){ //Diagonals capturing
                        moveType = 1;
                    } else if(!moveHistory.isEmpty() && toIndex == gameState.getCurrentEnPassantSquare()){
                        moveType = 3;
                    } else{
                        pawnMask &= ~(1L << toIndex);
                        continue;
                    }
                    if(toIndex / 8 == 0 || toIndex / 8 == 7){
                        for(int i = 1; i<=4; i++){
                            list.add(new Move(pawnIndex,toIndex,moveType,i));
                        }
                    }else{
                        list.add(new Move(pawnIndex,toIndex,moveType,0));
                    }
                    pawnMask &= ~(1L << toIndex);
                }
                pawnBoard &= ~ (1L<<pawnIndex);
            }
            return list;
        }

        @Override
        public long getAttackMask(GameState gameState, long [][] pieceMoveMasks, boolean isWhite){
            long l = 0L;
            int shift = isWhite ? 0 : 6;
            int dir = isWhite ? 1 : -1;
            long pawnBoard = gameState.getBoard().getBitboard()[shift];
            while(pawnBoard != 0){
                int pawnIndex = Long.numberOfTrailingZeros(pawnBoard);
                int row = pawnIndex / 8;
                int col = pawnIndex % 8;
                if(col + dir < 8){ //checking if outof bounds row will never be out of bounds only col
                    l |= (1L << (row + dir) * 8 + col + dir);
                }
                if(col - dir >= 0) {
                    l |= (1L << (row + dir) * 8 + col - dir);
                }
                pawnBoard &= ~(1L << pawnIndex);
            }
            return l;
        }
    },
    KNIGHT(1){
        @Override
        public boolean isValidMove(GameState gameState, Move move, long [][] moveMasks){
            int fromIndex = move.getFromLocation();
            int toIndex = move.getToLocation();
            return (moveMasks[index][fromIndex] & (1L << toIndex)) != 0;
        }

        @Override
        public boolean attacksKing(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) { //Early return statement as opposed to others
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long kingIndex = isWhite ? boards[11]: boards[5];
            long knightBoard = boards[shift + 1];

            while(knightBoard != 0){
                int knightIndex = Long.numberOfTrailingZeros(knightBoard);
                if((pieceMoveMasks[1][knightIndex] & kingIndex) != 0){
                    return true;
                }
                knightBoard &= ~(1L << knightIndex);
            }
            return false;
        }

        @Override
        public int numAttackers(GameState gameState, long[][] pieceMoveMasks) {
            long[] boards = gameState.getBoard().getBitboard();
            int shift = gameState.getTurn() ? 0 : 6;
            long kingIndex = gameState.getTurn() ? boards[11]: boards[5];
            long knightBoard = boards[shift + 1];
            int count = 0;
            while(knightBoard != 0){
                int knightIndex = Long.numberOfTrailingZeros(knightBoard);
                if((pieceMoveMasks[1][knightIndex] & kingIndex) != 0){
                    count++;
                }
                knightBoard &= ~(1L << knightIndex);
            }
            return count;
        }

        @Override
        public List<Move> generateMoves(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            List<Move> list =  new ArrayList<>();
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long knightBoard = boards[shift + 1];
            long enemyPieces = gameState.getTurn() ? gameState.getBlackBoard() : gameState.getWhiteBoard();
            long[] knightMoveMasks = pieceMoveMasks[1];
            long friendlyPieces = gameState.getTurn() ? gameState.getWhiteBoard() : gameState.getBlackBoard();
            while(knightBoard != 0){
                int knightIndex = Long.numberOfTrailingZeros(knightBoard);
                long moves = knightMoveMasks[knightIndex] &= ~friendlyPieces;
                while (moves != 0){
                    int toIndex = Long.numberOfTrailingZeros(moves);
                    int moveType = (1L << toIndex & enemyPieces) == 0 ? 0 : 1;
                    Move move = new Move(knightIndex,toIndex, moveType, 0);
                    list.add(move);
                    moves &= ~(1L << toIndex);
                }
                knightBoard &= ~(1L << knightIndex);
            }
            return list;
        }

        @Override
        public long getAttackMask(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            long attackMask = 0L;
            long [] board = gameState.getBoard().getBitboard();
            int index = isWhite ? 1 : 7;
            long knightBoard = board[index];
            long friendlyPieces = isWhite ? gameState.getWhiteBoard() : gameState.getBlackBoard();
            while(knightBoard != 0){
                int knightIndex = Long.numberOfTrailingZeros(knightBoard);
                long knightMask = pieceMoveMasks[1][knightIndex];
                while(knightMask != 0){
                    int moveIndex = Long.numberOfTrailingZeros(knightMask);
                    attackMask |= (1L << moveIndex);
                    knightMask &= ~(1L << moveIndex);
                }
                knightBoard &= ~(1L << knightIndex);
            }
            return attackMask & ~friendlyPieces;
        }
    },
    BISHOP(2){
        @Override
        public boolean isValidMove(GameState gameState, Move move, long [][] moveMasks) {
            int fromIndex = move.getFromLocation();
            int toIndex = move.getToLocation();
            long [] magicNumbers = MoveValidator.getBishopMagicNumbers();
            long [][] attackTable = MoveValidator.getBishopAttackTable();
            long blockerBoard = MoveValidator.getBishopBlockerBoard(gameState, fromIndex);
            int bits = countBits(MoveValidator.getBishopBlockerMask(fromIndex));
            int attackIndex = (int) ((blockerBoard * magicNumbers[fromIndex]) >>> (64-bits));
            return (( (1L << toIndex) & attackTable[fromIndex][attackIndex]) != 0);
        }

        @Override
        public boolean attacksKing(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long kingIndex = isWhite ? boards[11]: boards[5];
            long [] magicNumbers = MoveValidator.getBishopMagicNumbers();
            long [][] attackTable = MoveValidator.getBishopAttackTable();
            long bishopBoard = boards[shift + 2];

            while(bishopBoard != 0){
                int bishopIndex = Long.numberOfTrailingZeros(bishopBoard);
                long blockerBoard = MoveValidator.getBishopBlockerBoard(gameState, bishopIndex);
                int numBits = countBits(MoveValidator.getBishopBlockerMask(bishopIndex));
                int attackIndex = (int) ((blockerBoard * magicNumbers[bishopIndex]) >>> (64-numBits));
                if( (attackTable[bishopIndex][attackIndex] & kingIndex) != 0){
                    return true;
                }
                bishopBoard &= ~(1L << bishopIndex);
            }
            return false;
        }

        @Override
        public int numAttackers(GameState gameState, long[][] pieceMoveMasks) {
            int count = 0;
            long[] boards = gameState.getBoard().getBitboard();
            int shift = gameState.getTurn() ? 0 : 6;
            long kingIndex = gameState.getTurn() ? boards[11]: boards[5];
            long [] magicNumbers = MoveValidator.getBishopMagicNumbers();
            long [][] attackTable = MoveValidator.getBishopAttackTable();
            long bishopBoard = boards[shift + 2];

            while(bishopBoard != 0){
                int bishopIndex = Long.numberOfTrailingZeros(bishopBoard);
                long blockerBoard = MoveValidator.getBishopBlockerBoard(gameState, bishopIndex);
                int numBits = countBits(MoveValidator.getBishopBlockerMask(bishopIndex));
                int attackIndex = (int) ((blockerBoard * magicNumbers[bishopIndex]) >>> (64-numBits));
                if( (attackTable[bishopIndex][attackIndex] & kingIndex) != 0){
                    count++;
                }
                bishopBoard &= ~(1L << bishopIndex);
            }
            return count;
        }

        @Override
        public List<Move> generateMoves(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            List<Move> list = new ArrayList<>();
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long [] magicNumbers = MoveValidator.getBishopMagicNumbers();
            long [][] attackTable = MoveValidator.getBishopAttackTable();
            long bishopBoard = boards[shift + 2];
            long enemyPieces = gameState.getTurn() ? gameState.getBlackBoard() : gameState.getWhiteBoard();
            long friendlyPieces = gameState.getTurn() ? gameState.getWhiteBoard() : gameState.getBlackBoard();


            while(bishopBoard != 0){
                int bishopIndex = Long.numberOfTrailingZeros(bishopBoard);
                long blockerBoard = MoveValidator.getBishopBlockerBoard(gameState, bishopIndex);
                int numBits = countBits(MoveValidator.getBishopBlockerMask(bishopIndex));
                int attackIndex =  (int) ((blockerBoard * magicNumbers[bishopIndex])  >>> (64-numBits));
                long attackBoard =  attackTable[bishopIndex][attackIndex] & ~friendlyPieces;

                while(attackBoard != 0){
                    int toIndex = Long.numberOfTrailingZeros(attackBoard);
                    int moveType = (((1L << toIndex) & enemyPieces) != 0) ? 1 : 0;
                    list.add(new Move(bishopIndex, toIndex, moveType, 0));
                    attackBoard &= ~(1L << toIndex);
                }
                bishopBoard &= ~(1L << bishopIndex);
            }

            return list;
        }

        @Override
        public long getAttackMask(GameState gameState, long[][] pieceMoveMasks, boolean isWhite){
            long l = 0L;
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long [] magicNumbers = MoveValidator.getBishopMagicNumbers();
            long [][] attackTable = MoveValidator.getBishopAttackTable();
            long bishopBoard = boards[shift + 2];
            long friendlyPieces = isWhite ? gameState.getWhiteBoard() : gameState.getBlackBoard();

            while(bishopBoard != 0){
                int bishopIndex = Long.numberOfTrailingZeros(bishopBoard);
                long blockerBoard = MoveValidator.getBishopBlockerBoard(gameState, bishopIndex);
                int numBits = countBits(MoveValidator.getBishopBlockerMask(bishopIndex));
                int attackIndex =  (int) ((blockerBoard * magicNumbers[bishopIndex])  >>> (64-numBits));
                long attackBoard =  attackTable[bishopIndex][attackIndex] & ~friendlyPieces;
                l |= attackBoard;
                bishopBoard &= ~(1L << bishopIndex);
            }
            return l;
        }


    },
    ROOK(3){
        @Override
        public boolean isValidMove(GameState gameState, Move move, long [][] moveMasks){
            int fromIndex = move.getFromLocation();
            int toIndex = move.getToLocation();
            long [] magicNumbers = MoveValidator.getRookMagicNumbers();
            long [][] attackTable = MoveValidator.getRookAttackTable();

            long blockerBoard = MoveValidator.getRookBlockerBoard(gameState, fromIndex);
            int bits = countBits(MoveValidator.getRookBlockerMask(fromIndex));
            int attackIndex = (int) ((blockerBoard * magicNumbers[fromIndex]) >>> (64-bits));
            return  (( (1L << toIndex) & attackTable[fromIndex][attackIndex]) != 0 );
        }

        @Override
        public boolean attacksKing(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long kingIndex = isWhite ? boards[11]: boards[5];
            long [] magicNumbers = MoveValidator.getRookMagicNumbers();
            long [][] attackTable = MoveValidator.getRookAttackTable();
            long rookBoard = boards[shift + 3];

            while(rookBoard != 0){
                int rookIndex = Long.numberOfTrailingZeros(rookBoard);
                long blockerBoard = MoveValidator.getRookBlockerBoard(gameState, rookIndex);
                int bits = countBits(MoveValidator.getRookBlockerMask(rookIndex));
                int attackIndex = (int)((blockerBoard * magicNumbers[rookIndex]) >>> (64 - bits));
                if( (attackTable[rookIndex][attackIndex] & kingIndex) != 0){
                    return true;
                }
                rookBoard &= ~(1L << rookIndex);
            }
            return false;
        }

        @Override
        public int numAttackers(GameState gameState, long[][] pieceMoveMasks) {
            int count = 0;
            long[] boards = gameState.getBoard().getBitboard();
            int shift = gameState.getTurn() ? 0 : 6;
            long kingIndex = gameState.getTurn() ? boards[11]: boards[5];
            long [] magicNumbers = MoveValidator.getRookMagicNumbers();
            long [][] attackTable = MoveValidator.getRookAttackTable();
            long rookBoard = boards[shift + 3];

            while(rookBoard != 0){
                int rookIndex = Long.numberOfTrailingZeros(rookBoard);
                long blockerBoard = MoveValidator.getRookBlockerBoard(gameState, rookIndex);
                int bits = countBits(MoveValidator.getRookBlockerMask(rookIndex));
                int attackIndex = (int)((blockerBoard * magicNumbers[rookIndex]) >>> (64 - bits));
                if( (attackTable[rookIndex][attackIndex] & kingIndex) != 0){
                    count++;
                }
                rookBoard &= ~(1L << rookIndex);
            }
            return count;
        }

        @Override
        public List<Move> generateMoves(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            List<Move> list = new ArrayList<>();
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long [] magicNumbers = MoveValidator.getRookMagicNumbers();
            long [][] attackTable = MoveValidator.getRookAttackTable();
            long enemyPieces = gameState.getTurn() ? gameState.getBlackBoard() : gameState.getWhiteBoard();
            long friendlyPieces = gameState.getTurn() ? gameState.getWhiteBoard() : gameState.getBlackBoard();

            long rookBoard = boards[shift + 3];

            while(rookBoard != 0){
                int rookIndex = Long.numberOfTrailingZeros(rookBoard);
                long blockerBoard = MoveValidator.getRookBlockerBoard(gameState,rookIndex);
                int numBits = countBits(MoveValidator.getRookBlockerMask(rookIndex));
                int attackIndex = (int)((blockerBoard * magicNumbers[rookIndex]) >>> (64-numBits));
                long attackMask = attackTable[rookIndex][attackIndex] & ~friendlyPieces;
                while(attackMask != 0){
                    int toIndex = Long.numberOfTrailingZeros(attackMask);
                    int moveType = ((1L << toIndex & enemyPieces) != 0) ? 1 : 0;
                    list.add(new Move(rookIndex, toIndex, moveType, 0));
                    attackMask &= ~(1L << toIndex);
                }
                rookBoard &= ~(1L << rookIndex);
            }

            return list;
        }

        @Override
        public long getAttackMask(GameState gameState, long[][] pieceMoveMasks, boolean isWhite){
            long l = 0L;
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long [] magicNumbers = MoveValidator.getRookMagicNumbers();
            long [][] attackTable = MoveValidator.getRookAttackTable();
            long friendlyPieces = gameState.getTurn() ? gameState.getWhiteBoard() : gameState.getBlackBoard();
            long rookBoard = boards[shift + 3];

            while(rookBoard != 0){
                int rookIndex = Long.numberOfTrailingZeros(rookBoard);
                long blockerBoard = MoveValidator.getRookBlockerBoard(gameState,rookIndex);
                int numBits = countBits(MoveValidator.getRookBlockerMask(rookIndex));
                int attackIndex = (int)((blockerBoard * magicNumbers[rookIndex]) >>> (64-numBits));
                long attackMask = attackTable[rookIndex][attackIndex] & ~friendlyPieces;

                l |= attackMask;
                rookBoard &= ~(1L << rookIndex);
            }
            return l;
        }

    },
    QUEEN(4){
        @Override
        public boolean isValidMove(GameState gameState, Move move, long [][] moveMasks){
            int fromIndex = move.getFromLocation();
            int toIndex = move.getToLocation();
            long [] bishopMagic = MoveValidator.getBishopMagicNumbers();
            long [] rookMagic = MoveValidator.getRookMagicNumbers();
            long [][] bishopAttackTable = MoveValidator.getBishopAttackTable();
            long [][] rookAttackTable = MoveValidator.getRookAttackTable();

            long bishopBlocker = MoveValidator.getBishopBlockerBoard(gameState, fromIndex);
            int bishopBits = countBits(MoveValidator.getBishopBlockerMask(fromIndex));
            long bishopAttack = bishopAttackTable[fromIndex] [(int) ((bishopBlocker * bishopMagic[fromIndex]) >>> (64-bishopBits))];

            long rookBlocker = MoveValidator.getRookBlockerBoard(gameState, fromIndex);
            int rookBits = countBits(MoveValidator.getRookBlockerMask(fromIndex));
            long rookAttack = rookAttackTable[fromIndex][(int) ((rookBlocker * rookMagic[fromIndex]) >>> (64 - rookBits))];
            long queenAttack = rookAttack | bishopAttack;
            return ((1L << toIndex) & queenAttack) != 0;
        }

        @Override
        public boolean attacksKing(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long queenBoard = boards[4 + shift];
            long kingIndex = isWhite ? boards[11]: boards[5];
            long [] bishopMagic = MoveValidator.getBishopMagicNumbers();
            long [] rookMagic = MoveValidator.getRookMagicNumbers();
            long [][] bishopAttackTable = MoveValidator.getBishopAttackTable();
            long [][] rookAttackTable = MoveValidator.getRookAttackTable();

            while(queenBoard != 0){
                int queenIndex = Long.numberOfTrailingZeros(queenBoard);
                long bishopBlocker = MoveValidator.getBishopBlockerBoard(gameState, queenIndex);
                int bishopBits = countBits(MoveValidator.getBishopBlockerMask(queenIndex));
                long bishopAttack = bishopAttackTable[queenIndex] [(int) ((bishopBlocker * bishopMagic[queenIndex]) >>> (64-bishopBits))];

                long rookBlocker = MoveValidator.getRookBlockerBoard(gameState, queenIndex);
                int rookBits = countBits(MoveValidator.getRookBlockerMask(queenIndex));
                long rookAttack = rookAttackTable[queenIndex][(int) ((rookBlocker * rookMagic[queenIndex]) >>> (64 - rookBits))];
                long queenAttack = rookAttack | bishopAttack;

                if((queenAttack & kingIndex) != 0){
                    return true;
                }
                queenBoard &= ~(1L << queenIndex);
            }
            return false;
        }

        @Override
        public int numAttackers(GameState gameState, long[][] pieceMoveMasks) {
            int count = 0;
            long[] boards = gameState.getBoard().getBitboard();
            int shift = gameState.getTurn() ? 0 : 6;
            long queenBoard = boards[4 + shift];
            long kingIndex = gameState.getTurn() ? boards[11]: boards[5];
            long [] bishopMagic = MoveValidator.getBishopMagicNumbers();
            long [] rookMagic = MoveValidator.getRookMagicNumbers();
            long [][] bishopAttackTable = MoveValidator.getBishopAttackTable();
            long [][] rookAttackTable = MoveValidator.getRookAttackTable();

            while(queenBoard != 0){
                int queenIndex = Long.numberOfTrailingZeros(queenBoard);
                long bishopBlocker = MoveValidator.getBishopBlockerBoard(gameState, queenIndex);
                int bishopBits = countBits(MoveValidator.getBishopBlockerMask(queenIndex));
                long bishopAttack = bishopAttackTable[queenIndex] [(int) ((bishopBlocker * bishopMagic[queenIndex]) >>> (64-bishopBits))];

                long rookBlocker = MoveValidator.getRookBlockerBoard(gameState, queenIndex);
                int rookBits = countBits(MoveValidator.getRookBlockerMask(queenIndex));
                long rookAttack = rookAttackTable[queenIndex][(int) ((rookBlocker * rookMagic[queenIndex]) >>> (64 - rookBits))];
                long queenAttack = rookAttack | bishopAttack;

                if((queenAttack & kingIndex) != 0){
                    count++;
                }
                queenBoard &= ~(1L << queenIndex);
            }
            return count;
        }

        @Override
        public List<Move> generateMoves(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            List<Move> list = new ArrayList<>();
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long queenBoard = boards[4 + shift];
            long [] bishopMagic = MoveValidator.getBishopMagicNumbers();
            long [] rookMagic = MoveValidator.getRookMagicNumbers();
            long [][] bishopAttackTable = MoveValidator.getBishopAttackTable();
            long [][] rookAttackTable = MoveValidator.getRookAttackTable();
            long friendlyPieces = gameState.getTurn() ? gameState.getWhiteBoard() : gameState.getBlackBoard();
            long enemyPieces = !gameState.getTurn() ? gameState.getWhiteBoard() : gameState.getBlackBoard();
            while(queenBoard != 0){
                int queenIndex = Long.numberOfTrailingZeros(queenBoard);
                long rookBlocker = MoveValidator.getRookBlockerBoard(gameState, queenIndex);
                int rookBits = countBits(MoveValidator.getRookBlockerMask(queenIndex));
                long bishopBlocker = MoveValidator.getBishopBlockerBoard(gameState, queenIndex);
                int bishopBits = countBits(MoveValidator.getBishopBlockerMask(queenIndex));

                long bishopAttack = bishopAttackTable[queenIndex][(int) ((bishopMagic[queenIndex] * bishopBlocker) >>> (64-bishopBits))];
                long rookAttack = rookAttackTable[queenIndex][(int) ((rookMagic[queenIndex] * rookBlocker) >>> (64-rookBits))];

                long queenAttack = (bishopAttack | rookAttack) & ~friendlyPieces;

                while(queenAttack != 0){
                    int toIndex = Long.numberOfTrailingZeros(queenAttack);
                    int moveType = ((1L << toIndex) & enemyPieces) != 0 ? 1 : 0;
                    list.add(new Move(queenIndex, toIndex, moveType, 0));
                    queenAttack &= ~(1L << toIndex);
                }
                queenBoard &= ~(1L << queenIndex);
            }
            return list;
        }
        @Override
        public long getAttackMask(GameState gameState, long [][] pieceMoveMasks, boolean isWhite){
            long l = 0L;
            long[] boards = gameState.getBoard().getBitboard();
            int shift = isWhite ? 0 : 6;
            long queenBoard = boards[4 + shift];
            long [] bishopMagic = MoveValidator.getBishopMagicNumbers();
            long [] rookMagic = MoveValidator.getRookMagicNumbers();
            long [][] bishopAttackTable = MoveValidator.getBishopAttackTable();
            long [][] rookAttackTable = MoveValidator.getRookAttackTable();
            long friendlyPieces = gameState.getTurn() ? gameState.getWhiteBoard() : gameState.getBlackBoard();

            while(queenBoard != 0){
                int queenIndex = Long.numberOfTrailingZeros(queenBoard);
                long rookBlocker = MoveValidator.getRookBlockerBoard(gameState, queenIndex);
                int rookBits = countBits(MoveValidator.getRookBlockerMask(queenIndex));
                long bishopBlocker = MoveValidator.getBishopBlockerBoard(gameState, queenIndex);
                int bishopBits = countBits(MoveValidator.getBishopBlockerMask(queenIndex));

                long bishopAttack = bishopAttackTable[queenIndex][(int) ((bishopMagic[queenIndex] * bishopBlocker) >>> (64-bishopBits))];
                long rookAttack = rookAttackTable[queenIndex][(int) ((rookMagic[queenIndex] * rookBlocker) >>> (64-rookBits))];

                long queenAttack = (bishopAttack | rookAttack) & ~friendlyPieces;
                l |= queenAttack;
                queenBoard &= ~(1L << queenIndex);
            }
            return l;
        }
    },
    KING(5){
        @Override
        public boolean isValidMove(GameState gameState, Move move, long [][] moveMasks){
            int movingPiece = gameState.getTurn() ? 5 : 11;
            if(move.getMoveType() == 2){
                return MoveValidator.canCastle(gameState, move, movingPiece);
            }
            return true;
        }
        @Override
        public boolean attacksKing(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            long [] boards = gameState.getBoard().getBitboard();
            int ourKingIndex = isWhite ? Long.numberOfTrailingZeros(boards[5]) : Long.numberOfTrailingZeros(boards[11]);
            long oppositeKing = !isWhite ? boards[5] : boards[11];
            int index = isWhite ? 5 : 7;
            if(ourKingIndex == 64){
                gameState.getBoard().printBoard();
                System.err.println("Here");
                System.out.println(gameState.getBoard().isCollision());
                throw new RuntimeException();
            }
            return (pieceMoveMasks[index][ourKingIndex] & oppositeKing) != 0;
        }

        @Override
        public int numAttackers(GameState gameState, long[][] pieceMoveMasks) {
            return attacksKing(gameState, pieceMoveMasks, gameState.getTurn()) ? 1 : 0;
        }

        @Override
        public List<Move> generateMoves(GameState gameState, long[][] pieceMoveMasks, boolean isWhite) {
            List<Move> list = new ArrayList<>();
            long [] boards = gameState.getBoard().getBitboard();
            long kingBoard = isWhite ? boards[5] : boards[11];
            int kingIndex = Long.numberOfTrailingZeros(kingBoard);
            long kingMoves = isWhite ? pieceMoveMasks[5][kingIndex] : pieceMoveMasks[7][kingIndex];
            long enemyPieces = !isWhite ? gameState.getWhiteBoard() : gameState.getBlackBoard();
            long friendlyPieces = isWhite ? gameState.getWhiteBoard() : gameState.getBlackBoard();

            while(kingMoves != 0){
                int toIndex = Long.numberOfTrailingZeros(kingMoves);
                if(Math.abs(toIndex - kingIndex) == 2){
                    Move move = new Move(kingIndex,toIndex,2,0);
                    int movingPiece = gameState.getTurn() ? 5 : 11;
                    if(MoveValidator.canCastle(gameState,move,movingPiece)){list.add(move);}
                } else if( ( (1L << toIndex) & friendlyPieces) == 0){
                    int moveType = ((1L << toIndex) & enemyPieces) != 0 ? 1 : 0;
                    Move move = new Move(kingIndex,toIndex,moveType,0);
                    list.add(move);
                }
                kingMoves &= ~(1L << toIndex);
            }
            return list;
        }

        @Override
        public long getAttackMask(GameState gameState, long [][] pieceMoveMasks, boolean isWhite){
            long l = 0L;
            int shift = isWhite ? 0 : 6;
            int kingIndex = Long.numberOfTrailingZeros(gameState.getBoard().getBitboard()[5 + shift]);
            long friendlyPieces = isWhite ? gameState.getWhiteBoard() : gameState.getBlackBoard();
            int [][] directions =  { {1,1} , {1,-1} , {-1,1} , {-1,-1}, {1,0}, {-1,0}, {0,1}, {0,-1} };
            int row = kingIndex / 8;
            int col = kingIndex % 8;
            for(int [] d : directions){
                int newRow = row + d[0];
                int newCol = col + d[1];
                if(newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8){
                    int newIndex = newRow * 8 + newCol;
                    l |= (1L << newIndex);
                }
            }
            return l & ~friendlyPieces;
        }
    };

    public final int index;

    PieceType(int index) {
        this.index = index;  // This sets the `index` for that piece type
    }

    private static int countBits(long num) { //helper
        int count = 0;
        while (num > 0) {
            num &= (num - 1);
            count++;
        }
        return count;
    }

    public abstract boolean isValidMove(GameState gameState, Move move, long[][] pieceMoveMasks);

    public abstract boolean attacksKing(GameState gameState, long[][] pieceMoveMasks, boolean isWhite);

    public abstract int numAttackers(GameState gameState, long [][] pieceMoveMasks);

    public abstract List<Move> generateMoves(GameState gameState, long[][] pieceMoveMasks, boolean isWhite);

    public abstract long getAttackMask(GameState gameState, long [][] pieceMoveMasks, boolean isWhite);

}