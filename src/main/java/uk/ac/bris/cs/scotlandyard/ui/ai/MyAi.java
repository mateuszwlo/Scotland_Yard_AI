package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;


public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "Princess Phillip"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		var moves = board.getAvailableMoves().asList();

		//Calculates score for each available move and stores in a list of object ScoredMove
		List<ScoredMove> scoredMoves = moves.stream()
				.map((Move m) -> new ScoredMove(m, score(board, m)))
				.collect(Collectors.toList());

		//Sorting the list and reversing so that move at index 0 has the highest score
		Collections.sort(scoredMoves);
		Collections.reverse(scoredMoves);

		//Removing moves with the same destination but different required tickets
		List<ScoredMove> withoutDuplicates = removeDuplicates(scoredMoves);

		//Only look ahead a second move for the top 10 moves or the the number of available non duplicate moves
		List<ScoredMove> topMoves = withoutDuplicates.subList(0, Math.min(withoutDuplicates.size(), 10));

		for(int i = 0; i < topMoves.size(); i++){
			ScoredMove move = topMoves.get(i);
			//Updating score of move based on look ahead function
			topMoves.set(i, new ScoredMove(move.move, move.score + lookAhead(board, move.move)));

			System.out.println(move.move + " - " + topMoves.get(i).score);
		}

		Collections.sort(topMoves);
		Collections.reverse(topMoves);

		Move bestMove = topMoves.get(0).move;

		System.out.println("\n");
		System.out.println(topMoves.get(0).score+ bestMove.toString());
		System.out.println("\n\n\n\n");
		return bestMove;
	}

	int getDestination(Move move){
		return  move.visit(new Move.FunctionalVisitor<>(
				m -> m.destination,
				m -> m.destination2
		));
	}

	boolean isDoubleMove(Move move){
		return  move.visit(new Move.FunctionalVisitor<>(
				m -> false,
				m -> true
		));
	}

	List<ScoredMove> removeDuplicates(List<ScoredMove> scoredMoves){
		List<ScoredMove> withoutDuplicates = new ArrayList<>();
		withoutDuplicates.add(scoredMoves.get(0));

		for(int i = 1; i < scoredMoves.size(); i++){
			int dest = getDestination(scoredMoves.get(i).move);
			boolean isAlreadyAdded = false;
			//Loop over each element in withoutDuplicates list and see if any element has the same destination
			//If not, add the move to the list
			for(int j = 0; j < withoutDuplicates.size(); j++){
				if(getDestination(withoutDuplicates.get(j).move) == dest) {
					isAlreadyAdded = true;
					break;
				}
			}
			if(!isAlreadyAdded) withoutDuplicates.add(scoredMoves.get(i));
		}

		return withoutDuplicates;
	}

	int getMin(List<Integer> list){
		int min = list.get(0);

		for(Integer i : list){
			if(i < min) min = i;
		}

		return min;
	}

	int getSum(List<Integer> list){
		int sum = 0;

		for(Integer i : list){
			sum += i;
		}

		return sum;
	}

	float score(Board board, Move move){
		int destination = getDestination(move);
		boolean isDoubleMove = isDoubleMove(move);
		boolean isSecretMove = ((ImmutableList)(move.tickets())).contains(ScotlandYard.Ticket.SECRET);

		List<Integer> detectiveLocations = getDetectiveLocations(board);
		//List of the distances between MrX and all the detectives before and after move
		List<Integer> oldDistances = Dijkstra.distanceBetween(board.getSetup().graph, move.source(), detectiveLocations);
		List<Integer> distances = Dijkstra.distanceBetween(board.getSetup().graph, destination, detectiveLocations);

		int oldMin = getMin(oldDistances);
		int min = getMin(distances);
		int sum = getSum(distances);

		float score = 100 + sum / distances.size();

		//If MrX's move places him within 2 stations of any detectives, subtract 50 from the move's score
		double minPenalty = min <= 2 ? 50 : 0;
		score -= minPenalty;

		//Different penalties depending on how far MrX was before using a double move
		//50 if 5 or more stations away from any detectives, 20 for 3 or more stations away
		double doublePenalty = isDoubleMove && oldMin >= 3 ? (oldMin >= 5 ? 50 : 20) : 0;
		score -= doublePenalty;

		//Small penalty for secret moves so that they are not wasted by the AI
		if (isSecretMove) score -= 0.1;

		System.out.println(move.toString() + " - " + score + "(min penalty: " + minPenalty + ", double penalty: " + doublePenalty + ")");

		return score;
	}

	float lookAhead(Board board, Move move){
		//Gets the new gameState after playing the move and fetches the available moves for the next round
		Board.GameState stateAfter = ((Board.GameState) board).advance(move);
		List<Move> newMoves = stateAfter.getAvailableMoves().asList();

		//Calculates score for each available move and stores in a list of object ScoredMove
		List<ScoredMove> newScores = newMoves.stream().map((Move m) -> new ScoredMove(m, score(board, m))).collect(Collectors.toList());

		//Sorting the list and reversing so that move at index 0 has the highest score
		Collections.sort(newScores);
		Collections.reverse(newScores);


		//Weighted sum of the top 4 scoring moves for the second round
		float scoreSum = 0;
		for(int i = 0; i < 4; i++){
			scoreSum += newScores.get(i).score * (1-0.25*i);
		}

		scoreSum /= 30;

		//Calculates how many available moves MrX has after advancing with the move
		//The more available moves after the move has been played, the higher the score increases by
		int secondOrderMovesSum = 0;
		for(ScoredMove m : newScores){
			List<Move> secondNewMoves = stateAfter.advance(m.move).getAvailableMoves().asList();
			secondOrderMovesSum += secondNewMoves.size();
		}
		scoreSum += secondOrderMovesSum / 50;

		return scoreSum;
	}

	List<Integer> getDetectiveLocations(Board board){
		List<Integer> detectiveLocations = new ArrayList<>();

		for(Piece p : board.getPlayers()){
			if(p.isDetective()) detectiveLocations.add(board.getDetectiveLocation((Piece.Detective) p).get());
		}

		return detectiveLocations;
	}

	@Override
	public void onStart() {

	}

	@Override
	public void onTerminate() {

	}
}
