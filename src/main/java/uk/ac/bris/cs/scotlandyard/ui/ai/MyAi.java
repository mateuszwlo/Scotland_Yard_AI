package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import org.w3c.dom.Node;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "Princess Phillip"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();

		List<ScoredMove> scoredMoves = moves.stream()
				.map((Move m) -> new ScoredMove(m, score(board, m)))
				.collect(Collectors.toList());

		Collections.sort(scoredMoves);
		Collections.reverse(scoredMoves);

		List<ScoredMove> withoutDuplicates = removeDuplicates(scoredMoves);

		List<ScoredMove> topMoves = withoutDuplicates.subList(0, Math.min(withoutDuplicates.size(), 10));

		//Check for duplicate destinations

		for(int i = 0; i < topMoves.size(); i++){
			ScoredMove move = topMoves.get(i);
			topMoves.set(i, new ScoredMove(move.m, move.score + lookAhead(board, move.m)));

			System.out.println(move.m + " - " + topMoves.get(i).score);
		}

		Collections.sort(topMoves);
		Collections.reverse(topMoves);

		Move bestMove = topMoves.get(0).m;

		System.out.println("\n");
		System.out.println(topMoves.get(0).score+ bestMove.toString());
		System.out.println("\n\n\n\n");
		return bestMove;
	}

	int getDestination(Move move){
		int destination;
		boolean isDoubleMove = move.getClass().equals(Move.DoubleMove.class);
		if(isDoubleMove) destination = ((Move.DoubleMove) move).destination2;
		else destination = ((Move.SingleMove) move).destination;

		return destination;
	}

	List<ScoredMove> removeDuplicates(List<ScoredMove> scoredMoves){
		List<ScoredMove> withoutDuplicates = new ArrayList<>();
		withoutDuplicates.add(scoredMoves.get(0));

		for(int i = 1; i < scoredMoves.size(); i++){
			int dest = getDestination(scoredMoves.get(i).m);
			boolean isAlreadyAdded = false;
			for(int j = 0; j < withoutDuplicates.size(); j++){
				if(getDestination(withoutDuplicates.get(j).m) == dest) {
					isAlreadyAdded = true;
					break;
				}
			}
			if(!isAlreadyAdded) withoutDuplicates.add(scoredMoves.get(i));
		}

		return withoutDuplicates;
	}

	float score(Board board, Move move){
		int destination = getDestination(move);
		boolean isDoubleMove = move.getClass().equals(Move.DoubleMove.class);
		boolean isSecretMove = ((ImmutableList)(move.tickets())).contains(ScotlandYard.Ticket.SECRET);

		List<Integer> detectiveLocations = getDetectiveLocations(board);
		List<Integer> oldDistances = dijkstras(board.getSetup().graph, move.source(), detectiveLocations);// we're calculating this multiple times -> move up in call stack
		List<Integer> distances = dijkstras(board.getSetup().graph, destination, detectiveLocations);

		int oldMin = distances.get(0);

		for(Integer i : oldDistances){
			if(i < oldMin) oldMin = i;
		}

		int min = distances.get(0);
		int sum = 0;

		for(Integer i : distances){
			if(i < min) min = i;
			if(i > 5) continue;
			sum += i;
		}

		float score = 100 + sum / distances.size();

		double minPenalty = min <= 2 ? 50 : 0;
		score -= minPenalty;

		double doublePenalty = isDoubleMove && oldMin >= 2 ? (oldMin >= 5 ? 50 : 20) : 0;
		score -= doublePenalty;

//		//Penalty for certain tickets
		if (isSecretMove) score -= 0.1;

		System.out.println(move.toString() + " - " + score + "(min penalty: " + minPenalty + ", double penalty: " + doublePenalty + ")");

		return score;
	}

	float lookAhead(Board board, Move move){
		List<Move> newMoves = ((Board.GameState) board).advance(move).getAvailableMoves().asList();

		List<ScoredMove> newScores = newMoves.stream().map((Move m) -> new ScoredMove(m, score(board, m))).collect(Collectors.toList());
		newScores.sort(null);

		float scoreSum = 0;
		for(int i = 0; i < 4; i++){
			scoreSum += newScores.get(i).score * (1-0.25*i);
		}

		scoreSum /= 30;

//		int availableMoves = removeDuplicates(newScores).size();
//		if (availableMoves < 5) scoreSum -= availableMoves < 3 ? 50 : 15;

		return scoreSum;
	}

	List<Integer> getDetectiveLocations(Board board){
		List<Integer> detectiveLocations = new ArrayList<>();

		for(Piece p : board.getPlayers()){
			if(p.isDetective()) detectiveLocations.add(board.getDetectiveLocation((Piece.Detective) p).get());
		}

		return detectiveLocations;
	}

	List<Integer> dijkstras(ImmutableValueGraph graph, int mrXLocation, List<Integer> detectiveLocations){
		Set<NodeWrapper> allNodes = new HashSet<>();
		Set<NodeWrapper> visted = new HashSet<>();
		Set<NodeWrapper> unvisited = new HashSet<>();
		visted.add(new NodeWrapper(mrXLocation, 0, (int) Double.POSITIVE_INFINITY));

		for(Object n : graph.nodes()) {
			if(n.equals(mrXLocation)) allNodes.add(new NodeWrapper((int) n, 0, (int) Double.POSITIVE_INFINITY));
			else {
				NodeWrapper node = new NodeWrapper((int) n, distanceBetweenNodes(graph, (int) n, mrXLocation),  mrXLocation);
				allNodes.add(node);
				unvisited.add(node);
			}
		}

		while (!unvisited.isEmpty()) {
			NodeWrapper currentVertex = unvisited.iterator().next();

			for(NodeWrapper node : unvisited){
				if(node.distance < currentVertex.distance) currentVertex = node;
			}

			visted.add(currentVertex);
			unvisited.remove(currentVertex);

			for (NodeWrapper node : unvisited) {
				node.distance = Math.min(node.distance, currentVertex.distance + distanceBetweenNodes(graph, node.node, currentVertex.node));
			}

		}

		List<Integer> distances = new ArrayList<>();

		for(int detective : detectiveLocations){
			for(NodeWrapper node : allNodes){
				if(detective == node.node) {
					distances.add((int) node.distance);
					break;
				}

			}
		}

		return distances;

	}

	double distanceBetweenNodes(ImmutableValueGraph graph, int n1, int n2){
		return graph.hasEdgeConnecting(n1, n2) ? 1 : Double.POSITIVE_INFINITY;
	}

	@Override
	public void onStart() {

	}

	@Override
	public void onTerminate() {

	}
}

class NodeWrapper{
	int node;
	double distance;
	int previous;

	public NodeWrapper(int node, double distance, int previous) {
		this.node = node;
		this.distance = distance;
		this.previous = previous;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NodeWrapper that = (NodeWrapper) o;
		return node == that.node;
	}
}

class ScoredMove implements Comparable{
	Move m;
	float score;

	public ScoredMove(Move m, float score) {
		this.m = m;
		this.score = score;
	}

	@Override
	public boolean equals(Object obj) {
		return score == ((ScoredMove) obj).score;
	}

	@Override
	public int compareTo(Object o) {
		ScoredMove m = (ScoredMove) o;
		return Float.compare(score, m.score);
	}
}
