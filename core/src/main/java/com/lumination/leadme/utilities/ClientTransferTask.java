package com.lumination.leadme.utilities;

import android.util.Log;

import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.managers.FileTransferManager;
import com.lumination.leadme.services.FileTransferService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * A runnable used by the clientProcessingPool to receive information from the client sockets
 * and send the selected file to the client.
 */
public class ClientTransferTask implements Runnable {
    private static final String TAG = "ClientTransferTask";

    private final Socket clientSocket;
    private int ID;

    public ClientTransferTask(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        Log.d(TAG, "Got a client !");

        receiveID();

        // Do whatever required to process the client's request
        sendSelectedFile();
    }

    /**
     * Reads the input from the clientSocket, waiting to receive the ID of the newly connected
     * client. ID is used for updating the guide's progress as well as removing the specific
     * learner from waiting queues.
     */
    private void receiveID() {
        try {
            // get the input stream from the connected socket
            InputStream inputStream = this.clientSocket.getInputStream();
            // create a DataInputStream so we can read data from it.
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            // read the message from the socket
            String message = dataInputStream.readUTF();
            Log.d(TAG, "Peer number: " + message + " has just connected.");

            this.ID = Integer.parseInt(message);

            //ID has been received, nothing else is sent so close just the input streams
            this.clientSocket.shutdownInput();

        } catch (IOException e) {
            Log.e(TAG, "Unable to process client request");
            e.printStackTrace();
        }
    }

    /**
     * Begin transferring the selected file to the newly connected socket. Updates the guides
     * progress periodically through the notification channel.
     */
    private void sendSelectedFile() {
        File file = FileTransferManager.file;

        long fileLength = file.length();

        try {
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            FileInputStream fis = new FileInputStream(file);

            byte[] buffer = new byte[4096];

            Log.d(TAG, "File name to send: " + file.getName());

            dos.writeUTF(file.getName());
            dos.writeLong(fileLength);

            int progress = 0;
            int read;

            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
                dos.flush();
                progress += read;
                FileTransferManager.updateGuideProgress(fileLength, progress, this.ID);
            }

            fis.close();
            dos.close();

            Log.d(TAG, "File sent");
            removeRequest(this.ID);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "SendingFile: File not sent");
            FileTransferManager.transferComplete = false;
            FileTransferManager.transfers.remove(this.ID);
            removeRequest(this.ID);
        } finally {
            FileTransferManager.transferCount++;

            Log.d(TAG, " Transfer end. Count: " + FileTransferManager.transferCount + " Selected size: " + FileTransferManager.selected.size());

            //check that all the files have been transferred - reset connections and progress
            if(FileTransferManager.transferCount == FileTransferManager.selected.size()) {
                reset();
            }
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove the peer from the fileRequests. Internally uses Integer.valueOf() so
     * that it is does not remove the index value instead.
     */
    private void removeRequest(int ID) {
        if(LeadMeMain.fileRequests.size() > 0) {
            LeadMeMain.fileRequests.remove(Integer.valueOf(ID));
        }
    }

    /**
     * Reset the sockets and variables for next time.
     */
    private void reset() {
        try {
            this.clientSocket.close();
            FileTransferService.isRunning = false;

            FileTransferService.stopFileServer();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "SocketClosure: socket closing issue");
        } finally {
            LeadMeMain.fileRequests = new ArrayList<>();
            FileTransferManager.executor.shutdown();

            //reset for next time
            FileTransferManager.transfer_progress = -1;
            FileTransferService.isRunning = true;
        }

        Log.d(TAG, "SocketClosure: file transfer server reset");
    }
}