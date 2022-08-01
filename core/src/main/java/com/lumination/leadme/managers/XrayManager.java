package com.lumination.leadme.managers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import eu.bolt.screenshotty.ScreenshotManager;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.controller.Controller;

public class XrayManager {
    private final String TAG = "XrayManager";

    private final LeadMeMain main;
    public ScreenshotManager screenshotManager;

    public Bitmap response;
    public Boolean monitorInProgress = false;
    private ServerSocket serverSocket = null;

    private final View xrayScreen, loadingPanel;
    private View nextXrayStudent, prevXrayStudent;
    private TextView xrayStudentSelectedView, xrayStudentDisplayNameView;
    private final ImageView xrayScreenshotView;
    private ImageView xrayStudentIcon;
    private Button xrayButton;

    private HashMap<String, Bitmap> clientRecentScreenshots = new HashMap<>();
    private Thread imageSocketThread;
    private String monitoredPeer;

    public XrayManager(LeadMeMain main, View xrayScreen) {
        this.main = main;
        this.xrayScreen = xrayScreen;
        xrayScreenshotView = xrayScreen.findViewById(R.id.monitor_popup_img);
        xrayScreenshotView.setImageResource(R.color.transparent);
        //spinner to let the teachers know it is loading
        loadingPanel = xrayScreen.findViewById(R.id.xrayLoadingPanel);
    }

    private void setupXrayView() {
        xrayStudentIcon = xrayScreen.findViewById(R.id.student_icon);
        xrayStudentDisplayNameView = xrayScreen.findViewById(R.id.student_display_name);
        xrayStudentSelectedView = xrayScreen.findViewById(R.id.student_is_selected);
        nextXrayStudent = xrayScreen.findViewById(R.id.next_student_btn);
        prevXrayStudent = xrayScreen.findViewById(R.id.previous_student_btn);
        xrayScreenshotView.setImageResource(R.drawable.core_xray);
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
                    selectToggleBtn.setText(R.string.unselect);

                } else {
                    selectToggleBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_select_peer, 0, 0, 0);
                    selectToggleBtn.setText(R.string.select);
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
            Controller.getInstance().getConnectedLearnersAdapter().showLogoutPrompt(thisPeer.getID());
            xrayButton.callOnClick();
        });

        selectToggleBtn.setOnClickListener(view -> {
            ConnectedPeer thisPeer = getCurrentlyDisplayedStudent();
            String displayedText = selectToggleBtn.getText().toString();
            boolean currentlySelected = thisPeer.isSelected();
            Log.e(TAG, "ITEM SELECTED! " + thisPeer.getDisplayName() + ", " + thisPeer.getID() + ", " + currentlySelected + ", " + displayedText);

            if (displayedText.equals("Select") && !currentlySelected) {
                Log.e(TAG, "Setting SELECTED!");
                Controller.getInstance().getConnectedLearnersAdapter().selectPeer(thisPeer.getID(), true);
                updateXrayForSelection(thisPeer);

                selectToggleBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_unselect_peer, 0, 0, 0);
                selectToggleBtn.setText(R.string.unselect);
                xrayButton.callOnClick();

            } else if (displayedText.equals("Unselect") && currentlySelected) {
                Log.e(TAG, "Setting UNSELECTED!");
                Controller.getInstance().getConnectedLearnersAdapter().selectPeer(thisPeer.getID(), false);
                updateXrayForSelection(thisPeer);

                selectToggleBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_select_peer, 0, 0, 0);
                selectToggleBtn.setText(R.string.select);
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
        xrayScreen.findViewById(R.id.unlock_selected_btn).setOnClickListener(v -> main.unlockFromMainAction());

        xrayScreen.findViewById(R.id.lock_selected_btn).setOnClickListener(v -> main.lockFromMainAction());

        xrayScreen.findViewById(R.id.block_selected_btn).setOnClickListener(v -> main.blackoutFromMainAction());

        ImageView closeButton = xrayScreen.findViewById(R.id.back_btn);
        closeButton.setOnClickListener(v -> {
            hideXrayView();
            try {
                Log.e(TAG, "CLOSING SERVER SOCKET");
                serverSocket.close();
                serverSocket = null;
                monitorInProgress = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.XRAY_OFF, NearbyPeersManager.getAllPeerIDs());
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.XRAY_OFF, NearbyPeersManager.getSelectedPeerIDs());
            //remove last
            if (xrayDropdown.getVisibility() == VISIBLE) {
                xrayButton.callOnClick(); //hide it
            }
        });
    }

    private void hideXrayView() {
        main.exitCurrentView();
        selectedXrayStudents.clear(); //reset
    }

    public ConnectedPeer getCurrentlyDisplayedStudent() {
        Log.w(TAG, "Getting matching peer: " + currentXrayStudentIndex + ", " + selectedXrayStudents.get(currentXrayStudentIndex));
        return ConnectedLearnersAdapter.getMatchingPeer(selectedXrayStudents.get(currentXrayStudentIndex));
    }

    private ArrayList<String> selectedXrayStudents = new ArrayList<>();
    private int currentXrayStudentIndex = -1;
    private boolean xrayInit = false;

    public void showXrayView(String peer) {
        Log.w(TAG, "Showing xray view for " + peer + "!");

        if (!xrayInit) {
            setupXrayView();
            xrayInit = true;
        }

        //populate list with selected students (or all if none selected)
        selectedXrayStudents = new ArrayList<>();
        selectedXrayStudents.addAll(NearbyPeersManager.getSelectedPeerIDsOrAll());

        if (selectedXrayStudents.size() > 0) {
            setXrayStudent(peer);
            if (xrayScreen.getVisibility() != VISIBLE) {
                main.displayXrayView();
            }
        } else {
            Toast.makeText(main.getApplicationContext(), "No students available.", Toast.LENGTH_SHORT).show();
            if (xrayScreen.getVisibility() == VISIBLE) {
                main.exitCurrentView();
            }
        }
    }

    private void setXrayStudent(String peer) {
        //update variables to align with currently monitored peer
        currentXrayStudentIndex = selectedXrayStudents.indexOf(peer);
        monitoredPeer = peer;

        //show loading symbol to wait for the first screen shot
        isAwaitingImage(true);

        Log.w(TAG, "Finding details for: " + peer + ", " + currentXrayStudentIndex);

        //If the peer has not connected properly will throw an exception in the next if statement
        if(selectedXrayStudents.size() == 0) {
            Toast.makeText(main.getApplicationContext(), "No students available.", Toast.LENGTH_SHORT).show();
            return;
        }

        //no matching peer was found, use defaults
        if (peer.trim().isEmpty() || currentXrayStudentIndex < 0) {
            currentXrayStudentIndex = 0;
            monitoredPeer = selectedXrayStudents.get(currentXrayStudentIndex);
        }

        ConnectedPeer xrayStudent = ConnectedLearnersAdapter.getMatchingPeer(monitoredPeer);

        Log.w(TAG, "Setting arrows! " + monitoredPeer + ", " + currentXrayStudentIndex + ", " + selectedXrayStudents.size() + ", " + xrayStudent);

        if (xrayStudent == null) {
            //this student must have disconnected, refresh the UI
            hideXrayView();
            //Check if a learner is connected AND if the xray list is filling properly
            if (ConnectedLearnersAdapter.mData.size() > 0) {
                Log.w(TAG, "Got connected learners! Showing xray again! " + ConnectedLearnersAdapter.mData.size());
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
                icon = main.leadMeIcon;
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
        Set<String> notSelected = NearbyPeersManager.getAllPeerIDs();
        notSelected.remove(xrayStudent.getID());
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.XRAY_OFF, notSelected);

        //let student know we're watching so they send screenshots
        Set<String> selected = new HashSet<>();
        selected.add(xrayStudent.getID());

        Log.e(TAG, "Peers: SEL:" + selected + ", NOT:" + notSelected);

        if (xrayStudent.isSelected()) {
            xrayStudentSelectedView.setText(R.string.selected);
            xrayStudentSelectedView.setTextColor(main.getResources().getColor(R.color.light, null));
        } else {
            xrayStudentSelectedView.setText(R.string.unselected);
            xrayStudentSelectedView.setTextColor(main.getResources().getColor(R.color.leadme_dark_grey, null));
        }
    }

    private void imageRunnableFunction(String imgPeer) {
        Log.e(TAG, "Starting imageRunnable: " + serverSocket.isClosed() + ", " + serverSocket.isBound());
        Socket socket = null;
        try {
            socket = serverSocket.accept();
            Log.w(TAG, "Accepting SERVER socket from " + serverSocket.getInetAddress() + ", I'm " + socket.getLocalAddress() + " / " + imgPeer);
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server socket");
            if (socket != null) {
                try {
                    Log.e(TAG, "Closing server socket after failure");
                    socket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            e.printStackTrace();
            monitorInProgress = false;
            return;
        }

        while (monitorInProgress && !socket.isClosed()) { //global state, maybe change to individuals?
            if (monitoredPeer == null) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            byte[] buffer = null;
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                if (in.available() > 0) {
                    int length = in.readInt();
                    Log.d(TAG, "run: image received of size " + length + " from " + imgPeer);
                    /*
                    * Lowered the length range to 5,000, when in VR the blank screen at the start is less
                    * than 10,000. The build up from this causes an overflow error and stops the projections
                    * coming through.
                    */
                    if (length > 5000 && length < 300000) {
                        buffer = new byte[length];
                        in.readFully(buffer, 0, length);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (buffer != null) {
                Bitmap tmpBmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                response = tmpBmp;

                LeadMeMain.runOnUI(() -> {
                    //remove the loading spinner
                    isAwaitingImage(false);
                    //check if we are receiving images from the correct peer
                    Log.d(TAG, "Adding screenshot! " + monitoredPeer + " == " + imgPeer);

                    Log.d(TAG, "Bitmap height: " + response.getHeight() + " | width: " + response.getWidth());

                    clientRecentScreenshots.put(imgPeer, tmpBmp); //store it
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

    //client socket for monitoring
    public void startImageClient(String peer) {
        Log.d(TAG, "Starting image client for " + peer);
        if (serverSocket != null) {
            try {
                Log.e(TAG, "Closing SERVER socket");
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
        }
        while (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(54322);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "ServerSocket: attempting to create socket");
        }

        if (imageSocketThread != null) {
            imageSocketThread.interrupt();
        }

        monitorInProgress = true;
        imageSocketThread = new Thread(() -> imageRunnableFunction(peer));
        imageSocketThread.start();
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.XRAY_ON, NearbyPeersManager.getSelectedPeerIDs());
        Log.d(TAG, "Socket thread created");
    }

    public void removePeerFromMap(String peer) {
        Log.d(TAG, "Peer removed: " + peer);
        clientRecentScreenshots.remove(peer);
    }

    public void resetClientMaps(String peer) {
        //Removing a single peer from the HashMaps
        if(peer != null) {
            removePeerFromMap(peer);
        } else {
            Log.d(TAG, "Resetting hash maps");
            clientRecentScreenshots = new HashMap<>();

            //Close the serverSocket
            if(serverSocket != null) {
                try {
                    serverSocket.close();
                    serverSocket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void isAwaitingImage(boolean waiting) {
        if(waiting) {
            loadingPanel.setVisibility(VISIBLE);
            xrayScreenshotView.setVisibility(GONE);
        } else {
            loadingPanel.setVisibility(GONE);
            xrayScreenshotView.setVisibility(VISIBLE);
        }
    }
}
