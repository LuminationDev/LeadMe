package com.lumination.leadme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class ScreenCap {
    static final String TAG = "ScreenCap";

    LeadMeMain main;
    MediaProjectionManager projectionManager;
    MediaProjection mProjection;
    ImageReader mImageReader;

    public Socket clientToServerSocket=null;
    ExecutorService screenshotSender = Executors.newFixedThreadPool(1);
    public boolean sendImages = false;

    public ScreenCap(LeadMeMain main){
        this.main=main;
    }
    public void connectToServer(){
        if(clientToServerSocket==null) {
            screenshotSender.submit(new Runnable() {
                @Override
                public void run() {
                    if (clientToServerSocket != null) {
                        try {
                            clientToServerSocket.close();
                            clientToServerSocket = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d(TAG, "connectToServer: unable to close previous socket");
                            return;
                        }
                    }
                    try {
                        clientToServerSocket = new Socket(main.getNearbyManager().networkAdapter.clientsServerSocket.getInetAddress(), 54322);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "connectToServer: unable to connect to socket");
                    }
                }
            });
        }

    }
    public void startService(){
            projectionManager = (MediaProjectionManager) main.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent screen_share_intent = new Intent(main.getApplicationContext(), ScreensharingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                main.startForegroundService(screen_share_intent);
            } else {
                main.startService(screen_share_intent);
            }
            main.startActivityForResult(projectionManager.createScreenCaptureIntent(), main.SCREEN_CAPTURE);
    }
    public void handleResultReturn(int resultCode, Intent data){
        mProjection = projectionManager.getMediaProjection(resultCode, data);
        Log.d(TAG, "handleResultReturn: service started");
        setupScreenCap();
        getBitmapsFromScreen();
    }
    @SuppressLint("WrongConstant")
    public void setupScreenCap(){
        mImageReader = ImageReader.newInstance(getScreenWidth(), getScreenHeight(), PixelFormat.RGBA_8888, 1);
        DisplayMetrics metrics = new DisplayMetrics();
        main.getDisplay().getRealMetrics(metrics);
        mProjection.createVirtualDisplay("screen-mirror", getScreenWidth(), getScreenHeight(), metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mImageReader.getSurface(), null, null);
    }
    public void getBitmapsFromScreen(){

        mImageReader.setOnImageAvailableListener((ImageReader.OnImageAvailableListener) reader -> {

                Image image = mImageReader.acquireNextImage();
                if (image == null) {
                    return;
                }
            if(sendImages) {
                final Image.Plane[] planes = image.getPlanes();
                final ByteBuffer buffer = planes[0].getBuffer();
                int offset = 0;
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * getScreenWidth();
// create bitmap
                Bitmap bmp = Bitmap.createBitmap(getScreenWidth() + rowPadding / pixelStride, getScreenHeight(), Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buffer);

                //latestImage=bmp;
                sendScreenShot(bmp);
                Log.d(TAG, "getBitmapFromScreen: ");
            }
            image.close();
        },main.getHandler());
    }
    private static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    private static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    Future<?> lastTask= null;
    private void sendScreenShot(Bitmap image){
        if(clientToServerSocket==null){
            Log.d(TAG, "sendScreenShot: socket not initilised yet");
            return;
        }
        if(clientToServerSocket.isConnected()){
            if(lastTask!=null) {
                lastTask.cancel(false);
            }

                lastTask = screenshotSender.submit(new Runnable() {
                    Bitmap bmpToSend = image;
                    @Override
                    public void run() {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bmpToSend.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                        if (stream.toByteArray().length > 300000) {
                            stream.reset();
                            bmpToSend.compress(Bitmap.CompressFormat.JPEG, 0, stream);
                        }
                        bmpToSend.recycle();
                        bmpToSend = null;
                        byte[] byteArray = stream.toByteArray();
                        try {
                            DataOutputStream out = new DataOutputStream(clientToServerSocket.getOutputStream());
                            out.writeInt(byteArray.length);
                            out.write(byteArray, 0, byteArray.length);
                            Log.d(TAG, "run: image sent of size " + byteArray.length + " to " + clientToServerSocket.getInetAddress() + ", from " + clientToServerSocket.getLocalAddress());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        }else{
            Log.d(TAG, "sendScreenShot: server not connected");
            //connectToServer();
        }
    }
}
