package com.lumination.leadme;

import android.graphics.drawable.Drawable;

public class ConnectedPeer {

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_ERROR_DISCONNECT = 1;
    public static final int STATUS_OFF_TASK_ALERT = 6;
    public static final int STATUS_INSTALLING = 2;
    public static final int STATUS_BLACKOUT = 3;
    public static final int STATUS_LOCK = 4;
    public static final int STATUS_UNLOCK = 5;

    private String buddyName;
    private String id;
    private boolean selected = false;
    private Drawable icon = null;
    private Drawable statusIcon = null;

    private int status = ConnectedPeer.STATUS_SUCCESS;
    private int previousStatus = -1;
    private boolean locked = true;
    private boolean blackedOut = false;

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

    public void setStatus(int newStatus) {
        previousStatus = status;
        status = newStatus;

        if (status == STATUS_BLACKOUT) {
            blackedOut = true;
            locked = false;
        } else if (status == STATUS_LOCK) {
            locked = true;
            blackedOut = false;
        } else if (status == STATUS_UNLOCK) {
            locked = false;
            blackedOut = false;
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

            case STATUS_ERROR_DISCONNECT:
                return "disconnected";

            case STATUS_OFF_TASK_ALERT:
                return "off-task";

            case STATUS_INSTALLING:
                return "installing";

            case STATUS_SUCCESS:
                return "success";
        }
        ;

        return "unknown";
    }

    public int getStatus() {
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
