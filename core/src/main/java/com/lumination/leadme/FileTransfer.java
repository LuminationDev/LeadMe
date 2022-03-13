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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class FileTransfer {
    private static final String TAG = "FileTransfer";

    private final LeadMeMain main;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FileTransfer";

    private final int numberOfThreads = 2; //how many transfers can operate simultaneously
    private Thread serverThread;
    private ThreadPoolExecutor executor;

    //Notification Alerts
    private boolean transferComplete = true;
    private final CharSequence notificationName = "File Transfer";
    private final String notificationDescription = "File Transfer in progress.";

    //Transfer error messages
    private final String fileOnDevice = "File already on selected device.";
    private final String transferNotSaved = "Transfer could not be saved on device.";

    private NotificationManager notifyManager;
    private NotificationCompat.Builder builder;
    private final ProgressBar transferBar;
    private Timer timestampUpdater;
    private final boolean testBar = true;

    private ServerSocket serverSocket = null;
    private Socket fileSocket = null;
    //TODO this might cause issues with school's wifi if ports are blocked - keep note of this
    final int PORT = 54323;

    private ArrayList<Integer> selected;
    private HashMap<Integer, Double> transfers;

    private boolean isRunning = true;
    private boolean request = false; //if a learner is requesting a file
    private double transfer_progress;
    private File file;

    /**
     * Learner devices track the file type being sent, used to handle the transfer completion on
     * the guide's side.
     */
    private static String fileType;

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
    public int transferCount;

    /**
     * Basic constructor for the file transfer class. Responsible for setting up a secondary server
     * and sending files via DataOutputStreams to the selected peers.
     * @param main A reference to the LeadMe main activity.
     */
    public FileTransfer(LeadMeMain main) {
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

        startServer();

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

        startServer();

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
     * Start the transfer server, continue accepting connections until the last transfer has been
     * completed.
     */
    public void startServer() {
        stopServer();

        //TODO test with 10+ phones in case the server shuts down before finishing
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);

        Runnable serverTask = () -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));

                Log.d(TAG, "Waiting for clients to connect...");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    clientProcessingPool.submit(new ClientTask(clientSocket));
                }

                Log.d(TAG, "Transfer server shutdown...");
                isRunning = true;

            } catch (IOException e) {
                Log.e(TAG, "Unable to start or continue transfer server");
                e.printStackTrace();
            }
        };

        serverThread = new Thread(serverTask);
        serverThread.start();
    }

    /**
     * Stop any current or old instances of the server thread and socket.
     */
    private void stopServer() {
        if(serverThread != null) {
            serverThread.interrupt();
            Log.d(TAG, "Old server thread interrupted");
        }

        if(serverSocket != null) {
            try {
                serverSocket.close();
                Log.d(TAG, "Old server socket interrupted");
            } catch (IOException e) {
                Log.e(TAG, "Unable to stop old server socket");
                e.printStackTrace();
            }
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

        scheduleForExecutor();
    }

    /**
     * Determine the list of peers that the file is going to be sent to.
     * @return A boolean determining if any peers have been selected.
     */
    private boolean peerList() {
        selected = new ArrayList<>();

        //Determine if file request of regular transfer
        if(request) {
            selected.addAll(main.fileRequests);
        } else {
            //cycle through the selected peers adding them to the selected array
            for (ConnectedPeer peer : main.getConnectedLearnersAdapter().mData) {
                if (peer.isSelected()) {
                    int ID = Integer.parseInt(peer.getID());
                    selected.add(ID);
                }
            }
        }

        if (selected.size() < 1) {
            main.runOnUiThread(() -> Toast.makeText(
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
        for (int ID: selected) {
            peerSet.add(String.valueOf(ID));
        }

        main.sendDeviceMessage(R.string.waiting_for_transfer, peerSet);
    }

    /**
     * Schedule file transfers for all the selected peers using the ThreadPoolExecutor for simultaneous
     * transfer management and update the status icons on the guide's device.
     */
    private void scheduleForExecutor() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfThreads);

        //get the total file size for work out the percentage of total transfer
        transfers = new HashMap<>();

        for (int x = 0; x < selected.size(); x++) {
            int finalX = x;
            main.runOnUiThread(() -> main.updatePeerStatus(
                    String.valueOf(selected.get(finalX)),
                    ConnectedPeer.STATUS_FILE_TRANSFER, null));

            transfers.put(selected.get(x), 0.0); //initial peers and default starting value
            Transfer transfer = new Transfer(selected.get(x));
            executor.execute(transfer);
        }

        Log.d(TAG, "Amount of transfers: " + transfers.size());
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
            //Regular getLocalPort method from network adapter
//            fileSocket.connect(new InetSocketAddress(ip, port), 10000);
            //Hard coded port in case getLocalPort is blocked (same as the connection issue)
            fileSocket.connect(new InetSocketAddress(ip, PORT), 10000);

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
        dataOutputStream.writeUTF(main.getNearbyManager().getID());
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
                    fileURI = FileUtilities.getFileByName(main, fileName);
                } else {
                    fileExists = FileUtilities.findFile(Environment.getExternalStorageDirectory(), fileName);
                }

                Log.d(TAG, "File URI: " + fileURI + "File exists: " + fileExists);

                //Send message to guide that it already has the video
                if(fileURI != null || fileExists != null) {
                    main.transferError(fileOnDevice, main.getNearbyManager().myID);
                    return;
                }

                main.setDeviceStatusMessage(R.string.transfer_in_progress);

                if(testBar) {
                    main.runOnUiThread(() -> transferBar.setVisibility(View.VISIBLE));
                }

                builder.setProgress(100, 0, false)
                        .setContentText(notificationDescription)
                        .setOngoing(true);
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText("File Name: " + fileName));
                notifyManager.notify(NOTIFICATION_ID, builder.build());

                //TODO determine file type - extend this in the future
                boolean isMovie = fileName.toLowerCase().contains(".mp4")
                        || fileName.toLowerCase().contains(".mov");

                boolean isPicture = fileName.toLowerCase().contains(".jpg")
                        || fileName.toLowerCase().contains(".png");

                boolean isDocument = fileName.toLowerCase().contains(".pdf");

                //Only use MediaStore for API 29+
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                        return;
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
                        return;
                    }

                    File newFile = new File(directory, fileName);
                    fos = new FileOutputStream(newFile);
                }

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
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.FILE_REQUEST_TAG + ":" + main.getNearbyManager().getID()
                                + ":" + "true" + ":" + fileType, main.getNearbyManager().getSelectedPeerIDs());
            } catch (IOException e) {
                try {
                    fileSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                Log.e(TAG, String.valueOf(e));
                transferComplete = false;
                main.transferError(transferNotSaved, main.getNearbyManager().myID);
            } finally {
                //while transferring show a loading screen
                try {
                    fileSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                if(testBar) {
                    main.runOnUiThread(() -> transferBar.setVisibility(View.GONE));
                }
                stopVisualTimer();
                dismissPopup();
                main.setDeviceStatusMessage(R.string.connected_label);
            }
        });
        Log.d(TAG, "Starting to save file");

        saveFile.start();
    }

    /**
     * Removes a peer from the selected array as they already have the video.
     * @param ID The peer to be removed from the transfer array.
     * @param error The error that has occurred.
     */
    public void removePeer(String ID, String error) {
        String name = main.getConnectedLearnersAdapter().getMatchingPeer(ID).getDisplayName();

        main.runOnUiThread(() -> Toast.makeText(main,
                        "Message: " + error + " " + "Peer: " + name, Toast.LENGTH_LONG).show());

        Log.d(TAG, "Message: " + error + " " + "Peer: " + name);

        int peerID = Integer.parseInt(ID);

        main.runOnUiThread(() -> main.updatePeerStatus(ID, ConnectedPeer.STATUS_INSTALLED, null));

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
     * Update the guies loading bar within the notification icon.
     * @param total A long defining the total data length that is being transferred.
     * @param current An integer representing the current amount that has been transferred.
     * @param index An integer representing a Peer ID within the transfers hashmap for which the
     *              specific progress is being updated.
     */
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
    }

    /**
     * Update the students loading bar within the notification icon.
     * @param total A long defining the total data length that is being transferred.
     * @param current An integer representing the current amount that has been transferred.
     */
    protected void updateStudentProgress(long total, int current) {
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

        //update the leaders timestamp on firebase (mins)
        timestampUpdater.scheduleAtFixedRate(updateTimestamp, 0L, 5000);
    }

    /**
     * Update the progress bar and notification with the current transfer progress.
     */
    private void visualUpdate() {
        Log.e(TAG, "Updating progress bar: " + transfer_progress);

        builder.setProgress(100, (int) transfer_progress, false);
        notifyManager.notify(NOTIFICATION_ID, builder.build());

        if(testBar && !LeadMeMain.isGuide) {
            main.runOnUiThread(() -> transferBar.setProgress((int) transfer_progress));
        }
    }

    /**
     * Stop the timer related to visual updates.
     */
    private void stopVisualTimer() {
        timestampUpdater.cancel();
    }

    /**
     * Dismiss the loading bar after a transfer is completed or set an error message if something
     * has gone wrong.
     */
    protected void dismissPopup() {
        Log.e(TAG, "Dismiss popup");

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

        //notifyManager.notify(NOTIFICATION_ID, builder.build());

        //Reset for next time
        transferComplete = true;
        transfers = new HashMap<>();
    }

    /**
     * A runnable used by the clientProcessingPool to receive information from the client sockets
     * and send the selected file to the client.
     */
    private class ClientTask implements Runnable {
        private final Socket clientSocket;
        private int ID;

        private ClientTask(Socket clientSocket) {
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
            long fileLength = file.length();

            try {
                startVisualTimer();

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
                    updateGuideProgress(fileLength, progress, this.ID);
                }

                fis.close();
                dos.close();

                Log.d(TAG, "File sent");
                removeRequest(this.ID);
            } catch (IOException e) {
                stopVisualTimer();
                e.printStackTrace();
                Log.e(TAG, "SendingFile: File not sent");
                transferComplete = false;
                transfers.remove(this.ID);
                removeRequest(this.ID);
            } finally {
                transferCount++;
                main.runOnUiThread(() -> main.updatePeerStatus(String.valueOf(this.ID), ConnectedPeer.STATUS_INSTALLED, null));

                Log.d(TAG, " Transfer end. Count: " + transferCount + " Selected size: " + selected.size());

                //check that all the files have been transferred - reset connections and progress
                if(transferCount == selected.size()) {
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
            if(main.fileRequests.size() > 0) {
                main.fileRequests.remove(Integer.valueOf(ID));
            }
        }

        /**
         * Reset the sockets and variables for next time.
         */
        private void reset() {
            stopVisualTimer();
            dismissPopup();

            try {
                this.clientSocket.close();
                isRunning = false;

                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "SocketClosure: socket closing issue");
            } finally {
                main.fileRequests = new ArrayList<>();
                executor.shutdown();

                //reset for next time
                isRunning = true;
            }

            Log.d(TAG, "SocketClosure: file transfer server reset");
            main.runOnUiThread(() -> Toast.makeText(main, "Transfers complete", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * A runnable used by the transfer executor to manage the file transfer for each peer that has been
     * selected.
     */
    private class Transfer implements Runnable {
        private final int ID;

        public Transfer(int ID) {
            this.ID = ID;
            Log.d(TAG, "Transfer thread created: " + this.ID);
        }

        public void run() {
            main.getNetworkManager().sendFile(ID, serverSocket.getLocalPort(), fileType);
        }
    }
}
