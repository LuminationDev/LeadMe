package com.lumination.leadme.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.services.FileTransferService;
import com.lumination.leadme.utilities.FileUtilities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class FileTransferManager {
    private static final String TAG = "FileTransferManager";

    private final LeadMeMain main;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FileTransfer";

    //Notification Alerts
    public static boolean transferComplete = true;
    private final CharSequence notificationName = "File Transfer";
    private final String notificationDescription = "File Transfer in progress.";

    //Transfer error messages
    private final String fileOnDevice = "File already on selected device.";
    private final String transferNotSaved = "Transfer could not be saved on device.";

    private Socket fileSocket = null;
    private NotificationManager notifyManager;
    private NotificationCompat.Builder builder;
    private final ProgressBar transferBar;
    private Timer timestampUpdater;
    private final boolean testBar = true;
    private boolean request = false; //if a learner is requesting a file

    public static int selectedCounter = 0;
    public static ArrayList<String> selected;
    public static HashMap<String, Double> transfers;
    public static double transfer_progress = -1;
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

    /**
     * Integer to keep track of how many peers a file is being sent to.
     */
    public static int transferCount;

    /**
     * Basic constructor for the file transfer class. Responsible for setting up a secondary server
     * and sending files via DataOutputStreams to the selected peers.
     * @param main A reference to the LeadMe main activity.
     */
    public FileTransferManager(LeadMeMain main) {
        this.main = main;
        transferBar = main.mainLearner.findViewById(R.id.transfer_progress_bar);
        setupNotificationChannel();
    }

    /**
     * Create a notification channel, only for API 26+
     */
    private void setupNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, notificationName, importance);

            //Register the channel with the system
            main.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        buildNotification();
    }

    /**
     * Set the manager and builder for the notification system.
     */
    private void buildNotification() {
        notifyManager = (NotificationManager) main.getSystemService(Context.NOTIFICATION_SERVICE);

        builder = new NotificationCompat.Builder(main, CHANNEL_ID).setSmallIcon(R.drawable.leadme_icon)
                .setContentTitle(notificationName)
                .setContentText(notificationDescription)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(""))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    /**
     * Start the file transfer service, this is simply for displaying the notification for the
     * user to see.
     */
    private void startService() {
        Log.d(TAG, "startService: ");
        Intent transfer_intent = new Intent(main.getApplicationContext(), FileTransferService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            main.startForegroundService(transfer_intent);
        } else {
            main.startService(transfer_intent);
        }
    }

    /**
     * Stop the service, this stops the file transfer server if it is still running in the background.
     */
    public void stopService() {
        Intent stop_transfer_intent = new Intent(main.getApplicationContext(), FileTransferService.class);
        main.stopService(stop_transfer_intent);
    }

    /**
     * Start a secondary server on the Guide's device. This server directly controls the transfer of files
     * through a DataOutputStream. Need to have secondary server as these connections need to be closed
     * once a file has been transferred.
     * @param fileUri A Uri representing a file that has been selected.
     * @param requesting A boolean determining if the file has been requested by a learner device
     *                   or if it is a regular transfer initiated by a guide.
     */
    public void startFileServer(Uri fileUri, boolean requesting) {
        request = requesting;

        if(!getFilePath(fileUri)) {
            Log.e(TAG,"File is unreachable, possibly due to permissions.");
            return;
        }

        startService();
        FileTransferService.startFileServer();

        sendFile();
    }

    /**
     * Used for older Xiaomi phones as they return the file path instead of a UriStart a secondary server on the Guide's device. This server directly controls the transfer of files
     * through a DataOutputStream. Need to have secondary server as these connections need to be closed
     * once a file has been transferred.
     * @param filePath A string representing the path of athe selected file on the device.
     * @param requesting A boolean representing if the file is being requested by a learner device.
     */
    public void startFileServer(String filePath, boolean requesting) {
        request = requesting;

        internalOrExternal(filePath);

        startService();
        FileTransferService.startFileServer();

        sendFile();
    }

    /**
     * Get the exact file path for a file from a supplied Uri. The file path is needed as Uri's are
     * meaningless across separate devices.
     * @param fileUri A content Uri of the selected file.
     * @return A boolean representing if the file is available to send.
     */
    private boolean getFilePath(Uri fileUri) {
        Log.d(TAG, "Uri: " + fileUri);

        String path = null;
        try {
            path = FileUtilities.getPath(main.context, fileUri);
        } catch (Exception e) {
            Log.e(TAG,"Error: " + e);
        }

        Log.d(TAG + " File", path);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e("File Result: ", String.valueOf(e));
        }

        if(path == null) {
            return false;
        } else {
            internalOrExternal(path);
            return true;
        }
    }

    /**
     * Determine if loading from sdcard or internal storage and set the file accordingly.
     * @param path A string representing the exact path of the file on a device's storage.
     */
    private void internalOrExternal(String path) {
        Log.d(TAG + " Path", path);

        if (path.contains("sdcard")) {
            String newPath = null;
            String[] editedPath = path.split("sdcard/");
            File[] dirs = main.getExternalFilesDirs(null);
            if (dirs != null && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                newPath = dirs[dirs.length - 1].getAbsolutePath();
                newPath = newPath.substring(0, newPath.indexOf("Android"));
                newPath += editedPath[1];
                Log.d(TAG, "SD card Path: " + newPath);
            }
            assert newPath != null;
            file = new File(newPath);
        } else {
            file = new File(path);
        }
    }

    /**
     * Get the correct file path, selected peers and start the thread executor.
     */
    private void sendFile() {
        //Cancel any previous notification
        notifyManager.cancel(NOTIFICATION_ID);

        //make sure at 0 for each new transfer
        transfer_progress = 0;
        transferCount = 0;

        if(!peerList()) {
            return;
        }

        updateDeviceMessages();

        builder.setProgress(100, 0, false)
                .setOngoing(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("File Name: " + file.getName()));
        notifyManager.notify(NOTIFICATION_ID, builder.build());

        scheduleForTransfer();
    }

    /**
     * Determine the list of peers that the file is going to be sent to.
     * @return A boolean determining if any peers have been selected.
     */
    private boolean peerList() {
        selected = new ArrayList<>();

        //Determine if file request of regular transfer
        if(request) {
            selected.addAll(LeadMeMain.fileRequests);
        } else {
            //cycle through the selected peers adding them to the selected array
            for (ConnectedPeer peer : ConnectedLearnersAdapter.mData) {
                if (peer.isSelected()) {
                    selected.add(peer.getID());
                }
            }
        }

        if (selected.size() < 1) {
            LeadMeMain.runOnUI(() -> Toast.makeText(
                    main.context,
                    "Peers need to be selected.", Toast.LENGTH_LONG).show());

            return false;
        }

        Log.d("Selected Peers", String.valueOf(selected.size()));

        return true;
    }

    /**
     * Updates the learner devices that something is about to happen, in this instance a transfer
     * is being queued up.
     */
    private void updateDeviceMessages() {
        Set<String> peerSet = new HashSet<>();

        //Add the peer to the set for message updates
        for (String ID: selected) {
            peerSet.add(ID);
        }

        DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                Controller.UPDATE_DEVICE_MESSAGE + ":" + R.string.waiting_for_transfer, peerSet);
    }

    /**
     * Schedule file transfers for all the selected peers using the ThreadPoolExecutor for simultaneous
     * transfer management and update the status icons on the guide's device.
     */
    private void scheduleForTransfer() {
        //get the total file size for work out the percentage of total transfer
        transfers = new HashMap<>();

        for (int x = 0; x < selected.size(); x++) {
            int finalX = x;
            LeadMeMain.runOnUI(() -> main.updatePeerStatus(
                    String.valueOf(selected.get(finalX)),
                    ConnectedPeer.STATUS_FILE_TRANSFER, null));

            transfers.put(selected.get(x), 0.0); //initial peers and default starting value

            //Only start the appropriate number of threads
            if(x < FileTransferService.numberOfTransferThreads) {
                selectedCounter = FileTransferService.numberOfTransferThreads;
                NetworkManager.sendFile(selected.get(x), FileTransferService.PORT, fileType);
            }
        }

        startVisualTimer();
        Log.d(TAG, "Amount of transfers: " + transfers.size());
    }

    /**
     * Send the file transfer initiator to the next learner in the selected learners list. This
     * start the next transfer.
     */
    public static void nextTransfer() {
        NetworkManager.sendFile(selected.get(selectedCounter), FileTransferService.PORT, fileType);
        selectedCounter++;
    }

    /**
     * Create a socket to receive a file from the Guide.
     * @param ip An InetAddress of the current Guide.
     * @param port The port at which the Guide is connected to.
     */
    public void receivingFile(InetAddress ip, int port) {
        Log.d(TAG, "Connecting to server: " + ip + " : " + port);

        try {
            fileSocket = new Socket();

            fileSocket.connect(new InetSocketAddress(ip, FileTransferService.PORT), 10000);

            Log.d(TAG, "saveFile: socket connected");

            sendIDToGuide();

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "saveFile: socket not connected");
        }

        saveFile();
    }

    /**
     * Send the ID of the current peer to the guide through an output stream. It closes the streams
     * output on completion, ready for data from the server.
     * @throws IOException Throws exception if the socket is unable to manage the action.
     */
    private void sendIDToGuide() throws IOException {
        // get the output stream from the socket.
        OutputStream outputStream = fileSocket.getOutputStream();
        // create a data output stream from the output stream so we can send data through it
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        Log.d(TAG, "Sending ID to the ServerSocket");

        // write the message we want to send
        dataOutputStream.writeUTF(NearbyPeersManager.getID());
        dataOutputStream.flush(); // send the message

        //ID has been sent, nothing else is going to be sent so close just the out streams
        fileSocket.shutdownOutput();
    }

    /**
     * Saves the file to the local gallery using the MediaStore
     */
    private void saveFile() {
        Thread saveFile = new Thread(() -> {
            try {
                DataInputStream dis = new DataInputStream(fileSocket.getInputStream());
                OutputStream fos; //file output stream - depends on the SDK
                Uri fileURI = null;
                File fileExists = null;

                String fileName = dis.readUTF();

                Log.d(TAG, "File name: " + fileName);

                //Works for API 27+ at least - needs more testing
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    fileURI = FileUtilities.searchStorage(main, fileName);
                } else {
                    fileExists = FileUtilities.findFile(Environment.getExternalStorageDirectory(), fileName);
                }

                Log.d(TAG, "File URI: " + fileURI + "File exists: " + fileExists);

                //Send message to guide that it already has the video
                if(fileURI != null || fileExists != null) {
                    DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                            Controller.TRANSFER_ERROR
                                    + ":" + NearbyPeersManager.myID
                                    + ":" + fileOnDevice,
                            NearbyPeersManager.getSelectedPeerIDsOrAll());
                    return;
                }

                main.setDeviceStatusMessage(R.string.transfer_in_progress);

                if(testBar) {
                    LeadMeMain.runOnUI(() -> transferBar.setVisibility(View.VISIBLE));
                }

                builder.setProgress(100, 0, false)
                        .setContentText(notificationDescription)
                        .setOngoing(true);
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText("File Name: " + fileName));
                notifyManager.notify(NOTIFICATION_ID, builder.build());

                fos = determineFileType(fileName);

                if(fos == null) {
                    throw new IOException("File type not supported.");
                }

                transfer_progress = 0;
                startVisualTimer();

                byte[] buffer = new byte[4096];

                long size = dis.readLong();
                long fileLength = size;

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
                Log.d(TAG, "File saved");

                //Send confirmation that the transfer is complete - guide can then handle it however necessary
                DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.FILE_REQUEST_TAG + ":" + NearbyPeersManager.getID()
                                + ":" + "true" + ":" + fileType, NearbyPeersManager.getSelectedPeerIDs());
            } catch (IOException e) {
                try {
                    fileSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                e.printStackTrace();
                transferComplete = false;
                DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                        Controller.TRANSFER_ERROR
                                + ":" + NearbyPeersManager.myID
                                + ":" + transferNotSaved,
                        NearbyPeersManager.getSelectedPeerIDsOrAll());
            } finally {
                finishTransfer();
            }
        });
        Log.d(TAG, "Starting to save file");

        saveFile.start();
    }

    /**
     * Determine the type of file that is being saved to a device and what method is needed
     * to save it.
     * @param fileName A string representing the name of the file to be saved containing
     *                 it's file extension.
     * @return An OutputStream directed at the particular storage location required.
     */
    private OutputStream determineFileType(String fileName) {
        OutputStream fos = null;

        try {
            boolean isMovie = fileName.toLowerCase().contains(".mp4")
                    || fileName.toLowerCase().contains(".avi")
                    || fileName.toLowerCase().contains(".mkv")
                    || fileName.toLowerCase().contains(".webm")
                    || fileName.toLowerCase().contains(".mov");

            boolean isPicture = fileName.toLowerCase().contains(".jpg")
                    || fileName.toLowerCase().contains(".jpeg")
                    || fileName.toLowerCase().contains(".png");

            boolean isDocument = fileName.toLowerCase().contains(".pdf");

            //Only use MediaStore for API 29+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Save to phone gallery
                Uri mediaCollection;
                ContentResolver resolver = main.getContentResolver();
                ContentValues newCaptureDetails = new ContentValues();

                if (isPicture) {
                    mediaCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    newCaptureDetails.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                } else if (isMovie) {
                    mediaCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    newCaptureDetails.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/");
                    newCaptureDetails.put(MediaStore.Video.Media.TITLE, fileName);
                    newCaptureDetails.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
                    //newCaptureDetails.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                } else if (isDocument) {
                    mediaCollection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    newCaptureDetails.put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName);
                } else {
                    return null;
                }

                Uri newCaptureUri = resolver.insert(mediaCollection, newCaptureDetails);
                fos = resolver.openOutputStream(newCaptureUri);

            } else {
                Log.d(TAG, "Pre API 29 way to save the file");

                String directory;

                if (isPicture) {
                    directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                } else if (isMovie) {
                    directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
                } else if (isDocument) {
                    directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
                } else {
                    return null;
                }

                File newFile = new File(directory, fileName);
                fos = new FileOutputStream(newFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fos;
    }

    /**
     * Tidy up any lose ends for the file transfer. Close the file socket, stop the visual timer
     * and remove the notification/progress bar from view.
     */
    private void finishTransfer() {
        //while transferring show a loading screen
        try {
            fileSocket.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        if(testBar) {
            LeadMeMain.runOnUI(() -> transferBar.setVisibility(View.GONE));
        }

        transfer_progress = -1;
        stopVisualTimer();
        dismissPopup();
        main.setDeviceStatusMessage(R.string.connected_label);
    }

    /**
     * Removes a peer from the selected array as they already have the video.
     * @param ID The peer to be removed from the transfer array.
     * @param error The error that has occurred.
     */
    public void removePeer(String ID, String error) {
        String name = ConnectedLearnersAdapter.getMatchingPeer(ID).getDisplayName();

        LeadMeMain.runOnUI(() -> Toast.makeText(main,
                        "Message: " + error + " " + "Peer: " + name, Toast.LENGTH_LONG).show());

        Log.d(TAG, "Message: " + error + " " + "Peer: " + name);

        int peerID = Integer.parseInt(ID);

        LeadMeMain.runOnUI(() -> main.updatePeerStatus(ID, ConnectedPeer.STATUS_INSTALLED, null));

        //remove from transfers
        transfers.remove(peerID);

        //check if actually removed as could be updated at the same time.
        Log.d(TAG, "Transfer size after removal: " + transfers.size());
        if(transfers.get(peerID) != null) {
            transfers.remove(peerID);
        }

        if(transfers.size() < 1) {
            dismissPopup();
        }
    }

    /**
     * Update the guides loading bar within the notification icon.
     * @param total A long defining the total data length that is being transferred.
     * @param current An integer representing the current amount that has been transferred.
     * @param index An integer representing a Peer ID within the transfers hashmap for which the
     *              specific progress is being updated.
     */
    public static void updateGuideProgress(long total, int current, String index) {
        double overallPercent = 0;
        double percent = (((double) current / (double) total) * 100);

        //Check if it has been created/deleted before updating
        if(transfers.get(index) != null) {
            transfers.put(index, percent); //update the original entry
        }

        //get the average of array of percentages
        for(Map.Entry<String, Double> entry : transfers.entrySet()) {
            overallPercent += entry.getValue();
        }

        transfer_progress = overallPercent/transfers.size();
    }

    /**
     * Update the students loading bar within the notification icon.
     * @param total A long defining the total data length that is being transferred.
     * @param current An integer representing the current amount that has been transferred.
     */
    protected static void updateStudentProgress(long total, int current) {
        transfer_progress = (((double) current / (double) total) * 100);
    }

    /**
     * At set time intervals update the progress of the transfer, this drastically limits the
     * amount of calls to the notification channel and UI thread.
     */
    private void startVisualTimer() {
        timestampUpdater = new Timer();
        TimerTask updateTimestamp = new TimerTask() {
            @Override
            public void run() {
                visualUpdate();
            }
        };

        //update the progress bar and notification bar every (3s)
        timestampUpdater.scheduleAtFixedRate(updateTimestamp, 0L, 3000);
    }

    /**
     * Update the progress bar and notification with the current transfer progress.
     */
    private void visualUpdate() {
        if(transfer_progress != -1) {
            builder.setProgress(100, (int) transfer_progress, false);
            notifyManager.notify(NOTIFICATION_ID, builder.build());

            checkProgress();

            if (testBar && !LeadMeMain.isGuide) {
                LeadMeMain.runOnUI(() -> transferBar.setProgress((int) transfer_progress));
            }
        } else {
            checkProgress();
            dismissPopup();
            stopVisualTimer();
        }
    }

    /**
     * Run through the transfers to see if any have completed on the leaders side. Update the
     * connected peer icon if it has.
     */
    private void checkProgress() {
        if(LeadMeMain.isGuide) {
            for (Map.Entry<String, Double> entry : transfers.entrySet()) {
                String key = entry.getKey();
                Double value = entry.getValue();

                if (value == 100.0) {
                    LeadMeMain.runOnUI(() -> main.updatePeerStatus(String.valueOf(key), ConnectedPeer.STATUS_INSTALLED, null));
                    //Reset the position as to not constantly call update
                    transfers.put(key, -1.0);
                }
            }
        }
    }

    /**
     * Stop the timer related to visual updates.
     */
    private void stopVisualTimer() {
        if(timestampUpdater != null) {
            timestampUpdater.cancel();
        }
    }

    /**
     * Dismiss the loading bar after a transfer is completed or set an error message if something
     * has gone wrong.
     */
    public void dismissPopup() {
        String success = "Transfer Complete";
        String failure = "Error: Transfer incomplete";
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

        //Reset for next time
        transferComplete = true;
        transfers = new HashMap<>();

        if(LeadMeMain.isGuide) {
            stopService();
        }
    }
}
