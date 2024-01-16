# Gitlet
## Overview
#### Gitlet is a version-control system that mimics some local features of Git.
The main functions Gitlet supports:
1. **Commit**: Saving the snapshot of contents of the entire directory.

2. **Checkout**: Restoring a version of one or more files or entire commits.

3. **Log**: Viewing the history of your backups.

4. **Branch**: Maintaining related sequences of commits, called branches.

5. **Merge**: Merging changes made in one branch into another.

## Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
- [Usage](#usage)

  - [Gitlet commands](#gitlet-commands)
    - [Init](#init)
    - [Add](#add)
    - [Commit](#commit)
    - [Remove](#remove)
    - [Log](#log)
    - [Status](#status)
    - [Checkout](#checkout)
    - [Branch](#branch)
    - [Find](#find)
    - [Remove branch](#remove-branch)
    - [Reset](#reset)
    - [Merge](#merge)
    - [Change author](#change-author)
- [Project Design](#project-design)
- [Contact](#contact)
- [Acknowledgements](#acknowledgements)

## Getting started
clone the repository

`git clone https://github.com/astrid2205/Gitlet.git`

and put the gitlet directory into the directory you want to perform version control.

Compile the java files `javac gitlet/Main.java` and start using Gitlet with commands `java gitlet.Main <args>` (See below section).

## Usage

### Gitlet Commands
#### Init

`java gitlet.Main init`
- Creates a new Gitlet version-control system in the current directory.
  This system will automatically start with one commit that contains no files
  and has the commit message initial commit.
  
- It will have a single branch: `master`, which initially points to this initial commit,
  and master will be the current branch.
  
- The timestamp for this initial commit will be `00:00:00 UTC, Thursday, 1 January 1970`.

#### Add

`java gitlet.Main add [file name]`
- Add a copy of the file as it currently exists to the staging area.
  Staging an already-staged file overwrites the previous entry in the
  staging area with the new contents. The file won't be added if the
  current working version of the file is identical to the version in
  the current commit.

#### Commit

`java gitlet.Main commit [commit message]`  
- Saves a snapshot of tracked files in the current commit and
  staging area, so they can be restored at a later time. A commit
  will only update the contents of files it is tracking that have
  been staged for addition at the time of commit.

#### Remove

`java gitlet.Main rm [file name]`
- Unstage the file if it is currently staged for addition. If the file is
  tracked in the current commit, stage it for removal and remove the file
  from the working directory if the user has not already done so.


#### Log
`java gitlet.Main log` 
- Print out commit logs starting at the current head commit, display information about
  each commit backwards along the commit tree until the initial commit, following the
  first parent commit links, ignoring any second parents found in merge commits.
- For example, after we added and commited a file `hello.txt`, the log will look like this:  
    
    ```
    $ java gitlet.Main add hello.txt
    
    $ java gitlet.Main commit "Add hello.txt version 1."
    
    $ java gitlet.Main log
    ===
    commit 61b506fd87966affefb4f66a16d566bd220fbcef
    Date: Wed Aug 03 15:48:40 2022 +0800
    Add hello.txt version 1.
    
    ===
    commit b5ab68c4067f41705f520ee59183d9c6ceb5f032
    Date: Thu Jan 01 08:00:00 1970 +0800
    initial commit
    ```

<br>

`java gitlet.Main global-log` 
- Displays information about all commits ever made.

#### Status
`java gitlet.Main status` 
- Displays what branches currently exist, and marks the current branch with a *.
  Also displays what files have been staged for addition or removal.
- For example, after we added the file `hi.txt` and revomed `hello.txt`, we are able to 
  figure out what changes have been staged using the `status` command.  

    ```
    $ java gitlet.Main status
    === Branches ===
    *master
    
    === Staged Files ===
    hi.txt
    
    === Removed Files ===
    hello.txt

    === Modifications Not Staged For Commit ===
    
    === Untracked Files ===
    
    ```
#### Checkout
There are 3 possible use cases of the checkout command.

1. `java gitlet.Main checkout -- [file name]`
   - Takes the version of the file as it exists in the head commit, the front of the
     current branch, and puts it in the working directory, overwriting the version
     of the file that's already there if there is one. The new version of the file
     is not staged.


2. `java gitlet.Main checkout [commit id] -- [file name]`
   - Takes the version of the file as it exists in the commit with the given id, and
     puts it in the working directory, overwriting the version of the file that's
     already there if there is one. The new version of the file is not staged.
     The commit id may be abbreviated as a SHA-1 id fewer than 40 characters.


3. `java gitlet.Main checkout [branch name]`
   - Takes all files in the commit at the head of the given branch, and puts them in
     the working directory, overwriting the versions of the files that are already
     there if they exist. Also, at the end of this command, the given branch will now
     be considered the current branch (HEAD). Any files that are tracked in the
     current branch but are not present in the checked-out branch are deleted. The
     staging area is cleared, unless the checked-out branch is the current branch.

#### Branch
`java gitlet.Main branch [branch name]`
- Creates a new branch with the given name, and points it at the
  current head node. This command does NOT immediately switch to the
  newly created branch. Before you ever call branch command, the code
  will be running with a default branch called "master".

#### Find
`java gitlet.Main find [commit message]` 
- Prints out the ids of all commits that have the given commit
  message, one per line. If there are multiple such commits, it
  prints the ids out on separate lines. To indicate a multiword
  message, put the operand in quotation marks.
- For example, we want to find all commit having the keyword "hello":

  ```
  $ java gitlet.Main log
  ===
  commit d62a0c20c8702b06da0fdd1201fe29f846cdeb18
  Date: Wed Aug 03 16:01:57 2022 +0800
  Deleted hello.txt and added hi.txt.
  
  ===
  commit 61b506fd87966affefb4f66a16d566bd220fbcef
  Date: Wed Aug 03 15:48:40 2022 +0800
  Add hello.txt version 1.
  
  ===
  commit b5ab68c4067f41705f520ee59183d9c6ceb5f032
  Date: Thu Jan 01 08:00:00 1970 +0800
  initial commit
  
  
  $ java gitlet.Main find hello
  d62a0c20c8702b06da0fdd1201fe29f846cdeb18
  61b506fd87966affefb4f66a16d566bd220fbcef


  ```
#### Remove branch
`java gitlet.Main rm-branch [branch name]` 
- Deletes the branch with the given name. This only deletes the
  pointer associated with the branch; it does not mean to delete
  all commits that were created under the branch.

#### Reset
`java gitlet.Main reset [commit id]` 
- Checks out all the files tracked by the given commit. Removes tracked
  files that are not present in that commit. Also moves the current
  branch's head to that commit node. The `[commit id]` may be abbreviated.
  The staging area is cleared.

#### Merge
`java gitlet.Main merge [branch name]` 

Merges files from the given branch into the current branch.
Rules for the merging command are as follows: 
1. Any files that have been modified in the given branch since the
   split point, but not modified in the current branch since the
   split point should be changed to their versions in the given
   branch. These files should then all be automatically staged.
2. Any files that have been modified in the current branch but not
   in the given branch since the split point should stay as they are.
3. Any files that have been modified in both the current and given
   branch in the same way (i.e., both files now have the same content
   or were both removed) are left unchanged by the merge.
4. Any files that were not present at the split point and are present
   only in the current branch should remain as they are.
5. Any files that were not present at the split point and are present
   only in the given branch should be checked out and staged.
6. Any files present at the split point, unmodified in the current
   branch, and absent in the given branch should be removed
   (and untracked).
7. Any files present at the split point, unmodified in the given
   branch, and absent in the current branch should remain absent.
8. Any files modified in different ways in the current and given
   branches are in conflict. The file will be replaced as the
   conflicted contents.

#### Change author
`java gitlet.Main author [author name]` 
- Change the author of the commit to the given author.
  If this command is never run, the author of the repository
  will be "Default author".



## Project Design
This project is built with Java.<br>
All the files required are put in the package `gitlet`, 
which includes the following  classes:
1. `Main`<br>
    This is the entry point to the program. 
    It validates and takes in arguments from the command line and calls the corresponding command 
    in the `Repository` class which will actually execute the logic of the command.<br><br>
2. `Repository`
    - This class represents a `Repository` object, which stores the information about the active 
    repository. <br> 
    The information includes: the path of the working directory that we want to perform version 
    control, information of the head commit of the current branch, the files staged for addition
    or removal, and the author of the repository.    
    - This class also handles all commands by reading/writing from/to the correct file, setting up 
    persistence, and additional error checking. <br>
    Supported commands include: init, add, remove, commit, log, status, checkout, branch, find, 
    remove branch, reset, merge, and change the author. 
    (Check out [Gitlet Commands](#gitlet-commands) for more information.)<br><br>
3. `Blob`<br>
    Represent saved content of files. A `Blob` object contains the file content (saved as a  
    byte array) and the SHA-1 ID of the file. Each unique file will have a unique Blob ID.
    This class also supports methods for serializing the `Blob` object.<br><br> 
4. `Commit`<br>
    - This class represents a `Commit` object, which stores detailed information about the 
      commit. <br>
      The information includes: the commit ID, commit author, commit time, commit message, 
      the files tracked in this commit(blobs), and the parent commit ID of this commit.
    - This class also supports methods to read/write a `Commit` object to a file. <br><br>
5. `CommitDist`<br>
    A `CommitDist` object stores the commit ID and the distance of this commit from the head 
    of its branch.
    This class is used to find the latest common ancestor of two commits. <br><br>
6. `GitletObjects`<br>
    This class contains shared methods for Gitlet objects (`Commit` and `Blob`). For example,
    `GitletObjects.objectFileDesignator()` generate the path where the serialized objects 
    should be saved. <br><br> 
7. `Utils`<br>
    This class contains helpful utility methods to read/write objects or `String` contents from/to 
    files, as well as reporting errors when they occur. This class is kindly provided by UCB 
    CS61B instructor.


After a new Gitlet version-control system is created in the working directory, all data will 
be stored in a directory called `.gitlet` inside the working directory.

When the program is running, a `Repository` object records the repository status, 
and the object is serialized and stored in a file named `repo` in `.gitlet` directory. 
Each time the repository status has changed (for example, adding a file), the `repo` file will 
be updated. 

The information of the commit (`Commit` objects) and file contents (`Blob` objects)
is serialized and stored in `.gitlet/objects` directory. The file will be saved in the directory
with the object's first two character of its SHA-1 ID as the directory name, and the remaining 
characters as its file name. 

For example, a `Commit` object with commid ID `61b506fd87966affefb4f66a16d566bd220fbcef`
will be serialized and saved in the directory `.gitlet/objects/61/`, and its file name will be 
`b506fd87966affefb4f66a16d566bd220fbcef`. 

The overall directory structure looks like this:
```
CWD                             <==== Whatever the current working directory is.
└── .gitlet                     <==== All persistant data is stored within here
    ├── objects                 <==== Where the commits and blobs are stored (as file)
    │   ├── 61                  
    │   │   └── b506fd87966affefb4f66a16d566bd220fbcef   <==== a commit/blob file 
    │   └── cf              
    │       └── b782f0843980b51e898eb869da2d0c50a6c37    <==== another commit/blob file 
    └── repo                    <==== A single Repository object stored as a file
```


## Contact
The author of this project is Wu Ting-Hsuan.

Contact me via [email](tinghsuan.th@gmail.com) or 
[GitHub](https://github.com/astrid2205/).

## Acknowledgements
Special thanks to UCB CS61B Data Structures course for the amazing class content and the inspiration of this project. 
