package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Ting-Hsuan, Wu
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2>
     *
     * init -- Creates a new Gitlet version-control system in the current directory.
     *         This system will automatically start with one commit that contains no files
     *         and has the commit message initial commit.
     *         It will have a single branch: master, which initially points to this initial commit,
     *         and master will be the current branch.
     *         The timestamp for this initial commit will be 00:00:00 UTC, Thursday, 1 January 1970.
     *
     * add [file name] -- Add a copy of the file as it currently exists to the staging area.
     *                    Staging an already-staged file overwrites the previous entry in the
     *                    staging area with the new contents. The file won't be added if the
     *                    current working version of the file is identical to the version in
     *                    the current commit.
     *
     * commit [commit message] -- Saves a snapshot of tracked files in the current commit and
     *                            staging area, so they can be restored at a later time. A commit
     *                            will only update the contents of files it is tracking that have
     *                            been staged for addition at the time of commit.
     *
     * rm [file name] -- Unstage the file if it is currently staged for addition. If the file is
     *                   tracked in the current commit, stage it for removal and remove the file
     *                   from the working directory if the user has not already done so.
     *
     * log -- Print out commit logs starting at the current head commit, display information about
     *        each commit backwards along the commit tree until the initial commit, following the
     *        first parent commit links, ignoring any second parents found in merge commits.
     *
     * global-log -- Displays information about all commits ever made.
     *
     * status -- Displays what branches currently exist, and marks the current branch with a *.
     *           Also displays what files have been staged for addition or removal.
     *
     * checkout -- There are 3 possible use cases of the checkout command.
     *     1. checkout -- [file name]
     *            Takes the version of the file as it exists in the head commit, the front of the
     *            current branch, and puts it in the working directory, overwriting the version
     *            of the file that's already there if there is one. The new version of the file
     *            is not staged.
     *
     *     2. checkout [commit id] -- [file name]
     *            Takes the version of the file as it exists in the commit with the given id, and
     *            puts it in the working directory, overwriting the version of the file that's
     *            already there if there is one. The new version of the file is not staged.
     *            The commit id may be abbreviated as a SHA-1 id fewer than 40 characters.
     *
     *     3. checkout [branch name]
     *            Takes all files in the commit at the head of the given branch, and puts them in
     *            the working directory, overwriting the versions of the files that are already
     *            there if they exist. Also, at the end of this command, the given branch will now
     *            be considered the current branch (HEAD). Any files that are tracked in the
     *            current branch but are not present in the checked-out branch are deleted. The
     *            staging area is cleared, unless the checked-out branch is the current branch.
     *
     * branch [branch name] -- Creates a new branch with the given name, and points it at the
     *                         current head node. This command does NOT immediately switch to the
     *                         newly created branch. Before you ever call branch command, the code
     *                         will be running with a default branch called "master".
     *
     * find [commit message] -- Prints out the ids of all commits that have the given commit
     *                          message, one per line. If there are multiple such commits, it
     *                          prints the ids out on separate lines. To indicate a multiword
     *                          message, put the operand in quotation marks.
     *
     * rm-branch [branch name] -- Deletes the branch with the given name. This only deletes the
     *                            pointer associated with the branch; it does not mean to delete
     *                            all commits that were created under the branch.
     *
     * reset [commit id] -- Checks out all the files tracked by the given commit. Removes tracked
     *                      files that are not present in that commit. Also moves the current
     *                      branch's head to that commit node. The [commit id] may be abbreviated.
     *                      The staging area is cleared.
     *
     * merge [branch name] -- Merges files from the given branch into the current branch.
     *                        1. Any files that have been modified in the given branch since the
     *                           split point, but not modified in the current branch since the
     *                           split point should be changed to their versions in the given
     *                           branch. These files should then all be automatically staged.
     *                        2. Any files that have been modified in the current branch but not
     *                           in the given branch since the split point should stay as they are.
     *                        3. Any files that have been modified in both the current and given
     *                           branch in the same way (i.e., both files now have the same content
     *                           or were both removed) are left unchanged by the merge.
     *                        4. Any files that were not present at the split point and are present
     *                           only in the current branch should remain as they are.
     *                        5. Any files that were not present at the split point and are present
     *                           only in the given branch should be checked out and staged.
     *                        6. Any files present at the split point, unmodified in the current
     *                           branch, and absent in the given branch should be removed
     *                           (and untracked).
     *                        7. Any files present at the split point, unmodified in the given
     *                           branch, and absent in the current branch should remain absent.
     *                        8. Any files modified in different ways in the current and given
     *                           branches are in conflict. The file will be replaced as the
     *                           conflicted contents.
     *
     * author [author name] -- Change the author of the commit to the given author.
     *                         If this command is never run, the author of the repository
     *                         will be "Default author".
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        String fileName, branchName;

        if (firstArg.equals("init")) {
            validateOperands(args, 1);
            Repository.init();
            System.exit(0);
        }

        Repository repo = Repository.loadRepo();

        switch(firstArg) {
            case "add":
                validateOperands(args, 2);
                fileName = args[1];
                repo.add(fileName);
                break;
            case "commit":
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                validateOperands(args, 2);
                String commitMessage = args[1];
                if (commitMessage.equals("")) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                repo.commit(commitMessage);
                break;
            case "rm":
                validateOperands(args, 2);
                fileName = args[1];
                repo.remove(fileName);
                break;
            case "log":
                validateOperands(args, 1);
                repo.log();
                break;
            case "global-log":
                repo.globalLog();
                break;
            case "status":
                validateOperands(args, 1);
                repo.status();
                break;
            case "checkout":
                // checkout -- [file name]
                if (args.length == 3 && "--".equals(args[1])) {
                    fileName = args[2];
                    repo.checkoutHead(fileName);
                    break;
                }
                // checkout [commit id] -- [file name]
                if (args.length == 4 && "--".equals(args[2])) {
                    String commitID = args[1];
                    fileName = args[3];
                    repo.checkoutCommitFile(commitID, fileName);
                    break;
                }
                // checkout [branch name]
                if ((args.length == 2)) {
                    branchName = args[1];
                    repo.checkoutBranch(branchName);
                    break;
                }
                System.out.println("Incorrect operands.");
                System.exit(0);
                break;
            case "branch":
                validateOperands(args, 2);
                branchName = args[1];
                repo.branch(branchName);
                break;
            case "find":
                validateOperands(args, 2);
                String keyword = args[1];
                repo.find(keyword);
                break;
            case "rm-branch":
                validateOperands(args, 2);
                branchName = args[1];
                repo.rmBranch(branchName);
                break;
            case "reset":
                validateOperands(args, 2);
                String commitID = args[1];
                repo.reset(commitID);
                break;
            case "merge":
                validateOperands(args, 2);
                branchName = args[1];
                repo.merge(branchName);
                break;
            case "author":
                validateOperands(args, 2);
                String authorName = args[1];
                repo.changeAuthor(authorName);
            default:
                System.out.println("No command with that name exists.");
        }
    }

    /**
     * Checks the number of arguments versus the expected number,
     * Exit the program if they do not match.
     *
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateOperands(String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
