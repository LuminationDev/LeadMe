package com.lumination.leadme;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.collection.ArraySet;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ConnectedLearnersAdapter extends BaseAdapter {

    private final String TAG = "ConnectedStudentsAdapter";

    //default is that students with a warning/error/install status will be displayed first
    private final boolean reorderByStatus = true;

    public ArrayList<ConnectedPeer> mData = new ArrayList<>();
    private LayoutInflater mInflater;
    private LeadMeMain main;
    private View studentDisconnectedView;
    private View studentLogoutView;
    public StudentAlertsAdapter alertsAdapter;

    ConnectedLearnersAdapter(LeadMeMain main, List<ConnectedPeer> data, StudentAlertsAdapter alertsAdapter) {
        this.main = main;
        this.mInflater = LayoutInflater.from(main);
        this.alertsAdapter = alertsAdapter;
        mData.addAll(data);
    }

    /**
     * Refreshes the ArrayList of alerts associated to a particular peer. Called when
     * updating the status and opening the alerts dialog.
     */
    public void refreshAlertsView() {
        ArrayList<ConnectedPeer> peersWithWarnings = new ArrayList<>();
        for (ConnectedPeer peer : mData) {
            if (!peer.getAlertsList().isEmpty()) {
                peersWithWarnings.add(peer);
            }
        }

        if (peersWithWarnings.isEmpty()) {
            main.setAlertsBtnVisibility(View.GONE);
        } else {
            main.setAlertsBtnVisibility(View.VISIBLE);
        }

        Log.d(TAG, "refreshAlertsView: size:"+peersWithWarnings.size());
        main.alertsBtn.setText("Alerts ("+peersWithWarnings.size()+")");
        alertsAdapter.setData(peersWithWarnings);
        alertsAdapter.notifyDataSetChanged();
    }

    public void resetOnLogout() {
        mData = new ArrayList<>();
        ((TextView)main.findViewById(R.id.learners_txt)).setText("All Learners");
    }

    public void refresh() {
        refreshAlertsView();
        notifyDataSetChanged();
        super.notifyDataSetChanged();
        ((TextView)main.findViewById(R.id.learners_txt)).setText("All Learners ("+getCount()+")");
        main.displaySelectBar(getSelectedCount());
    }

    public void alertStudentDisconnect(String id) {
        ConnectedPeer peer = getMatchingPeer(id);
        if (peer != null) {
            //main.xrayManager.monitorInProgress = false;
            peer.setStatus(ConnectedPeer.STATUS_ERROR);
            moveToFrontOfList(peer);
            refresh();
        }
    }

    public boolean removeStudent(String id) {
        ConnectedPeer found = getMatchingPeer(id);
        Log.d(TAG, "Removing from mData! " + found);
        if (found != null) {
            //remove old one so we keep the newest version
            mData.remove(found);
            refresh();
            Log.d(TAG, "Now have " + mData.size());
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

        Log.d(TAG, "Adding " + peer.getDisplayName() + " to my student list, ID: " + peer.getID() + ". Now: " + mData.size() + " || " + mData);

        //Blocks call if guide logs out on a device then logs back in as a student.
        if(main.isGuide) {
            updateOnConnect(peer.getID());
        }
    }

    /**
     * Update the new guide with any settings that have been turned on prior to the peer connecting.
     * @param ID A string representing the ID of the peer that has just connected.
     */
    private void updateOnConnect(String ID) {
        //update the student device upon login with the teachers auto install setting
        Set<String> newPeer = new HashSet<String>();
        newPeer.add(ID);

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        final Runnable runnable = () -> {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL + ":"
                    + main.autoInstallApps, newPeer);
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.FILE_TRANSFER + ":"
                    + main.fileTransferEnabled, newPeer);

            scheduler.shutdown();
        };
        scheduler.scheduleAtFixedRate(runnable, 5, 5, SECONDS);
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
        refresh();
    }

    public void appLaunchSuccess(String id, String lastApp) {
        ConnectedPeer thisPeer = getMatchingPeer(id);
        if (thisPeer != null) {
            thisPeer.setLastLaunchedApp(lastApp);
            thisPeer.setWarning(LeadMeMain.APP_TAG, true);
            refresh();
        }
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
                case LeadMeMain.STUDENT_NO_XRAY:
                    warningMessage = "xray popup was denied";
                    break;
                case LeadMeMain.PERMISSION_TRANSFER_DENIED:
                    warningMessage = "file transfer permission was denied";
                    break;
                case LeadMeMain.PERMISSION_AUTOINSTALL_DENIED:
                    warningMessage = "auto installer permission was denied";
                    break;
            }
            thisPeer.setWarning(msg, false);

        } else if (status == ConnectedPeer.STATUS_SUCCESS) {
            Log.d(TAG, "updateStatus: message: "+msg);
            thisPeer.setWarning(msg, true);
            //warningMessage = msg;
        }

        notifyDataSetChanged();
        main.getConnectedLearnersAdapter().refreshAlertsView();
    }

    //standard status update
    public void updateStatus(String name, int status) {
        ConnectedPeer thisPeer = getMatchingPeer(name);
        if (thisPeer == null) {
            Log.e(TAG, "Couldn't find matching peer! " + name + ", " + status + ", " + mData);
            return;
        }

        Log.d(TAG, "Updating status for " + name + " to " + status + " (" + thisPeer + ")"); // with " + warningMessage);
        Log.d(TAG, "updateStatus: current Status "+thisPeer.getStatus());
        if(thisPeer.hasWarning() || thisPeer.getAlertsList().length()>0 ){
            Log.d(TAG, "updateStatus: student has warning");
        }
        thisPeer.setStatus(status);
        Log.d(TAG, "updateStatus: new status :"+thisPeer.getStatus());
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


    private String lastPromptedID = "";

    protected void showLogoutPrompt(String peerID) {
        lastPromptedID = peerID;
        if (studentLogoutView == null) {
            studentLogoutView = View.inflate(main, R.layout.e__logout_student_popup, null);
            Button ok_btn = studentLogoutView.findViewById(R.id.ok_btn);
            Button back_btn = studentLogoutView.findViewById(R.id.back_btn);

            ok_btn.setOnClickListener(v12 -> {
                Log.d(TAG, "[Logout] Removing student: " + lastPromptedID);

                removeStudent(lastPromptedID);
                refresh();
                main.getNearbyManager().networkAdapter.stopMonitoring(Integer.parseInt(lastPromptedID));
                main.getNearbyManager().networkAdapter.removeClient(Integer.valueOf(lastPromptedID));

                logoutPrompt.dismiss();
                if (main.getConnectedLearnersAdapter().mData.size() > 0) {
                    //main.xrayManager.showXrayView(""); //refresh this view
                } else {
                    main.exitCurrentView();
                }
            });

            back_btn.setOnClickListener(v1 -> logoutPrompt.dismiss());
        }

        if (logoutPrompt == null) {
            logoutPrompt = new AlertDialog.Builder(main)
                    .setView(studentLogoutView)
                    .show();
            logoutPrompt.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    main.hideSystemUI();
                }
            });
        } else {
            logoutPrompt.show();
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
    protected void forceWrapContent(View v) {
        // Start with the provided view
        View current = v;

        // Travel up the tree until fail, modifying the LayoutParams
        do {
            // Get the parent
            ViewParent parent = current.getParent();

            // Check if the parent exists
            if (parent != null) {
                // Get the view
                try {
                    current = (View) parent;
                } catch (ClassCastException e) {
                    // This will happen when at the top view, it cannot be cast to a View
                    break;
                }

                // Modify the layout
                current.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        } while (current.getParent() != null);

        // Request a layout to be re-done
        current.requestLayout();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //if (convertView == null) {
        convertView = mInflater.inflate(R.layout.row_follower, parent, false);
        //}

        TextView studentName = convertView.findViewById(R.id.student_name);
        ImageView selectedIndicator = convertView.findViewById(R.id.selected_indicator);
        ImageView studentIcon = convertView.findViewById(R.id.student_icon);
        ImageView warningIcon = convertView.findViewById(R.id.student_warning_icon);
        ImageView statusIcon = convertView.findViewById(R.id.status_icon);
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
            setLockStatus(peer, statusIcon);


            convertView.setLongClickable(true);
            View finalConvertView = convertView;
            convertView.setOnLongClickListener(v -> {
                lastClickedID = peer.getID();
                final View popupView = View.inflate(main, R.layout.c__student_menu, null);
                TextView disconnect = popupView.findViewById(R.id.remove_learner);
                TextView settings = popupView.findViewById(R.id.student_settings);

                PopupWindow popupWindow = new PopupWindow(
                        popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

                //popupWindow.setBackgroundDrawable(new BitmapDrawable());
                popupWindow.setOutsideTouchable(true);
                popupWindow.setElevation(10);
                popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        //TODO do sth here on dismiss
                        main.hideSystemUI();
                    }
                });

                popupWindow.showAsDropDown(v,200,-100);
                disconnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "[adapter] Removing student: " + lastClickedID);
                        main.getXrayManager().resetClientMaps(lastClickedID); //remove the peer from the HashMaps
                        ArrayList<Integer> selected = new ArrayList<>();
                        selected.add(Integer.valueOf(lastClickedID));
                        main.getNearbyManager().networkAdapter.sendToSelectedClients("", "DISCONNECT", selected);
                        removeStudent(lastClickedID);
                        refresh();
                        finalConvertView.setVisibility(View.GONE);
                        popupWindow.dismiss();
                    }
                });
                settings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BuildAndDisplaySettings(peer);
                        popupWindow.dismiss();
                    }
                });
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
                            Log.d(TAG, "[adapter] Removing student: " + lastClickedID);
                            ArrayList<Integer> selected = new ArrayList<>();
                            selected.add(Integer.valueOf(lastClickedID));
                            main.getNearbyManager().networkAdapter.sendToSelectedClients("", "DISCONNECT", selected);
                            removeStudent(lastClickedID);
                            refresh();
                            disconnectPrompt.dismiss();
                        });

                        back_btn.setOnClickListener(v1 -> disconnectPrompt.dismiss());
                    }

                    if (disconnectPrompt == null) {
                        disconnectPrompt = new AlertDialog.Builder(main)
                                .setView(studentDisconnectedView)
                                .show();
                        disconnectPrompt.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                main.hideSystemUI();
                            }
                        });
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

    private void BuildAndDisplaySettings(ConnectedPeer peer) {
        View Settings = View.inflate(main, R.layout.d__student_settings, null);
        TextView Name = Settings.findViewById(R.id.student_set_name);
        TextView EditName = Settings.findViewById(R.id.student_set_edit_name);
        Switch ViewToggle = Settings.findViewById(R.id.student_set_view_toggle);
        Switch BlockToggle = Settings.findViewById(R.id.student_set_block_toggle);
        TextView Disconnect = Settings.findViewById(R.id.student_set_disconnect);
        Button Close = Settings.findViewById(R.id.student_set_close);

        Name.setText(peer.getDisplayName());
        AlertDialog studentSettingsDialog = new AlertDialog.Builder(main)
                .setView(Settings)
                .show();


        ViewToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    ViewToggle.setText("View Mode ON");
                    ImageViewCompat.setImageTintList(Settings.findViewById(R.id.student_set_view_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_blue)));
                    ((TextView) Settings.findViewById(R.id.student_set_view_text)).setText("*Touch screens diabled");
                    Set<String> student = new ArraySet<>();
                    student.add(peer.getID());
                    if(!BlockToggle.isChecked()){
                        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOCK_TAG, student);
                    }

//                    stream=false;
                }else{
                    ImageViewCompat.setImageTintList(Settings.findViewById(R.id.student_set_view_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_medium_grey)));
                    ((TextView) Settings.findViewById(R.id.student_set_view_text)).setText("*Touch screens enabled");
                    ViewToggle.setText("View Mode OFF");
                    Set<String> student = new ArraySet<>();
                    student.add(peer.getID());
                    if(!BlockToggle.isChecked()) {
                        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.UNLOCK_TAG, student);
                    }else {
                        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.BLACKOUT_TAG, student);
                    }
//                    stream=true;
                }

            }
        });
        BlockToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    BlockToggle.setText("Blocked Mode ON");
                    ImageViewCompat.setImageTintList(Settings.findViewById(R.id.student_set_block_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_blue)));
                    ((TextView) Settings.findViewById(R.id.student_set_block_text)).setText("*Full access disabled");
                    Set<String> student = new ArraySet<>();
                    student.add(peer.getID());
                    main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.BLACKOUT_TAG, student);
//                    stream=false;
                }else{
                    ImageViewCompat.setImageTintList(Settings.findViewById(R.id.student_set_block_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_medium_grey)));
                    ((TextView) Settings.findViewById(R.id.student_set_block_text)).setText("*Normal access enabled");
                    BlockToggle.setText("Blocked Mode OFF");
                    Set<String> student = new ArraySet<>();
                    student.add(peer.getID());
                    if(ViewToggle.isChecked()){
                        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOCK_TAG, student);
                    }else {
                        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.UNLOCK_TAG, student);
                    }
//                    stream=true;
                }
            }
        });
        if(peer.locked){
            ViewToggle.setChecked(true);
        }
        if(peer.blackedOut){
            BlockToggle.setChecked(true);
        }
        Disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "[adapter] Removing student: " + lastClickedID);
                ArrayList<Integer> selected = new ArrayList<>();
                selected.add(Integer.valueOf(lastClickedID));
                main.getNearbyManager().networkAdapter.sendToSelectedClients("", "DISCONNECT", selected);
                removeStudent(lastClickedID);
                refresh();
                studentSettingsDialog.dismiss();
                main.hideSystemUI();
            }
        });
        Close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                studentSettingsDialog.dismiss();
                main.hideSystemUI();
            }
        });
        EditName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BuildAndDisplayNameChange(studentSettingsDialog,peer);
            }
        });
        studentSettingsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                main.hideSystemUI();
            }
        });

    }

    private void BuildAndDisplayNameChange(AlertDialog studentSettingsDialog, ConnectedPeer peer) {
        View NameChange = View.inflate(main, R.layout.e__name_change, null);
        TextView OldName = NameChange.findViewById(R.id.name_change_old);
        EditText NewName = NameChange.findViewById(R.id.name_change_edit);
        Button Confirm = NameChange.findViewById(R.id.name_change_confirm);
        Button Request = NameChange.findViewById(R.id.name_change_request);
        Button Back = NameChange.findViewById(R.id.name_change_back);
        studentSettingsDialog.dismiss();
        AlertDialog studentNameChange = new AlertDialog.Builder(main)
                .setView(NameChange)
                .show();
        OldName.setText(peer.getDisplayName());
        Confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(NewName.getText()==null){
                    return;
                }
                String newName = NewName.getText().toString();
                if(newName.length()>0){
                    studentNameChange.dismiss();
                    //todo send name change
                    Set<String> student = new ArraySet<>();
                    student.add(peer.getID());
                    main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.NAME_CHANGE+newName, student);
                    peer.setName(newName);
                    BuildAndDisplayNameConfirm(OldName.getText().toString(),newName,false);
                    OldName.setText(newName);
                    ((TextView)studentSettingsDialog.findViewById(R.id.student_set_name)).setText(newName);
                    refresh();

                }
            }
        });
        Request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<String> student = new ArraySet<>();
                student.add(peer.getID());
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.NAME_REQUEST, student);
                studentNameChange.dismiss();
                BuildAndDisplayNameConfirm(OldName.getText().toString(),"",true);
            }
        });
        Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                studentNameChange.dismiss();
                studentSettingsDialog.show();
            }
        });
    studentNameChange.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                main.hideSystemUI();
            }
        });
    }
    protected void BuildAndDisplayNameConfirm(String oldName, String newName, boolean request){
        View NameChangedConfirm = View.inflate(main, R.layout.f__name_push_confirm, null);
        AlertDialog studentNameConfirm = new AlertDialog.Builder(main)
                .setView(NameChangedConfirm)
                .show();
        LinearLayout changed = NameChangedConfirm.findViewById(R.id.name_changed_view);
        LinearLayout requested = NameChangedConfirm.findViewById(R.id.name_request_view);
        if(request){
            changed.setVisibility(View.GONE);
            requested.setVisibility(View.VISIBLE);
        }else{
            changed.setVisibility(View.VISIBLE);
            requested.setVisibility(View.GONE);
            TextView nameChangedText = NameChangedConfirm.findViewById(R.id.name_changed_text);
            nameChangedText.setText(oldName+" name was changed to "+newName);
        }
        Button confirm = NameChangedConfirm.findViewById(R.id.name_changed_confirm);
        confirm.setOnClickListener(v -> studentNameConfirm.dismiss());
        studentNameConfirm.setOnDismissListener(dialog -> main.hideSystemUI());

    }

    protected void moveToFrontOfList(ConnectedPeer peer) {
        //only reorder if we haven't already
        if (reorderByStatus && peer.getPriority() != ConnectedPeer.PRIORITY_TOP) {
            peer.setPriority(ConnectedPeer.PRIORITY_TOP);
            mData.remove(peer);
            mData.add(0, peer);
        }
    }

    /**
     * Set the alert to be attached to an individual student's icon.
     * @param peer A ConnectedPeer that is getting its alert updated.
     * @param statusIcon An ImageView that represents the type of alert.
     */
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

            case ConnectedPeer.STATUS_FILE_TRANSFER:
                statusIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_transfering, null));
                break;

            default:
                if (peer.hasWarning()) {
                    Log.d(TAG, peer.getDisplayName() + " has warning: " + peer.getAlertsList());
                    statusIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_warning, null));
                }else{
                    statusIcon.setImageDrawable(null);
                }
//                else {
//                    setLockStatus(peer, statusIcon);
//                }
                break;
        }
    }

    private void setLockStatus(ConnectedPeer peer, ImageView statusIcon) {
        //Log.d(TAG, "Actually, is there a warning? "+peer.hasWarning()+", "+peer.isBlackedOut()+", "+peer.isLocked());
        //sometimes multiple status are possible.
        if ( peer.isBlackedOut()) {
            statusIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_blocked, null));

        } else if ( peer.isLocked()) {
            statusIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.view_learneralert, null));

        } else if (!peer.isLocked() &&!peer.isBlackedOut() ) {
            statusIcon.setImageDrawable(null);
        }
    }

    public void selectPeer(String id, boolean selected) {
        ConnectedPeer peer = getMatchingPeer(id);
        peer.setSelected(selected);
        refresh();
    }

    public int getSelectedCount() {
        int count = 0;
        for (ConnectedPeer peer : mData) {
            if (peer.isSelected()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check through the connected learners to see if anyone is currently selected.
     * @return A boolean representing if a learner is selected.
     */
    public boolean someoneIsSelected() {
        for (ConnectedPeer peer : mData) {
            if (peer.isSelected()) {
                return true;
            }
        }
        return false;
    }
}