package com.lumination.leadme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
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
    VirtualDisplay mVirtualDisplay;
    private int height;
    private int width;
    private int densityDPI;
    public boolean permissionGranted =false;
    public boolean sendImages = false;

    public Socket clientToServerSocket=null;
    ExecutorService screenshotSender = Executors.newFixedThreadPool(1);


    public ScreenCap(LeadMeMain main){
        this.main=main;
    }

    /**
     * Creates a new socket connection to the screen shot server hosted by the guides device. If
     * there is already a connection it will close it and establish a new one.
     */
    public void connectToServer(){
        if(clientToServerSocket==null) {
            screenshotSender.submit(() -> {
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
            });
        }

    }
    boolean startImed = false;

    /**
     * Starts the screen shot service for a learner device. Issues a foreground service for API levels
     * 26+.
     * @param StartOnReturn A boolean representing if the service should start sending screen shots
     *                      immediately. In the case where a guide is actively trying to view the
     *                      learners screen.
     */
    public void startService(boolean StartOnReturn){
        Log.d(TAG, "startService: ");
        projectionManager = (MediaProjectionManager) main.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent screen_share_intent = new Intent(main.getApplicationContext(), ScreensharingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            main.startForegroundService(screen_share_intent);
        } else {
            main.startService(screen_share_intent);
        }
        if(StartOnReturn){
            startImed=true;
        }
        main.startActivityForResult(projectionManager.createScreenCaptureIntent(), main.SCREEN_CAPTURE);
    }

    public void stopService() {
        Intent stop_screen_share_intent = new Intent(main.getApplicationContext(), ScreensharingService.class);
        main.stopService(stop_screen_share_intent);
    }

    public void handleResultReturn(int resultCode, Intent data){
        Log.d(TAG, "handleResultReturn: "+ resultCode);
        if(resultCode == -1){
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,LeadMeMain.STUDENT_NO_XRAY+"OK:"+main.getNearbyManager().myID,main.getNearbyManager().getAllPeerIDs());
            permissionGranted=true;
        }else{
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,LeadMeMain.STUDENT_NO_XRAY+"BAD:"+main.getNearbyManager().myID,main.getNearbyManager().getAllPeerIDs());
            return;
        }

        mProjection = projectionManager.getMediaProjection(resultCode, data);
        Log.d(TAG, "handleResultReturn: service started");

        setupScreenCap();
        getBitmapsFromScreen();
        if(startImed) {
            if (clientToServerSocket == null) {
                connectToServer();
            }
            sendImages = true;
        }
        startImed=false;
    }

    /**
     * Get the dimensions of the phone that will be sending screen shots. Sets up a virtual display
     * to act as the screen-mirror.
     */
    @SuppressLint("WrongConstant")
    public void setupScreenCap(){
        height = getScreenHeight();
        width = getScreenWidth();
        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        DisplayMetrics metrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            main.getDisplay().getRealMetrics(metrics);
        } else {
            main.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        }
        densityDPI = metrics.densityDpi;
        mVirtualDisplay = mProjection.createVirtualDisplay("screen-mirror", width, height, densityDPI, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mImageReader.getSurface(), null, null);
    }

    /**
     * Retrieve bitmaps of the current screen, these are to be sent to the guide when xray is on.
     * Note: If it randomly stops after a while of no movement check onTrimMemory in LeadMe main as
     * this stopped it in the past if memory was running low.
     */
    public void getBitmapsFromScreen(){
        mImageReader.setOnImageAvailableListener((ImageReader.OnImageAvailableListener) reader -> {

            Image image = mImageReader.acquireNextImage();

            if (image == null) {
                return;
            }
            if (sendImages) {
                //only change if the height changes
                //(meaning width has also changed)
                if (height != getScreenHeight()) {
                    height = getScreenHeight();
                    width = getScreenWidth();
                    changePeerOrientation(width, height);
                }
                final Image.Plane[] planes = image.getPlanes();
                final ByteBuffer buffer = planes[0].getBuffer();
                //int offset = 0;
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * getScreenWidth();
                // create bitmap
                Bitmap bmp = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buffer);
                image.close(); //close image as soon as possible
                //latestImage=bmp;
                sendScreenShot(bmp);
            } else {
                image.close();
            }
        }, main.getHandler());
    }

    private static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    private static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    /**
     * Change the virtual display between portrait and landscape
     * Note: did not like .resize() (rotation back to portrait resulted in a square)
     * @param width A integer representing the width of the new virtual display.
     * @param height A integer representing the height of the new virtual display.
     */
    public void changePeerOrientation(int width, int height) {
        mVirtualDisplay.release();
        mVirtualDisplay = mProjection.createVirtualDisplay("screen-mirror", width, height, densityDPI, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mImageReader.getSurface(), null, null);
    }

    Future<?> lastTask= null;
    private void sendScreenShot(Bitmap image){
        if(clientToServerSocket==null){
            Log.d(TAG, "sendScreenShot: socket not initilised yet");
            return;
        }
        if (clientToServerSocket.isConnected()){
            if (lastTask!=null) {
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
        } else{
            Log.d(TAG, "sendScreenShot: server not connected");
            //connectToServer();
        }
    }
}
