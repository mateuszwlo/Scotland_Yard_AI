package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

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
