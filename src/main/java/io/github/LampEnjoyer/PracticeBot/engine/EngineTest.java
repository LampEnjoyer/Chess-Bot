package io.github.LampEnjoyer.PracticeBot.engine;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;


@SpringBootApplication
public class EngineTest {

	public static void main(String[] args) throws IOException {
		GameState gameState = new GameState("r1bq1rk1/pp1n1ppp/2pbpn2/3p4/3P1B2/2NBPN2/PP3PPP/R2Q1RK1 b - - 0 10");
		gameState.getBoard().printBoard();
	/*	MoveScore moveScore = Evaluator.iterativeSearch(gameState,3);
		System.out.println(moveScore);
		System.out.println(Evaluator.positions); */
		System.out.println(gameState.getHash());
		gameState.makeNullMove();
		gameState.undoMove();
		System.out.println(gameState.getHash());
//		for(Move m : gameState.getAllCaptures()	){
//			System.out.println(m);
//		}

	}

	public static void printBoard(long board){
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


	public static long generateMagicNumbers(){
		Random rand = new Random();
		return rand.nextLong() & rand.nextLong() & rand.nextLong() & rand.nextLong(); //more sparse bits better random
	}

	public static ArrayList<Long> generateBlockers(int index){
		long mask = getRookBlockerMask(index);
		ArrayList<Integer> bits = new ArrayList<>();
		for (int i = 0; i < 64; i++) {
			if (((mask >> i) & 1) != 0) bits.add(i);
		}

		int n = bits.size();
		int permutations = 1 << n;
		ArrayList<Long> blockers = new ArrayList<>();

		for (int i = 0; i < permutations; i++) {
			long blocker = 0L;
			for (int j = 0; j < n; j++) {
				if (((i >> j) & 1) != 0) {
					blocker |= 1L << bits.get(j);
				}
			}
			blockers.add(blocker);
		}
		return blockers;
	}
	public static long getRookBlockerMask(int square) {
		long mask = 0L;
		int rank = square / 8;
		int file = square % 8;

		for (int f = file + 1; f < 7; f++) mask |= 1L << (rank * 8 + f);
		for (int f = file - 1; f > 0; f--) mask |= 1L << (rank * 8 + f);
		for (int r = rank + 1; r < 7; r++) mask |= 1L << (r * 8 + file);
		for (int r = rank - 1; r > 0; r--) mask |= 1L << (r * 8 + file);
		return mask;
	}


	public static long getBishopBlockerMask(int square){
		long mask = 0L;
		int rank = square / 8;
		int file = square % 8;

		int [][] dir = { {1,1}, {1,-1}, {-1,1}, {-1,-1} };
		for(int [] d : dir){
			int tempRank = rank + d[0];
			int tempFile = file + d[1];
			while(tempRank < 7 && tempRank > 0 && tempFile < 7 && tempFile > 0){
				mask |= 1L << ( (tempRank * 8) + tempFile);
				tempRank += d[0];
				tempFile += d[1];
			}
		}
		return mask;
	}

	public static int countBits(long num){
		int count = 0;
		while(num > 0){
			num &= (num-1);
			count++;
		}
		return count;
	}
}
