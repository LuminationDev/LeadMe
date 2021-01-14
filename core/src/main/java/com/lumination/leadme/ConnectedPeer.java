package com.lumination.leadme;

import android.graphics.drawable.Drawable;
import android.util.Log;

public class ConnectedPeer {

    private final String TAG = "ConnectedPeer";

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_ERROR = 1;
    public static final int STATUS_INSTALLING = 2;
    public static final int STATUS_BLACKOUT = 3;
    public static final int STATUS_LOCK = 4;
    public static final int STATUS_UNLOCK = 5;
    public static final int STATUS_WARNING = 6;

    public static final int PRIORITY_TOP = 2;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_STD = 0;
    private int priority = 0;

    private String buddyName;
    private String id;
    private boolean selected = false;
    private Drawable icon = null;

    private int status = ConnectedPeer.STATUS_SUCCESS;
    private int previousStatus = -1;
    private boolean locked = true;
    private boolean blackedOut = false;

    //assume the best, update if the worst
    private boolean onTask = true;
    private boolean accessEnabled = true;
    private boolean overlayEnabled = true;
    private boolean internetEnabled = true;
    private boolean lastAppLaunchSucceeded = true;

    private NearbyPeersManager.Endpoint myEndpoint;

    //FOR TESTING ONLY//
    public ConnectedPeer(String name, String id) {
        this.buddyName = name;
        this.id = id;
    }

    //CORRECT CONSTRUCTOR
    public ConnectedPeer(NearbyPeersManager.Endpoint endpoint) {
        if (endpoint != null) {
            myEndpoint = endpoint;
            buddyName = endpoint.getName();
            id = endpoint.getId();
        }
    }

    protected NearbyPeersManager.Endpoint getMyEndpoint() {
        return myEndpoint;
    }

    public void setWarning(String warning, boolean success) {
        switch (warning) {
            case LeadMeMain.AUTO_INSTALL:
                lastAppLaunchSucceeded = success;
                break;
            case LeadMeMain.STUDENT_NO_OVERLAY:
                overlayEnabled = success;
                break;
            case LeadMeMain.STUDENT_NO_ACCESSIBILITY:
                accessEnabled = success;
                break;
            case LeadMeMain.STUDENT_NO_INTERNET:
                internetEnabled = success;
                break;
            case LeadMeMain.STUDENT_OFF_TASK_ALERT:
                onTask = success;
                break;
        }
    }

    public void setStatus(int newStatus) {
        Log.d(TAG, "Setting status to " + newStatus + " in ConnectedPeer " + id);
        /*if(newStatus == STATUS_SUCCESS){
            //changes nothing!

        } else */
        if (newStatus == STATUS_BLACKOUT) {
            blackedOut = true;
            locked = false;

        } else if (newStatus == STATUS_LOCK) {
            locked = true;
            blackedOut = false;

        } else if (newStatus == STATUS_UNLOCK) {
            locked = false;
            blackedOut = false;

        } else {
            //warning status
            Log.d(TAG, "Actually updating status! " + newStatus);
            previousStatus = status;
            status = newStatus;
        }
    }

    public int getPreviousStatus() {
        return previousStatus;
    }

    //helpful for debugging
    public static String statusToString(int status) {
        switch (status) {
            case STATUS_LOCK:
                return "locked";

            case STATUS_UNLOCK:
                return "unlocked";

            case STATUS_BLACKOUT:
                return "screen blocked";

            case STATUS_ERROR:
                return "disconnected";

            case STATUS_WARNING:
                return "warning";

            case STATUS_INSTALLING:
                return "installing";

            case STATUS_SUCCESS:
                return "success";
        }

        return "unknown";
    }

    public boolean hasWarning() {
        return !(onTask && accessEnabled && overlayEnabled && lastAppLaunchSucceeded && internetEnabled);
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public int getStatus() {
        if (status == STATUS_WARNING && !hasWarning()) {
            //all warnings have been removed, update status
            status = STATUS_SUCCESS;
        }
        return status;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isBlackedOut() {
        return blackedOut;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setIcon(Drawable drawable) {
        icon = drawable;
    }


    public Drawable getIcon() {
        return icon;
    }

    public boolean hasDisplayName() {
        return !(buddyName == null || buddyName.length() == 0);
    }

    public String getID() {
        if (id == null) {
            return "";
        }
        return id;
    }

    public String getDisplayName() {
        if (hasDisplayName()) {
            return buddyName;
        }
        return "Anonymous";
    }

}
