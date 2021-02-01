package com.lumination.leadme;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class LocalServer {
    Activity activity;
    ServerSocket serverSocket;
    String folder;
    String fileToTransfer;
    Boolean monitoring;
    static final int socketServerPORT = 8080;
    //static int imgcnt=0;
    int imgcnt=0;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // check permissions for writing, reading and accessing external storage
    public void verifyStoragePermissions(Activity activity) { // Check if we have write permission
        int permissionRead = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionWrite = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionRead != PackageManager.PERMISSION_GRANTED ||
                permissionWrite != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
        System.out.println(permissionRead);
    }

    //start the local server
    public LocalServer(Activity activity, Boolean monitoring, String folder, String fileToTransfer) {
        this.activity = activity;
        this.monitoring = monitoring;
        this.folder = folder;
        this.fileToTransfer = fileToTransfer;
        this.imgcnt = 0;
        verifyStoragePermissions(this.activity);
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }


    private class SocketServerThread extends Thread {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(socketServerPORT);

                while (true) {
                    // up to here, server is created and connected to the client
                    Socket socket = serverSocket.accept();

                    //Log.e("newSocket", String.valueOf(socket));

                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(
                            socket);
                    socketServerReplyThread.run();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SocketServerReplyThread extends Thread {

        private final Socket hostThreadSocket;

        SocketServerReplyThread(Socket socket) {
            hostThreadSocket = socket;
        }

        //add separate methods for casting and file pushing
        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply;
            String path = activity.getFilesDir() + File.separator + "LeadMe" + File.separator + folder;

            //if monitoring do this
            if (monitoring) {
                File image = new File(path, "capturedscreenandroid" + String.valueOf(imgcnt) + ".jpg");

                if (image.exists()) {
                    msgReply = encodeToBase64(loadImageFromStorage(path, image), Bitmap.CompressFormat.JPEG, 70);
                } else msgReply = "true";
            } else {
                //File file = new File(path, fileToTransfer);
                msgReply = fileToTransfer; //this should be the file
            }

            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msgReply);
                printStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality) {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap loadImageFromStorage(String path, File image) {
        Bitmap b = null;
        try {
            if(imgcnt > 0) destroyImageFromStorage(path, imgcnt - 1);

            b = BitmapFactory.decodeStream(new FileInputStream(image));

            imgcnt++;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return b;
    }

    private void destroyImageFromStorage(String path, Integer offset) {
        try {
            File image = new File(path, "capturedscreenandroid"+String.valueOf(offset)+".jpg");
            if(image.exists()) {
                if(image.delete()) {
                    System.out.println("Image deleted");
                } else System.out.println("Image not deleted");
            }
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    public String getIpAddress() {
        List<String> ipArray = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress
                            .nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ipArray.add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipArray.get(0); //only return a single ip in the case of dual wifi devices
    }

    public void onDestroy() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
