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







}
