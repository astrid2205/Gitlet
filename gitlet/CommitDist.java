package gitlet;

import java.util.Comparator;

/** A class to store the commitID and the distance of this commit to the head of its branch.
 *  This class is used to find the latest common ancestor of two commits.
 *
 *  @author Ting-Hsuan, Wu
 */
public class CommitDist {
    private String commitID;
    private int dist;

    /** Constructor. */
    public CommitDist(String commitID, int dist) {
        this.commitID = commitID;
        this.dist = dist;
    }

    /** Get the commit ID. */
    public String getID() {
        return this.commitID;
    }

    /** Get the distance of this commit to the head of its branch. */
    public int getDist() {
        return this.dist;
    }

    private static class DistComparator implements Comparator<CommitDist> {
        @Override
        public int compare(CommitDist o1, CommitDist o2) {
            return o1.getDist() - o2.getDist();
        }
    }

    public static Comparator<CommitDist> getDistComparator() {
        return new DistComparator();
    }
}

