package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.GitletObjects.*;
import static gitlet.Utils.*;
import static gitlet.Commit.*;

/** This class represents a gitlet repository, which stores the information about the
 *  active repository.
 *  The information includes:
 *  the path of the working directory that we want to perform version control,
 *  information of the head commit of the current branch,
 *  the files staged for addition or removal, and the author of the repository.
 *
 *  This class also handles all commands by reading/writing from/to the correct file, setting up
 *  persistence, and additional error checking.
 *
 *  Supported commands include: init, add, remove, commit, log, status, checkout, branch, find,
 *  remove branch, reset, merge, and change the author.
 *
 *  @author Ting-Hsuan, Wu
 */
public class Repository implements Serializable {
    /* Directories. */
    /** The current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    static final File GITLET_DIR = new File(".gitlet");
    /** The directory to store objects (commits and blobs). */
    static final File OBJECTS_DIR = join(GITLET_DIR, "objects");

    /** The serialized file of the repository. */
    static File repo = join(GITLET_DIR, "repo");

    /** The text file of the gitlet log. */
    static File logs = join(GITLET_DIR, "logs.txt");

    /* Instance Variables. */
    /** Store all the branches in this repository and the heads of each branch. */
    private TreeMap<String, String> HEADS = new TreeMap<>();

    /** Store all the branches (not including master branch) in this repository and the commit
     * where the branch branches from the master branch.
     * Ex: Branch "B" branches out from master point at the split point with commit ID "ID",
     * {B, ID} will be stored in the splitPoints variable. */
    private TreeMap<String, String> splitPoints = new TreeMap<>();

    /** The HEAD pointer. */
    private String headPointer;

    /** The HEAD commit. */
    private Commit headCommit;

    /** Marks the current branch*/
    private String onBranch;

    /** The staging area for add, which is a TreeMap that stores the file name and file blob ID. */
    private TreeMap<String, String> stagingAdd = new TreeMap<>();

    /** The staging area for remove, which is a TreeMap that stores the file name
     * and file blob ID. */
    private TreeSet<String> stagingRm = new TreeSet<>();

    /** An array list which stores all the commits ever made. */
    private ArrayList<String> allCommits = new ArrayList<>();

    /** The author of this repository. */
    private String author = "Default author";

    /**
     *  Constructor for a repository.
     *  Creates an initial commit, and make the master pointer and
     *  the head pointer points to the initial commit.
     *  */
    public Repository() {
        Commit initial = Commit.initialCommit(author);
        String initialCommitID = initial.saveCommit();
        headCommit = initial;
        HEADS.put("master", initialCommitID);
        onBranch = "master";
        headPointer = initialCommitID;
        recordCommitID(initialCommitID);
    }

    /** Return the repository object with from the serialized file if the current working
     *  directory is an initialized Gitlet directory.*/
    public static Repository loadRepo() {
        if (!GITLET_DIR.exists() || !repo.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        Repository load = readObject(repo, Repository.class);
        load.headCommit = Commit.loadCommit(load.getHeadCommitID());
        return load;
    }

    /** Initialize a repository and set up persistent folders. */
    public static void init() {
        // Set up folders for persistent data.
        if (GITLET_DIR.exists()) {
            String errMsg =
                    "A Gitlet version-control system already exists in the current directory.";
            System.out.println(errMsg);
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        if (!OBJECTS_DIR.exists()) {
            OBJECTS_DIR.mkdir();
        }

        // Create and save a new repository object.
        Repository newRepo = new Repository();
        newRepo.saveRepo();
    }

    /**
     * Adds a copy of the file as it currently exists to the staging area.
     * Staging an already-staged file overwrites the previous entry
     * in the staging area with the new contents.
     * If the current working version of the file is identical to the version
     * in the current commit, do not stage it to be added, and remove it from
     * the staging area if it is already there.
     * */
    public void add(String fileName) {
        String blobID = Blob.computeBlobID(fileName);
        if (blobID == null) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        TreeMap<String, String> parentBlobs = this.getHeadCommit().getBlobs();
        if (stagingRm.contains(fileName)) {
            stagingRm.remove(fileName);
        } else if (blobID.equals(parentBlobs.get(fileName))) {
            System.exit(0);
        } else {
            stagingAdd.put(fileName, blobID);
        }

        // Add the file to persistent folder
        if (!Blob.checkRepeatedBlob(blobID)) {
            Blob newBlob = new Blob(fileName, blobID);
            newBlob.saveBlob();
        }
        saveRepo();
    }

    /**
     * Perform a commit. Saves a snapshot of tracked files in the current commit and
     * staging area, so they can be restored at a later time.
     * By default, each commit's snapshot of files will be exactly the same as
     * its parent commit's snapshot of files.  A commit will only update the contents
     * of files it is tracking that have been staged for addition at the time of commit.
     * The staging area is cleared after the commit.
     * */
    public void commit(String commitMsg) {
        ArrayList<String> parent = new ArrayList<>(Arrays.asList(headPointer));
        commit(commitMsg, parent);
    }

    public void commit(String commitMsg, ArrayList<String> parents) {
        TreeMap<String, String> newBlobs = trackFilesFromStagingArea();
        Commit newCommit = new Commit(author, parents, commitMsg, newBlobs);
        newCommit.commit();
        String newCommitID = newCommit.saveCommit();
        updateHEAD(newCommit, newCommitID);
        recordCommitID(newCommitID);
        saveRepo();
    }

    /** Perform a merge commit. The merge commit record as parents both the head of the current
     * branch (called the first parent) and the head of the branch given on the command line
     * to be merged in. The staging area is cleared after the commit. */
    public void mergeCommit(String commitMsg, ArrayList<String> parents) {
        commit(commitMsg, parents);
    }

    /** Update the head commit to the given NEWCOMMIT in this repository,
     * and clear the staging area. */
    private void updateHEAD(Commit newCommit, String newCommitID) {
        headCommit = newCommit;
        HEADS.put(getCurrentBranch(), newCommitID);
        headPointer = newCommitID;
        stagingAdd = new TreeMap<>();
        stagingRm = new TreeSet<>();
    }

    /** Copy the blobs from the current head commit, and update the file-blobID pairs in the blobs
     * according to staging area (staged for addition or removal). Exit the program if there's
     * nothing inside the staging area, otherwise returns the updated blobs. */
    private TreeMap<String, String> trackFilesFromStagingArea() {
        if ((stagingAdd.size() == 0) && (stagingRm.size() == 0)) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        TreeMap<String, String> newBlobs = this.headCommit.getBlobs();
        for (Map.Entry<String, String> s: stagingAdd.entrySet()) {
            String key = s.getKey();
            String value = s.getValue();
            newBlobs.put(key, value);
        }
        for (String fileName: stagingRm) {
            newBlobs.remove(fileName);
        }
        return newBlobs;
    }


    /**
     * Unstage the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and remove the file
     * from the working directory if the user has not already done so.
     * */
    public void remove(String fileName) {
        String blobID = Blob.computeBlobID(fileName);

        // Unstage the file if it is currently staged for addition.
        String stagedFileID = stagingAdd.get(fileName);
        String headCommitBlobID = headCommit.getBlobs().get(fileName);

        // If the file is tracked in the current commit, and the file is deleted in CWD, simply
        // unstage the file.
        if (blobID == null) {
            if (headCommitBlobID == null) {
                System.out.println("File does not exist.");
                System.exit(0);
            }
            stagingRm.add(fileName);
        } else if (blobID.equals(stagedFileID)) {
            stagingAdd.remove(fileName);
        } else if (blobID.equals(headCommitBlobID)) {
            // If the file is tracked in the current commit, stage it for removal and
            // remove the file from the working directory
            stagingRm.add(fileName);
            restrictedDelete(fileName);
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        saveRepo();
    }

    /**
     * Print out commit logs.
     * Starting at the current head commit, display information about each commit
     * backwards along the commit tree until the initial commit, following the
     * first parent commit links, ignoring any second parents found in merge commits.
     * */
    public void log() {
        logHelper(false);
    }

    /** Displays information about all commits ever made. */
    public void globalLog() {
        logHelper(true);
    }

    /** Prints out all the commits ever made in this repository if GLOBALLOG is true.
     * Otherwise, prints out commits on the current branch starting from the head commit. */
    private void logHelper(boolean globalLog) {
        StringBuilder logContent = new StringBuilder();
        if (globalLog) {
            for (String commitID: allCommits) {
                Commit commitToBePrinted = loadCommit(commitID);
                writeSingleCommitToLog(commitID, commitToBePrinted, logContent);
            }
        } else {
            String commitID = getHeadCommitID();
            while (commitID != null) {
                Commit commitToBePrinted = loadCommit(commitID);
                writeSingleCommitToLog(commitID, commitToBePrinted, logContent);
                commitID = commitToBePrinted.getParentID();
            }
        }
        if (logContent.length() != 0) {
            System.out.println(logContent.substring(0, logContent.length() - 1));
        }
    }

    /** Add the log message of the given commitID and commitToBePrinted to the logContent. */
    private void writeSingleCommitToLog(String commitID, Commit commitToBePrinted,
                                        StringBuilder logContent) {
        String logDate = commitToBePrinted.getCommitDate();
        String logMsg = commitToBePrinted.getCommitMessage();
        String mergeLog = commitToBePrinted.getMergeParentsLog();
        String log = String.format("===\ncommit %s\n%sDate: %s\n%s\n\n",
                commitID, mergeLog, logDate, logMsg);
        logContent.append(log);
    }

    /** Save the repository to a file name REPO in GITLET_DIR for future use. */
    public void saveRepo() {
        File outFile = Utils.join(GITLET_DIR, "repo");
        Utils.writeObject(outFile, this);
    }

    /** Change the author of this repository. */
    public void changeAuthor(String newAuthor) {
        this.author = newAuthor;
        saveRepo();
    }

    /** Get the HEAD commit ID of this repository. */
    public String getHeadCommitID() {
        return this.headPointer;
    }

    /** Get the HEAD commit ID of this repository. */
    public Commit getHeadCommit() {
        return this.headCommit;
    }

    /** Get the current branch name of this repository. */
    public String getCurrentBranch() {
        return this.onBranch;
    }

    /** Set the current branch of this repository to branch BRANCHNAME. */
    public void setBranch(String branchName) {
        this.onBranch = branchName;
    }

    /** Takes the version of the file FILENAME as it exists in the head commit
     * and puts it in the working directory, overwriting the version of the file
     * that's already there if there is one.
     * */
    public void checkoutHead(String fileName) {
        TreeMap<String, String> blobs = headCommit.getBlobs();
        checkoutFile(fileName, blobs);
    }

    /** Takes the version of the file FILENAME as it exists in the commit with COMMITID
     * and puts it in the working directory, overwriting the version of the file
     * that's already there if there is one.
     * */
    public void checkoutCommitFile(String commitID, String fileName) {
        String fullCommitID = Commit.searchFullCommitID(commitID);
        Commit c = Commit.loadCommit(fullCommitID);
        TreeMap<String, String> blobs = c.getBlobs();
        checkoutFile(fileName, blobs);
    }

    /** Find the file FILENAME in the given BLOBS and puts it in the working directory. */
    public void checkoutFile(String fileName, TreeMap<String, String> blobs) {
        String fileBlobID = blobs.get(fileName);
        if (fileBlobID != null) {
            updateFileFromBlobID(fileName, fileBlobID);
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    /** Updeate the file with FILENAME from contents from the given FILEBLOBID*/
    private void updateFileFromBlobID(String fileName, String fileBlobID) {
        File inFile = objectFileDesignator(fileBlobID);
        if (inFile.exists()) {
            Blob blob = readObject(inFile, Blob.class);
            writeContents(join(CWD, fileName), blob.getFileContent());
        }
    }

    /**
     * Takes all files in the commit at the head of the given branch, and puts them
     * in the working directory, overwriting the versions of the files that are already
     * there if they exist. Also, at the end of this command, the given branch will now
     * be considered the current branch (HEAD). Any files that are tracked in the current
     * branch but are not present in the checked-out branch are deleted. The staging area
     * is cleared, unless the checked-out branch is the current branch.
     * */
    public void checkoutBranch(String branchName) {
        String branchCommitID = HEADS.get(branchName);
        if (branchCommitID == null) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (getCurrentBranch().equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        updateCWDFiles(branchCommitID);
        setBranch(branchName);
        saveRepo();
    }

    /**
     * Change the files in the CWD to the version in the commit with COMMITID.
     * Set the HEAD commit to the given commit, and clear the staging area.
     * */
    private void updateCWDFiles(String commitID) {
        Commit targetCommit = Commit.loadCommit(commitID);
        TreeMap<String, String> filesTrackedInBranch = targetCommit.getBlobs();
        checkUntrackedFiles(filesTrackedInBranch);
        for (String f: headCommit.getBlobs().navigableKeySet()) {
            if (!filesTrackedInBranch.containsKey(f)) {
                restrictedDelete(f);
            }
        }
        targetCommit.writeBlobFilesToCWD();
        headCommit = targetCommit;
        headPointer = commitID;
        stagingAdd = new TreeMap<>();
        stagingRm = new TreeSet<>();
    }

    /** Given a treemap of files tracked in target commit, print out an error message
     * and exit the program immediately if there's an untracked file in CWD that might be
     * replaced after performing merge or checkout. */
    private void checkUntrackedFiles(TreeMap<String, String> filesTrackedInTargetCommit) {
        List<String> filesInCWD = plainFilenamesIn(CWD);
        if (filesInCWD != null) {
            for (String file : filesInCWD) {
                if (!headCommit.fileTrackedInCommit(file)
                        && filesTrackedInTargetCommit.containsKey(file)) {
                    String errMsg = "There is an untracked file in the way; "
                            + "delete it, or add and commit it first.";
                    System.out.println(errMsg);
                    System.exit(0);
                }
            }
        }
    }

    /**
     * Displays what branches currently exist, and marks the current branch with a *.
     * Also displays what files have been staged for addition or removal.
     * */
    public void status() {
        StringBuilder status = new StringBuilder();
        status.append("=== Branches ===\n");
        for (String key: HEADS.navigableKeySet()) {
            if (key.equals(getCurrentBranch())) {
                status.append("*");
            }
            status.append(key + "\n");
        }
        status.append("\n=== Staged Files ===\n");
        for (String key : stagingAdd.navigableKeySet()) {
            status.append(key + "\n");
        }
        status.append("\n=== Removed Files ===\n");
        for (String fileName : stagingRm) {
            status.append(fileName + "\n");
        }
        status.append("\n=== Modifications Not Staged For Commit ===\n");
        status.append("\n=== Untracked Files ===\n");
        System.out.print(status);
    }

    /** Creates a new branch with the given name, and points it at the current head commit. */
    public void branch(String branchName) {
        if (HEADS.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        } else {
            splitPoints.put(branchName, getHeadCommitID());
            HEADS.put(branchName, getHeadCommitID());
        }
        saveRepo();
    }

    /** Merges files from the given branch into the current branch. */
    public void merge(String otherBranch) {
        String currentBranch = getCurrentBranch();
        String currentCommitID = getHeadCommitID();
        String otherBranchCommitID = HEADS.get(otherBranch);
        if ((stagingAdd.size() != 0) || (stagingRm.size() != 0)) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (otherBranchCommitID == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (otherBranchCommitID.equals(currentCommitID)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        String splitCommitID = findLatestCommonAncestor(getCurrentBranch(), otherBranch);
        if (otherBranchCommitID.equals(splitCommitID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (currentCommitID.equals(splitCommitID)) {
            HEADS.put(currentBranch, otherBranchCommitID);
            checkoutBranch(otherBranch);
            setBranch(currentBranch);
            System.out.println("Current branch fast-forwarded.");
            saveRepo();
            System.exit(0);
        }

        Commit currentCommit = getHeadCommit();
        Commit otherBranchCommit = loadCommit(otherBranchCommitID);
        Commit splitCommit = loadCommit(splitCommitID);
        checkUntrackedFiles(otherBranchCommit.getBlobs());
        boolean mergeConflict = mergeOperations(currentCommit, otherBranchCommit, splitCommit);
        String commitMsg = String.format("Merged %s into %s.", otherBranch, currentBranch);
        ArrayList<String> parents =
                new ArrayList<>(Arrays.asList(currentCommitID, otherBranchCommitID));
        mergeCommit(commitMsg, parents);
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        saveRepo();
    }

    /** Given the current commit, the branch commit to merge in, and their
     * latest common ancestor commit, perform file operations (add, remove...)
     * and return true if there's a merge conflict during the process. */
    private boolean mergeOperations(Commit currentCommit,
                                    Commit otherBranchCommit, Commit splitCommit) {
        // Copy the blob contents from each commit
        boolean conflict = false;
        TreeMap<String, String> currentBlobs = new TreeMap<>(currentCommit.getBlobs());
        TreeMap<String, String> otherBlobs = new TreeMap<>(otherBranchCommit.getBlobs());
        TreeMap<String, String> splitBlobs = new TreeMap<>(splitCommit.getBlobs());

        // Iterate through the split commit
        for (Map.Entry<String, String> b : splitBlobs.entrySet()) {
            String fileName = b.getKey();
            String splitBlobID = b.getValue();
            String currentBlobID = currentBlobs.remove(fileName);
            String otherBlobID = otherBlobs.remove(fileName);

            // File deleted in branch commit (remove from staging area)
            if (splitBlobID.equals(currentBlobID) && otherBlobID == null) {
                stagingRm.add(fileName);
                restrictedDelete(fileName);
            } else if (splitBlobID.equals(currentBlobID) && !currentBlobID.equals(otherBlobID)) {
                // File modified in branch commit (add to staging area)
                updateFileFromBlobID(fileName, otherBlobID);
                stagingAdd.put(fileName, otherBlobID);
            } else if (!splitBlobID.equals(currentBlobID) && !splitBlobID.equals(otherBlobID)) {
                // File modified in different ways
                String currentContent = "", otherContent = "";
                if (otherBlobID != null && !otherBlobID.equals(currentBlobID)) {
                    otherContent = Blob.fileContentAsString(otherBlobID);
                }
                if (currentBlobID != null && !currentBlobID.equals(otherBlobID)) {
                    currentContent = Blob.fileContentAsString(currentBlobID);
                }
                if ("".equals(otherContent) && "".equals(currentContent)) {
                    continue;
                }
                String content = "<<<<<<< HEAD\n"
                        + currentContent
                        + "=======\n"
                        + otherContent + ">>>>>>>\n";
                writeContents(join(CWD, fileName), content);
                add(fileName);
                conflict = true;
            }
        }
        // Iterate through branch commit for files that does not exist
        // in split commit but in branch commit
        for (Map.Entry<String, String> b : otherBlobs.entrySet()) {
            // File not exist in split commit nor current commit, but exist in other branch
            String fileName = b.getKey();
            if (currentBlobs.get(fileName) == null) {
                String blobID = b.getValue();
                updateFileFromBlobID(fileName, blobID);
                stagingAdd.put(fileName, blobID);
            }
        }
        return conflict;
    }

    /**
     * Return the commit ID of the latest common ancestor of given two branches.
     * Return null if there is no such branches or no common ancestor.
     * Include one parent commit from each branch each time to the ancestors map of each branch,
     * and check if there is a common ancestor. If not, go to the next round, include
     * one more parent commit from each branch. */
    private String findLatestCommonAncestor(String branchA, String branchB) {
        String headCommitAID = HEADS.get(branchA);
        String headCommitBID = HEADS.get(branchB);
        if (headCommitAID == null || headCommitBID == null) {
            return null;
        }
        HashMap<String, Integer> ancestorsOfA = new HashMap<>();
        HashMap<String, Integer> ancestorsOfB = new HashMap<>();
        PriorityQueue<CommitDist> unfinishedA = new PriorityQueue<>(CommitDist.getDistComparator());
        PriorityQueue<CommitDist> unfinishedB = new PriorityQueue<>(CommitDist.getDistComparator());
        unfinishedA.add(new CommitDist(headCommitAID, 0));
        unfinishedB.add(new CommitDist(headCommitBID, 0));
        CommitDist commonAncestor = new CommitDist(null, (int) Double.POSITIVE_INFINITY);
        while (!unfinishedA.isEmpty() || !unfinishedB.isEmpty()) {
            // Get one upstream commit for both branches
            includeOneMoreAncestor(ancestorsOfA, unfinishedA);
            includeOneMoreAncestor(ancestorsOfB, unfinishedB);

            // Find common ancestors of both branches
            Set<String> ancSet = new HashSet<>(ancestorsOfA.keySet());
            ancSet.retainAll(ancestorsOfB.keySet());

            // Get the latest common ancestor
            if (!ancSet.isEmpty()) {
                for (String anc: ancSet) {
                    int distAnc = ancestorsOfA.get(anc);
                    if (distAnc < commonAncestor.getDist()) {
                        commonAncestor = new CommitDist(anc, distAnc);
                    }
                }
                return commonAncestor.getID();
            }
        }
        return null;
    }

    /** Add the commit (head of the queue unfinishedAnc) ID and distance from the head of the
     *  branch to the map ancestorsSoFar. */
    private void includeOneMoreAncestor(HashMap<String, Integer> ancestorsSoFar,
                              PriorityQueue<CommitDist> unfinishedAnc) {
        CommitDist newAncestor = unfinishedAnc.poll();
        String ancID = newAncestor.getID();
        if (ancID != null) {
            int ancDist = newAncestor.getDist();
            if (ancestorsSoFar.get(ancID) == null || ancestorsSoFar.get(ancID) > ancDist) {
                ancestorsSoFar.put(ancID, ancDist);
            }
            Commit aCommit = Commit.loadCommit(ancID);
            updateUnfinishedAnc(unfinishedAnc, aCommit, ancDist);
        }
    }

    /** Given the commit C and its distance DIST from the head of the branch,
     *  put the parent(s) of C into the given UNFINISHEDQUEUE, so the parents
     *  can be visited in the next round in includeOneMoreAncestor method. */
    private void updateUnfinishedAnc(PriorityQueue<CommitDist> unfinishedQueue,
                                     Commit c, int dist) {
        unfinishedQueue.add(new CommitDist(c.getParentID(), dist + 1));
        if (c.isMergeCommit()) {
            unfinishedQueue.add(new CommitDist(c.getSecondParentID(), dist + 1));
        }
    }

    /**
     *  Deletes the branch with the given name. This only means to delete the pointer associated
     *  with the branch; it does not mean to delete all commits that were created under the branch,
     *  or anything like that.
     */
    public void rmBranch(String branchName) {
        if (branchName.equals(getCurrentBranch())) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        if (HEADS.remove(branchName) == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        saveRepo();
    }

    /** Checks out all the files tracked by the given commit. Removes tracked files that are
     * not present in that commit. Also moves the current branch's head to that commit node.*/
    public void reset(String commitID) {
        String fullCommitID = Commit.searchFullCommitID(commitID);
        if (!objectFileDesignator(fullCommitID).exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        updateCWDFiles(fullCommitID);
        HEADS.put(getCurrentBranch(), fullCommitID);
        saveRepo();
    }

    /** Prints out the ids of all commits that have the given commit message, one per line. */
    public void find(String keyword) {
        StringBuilder commitIDs = new StringBuilder();
        for (String commitID: allCommits) {
            String logMsg = loadCommit(commitID).getCommitMessage();
            if (logMsg.contains(keyword)) {
                commitIDs.append(commitID + "\n");
            }
        }
        if (commitIDs.length() != 0) {
            System.out.println(commitIDs.substring(0, commitIDs.length() - 1));
        } else {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Record the commit ID to the instance variable allCommits whenever
     * a commit is made. */
    private void recordCommitID(String commitID) {
        allCommits.add(0, commitID);
    }
}
