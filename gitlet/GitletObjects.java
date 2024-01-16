package gitlet;

import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;
import static gitlet.Repository.OBJECTS_DIR;

/** This class contains shared methods for Gitlet objects (Commit and Blob).
 *  @author Ting-Hsuan, Wu
 */

public class GitletObjects implements Serializable {
    /** Given a string OBJECTID, return the first two characters as the folder ID. */
    public static String calcFolderID(String objectID) {
        return objectID.substring(0, 2);
    }

    /** Given the OBJECTID, return a string starting from the third character as the file ID. */
    public static String calcFileID(String objectID) {
        return objectID.substring(2);
    }

    /** Return a File object, which designate the path of the object with OBJECTID. */
    public static File objectFileDesignator(String objectID) {
        String folderID = calcFolderID(objectID);
        String fileID = calcFileID(objectID);
        return join(OBJECTS_DIR, folderID, fileID);
    }

    /** Return a File object, which designate the path of the directory that stores the object
     * with OBJECTID. */
    public static File objectFolderDesignator(String objectID) {
        String folderID = calcFolderID(objectID);
        return join(OBJECTS_DIR, folderID);
    }
}
