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

        //Create and add NodeWrapper for MrX's location
        NodeWrapper mrXNode = new NodeWrapper(mrXLocation, 0, (int) Double.POSITIVE_INFINITY);
        visted.add(mrXNode);
        allNodes.add(mrXNode);

        for(Object n : graph.nodes()) {
            if(!n.equals(mrXLocation)){
                //Create a new object NodeWrapper with node number, distance between MrX's location and the node
                //The previous node is Mrx's location
                NodeWrapper node = new NodeWrapper((int) n, distanceBetweenNodes(graph, (int) n, mrXLocation),  mrXLocation);
                allNodes.add(node);
                unvisited.add(node);
            }
        }

        while (!unvisited.isEmpty()) {
            NodeWrapper currentVertex = unvisited.iterator().next();

            //Pick the node closest to MrX's location which hasn't been visited
            for(NodeWrapper node : unvisited){
                if(node.distance < currentVertex.distance) currentVertex = node;
            }

            visted.add(currentVertex);
            unvisited.remove(currentVertex);

            //Check if there's a quicker route from the currentVertex to MrX's location
            for (NodeWrapper node : unvisited) {
                node.distance = Math.min(node.distance, currentVertex.distance + distanceBetweenNodes(graph, node.node, currentVertex.node));
            }
        }

        List<Integer> distances = new ArrayList<>();

        //Add only the distances from the detectives to MrX's location
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
        //All distances between nodes are 1
        //If two nodes are not connected, the distance is infinite
        return graph.hasEdgeConnecting(n1, n2) ? 1 : Double.POSITIVE_INFINITY;
    }
}
