package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.graph.ImmutableValueGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Dijkstra {
    static List<Integer> distanceBetween(ImmutableValueGraph graph, int mrXLocation, List<Integer> detectiveLocations){
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

    static double distanceBetweenNodes(ImmutableValueGraph graph, int n1, int n2){
        return graph.hasEdgeConnecting(n1, n2) ? 1 : Double.POSITIVE_INFINITY;
    }
}
