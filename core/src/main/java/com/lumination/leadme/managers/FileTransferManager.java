package com.lumination.leadme.managers;



import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class FileTransferManager {
    public static ArrayList<String> selected;
    public static HashMap<String, Double> transfers;
    public static File file;

    /**
     * Learner devices track the file type being sent, used to handle the transfer completion on
     * the guide's side.
     */
    public static String fileType;

    /**
     * Get the file type that is being transferred.
     * @return A string representing the current file type that has been set.
     */
    public static String getFileType() {
        return fileType;
    }

    /**
     * Set the file type that is going to be transferred. Used for handling the completion of the
     * transfer on the guide's side.
     * @param type A string representing a file type.
     */
    public static void setFileType(String type) {
        fileType = type;
    }

}
