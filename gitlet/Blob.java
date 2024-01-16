package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static gitlet.GitletObjects.*;
import static gitlet.Repository.CWD;
import static gitlet.Utils.*;
import static gitlet.Utils.sha1;

/** Represent the saved contents of files.
 *  The file is transfer into byte array and saved in the blob object.
 *  The blob object is further hashed with the sha1 function.
 *  The serialized blob object is stored in a directory under .gitlet/objects,
 *  with the first two character of the sha1 value as its folder name,
 *  and the rest of the sha1 value as file name.
 *
 *  @author Ting-Hsuan, Wu
 */
public class Blob implements Serializable {

    /** The file content of this blob .*/
    private byte[] fileContent;

    /** The ID of this blob .*/
    private String blobID;

    /** The constructor of this blob. */
    public Blob(String fileName, String blobID) {
        this.fileContent = fileToByteArray(fileName);
        this.blobID = blobID;
    }

    /** Return the file with FILENAME as a byte array. */
    public static byte[] fileToByteArray(String fileName) {
        File inFile = join(CWD, fileName);
        return readContents(inFile);
    }

    /** Save the blob object to a file for further use. */
    public void saveBlob() {
        File outDir = objectFolderDesignator(blobID);
        if (!outDir.exists()) {
            outDir.mkdir();
        }
        File outFile = objectFileDesignator(blobID);
        if (!outFile.exists()) {
            Utils.writeObject(outFile, this);
        }
    }

    /** Check if file with same blob ID exists. */
    public static boolean checkRepeatedBlob(String blobID) {
        File expected = objectFolderDesignator(blobID);
        return expected.exists();
    }

    /** Return the blob ID of the file with FILENAME.
     * If the file does not exist in CWD, return null. */
    public static String computeBlobID(String fileName) {
        File f = join(CWD, fileName);
        if (!f.exists()) {
            return null;
        }
        byte[] fileContent = fileToByteArray(fileName);
        return sha1(fileContent);
    }

    /** Return the file content as a byte array. */
    public byte[] getFileContent() {
        return fileContent;
    }

    /** Given the blob ID of a serialized file, return the file content as string. */
    public static String fileContentAsString(String blobID) {
        byte[] c = readObject(objectFileDesignator(blobID), Blob.class).getFileContent();
        return new String(c, StandardCharsets.UTF_8);
    }
}
