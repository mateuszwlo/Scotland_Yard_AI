package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

class ScoredMove implements Comparable{
    Move move;
    float score;

    public ScoredMove(Move move, float score) {
        this.move = move;
        this.score = score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoredMove that = (ScoredMove) o;
        return score == that.score;
    }

    @Override
    public int compareTo(Object o) {
        ScoredMove m = (ScoredMove) o;
        return Float.compare(score, m.score);
    }
}
