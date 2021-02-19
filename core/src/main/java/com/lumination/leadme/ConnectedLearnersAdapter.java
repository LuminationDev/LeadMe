package com.lumination.leadme;

import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ConnectedLearnersAdapter extends BaseAdapter {

    private final String TAG = "ConnectedStudentsAdapter";

    //default is that students with a warning/error/install status will be displayed first
    private final boolean reorderByStatus = true;

    public ArrayList<ConnectedPeer> mData = new ArrayList<>();
    private LayoutInflater mInflater;
    private LeadMeMain main;
    private View studentDisconnectedView;
    private View studentLogoutView;
    private StudentAlertsAdapter alertsAdapter;

    ConnectedLearnersAdapter(LeadMeMain main, List<ConnectedPeer> data, StudentAlertsAdapter alertsAdapter) {
        this.main = main;
        this.mInflater = LayoutInflater.from(main);
        this.alertsAdapter = alertsAdapter;
        mData.addAll(data);
    }

    public void refreshAlertsView() {
        ArrayList<ConnectedPeer> peersWithWarnings = new ArrayList<>();
        for (ConnectedPeer peer : mData) {
            if (!peer.getAlertsList().isEmpty()) {
                peersWithWarnings.add(peer);
            }
        }
        alertsAdapter.setData(peersWithWarnings);
        alertsAdapter.notifyDataSetChanged();
    }

    public void refresh() {
        refreshAlertsView();
        notifyDataSetChanged();
        super.notifyDataSetChanged();
    }

    public void alertStudentDisconnect(String id) {
        ConnectedPeer peer = getMatchingPeer(id);
        if (peer != null) {
            peer.setStatus(ConnectedPeer.STATUS_ERROR);
            moveToFrontOfList(peer);
            refresh();
        }
    }

    public void removeAllStudents() {
        mData = new ArrayList<>();
        refresh();
    }


    public boolean removeStudent(String id) {
        ConnectedPeer found = getMatchingPeer(id);
        Log.d(TAG, "Removing student! " + found);
        if (found != null) {
            //remove old one so we keep the newest version
            mData.remove(found);
            refresh();
            return true;
        }
        refresh();
        return false;
    }

    public void addStudent(ConnectedPeer peer) {
        //does someone with this name already exist?
        ConnectedPeer found = getMatchingPeerByUUID(peer.getUUID());

        if (found != null) {
            Log.w(TAG, "Already have a record for this person! Replacing it with a new one.");
            //remove old one so we keep the newest version
            mData.remove(found);
        }

        //add the newest version
        mData.add(peer);
        refresh();

        Log.d(TAG, "Adding " + peer.getDisplayName() + " to my student list. Now: " + mData.size() + " || " + mData);
    }

    public boolean hasConnectedStudents() {
        return mData.size() > 0;
    }

    public ConnectedPeer getMatchingPeer(String peerID) {
        if (mData == null || mData.size() == 0) {
            return null;
        }

        //search against Nearby Connections ID
        for (ConnectedPeer peer : mData) {
            if (peer != null && peer.getID().equals(peerID)) {
                return peer;
            }
        }

        return null;
    }


    private ConnectedPeer getMatchingPeerByUUID(String peerID) {
        if (mData == null || mData.size() == 0) {
            return null;
        }

        //search against UUID
        for (ConnectedPeer peer : mData) {
            if (peer != null && peer.getUUID().equals(peerID)) {
                return peer;
            }
        }
        return null;
    }


    public void updateIcon(String name, Drawable icon) {
        //Log.d(TAG, "Updating icon for " + name);
        ConnectedPeer thisPeer = getMatchingPeer(name);
        if (thisPeer != null) {
            thisPeer.setIcon(icon);
            refresh();
        }
    }

    public void appLaunchFail(String id, String lastApp) {
        ConnectedPeer thisPeer = getMatchingPeer(id);
        thisPeer.setLastLaunchedApp(lastApp);
        thisPeer.setWarning(LeadMeMain.APP_TAG, false);
    }

    public void appLaunchSuccess(String id, String lastApp) {
        ConnectedPeer thisPeer = getMatchingPeer(id);
        thisPeer.setLastLaunchedApp(lastApp);
        thisPeer.setWarning(LeadMeMain.APP_TAG, true);
    }

    //helper to manage updating a warning status
    private String warningMessage = "";

    public void updateStatus(String id, int status, String msg) {
        ConnectedPeer thisPeer = getMatchingPeer(id);

        Log.w(TAG, "Received: " + id + ", " + thisPeer + ", " + status + ", " + msg);

        if (thisPeer == null) {
            Log.d(TAG, "Received warning for UNKNOWN student " + id + ": " + msg);
            return;
        }

        if (status == ConnectedPeer.STATUS_WARNING) {
            switch (msg) {
                case LeadMeMain.AUTO_INSTALL_FAILED:
                case LeadMeMain.AUTO_INSTALL:
                    warningMessage = "could not install requested app.";
                    break;
                case LeadMeMain.STUDENT_NO_OVERLAY:
                    warningMessage = "could not display blocking overlay";
                    break;
                case LeadMeMain.STUDENT_NO_INTERNET:
                    warningMessage = "is not connected to the Internet";
                    break;
                case LeadMeMain.STUDENT_NO_ACCESSIBILITY:
                    warningMessage = "is not restricted to LeadMe";
                    break;
                case LeadMeMain.STUDENT_OFF_TASK_ALERT:
                    warningMessage = "could be off task";
                    break;
            }
            thisPeer.setWarning(msg, false);

        } else if (status == ConnectedPeer.STATUS_SUCCESS) {
            thisPeer.setWarning(msg, true);
            warningMessage = msg;
        }

        notifyDataSetChanged();
    }

    //standard status update
    public void updateStatus(String name, int status) {
        ConnectedPeer thisPeer = getMatchingPeer(name);
        if (thisPeer == null) {
            Log.e(TAG, "Couldn't find matching peer! " + name + ", " + status + ", " + mData);
            return;
        }

        Log.d(TAG, "Updating status for " + name + " to " + status + " (" + thisPeer + ")"); // with " + warningMessage);
        thisPeer.setStatus(status);
        if (status == ConnectedPeer.STATUS_OFF_TASK || status == ConnectedPeer.STATUS_WARNING || status == ConnectedPeer.STATUS_ERROR || status == ConnectedPeer.STATUS_INSTALLING) {
            moveToFrontOfList(thisPeer);
        }
        refresh();

        //certain states should alert the guide - e.g. failure to load or attempt to install
        if (status == ConnectedPeer.STATUS_OFF_TASK || status == ConnectedPeer.STATUS_INSTALLING || status == ConnectedPeer.STATUS_ERROR || status == ConnectedPeer.STATUS_WARNING) {
            String msg = thisPeer.getDisplayName();
            switch (status) {
                case ConnectedPeer.STATUS_INSTALLING:
                    msg += " is installing requested app.";
                    break;
                case ConnectedPeer.STATUS_ERROR:
                    msg += " is disconnected.";
                    break;
                case ConnectedPeer.STATUS_OFF_TASK:
                    msg += " may be off task.";
                    break;
                default: //case ConnectedPeer.STATUS_WARNING:
                    msg += " " + warningMessage;
                    break;
            }

            Toast warningToast = Toast.makeText(main.getApplicationContext(), "WARNING: " + msg, Toast.LENGTH_LONG);
            warningToast.show();
        }

    }

    public void selectAllPeers(boolean select) {
        for (ConnectedPeer peer : mData) {
            peer.setSelected(select);
        }
        refresh();
    }

    @Override
    public int getCount() {
        if (mData != null) {
            return mData.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    AlertDialog disconnectPrompt;
    AlertDialog logoutPrompt;
    private String lastClickedID = "";

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //if (convertView == null) {
        convertView = mInflater.inflate(R.layout.row_follower, parent, false);
        //}

        TextView studentName = convertView.findViewById(R.id.student_name);
        ImageView selectedIndicator = convertView.findViewById(R.id.selected_indicator);
        ImageView studentIcon = convertView.findViewById(R.id.student_icon);
        ImageView warningIcon = convertView.findViewById(R.id.status_icon);

        final ConnectedPeer peer = mData.get(position);
        if (peer != null) {
            studentName.setText(peer.getDisplayName());

            if (peer.isSelected()) {
                selectedIndicator.setVisibility(View.VISIBLE);
            } else {
                selectedIndicator.setVisibility(View.INVISIBLE);
            }

            Drawable icon = peer.getIcon();
            if (icon == null) {
                icon = main.leadmeIcon;
            }

            //draw the student icon and status icon
            studentIcon.setImageDrawable(icon);
            drawAlertIcon(peer, warningIcon);

            convertView.setLongClickable(true);
            convertView.setOnLongClickListener(v -> {
                lastClickedID = peer.getID();
                if (studentLogoutView == null) {
                    studentLogoutView = View.inflate(main, R.layout.e__logout_student_popup, null);
                    Button ok_btn = studentLogoutView.findViewById(R.id.ok_btn);
                    Button monitor_btn = studentLogoutView.findViewById(R.id.monitor_btn);
                    Button back_btn = studentLogoutView.findViewById(R.id.back_btn);

                    ok_btn.setOnClickListener(v12 -> {
                        Log.d(TAG, "Disconnecting from student: " + lastClickedID);
                        main.getNearbyManager().disconnectStudent(lastClickedID);
                        main.getConnectedLearnersAdapter().removeStudent(lastClickedID);
                        main.getConnectedLearnersAdapter().refresh();
                        logoutPrompt.hide();
                    });
                    //added
                    monitor_btn.setOnClickListener(v2 -> {
                        logoutPrompt.hide();

                        main.setupMonitorScreen(lastClickedID);
                    });
                    back_btn.setOnClickListener(v1 -> logoutPrompt.hide());
                }

                if (logoutPrompt == null) {
                    logoutPrompt = new AlertDialog.Builder(main)
                            .setView(studentLogoutView)
                            .show();
                } else {
                    logoutPrompt.show();
                }
                return true;
            });

            convertView.setOnClickListener(v -> {
                Log.d(TAG, "Clicked on " + peer.getID() + ", " + peer.getMyEndpoint() + ", " + peer.getDisplayName());
                lastClickedID = peer.getID();
                if (peer.getStatus() == ConnectedPeer.STATUS_ERROR) {
                    //
                    if (studentDisconnectedView == null) {
                        studentDisconnectedView = View.inflate(main, R.layout.e__disconnected_student_popup, null);
                        Button ok_btn = studentDisconnectedView.findViewById(R.id.ok_btn);
                        Button back_btn = studentDisconnectedView.findViewById(R.id.back_btn);

                        ok_btn.setOnClickListener(v12 -> {
                            Log.d(TAG, "Removing student: " + lastClickedID);
                            main.getConnectedLearnersAdapter().removeStudent(lastClickedID);
                            main.getConnectedLearnersAdapter().refresh();
                            disconnectPrompt.hide();
                        });

                        back_btn.setOnClickListener(v1 -> disconnectPrompt.hide());
                    }

                    if (disconnectPrompt == null) {
                        disconnectPrompt = new AlertDialog.Builder(main)
                                .setView(studentDisconnectedView)
                                .show();
                    } else {
                        disconnectPrompt.show();
                    }

                } else {
                    //select the tapped peer
                    selectPeer(peer.getID(), !peer.isSelected());
                }
            });
        }
        return convertView;
    }

    protected void moveToFrontOfList(ConnectedPeer peer) {
        //only reorder if we haven't already
        if (reorderByStatus && peer.getPriority() != ConnectedPeer.PRIORITY_TOP) {
            peer.setPriority(ConnectedPeer.PRIORITY_TOP);
            mData.remove(peer);
            mData.add(0, peer);
        }
    }

    public void drawAlertIcon(ConnectedPeer peer, ImageView statusIcon) {
        if (statusIcon == null) {
            return;
        }

        switch (peer.getStatus()) {
            case ConnectedPeer.STATUS_ERROR:
                statusIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_error, null));
                break;

            case ConnectedPeer.STATUS_INSTALLING:
                statusIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_downloading, null));
                break;

            default:
                if (peer.hasWarning()) {
                    Log.d(TAG, peer.getDisplayName() + " has warning: " + peer.getAlertsList());
                    statusIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_warning, null));
                } else {
                    setLockStatus(peer, statusIcon);
                }
                break;
        }
    }

    private void setLockStatus(ConnectedPeer peer, ImageView statusIcon) {
        //Log.d(TAG, "Actually, is there a warning? "+peer.hasWarning()+", "+peer.isBlackedOut()+", "+peer.isLocked());
        //sometimes multiple status are possible.
        if (!peer.hasWarning() && peer.isBlackedOut()) {
            statusIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_blocked, null));

        } else if (!peer.hasWarning() && peer.isLocked()) {
            statusIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_locked, null));

        } else if (!peer.hasWarning()) {
            statusIcon.setImageDrawable(null);
        }
    }

    public void selectPeer(String id, boolean selected) {
        ConnectedPeer peer = getMatchingPeer(id);
        peer.setSelected(selected);
        refresh();
    }

    public boolean someoneIsSelected() {
        for (ConnectedPeer peer : mData) {
            if (peer.isSelected()) {
                return true;
            }
        }
        return false;
    }

}