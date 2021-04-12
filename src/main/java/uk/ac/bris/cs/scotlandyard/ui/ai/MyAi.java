package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

		float bestScore = -1;
		Move bestMove = moves.get(0);

		for(Move move : moves){
			float moveScore  = score(board, move);

			if(moveScore > bestScore){
				bestMove = move;
				bestScore = moveScore;
			}
		}

		System.out.println("\n");
		System.out.println(bestScore+ bestMove.toString());
		return bestMove;
	}

	float score(Board board, Move move){
		int destination;
		boolean isDoubleMove = move.getClass().equals(Move.DoubleMove.class);
		if(isDoubleMove) destination = ((Move.DoubleMove) move).destination2;
		else destination = ((Move.SingleMove) move).destination;
		boolean isSecretMove = ((ImmutableList)(move.tickets())).contains(ScotlandYard.Ticket.SECRET);

		List<Integer> detectiveLocations = getDetectiveLocations(board);
		List<Integer> distances = dijkstras(board.getSetup().graph, destination, detectiveLocations);

		int roundsTotal = board.getSetup().rounds.size();
		int roundsLeft = roundsTotal - board.getMrXTravelLog().size();
		int doubleMovesLeft = board.getPlayerTickets(move.commencedBy()).get().getCount(ScotlandYard.Ticket.DOUBLE);

		int min = distances.get(0);
		int sum = 0;

		for(Integer i : distances){
			sum += i;
			if(i < min) min = i;
		}

		float score = 20 * sum / distances.size();

		double minPenalty = 0;
		if (min < 5) {
			score -= 400 * (1 / Math.pow(min, 2));
			minPenalty = 400 * (1 / Math.pow(min, 2));
		}

		double doublePenalty = 0;
		if (isDoubleMove && roundsLeft > doubleMovesLeft) {
			final float penaltyScalerFuncArg = 7 * (roundsTotal - roundsLeft)/(roundsTotal - doubleMovesLeft);
			final float penaltyScaler = Math.max(10 - 2 * penaltyScalerFuncArg, 7 - penaltyScalerFuncArg);
			score -= (min == 1 ? 80 : 40) * penaltyScaler;
			doublePenalty = (min == 1 ? 80 : 40) * penaltyScaler;
		}

		if (isSecretMove) score -= 0.1;

		System.out.println(move.toString() + " - " + score + "(min penalty: " + minPenalty + ", double penalty: " + doublePenalty + ")");

		return score;
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
