package com.lumination.leadme.utilities;

import android.util.Log;

import com.lumination.leadme.managers.FileTransferManager;
import com.lumination.leadme.managers.NetworkManager;
import com.lumination.leadme.services.FileTransferService;

/**
 * A runnable used by the transfer executor to manage the file transfer for each peer that has been
 * selected.
 */
public class TransferThread implements Runnable {
    private static final String TAG = "Transfer";

    private final int ID;

    public TransferThread(int ID) {
        this.ID = ID;
        Log.d(TAG, "Transfer thread created: " + this.ID);
    }

    public void run() {
        NetworkManager.sendFile(ID, FileTransferService.PORT, FileTransferManager.fileType);
    }
}
