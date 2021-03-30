package com.lumination.leadme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import eu.bolt.screenshotty.ScreenshotManager;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class XrayManager {

    //added-------
    ScreenshotManager screenshotManager;

    String ipAddress;
    Intent screen_share_intent = null;
    private MediaProjectionManager projectionManager = null;
    private int displayWidth, displayHeight, imagesProduced = 0;
    ImageReader imageReader;

    public Bitmap response;
    public Bitmap bitmapToSend = null;
    Boolean monitorInProgress = false;
    ServerSocket serverSocket = null;
    boolean screenShot = false;
    int screenshotRate = 200;

    private TextView xrayStudentSelectedView, xrayStudentDisplayNameView;
    private ImageView xrayStudentIcon, xrayScreenshotView;
    private View nextXrayStudent, prevXrayStudent;
    private Button xrayButton;
    private View xrayScreen;
    private LeadMeMain main;

    private final String TAG = "XrayManager";

    public XrayManager(LeadMeMain main, View xrayScreen) {
        this.main = main;
        this.xrayScreen = xrayScreen;
    }

    public void generateScreenshots(boolean isWatching) {
        screenshotPaused = !isWatching;
    }

    boolean screenCapPermission = false;

    @SuppressLint("WrongConstant")
    public void manageResultsReturn(int requestCode, int resultCode, Intent data) {
        main.getPermissionsManager().waitingForPermission = false;
        Log.d(TAG, "RETURNED RESULT FROM SCREEN_CAPTURE! " + resultCode + ", " + data);
        MediaProjection mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection != null) {
            screenCapPermission = true;
            DisplayMetrics metrics = main.getResources().getDisplayMetrics();
            int density = metrics.densityDpi;
            int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                    | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

            Point size = new Point();
            size.y = metrics.heightPixels;
            size.x = metrics.widthPixels;
            displayHeight = size.y;
            displayWidth = size.x;

            imageReader = ImageReader.newInstance(size.x, size.y, PixelFormat.RGBA_8888, 2);

            mediaProjection.createVirtualDisplay("screencap",
                    size.x, size.y, density,
                    flags, imageReader.getSurface(), null, main.getHandler());
            imageReader.setOnImageAvailableListener(new ImageAvailableListener(), main.getHandler());
        } else {
            screenCapPermission = false;
        }
    }

    private void setupXrayView() {
        xrayStudentIcon = xrayScreen.findViewById(R.id.student_icon);
        xrayStudentDisplayNameView = xrayScreen.findViewById(R.id.student_display_name);
        xrayStudentSelectedView = xrayScreen.findViewById(R.id.student_is_selected);
        nextXrayStudent = xrayScreen.findViewById(R.id.next_student_btn);
        prevXrayStudent = xrayScreen.findViewById(R.id.previous_student_btn);
        xrayScreenshotView = xrayScreen.findViewById(R.id.monitor_popup_img);
        xrayButton = xrayScreen.findViewById(R.id.current_peer_btn);
        View xrayDropdown = xrayScreen.findViewById(R.id.dropdown_menu);
        xrayDropdown.setVisibility(GONE);

        TextView selectToggleBtn = xrayDropdown.findViewById(R.id.select_text);
        xrayButton.setOnClickListener(view -> {

            if (xrayDropdown.getVisibility() == View.VISIBLE) {
                xrayDropdown.setVisibility(GONE);

            } else {
                ConnectedPeer thisPeer = getCurrentlyDisplayedStudent();
                boolean currentlySelected = thisPeer.isSelected();
                //make sure UI is appropriate for displayed peer
                if (currentlySelected) {
                    selectToggleBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_unselect_peer, 0, 0, 0);
                    selectToggleBtn.setText("Unselect");

                } else {
                    selectToggleBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_select_peer, 0, 0, 0);
                    selectToggleBtn.setText("Select");
                }

                xrayDropdown.setVisibility(View.VISIBLE);
            }

            int[] btnLoc = new int[2];
            int[] dropLoc = new int[2];

            xrayDropdown.post(() -> {
                // Values should no longer be 0
                xrayDropdown.getLocationOnScreen(dropLoc);
                xrayButton.getLocationOnScreen(btnLoc);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) xrayDropdown.getLayoutParams();
                params.setMargins(btnLoc[0] + xrayButton.getWidth() - xrayDropdown.getWidth(), (int) (btnLoc[1] - (xrayButton.getHeight() / 2.0)), 0, 0);
                xrayDropdown.setLayoutParams(params);
                xrayDropdown.requestLayout();
            });
        });

        //disconnect student
        xrayDropdown.findViewById(R.id.disconnect_text).setOnClickListener(view -> {
            ConnectedPeer thisPeer = getCurrentlyDisplayedStudent();
            Log.w(TAG, "Removing student: " + thisPeer.getID() + ", " + thisPeer.getDisplayName());
            main.getConnectedLearnersAdapter().showLogoutPrompt(thisPeer.getID());
            xrayButton.callOnClick();
        });

        selectToggleBtn.setOnClickListener(view -> {
            ConnectedPeer thisPeer = getCurrentlyDisplayedStudent();
            String displayedText = selectToggleBtn.getText().toString();
            boolean currentlySelected = thisPeer.isSelected();
            Log.e(TAG, "ITEM SELECTED! " + thisPeer.getDisplayName() + ", " + thisPeer.getID() + ", " + currentlySelected + ", " + displayedText);

            if (displayedText.equals("Select") && !currentlySelected) {
                Log.e(TAG, "Setting SELECTED!");
                main.getConnectedLearnersAdapter().selectPeer(thisPeer.getID(), true);
                updateXrayForSelection(thisPeer);

                selectToggleBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_unselect_peer, 0, 0, 0);
                selectToggleBtn.setText("Unselect");
                xrayButton.callOnClick();

            } else if (displayedText.equals("Unselect") && currentlySelected) {
                Log.e(TAG, "Setting UNSELECTED!");
                main.getConnectedLearnersAdapter().selectPeer(thisPeer.getID(), false);
                updateXrayForSelection(thisPeer);

                selectToggleBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_select_peer, 0, 0, 0);
                selectToggleBtn.setText("Select");
                xrayButton.callOnClick();
            }
        });


        //set up next and prev buttons
        nextXrayStudent.setOnClickListener(view -> {
            currentXrayStudentIndex++;
            if (currentXrayStudentIndex < selectedXrayStudents.size()) {
                setXrayStudent(selectedXrayStudents.get(currentXrayStudentIndex));
                if (xrayDropdown.getVisibility() == VISIBLE) {
                    xrayButton.callOnClick(); //hide it
                }
            }
        });

        prevXrayStudent.setOnClickListener(view -> {
            currentXrayStudentIndex--;
            if (currentXrayStudentIndex >= 0) {
                setXrayStudent(selectedXrayStudents.get(currentXrayStudentIndex));
                if (xrayDropdown.getVisibility() == VISIBLE) {
                    xrayButton.callOnClick(); //hide it
                }
            }
        });

        //set up main buttons
        xrayScreen.findViewById(R.id.unlock_selected_btn).setOnClickListener(v -> {
            main.unlockFromMainAction();
        });

        xrayScreen.findViewById(R.id.lock_selected_btn).setOnClickListener(v -> {
            main.lockFromMainAction();
        });

        xrayScreen.findViewById(R.id.block_selected_btn).setOnClickListener(v -> {
            main.blackoutFromMainAction();
        });

        ImageView closeButton = xrayScreen.findViewById(R.id.back_btn);
        closeButton.setOnClickListener(v -> {
            hideXrayView();
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.XRAY_OFF, main.getNearbyManager().getAllPeerIDs());

            if (xrayDropdown.getVisibility() == VISIBLE) {
                xrayButton.callOnClick(); //hide it
            }
        });
    }

    private void hideXrayView() {
        main.exitXrayView();
        selectedXrayStudents.clear(); //reset
    }

    public ConnectedPeer getCurrentlyDisplayedStudent() {
        Log.w(TAG, "Getting matching peer: " + currentXrayStudentIndex + ", " + selectedXrayStudents.get(currentXrayStudentIndex));
        return main.getConnectedLearnersAdapter().getMatchingPeer(selectedXrayStudents.get(currentXrayStudentIndex));
    }

    private ArrayList<String> selectedXrayStudents = new ArrayList<>();
    private int currentXrayStudentIndex = -1;
    private boolean xrayInit = false;

    void showXrayView(String peer) {
        Log.w(TAG, "Showing xray view!");
        if (!xrayInit) {
            setupXrayView();
            xrayInit = true;
        }

        //populate list with selected students (or all if none selected)
        selectedXrayStudents.clear();
        selectedXrayStudents.addAll(main.getNearbyManager().getSelectedPeerIDsOrAll());

        if (selectedXrayStudents.size() > 0) {
            setXrayStudent(peer);
            if (xrayScreen.getVisibility() != VISIBLE) {
                main.displayXrayView();
            }
        } else {
            Toast.makeText(main.getApplicationContext(), "No students available.", Toast.LENGTH_SHORT).show();
            if (xrayScreen.getVisibility() == VISIBLE) {
                main.exitXrayView();
            }
        }
    }

    private void setXrayStudent(String peer) {
        //update variables to align with currently monitored peer
        currentXrayStudentIndex = selectedXrayStudents.indexOf(peer);
        monitoredPeer = peer;

        Log.w(TAG, "Finding details for: " + peer + ", " + currentXrayStudentIndex);

        //no matching peer was found, use defaults
        if (peer.trim().isEmpty() || currentXrayStudentIndex < 0) {
            currentXrayStudentIndex = 0;
            monitoredPeer = selectedXrayStudents.get(currentXrayStudentIndex);
        }

        ConnectedPeer xrayStudent = main.getConnectedLearnersAdapter().getMatchingPeer(monitoredPeer);

        Log.w(TAG, "Setting arrows! " + monitoredPeer + ", " + currentXrayStudentIndex + ", " + selectedXrayStudents.size() + ", " + xrayStudent);

        if (xrayStudent == null) {
            //this student must have disconnected, refresh the UI
            hideXrayView();
            if (main.getConnectedLearnersAdapter().mData.size() > 0) {
                Log.w(TAG, "Got connected learners! Showing xray again! " + main.getConnectedLearnersAdapter().mData.size());
                showXrayView("");
            }
            return;
        }

        //show/hide next and prev student buttons as needed
        if (selectedXrayStudents.size() > (currentXrayStudentIndex + 1)) {
            nextXrayStudent.setVisibility(View.VISIBLE);
            nextXrayStudent.setClickable(true);
        } else {
            nextXrayStudent.setVisibility(View.INVISIBLE);
            nextXrayStudent.setClickable(false);
        }

        if ((currentXrayStudentIndex - 1) >= 0) {
            prevXrayStudent.setVisibility(View.VISIBLE);
            prevXrayStudent.setClickable(true);
        } else {
            prevXrayStudent.setVisibility(View.INVISIBLE);
            prevXrayStudent.setClickable(false);
        }

        startImageClient(monitoredPeer);

        if (xrayStudent != null && xrayStudentDisplayNameView != null) {
            Log.w(TAG, "Updating display: " + xrayStudentDisplayNameView + ", " + xrayStudent.getDisplayName() + ", " + xrayStudent.getID() + ", " + monitoredPeer);
            xrayStudentDisplayNameView.setText(xrayStudent.getDisplayName());

            Drawable icon = xrayStudent.getIcon();
            if (icon == null) {
                icon = main.leadmeIcon;
            }
            //update app icon
            xrayStudentIcon.setImageDrawable(icon);

            //display most recent screenshot
            Bitmap latestScreenie = clientRecentScreenshots.get(xrayStudent.getID());
            xrayScreenshotView.setImageBitmap(latestScreenie);

            updateXrayForSelection(xrayStudent);
        }
    }

    void updateXrayForSelection(ConnectedPeer xrayStudent) {
        Log.w(TAG, "Updating UI for " + xrayStudent.getDisplayName() + "!");

        //let everyone else know we're NOT watching so they reduce computational load
        Set<String> notSelected = main.getNearbyManager().getAllPeerIDs();
        notSelected.remove(xrayStudent.getID());
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.XRAY_OFF, notSelected);

        //let student know we're watching so they send screenshots
        Set<String> selected = new HashSet<>();
        selected.add(xrayStudent.getID());
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.XRAY_ON, selected);

        Log.e(TAG, "Peers: " + notSelected);

        if (xrayStudent.isSelected()) {
            //selectedAdapter.setItemList(itemsForSelected, imgsForSelected);
            xrayStudentSelectedView.setText("Selected");
            xrayStudentSelectedView.setTextColor(main.getResources().getColor(R.color.light, null));
        } else {
            //selectedAdapter.setItemList(itemsForUnselected, imgsForUnselected);
            xrayStudentSelectedView.setText("Unselected");
            xrayStudentSelectedView.setTextColor(main.getResources().getColor(R.color.leadme_dark_grey, null));
        }
        //xrayStudentSelectedView.invalidate();
    }

    public void startServer() {
        Log.w(TAG, "Starting server...");
        main.getPermissionsManager().waitingForPermission = true;
        if (!screenCapPermission) {
            projectionManager = (MediaProjectionManager) main.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

            //start service class
            screen_share_intent = new Intent(main.getApplicationContext(), ScreensharingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                main.startForegroundService(screen_share_intent);
            } else {
                main.startService(screen_share_intent);
            }

            //start screen capturing
            main.startActivityForResult(projectionManager.createScreenCaptureIntent(), main.SCREEN_CAPTURE);
        }
    }

    public void stopServer() {
        main.stopService(screen_share_intent);
        ipAddress = null;
        imagesProduced = 0;
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //Log.d(TAG, "onImageAvailable: image available");

            Bitmap bitmap = null;
            ByteArrayOutputStream stream = null;
            //Log.d(TAG, "onImageAvailable: ");
            try (Image image = imageReader.acquireLatestImage()) {

                if (main.getPermissionsManager().waitingForPermission) {
                    main.getPermissionsManager().waitingForPermission = false;
                    main.refreshOverlay();
                }

                //if (takeScreenshots) {
                //Log.d(TAG, "onImageAvailable: image acquired");
                //sleep allows control over how many screenshots are taken
                //old/less powerful phones need this otherwise there is heavy lag (etc for >20 screen shots a second)
                //newer phones can have this disabled for a faster display
                if (screenshotRate > 0) Thread.sleep(screenshotRate);

                if (image != null) {
                    //Log.d(TAG, "onImageAvailable: image exists");
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * displayWidth;

                    stream = new ByteArrayOutputStream();

                    // create bitmap
                    bitmap = Bitmap.createBitmap(displayWidth + rowPadding / pixelStride,
                            displayHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    bitmapToSend = bitmap;
                    imagesProduced++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {


                if (main.getPermissionsManager().waitingForPermission) {
                    main.getPermissionsManager().waitingForPermission = false;
                    main.refreshOverlay();
                }

                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }

        }
    }


    private void imageRunnableFunction(String imgPeer) {
        Log.e(TAG, "Starting imageRunnable: " + serverSocket.isClosed() + ", " + serverSocket.isBound());
        Socket socket;
        try {
            socket = serverSocket.accept();
            Log.w(TAG, "Accepting SERVER socket from " + serverSocket.getInetAddress() + ", I'm " + socket.getLocalAddress() + " / " + imgPeer);

        } catch (IOException e) {
            e.printStackTrace();
            monitorInProgress = false;
            return;
        }

        while (monitorInProgress && !socket.isClosed()) { //global state, maybe change to individuals?
            if (monitoredPeer == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            byte[] buffer = null;
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                while (in.available() > 0) {
                    int length = in.readInt();
                    Log.d(TAG, "run: image received of size " + length + " from " + imgPeer);
                    if (length > 10000 && length < 300000) {
                        buffer = new byte[length];
                        in.readFully(buffer, 0, length);
                        //Log.d(TAG, "Packet Received!! ");
                    }
                }
            } catch (IOException e) {
                //monitorInProgress = false;
                e.printStackTrace();
            }
            //Log.w(TAG, buffer + ", " + response);
            if (buffer != null) {
                Bitmap tmpBmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                response = tmpBmp;
                main.runOnUiThread(() -> {
                    Log.d(TAG, "Adding screenshot! " + monitoredPeer + " == " + imgPeer);
                    clientRecentScreenshots.put(imgPeer, tmpBmp); //store it

                    //I'm on display!
                    if (monitoredPeer.equals(imgPeer)) {
                        xrayScreenshotView.setImageBitmap(tmpBmp);
                        Log.w(TAG, "Updated the image!");
                    }
                });
            } else {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.e(TAG, "imageRunnable ended! " + monitorInProgress + " && " + !Thread.currentThread().isInterrupted());
    }

    //Thread imageSocket;
    private String monitoredPeer;
    HashMap<String, Thread> clientSocketThreads = new HashMap();
    HashMap<String, Bitmap> clientRecentScreenshots = new HashMap();

    //client socket for monitoring
    public void startImageClient(String peer) {
        Log.d(TAG, "Starting image client for " + peer);
        while (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "ServerSocket: attempting to create socket");
        }

        //get the image socket for this peer
        Thread imageSocketThread = clientSocketThreads.get(peer);

        if (imageSocketThread == null) {
            monitorInProgress = true;
            imageSocketThread = new Thread(() -> imageRunnableFunction(peer));
            imageSocketThread.start();
            clientSocketThreads.put(peer, imageSocketThread); //store this
            Log.w(TAG, "Now have client sockets: " + clientSocketThreads.size() + " : " + peer);

            main.getNearbyManager().networkAdapter.startMonitoring(Integer.parseInt(peer), serverSocket.getLocalPort());
        }

    }

    //will only have one of these at the client
    //OK to store this way
    Socket screenshotSocket = null;
    public boolean screenshotPaused = false;
    Thread screenShotRunner = null;

    public void startScreenshotRunnable(InetAddress ip, int Port) {
        screenShot = true;
        if (screenshotSocket != null && screenshotSocket.isConnected()) {
            Log.w(TAG, "Already have a screenshot runnable going!");
            try {
                screenshotSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //return; //already got one!
        }

        screenShotRunner = new Thread(() -> {
            try {
                Log.w(TAG, "creating CLIENT socket with " + ip + ", " + Port);
                screenshotSocket = new Socket(ip, Port);

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "startScreenShot: socket not connected");
                main.getPermissionsManager().waitingForPermission = false;
                main.refreshOverlay();
                return;
            }
            while (screenShot) {
                if (screenshotPaused) {
                    Log.w(TAG, "SCREENSHOT PAUSED!");
                    try {
                        screenShotRunner.sleep(3000); //how long until we should check again?
                        continue;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.w(TAG, "SCREENSHOT RESUMED!");
                if (bitmapToSend != null && screenshotSocket.isConnected()) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmapToSend.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                    if (stream.toByteArray().length > 300000) {
                        bitmapToSend.compress(Bitmap.CompressFormat.JPEG, 0, stream);
                    }
                    bitmapToSend = null;
                    byte[] byteArray = stream.toByteArray();
                    try {
                        DataOutputStream out = new DataOutputStream(screenshotSocket.getOutputStream());
                        out.writeInt(byteArray.length);
                        out.write(byteArray, 0, byteArray.length);
                        Log.d(TAG, "run: image sent of size " + byteArray.length + " to " + screenshotSocket.getInetAddress() + ", from " + screenshotSocket.getLocalAddress());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(screenshotRate);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d(TAG, "Thread: thread wouldn't sleep");
                    }
                }
            }
            Log.e(TAG, "Image send thread closed. " + screenShot);
            screenShot = false;
        });
        screenShotRunner.start();
    }

    public void stopScreenshotRunnable() {
        screenShot = false;
    }

    public void setScreenshotRate(int rate) {
        screenshotRate = rate;
    }
}
