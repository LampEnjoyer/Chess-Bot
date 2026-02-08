package io.github.LampEnjoyer.PracticeBot;

import io.github.LampEnjoyer.PracticeBot.engine.Game;
import io.github.LampEnjoyer.PracticeBot.engine.GameState;
import io.github.LampEnjoyer.PracticeBot.engine.Move;
import io.github.LampEnjoyer.PracticeBot.engine.MoveValidator;
import io.github.LampEnjoyer.PracticeBot.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class PracticeBotApplicationTests {




	@Test
	void validFenForStartingPos(){
		GameState gameState = new GameState();
		String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	}

	@Test
	void testValidMove(){
		GameState gameState = new GameState();
		Move move = gameState.uciToMove("e2e4");
		assertTrue(MoveValidator.validateMove(gameState, move));
	}

	@Test
	void testIllegalMove(){
		GameState gameState = new GameState();
		Move move = gameState.uciToMove("e2e5");
		assertFalse(MoveValidator.validateMove(gameState, move));
	}

	@Test
	void startingPositionHas20Moves() {
		GameState game = new GameState();
		assertEquals(20, game.getAllPossibleMoves(true).size());
	}

	@Test
	void whiteMovesFirst() {
		GameState game = new GameState();
		assertTrue(game.getTurn());
	}

	@Test
	void turnFlipsAfterMove() {
		GameState gameState = new GameState();
		Move move = gameState.uciToMove("e2e4");
		gameState.makeMove(move);
		assertFalse(gameState.getTurn());
	}


	@Test
	void syncBoards(){
		boolean bool = false;
		bool = compareBoards(new GameState());
		assertTrue(bool);
		String [] arr = new String[]{
				"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", //Nothing
				"rnbqkbnr/ppp2ppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 1", //En Passant
				"rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1", //Regular Capture
				"r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1", //Castling
				"5rk1/P1pppppp/8/8/8/8/1PPPPPPP/R3K2R w KQ - 0 1", //Promotion
				"1n3rk1/P1pppppp/8/8/8/8/1PPPPPPP/R3K2R w KQ - 0 1" //Promotion with capture

		};
		Move [] moveArray = new Move[]{
			new Move(12, 28,0,0),
			new Move(36,43,3,0),
			new Move(28,35,1,0),
			new Move(4,2,2,0),
			new Move(48,56,0,1),
			new Move(48,57,0,1),
		};
		boolean makeMove = false;
		boolean undoMove = false;
		for(int i = 0; i<6; i++){
			GameState gameState = new GameState(arr[i]);
			gameState.makeMove(moveArray[i]);
			makeMove = compareBoards(gameState);
			gameState.undoMove();;
			undoMove = compareBoards(gameState);
			assertTrue(makeMove & undoMove);
		}
	}

	boolean compareBoards(GameState gameState){
		long [] bitboards = gameState.getBoard().getBitboard();
		int [] pieceBoard = gameState.getBoard().getPieceBoard();
		for(int i = 0; i<64; i++){
			if(pieceBoard[i] != -1){ //Piece there
				int type = pieceBoard[i];
				if( (bitboards[type] & (1L << i))  == 0){
					return false;
				}
			}else{
				for(int j = 0; j<12; j++){ //Piece not there
					if( (bitboards[j] & (1L << i)) != 0){ //We gotta verify there isn't one
						return false;
					}
				}
			}
		}
		return true;
	}






}
