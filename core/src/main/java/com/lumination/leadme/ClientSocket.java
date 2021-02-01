package com.lumination.leadme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientSocket implements Runnable {

    Socket socket;
    LeadMeMain main;
    Bitmap bitmap;
    String dstAddress;
    String response = "";
    int dstPort;
    SurfaceView surfaceView;
    final int BUFFER_SIZE = 65536;
    private final SurfaceHolder holder;

    ClientSocket(String address, int port, SurfaceView surfaceView, LeadMeMain main, SurfaceHolder holder) {
        dstAddress = address;
        dstPort = port;
        this.surfaceView = surfaceView;
        this.main = main;
        this.holder = holder;
    }

    public void run() {
        try {
            socket = new Socket(dstAddress, dstPort);

            //Log.e("Client", String.valueOf(socket));

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1000 * 1024);
            byte[] buffer = new byte[BUFFER_SIZE];

            int bytesRead;
            InputStream inputStream = socket.getInputStream();

            //notice: inputStream.read() will block if no data return
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            response = byteArrayOutputStream.toString();

            byteArrayOutputStream.close();

            if (holder != null) { //if holder exists then the client (guide) is monitoring
                main.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (main.monitorInProgress) {
                            main.startClientThread(holder, surfaceView);
                        }
                        if (Boolean.parseBoolean(response)) {
                            //Log.d("Waiting", response);
                        } else {
                            bitmap = decodeBase64(response);
                            main.response = bitmap;
                            main.tryDrawing(holder);
                        }
                    }
                });
            } else { //if holder is null then the client(peers) is receiving a file
                //Log.e("Response", response);
                //main.saveFile(response);
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Bitmap decodeBase64(String input) {
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inPreferredConfig = Bitmap.Config.valueOf("ARGB_8888");
        bfo.inMutable = true;   // this makes a mutable bitmap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bfo.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
        }

        byte[] decodedBytes = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, bfo);
    }
}
