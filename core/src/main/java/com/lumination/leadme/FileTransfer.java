package com.lumination.leadme;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class FileTransfer {
    private static final String TAG = "FileTransfer";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FileTransfer";
    private final int numberOfThreads = 2; //how many transfers can operate simultaneously

    //Notification Alerts
    private final CharSequence notificationName = "File Transfer";
    private final String notificationDescription = "File Transfer in progress.";
    private final String success = "Transfer Complete";
    private final String failure = "Error: Transfer incomplete";
    protected static boolean transferComplete = true;

    //Transfer error messages
    private final String fileOnDevice = "File already on selected device.";
    private final String transferNotSaved = "Transfer could not be saved on device.";

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfThreads);

    protected NotificationManager notifyManager;
    protected NotificationCompat.Builder builder;

    protected ServerSocket fileServerSocket = null;
    protected Socket fileSocket = null;
    protected ArrayList<Integer> selected;
    protected static HashMap<Integer, Double> transfers;

    private final LeadMeMain main;
    private static double transfer_progress;

    /**
     * Integer to keep track of how many peers a file is being sent to.
     */
    public static int transferCount;

    /**
     * Basic constructor for the file transfer class. Responsible for setting up a secondary server
     * and sending files via DataOutputStreams to the selected peers.
     * @param main A reference to the LeadMe main activity.
     */
    public FileTransfer(LeadMeMain main) {
        this.main = main;
        setupNotificationChannel();
    }

    /**
     * Start a secondary server on the Guide's device. This server directly controls the transfer of files
     * through a DataOutputStream. Need to have secondary server as these connections need to be closed
     * once a file has been transferred.
     * @param fileUri A Uri representing a file that has been selected.
     */
    public void startFileServer(Uri fileUri) {
        //section to reuse for selecting files for viewing
        //Uri fileUri = data.getData();
        Log.i(TAG, "Uri: " + fileUri);

        String filePath = null;
        try {
            filePath = FileUtilities.getPath(main.context, fileUri);
        } catch (Exception e) {
            Log.e(TAG,"Error: " + e);
            Toast.makeText(main.context, "Error: " + e, Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(main.context, "Path: " + filePath, Toast.LENGTH_LONG).show();

        Log.e("File", filePath);
        //end section

        if(filePath == null) {
            return;
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e("File Result: ", String.valueOf(e));
        }

        //TODO this might cause issues with school wifi's - keep note of this
        try {
            fileServerSocket = new ServerSocket(0);
            Log.d(TAG, "ServerSocket: socket created");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "ServerSocket: socket not created");
        }

        sendFile(filePath);
    }

    //Get the correct file path, selected peers and start the thread executor.
    private void sendFile(String path) {
        //make sure at 0 for each new transfer
        transfer_progress = 0;
        transferCount = 0;

        File file;
        Log.e("Path", path);

        //determine if loading from sdcard or internal storage
        if (path.contains("sdcard")) {
            String newPath = null;
            String[] editedPath = path.split("sdcard/");
            File[] dirs = main.getExternalFilesDirs(null);
            if (dirs != null && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                newPath = dirs[dirs.length - 1].getAbsolutePath();
                newPath = newPath.substring(0, newPath.indexOf("Android"));
                newPath += editedPath[1];
                Log.e("Path", newPath);
            }
            file = new File(newPath);
        } else {
            file = new File(path);
        }

        //TODO move this out and pass as parameter from main?
        selected = new ArrayList<>();

        //cycle through the selected peers adding them to the selected array
        for (ConnectedPeer peer : main.getConnectedLearnersAdapter().mData) {
            if (peer.isSelected()) {
                int ID = Integer.parseInt(peer.getID());
                selected.add(ID);
            }
        }

        if(selected.size() < 1) {
            //TODO create popup for information
            Toast.makeText(main.context, "Peers need to be selected.", Toast.LENGTH_LONG).show();
            return;
        }

        //get the total file size for work out the percentage of total transfer
        transfers = new HashMap<>();

        Log.d("Transfers", String.valueOf(transfers.size()));

        builder.setProgress(100, 0, false)
                .setOngoing(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("File Name: " + file.getName()));
        notifyManager.notify(NOTIFICATION_ID, builder.build());

        //schedule file transfers for all the selected peers and update the status icon
        for (int x = 0; x < selected.size(); x++) {
            main.updatePeerStatus(String.valueOf(selected.get(x)), ConnectedPeer.STATUS_FILE_TRANSFER, null);

            transfers.put(selected.get(x), 0.0); //initial peers and default starting value
            Transfer transfer = new Transfer(file, path, this, main, selected.get(x));
            executor.execute(transfer);
        }
    }

    //Create a notification channel, only for API 26+
    private void setupNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, notificationName, importance);

            //Register the channel with the system
            main.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        buildNotification();
    }

    //Set the manager and builder
    private void buildNotification() {
        notifyManager = (NotificationManager) main.getSystemService(Context.NOTIFICATION_SERVICE);

        builder = new NotificationCompat.Builder(main, CHANNEL_ID).setSmallIcon(R.drawable.leadme_icon)
                .setContentTitle(notificationName)
                .setContentText(notificationDescription)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(""))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    /**
     * Create a socket to receive a file from the Guide.
     * @param ip An InetAddress of the current Guide.
     * @param port The port at which the Guide is connected to.
     */
    public void receivingFile(InetAddress ip, int port) {
        try {
            fileSocket = new Socket(ip, port);
            Log.d(TAG, "saveFile: socket connected");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "saveFile: socket not connected");
        }

        saveFile();
    }

    //Saves the file to the local gallery using the MediaStore
    private void saveFile() {
        Thread saveFile = new Thread(() -> {
            try {
                main.setDeviceStatusMessage(R.string.transfer_in_progress);

                DataInputStream dis = new DataInputStream(fileSocket.getInputStream());
                OutputStream fos; //file output stream - depends on the SDK
                Uri fileExists;

                String fileName = dis.readUTF();

                //Works for API 29+ at least - needs more testing
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    fileExists = FileUtilities.getFileByName(main, fileName);
                } else {
                    fileExists = null;
                }

                Log.e(TAG, String.valueOf(fileExists));
                //Send message to guide that it already has the video
                if(fileExists != null) {
                    main.transferError(fileOnDevice, main.getNearbyManager().myID);
                    return;
                }

                Log.d("File name", fileName);

                builder.setProgress(100, 0, false)
                        .setContentText(notificationDescription)
                        .setOngoing(true);
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText("File Name: " + fileName));
                notifyManager.notify(NOTIFICATION_ID, builder.build());

                //Only use MediaStore for API 29+
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    //Save to phone gallery
                    Uri mediaCollection;
                    ContentResolver resolver = main.getContentResolver();
                    ContentValues newCaptureDetails = new ContentValues();

                    //determine file type - extend this in the future
                    if (fileName.toLowerCase().contains(".jpg") || fileName.toLowerCase().contains(".png")) {
                        mediaCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        newCaptureDetails.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    } else if (fileName.toLowerCase().contains(".mp4") || fileName.toLowerCase().contains(".mov")) {
                        mediaCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        newCaptureDetails.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/");
                        newCaptureDetails.put(MediaStore.Video.Media.TITLE, fileName);
                        newCaptureDetails.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
                        //newCaptureDetails.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                    } else return;

                    Uri newCaptureUri = resolver.insert(mediaCollection, newCaptureDetails);
                    fos = resolver.openOutputStream(newCaptureUri);

                } else {
                    Log.i(TAG, "Pre API 29 way to save the file");
                    String videoDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
                    File video = new File(videoDir, fileName);
                    fos = new FileOutputStream(video);
                }

                byte[] buffer = new byte[4096];

                long size = dis.readLong();
                long fileLength = size;
                Log.d("File size", String.valueOf(fileLength));

                int progress = 0;
                int read;
                while ((read = dis.read(buffer, 0, (int) Math.min(buffer.length, size))) != 0) {
                    fos.write(buffer, 0, read);
                    size -= read;
                    progress += read;
                    updateStudentProgress(fileLength, progress);
                }

                fos.close();
                dis.close();
                fileSocket.close();
                Log.d("File", "File saved");
            } catch (IOException e) {
                Log.e(TAG, String.valueOf(e));
                transferComplete = false;
                main.transferError(transferNotSaved, main.getNearbyManager().myID);
            } finally {
                //while transferring show a loading screen
                dismissPopup();
                main.setDeviceStatusMessage(R.string.connected_label);
            }
        });
        Log.d("SavingFile", "Starting to save file");

        saveFile.start();
    }

    /**
     * Removes a peer from the selected array as they already have the video.
     * @param ID The peer to be removed from the transfer array.
     * @param error The error that has occurred.
     */
    public void removePeer(String ID, String error) {
        Toast.makeText(main, "Message: " + error + " " + "Peer: " + ID, Toast.LENGTH_LONG).show();
        Log.d(TAG, "Message: " + error + " " + "Peer: " + ID);

        int peerID = Integer.parseInt(ID);

        main.runOnUiThread(() -> main.updatePeerStatus(ID, ConnectedPeer.STATUS_INSTALLED, null));

        //remove from transfers
        transfers.remove(peerID);

        if(transfers.size() == 0) {
            dismissPopup();
        }
    }

    //update the guides loading bar
    protected void updateGuideProgress(long total, int current, int index) {
        double overallPercent = 0;
        double percent = (((double) current / (double) total) * 100);

        //Check if it has been created/deleted before updating
        if(transfers.get(index) != null) {
            transfers.put(index, percent); //update the original entry
        }

        //get the average of array of percentages
        for(Map.Entry<Integer, Double> entry : transfers.entrySet()) {
            overallPercent += entry.getValue();
        }

        transfer_progress = overallPercent/transfers.size();

        //Do not call to frequently otherwise notifications are dropped, included in the
        //completion one.
        if((int) transfer_progress % 10 == 7) {
            builder.setProgress(100, (int) transfer_progress, false);
            notifyManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    //update the students loading bar
    protected void updateStudentProgress(long total, int current) {
        double percent = (((double) current / (double) total) * 100);

        //Do not call to frequently otherwise notifications are dropped, included in the
        //completion one.
        if((int) percent % 10 == 7) {
            builder.setProgress(100, (int) percent, false);
            notifyManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    //dismiss the loading bar after a transfer is completed
    protected void dismissPopup() {
        String status;
        status = transferComplete ? success : failure;

        if(!transferComplete) {
            builder.setContentText(status)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(status));

            builder.setOngoing(false)
                    .setProgress(0,0, false);
        } else {
            notifyManager.cancel(NOTIFICATION_ID);
        }

        //notifyManager.notify(NOTIFICATION_ID, builder.build());

        //Reset for next time
        transferComplete = true;
        transfers = new HashMap<>();
    }
}

/**
 * A runnable that creates a new thread that manages the file transfer for each peer that has been
 * selected.
 */
class Transfer implements Runnable {
    private static final String TAG = "FileTransfer";

    private long fileLength;
    private final File file;
    private final String path;
    private final FileTransfer fileTransfer;
    private final LeadMeMain main;
    private final int ID;

    public Transfer(File file, String path, FileTransfer fileTransfer, LeadMeMain main, int ID) {
        this.file = file;
        this.path = path;
        this.fileTransfer = fileTransfer;
        this.main = main;
        this.ID = ID; //used as a position in the transfer hashmap for progress tracking
    }

    public void run() {
        //pass the selected peers here and then thread executor for management
        main.getNearbyManager().networkAdapter.sendFile(ID, fileTransfer.fileServerSocket.getLocalPort());

        Log.d("Selected File", String.valueOf(file));
        Log.d("File size", String.valueOf(fileLength));

        try {
            Log.d(TAG, "ServerSocket: accepting...");
            fileTransfer.fileSocket = fileTransfer.fileServerSocket.accept(); //blocks until there is a connection
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "ServerSocket: socket not created");
        }

        fileLength = file.length();

        if (path != null) {
            try {
                DataOutputStream dos = new DataOutputStream(fileTransfer.fileSocket.getOutputStream());
                FileInputStream fis = new FileInputStream(file);

                byte[] buffer = new byte[4096];

                Log.e("File name to send", file.getName());

                dos.writeUTF(file.getName());
                dos.writeLong(fileLength);

                int progress = 0;
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, read);
                    dos.flush();
                    progress += read;
                    fileTransfer.updateGuideProgress(fileLength, progress, this.ID);
                }

                fis.close();
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "SendingFile: File not sent");
                FileTransfer.transferComplete = false;
                FileTransfer.transfers.remove(this.ID);
            } finally {
                FileTransfer.transferCount++;
                main.runOnUiThread(() -> main.updatePeerStatus(String.valueOf(this.ID), ConnectedPeer.STATUS_INSTALLED, null));
                //check that all the files have been transferred - reset connections and progress
                if(FileTransfer.transferCount == fileTransfer.selected.size()) {
                    reset();
                }
            }
        } else {
            Toast.makeText(main, "File has not been selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void reset() {
        fileTransfer.dismissPopup();
        try {
            fileTransfer.fileSocket.close();
            fileTransfer.fileServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "SocketClosure: socket closing issue");
        }
    }
}