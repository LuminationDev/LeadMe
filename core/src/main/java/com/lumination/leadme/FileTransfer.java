package com.lumination.leadme;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class FileTransfer {
    private static final String TAG = "FileTransfer";
    private final int numberOfThreads = 2; //how many transfers can operate simultaneously

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfThreads);

    protected ServerSocket fileServerSocket = null;
    protected Socket fileSocket = null;
    protected final ProgressBar psgBar;
    protected final TextView selectedPeers;
    protected ArrayList<Integer> selected;

    private AlertDialog fileTransferPopup;
    private final View fileTransferPopupView;
    private final LeadMeMain main;

    private double total_file_size;
    private static double[] transfers;
    private static double transfer_progress;

    /**
     * Integer to keep track of how many peers a file is being sent to.
     */
    public static int transferCount;

    public FileTransfer(LeadMeMain main) {
        this.main = main;

        this.fileTransferPopupView = View.inflate(main, R.layout.e__transfer_popup, null);
        this.psgBar = fileTransferPopupView.findViewById(R.id.pBar);
        this.selectedPeers = fileTransferPopupView.findViewById(R.id.transfer_file_comment);
    }

    /**
     * Start a secondary server on the Guide's device. This server directly controls the transfer of files
     * through a DataOutputStream. Need to have secondary server as these connections need to be closed
     * once a file has been transferred.
     * @param data An intent representing a file that has been selected.
     */
    public void startFileServer(Intent data) {
        //section to reuse for selecting files for viewing
        Uri fileUri = data.getData();
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
        total_file_size = file.length() * selected.size();
        transfers = new double[selected.size()];

        Log.d("Transfers", String.valueOf(transfers.length));

        //open the pop up for the guide
        popup("Sending File: " + transferCount + " of " + selected.size() + " Transferred.");

        //schedule file transfers for all the selected peers
        for (int x = 0; x < selected.size(); x++) {
            Transfer transfer = new Transfer(file, path, this, main, selected.get(x), x);
            executor.execute(transfer);
        }
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
                DataInputStream dis = new DataInputStream(fileSocket.getInputStream());

                String fileName = dis.readUTF();
                Log.d("File name", fileName);

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
                OutputStream fos = resolver.openOutputStream(newCaptureUri);

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
                Log.d("File", "File saved");
            } catch (IOException e) {
                Log.e(TAG, String.valueOf(e));
            } finally {
                //while transferring show a loading screen
                dismissPopup();
            }
        });
        Log.d("SavingFile", "Starting to save file");
        saveFile.start();

        //inflate the loading progress bar
        popup("Receiving File");
    }

    //update the guides loading bar
    protected void updateGuideProgress(long total, int current, int number) {
        double overallPercent = 0;
        double percent = (((double) current / (double) total) * 100);
        Array.set(transfers, number, percent);

        //get the average of array of percentages
        for(double p : transfers) {
            overallPercent += p;
        }
        transfer_progress = overallPercent/transfers.length;

        main.runOnUiThread(() -> psgBar.setProgress((int) transfer_progress));
    }

    //update the students loading bar
    protected void updateStudentProgress(long total, int current) {
        double percent = (((double) current / (double) total) * 100);
        main.runOnUiThread(() -> psgBar.setProgress((int) percent));
    }

    //creating a popup for the loading bar (file sent and file received)
    protected void popup(String message) {
        main.runOnUiThread(() -> {
            //set the comment section of the transfer popup
            selectedPeers.setText(message);

            if (fileTransferPopup == null) {
                fileTransferPopup = new AlertDialog.Builder(main)
                        .setView(fileTransferPopupView)
                        .show();
            } else {
                fileTransferPopup.show();
            }
        });
    }

    //dismiss the loading bar after each transfer is completed
    protected void dismissPopup() {
        main.runOnUiThread(() -> fileTransferPopup.dismiss());
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
    private final int number;

    public Transfer(File file, String path, FileTransfer fileTransfer, LeadMeMain main, int ID, int number) {
        this.file = file;
        this.path = path;
        this.fileTransfer = fileTransfer;
        this.main = main;
        this.ID = ID;
        this.number = number; //position in the selected ID array for transfer progress tracking
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
                    fileTransfer.updateGuideProgress(fileLength, progress, number);
                }

                fis.close();
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "SendingFile: File not sent");
            } finally {
                FileTransfer.transferCount++;
                fileTransfer.selectedPeers.setText("Sending File: " + FileTransfer.transferCount + " of " + fileTransfer.selected.size() + " Transferred.");

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
        main.runOnUiThread(() -> fileTransfer.psgBar.setProgress(0));
    }
}