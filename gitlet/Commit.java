package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.text.SimpleDateFormat;

import static gitlet.GitletObjects.*;
import static gitlet.Repository.CWD;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  This class represents a `Commit` object, which stores detailed information about the commit.
 *  The information includes: the commit ID, commit author, commit time, commit message, the files
 *  tracked in this commit(blobs), and the parent commit ID of this commit.
 *
 *  This class also supports methods to read/write a Commit object to a file.
 *
 *  @author Ting-Hsuan, Wu
 */
public class Commit implements Serializable {
    /** The author of this Commit. */
    private String author;

    /** The date of this Commit. */
    private String date;

    /** The parent Commit ID(s) of this Commit. */
    private ArrayList<String> parentID;

    /** The message of this Commit. */
    private String message;

    /** The pointer to the Blobs of this Commit. */
    private TreeMap<String, String> blobs;

    /** The time format. */
    private static SimpleDateFormat sdf =
            new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH);

    /** The constructor of the Commit. */
    public Commit(String author, ArrayList<String> parent, String message,
                  TreeMap<String, String> blobs) {
        this.author = author;
        this.parentID = parent;
        this.message = message;
        this.blobs = blobs;
    }

    /** Perform the commit and return the commit ID. */
    public Commit commit() {
        this.date = sdf.format(new Date());
        return this;
    }

    /** Perform the initial commit and return the commit ID.
     * The initial commit has:
     * - parent: an arraylist of size 1, containing null as its content
     * - commit message: "initial commit"
     * - blobs: an empty tree map
     * - timestamp: Epoch time (1970/01/01 00:00:00 UTC) */
    public static Commit initialCommit(String author) {
        ArrayList<String> nullParent = new ArrayList<>();
        nullParent.add(null);
        Commit initial = new Commit(author, nullParent, "initial commit", new TreeMap<>());
        initial.date = sdf.format(new Date(0));
        return initial;
    }

    /** Returns the author of the Commit. */
    public String getCommitAuthor() {
        return author;
    }

    /** Returns the date of the Commit. */
    public String getCommitDate() {
        return date;
    }

    /** Returns the parent of the Commit. */
    public String getParentID() {
        return parentID.get(0);
    }

    /** Returns the second parent of the Commit. */
    public String getSecondParentID() {
        if (parentID.size() == 1) {
            return null;
        } else {
            return parentID.get(1);
        }
    }

    /** Returns if this commit is a merge commit. */
    public boolean isMergeCommit() {
        return parentID.size() == 2;
    }

    /** Returns the message of the Commit. */
    public String getCommitMessage() {
        return message;
    }

    /** If this commit is a merge commit, returns the row of merge parents
     * ("Merge: parent1 parent2") for the log function in Repository.java.
     * The string consist of the first seven digits of the parents' commit ids. */
    public String getMergeParentsLog() {
        if (this.isMergeCommit()) {
            String parent1 = getParentID().substring(0, 7);
            String parent2 = getSecondParentID().substring(0, 7);
            return String.format("Merge: %s %s\n", parent1, parent2);
        } else {
            return "";
        }
    }

    /** Returns the contents(blobs) of the Commit. */
    public TreeMap<String, String> getBlobs() {
        return blobs;
    }

    /** Returns the corresponding blob ID of the given FILENAME in this commit.
     * Returns null if the file does not exist in this commit. */
    public String getFileBlobID(String fileName) {
        return blobs.get(fileName);
    }


    /** Save the commit to a file for future use and return the commit ID.
     *  The commit is serialized to a byte array,
     *  and the byte array is further hashed with the sha1 function.
     *  The serialized commit is stored in a directory under .gitlet/objects,
     *  with the first two character of the sha1 value as its folder name,
     *  and the rest of the sha1 value as file name.
     *
     *  EXAMPLE:
     *      If the sha1 value of a commit is 9884ee0be8bff907637d26220fbb18ab3bad62b8,
     *      then the serialized commit is stored in .gitlet/objects/98
     *      and its file name is 84ee0be8bff907637d26220fbb18ab3bad62b8.
     *  */
    public String saveCommit() {
        byte[] serializedCommit = serialize(this);
        String commitID = sha1(serializedCommit);

        File outDir = objectFolderDesignator(commitID);
        if (!outDir.exists()) {
            outDir.mkdir();
        }
        File outFile = objectFileDesignator(commitID);
        // Save the commit if there is no commit with the same content
        if (!outFile.exists()) {
            Utils.writeObject(outFile, this);
        }
        return commitID;
    }

    /** Return the Commit object with COMMITID from the serialized file. */
    public static Commit loadCommit(String commitID) {
        File inFile = objectFileDesignator(commitID);
        if (!inFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return readObject(inFile, Commit.class);
    }

    /** Return if the file with FILENAME is tracked in this commit. */
    public boolean fileTrackedInCommit(String fileName) {
        TreeMap<String, String> blobsOfThisCommit = this.getBlobs();
        String blobID = Blob.computeBlobID(fileName);
        if (blobsOfThisCommit == null || blobID == null) {
            return false;
        }
        return blobID.equals(blobsOfThisCommit.get(fileName));
    }

    /** Write the tracked files in this commit to CWD. */
    public void writeBlobFilesToCWD() {
        for (Map.Entry<String, String> blob : this.getBlobs().entrySet()) {
            File inFile = objectFileDesignator(blob.getValue());
            String fileName = blob.getKey();
            if (inFile.exists()) {
                byte[] fileContent = readObject(inFile, Blob.class).getFileContent();
                writeContents(join(CWD, fileName), fileContent);
            }
        }
    }

    /** Given a partial commit ID, return the full ID if it exists in the repository. */
    public static String searchFullCommitID(String partialID) {
        if (partialID.length() == 40) {
            return partialID;
        } else if (partialID.length() < 6) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        } else {
            File inFolder = objectFolderDesignator(partialID);
            List<String> filesInFolder = plainFilenamesIn(inFolder);
            if (filesInFolder != null) {
                String partialFileName = partialID.substring(2);
                for (String f : filesInFolder) {
                    if (f.indexOf(partialFileName, 0) != -1) {
                        return partialID.substring(0, 2) + f;
                    }
                }
            }
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return null;
    }
}
