package com.lumination.leadme;

import android.graphics.drawable.Drawable;

public class LumiPeer {
    private String buddyName;
    private String id;
    private String status = "Connected";
    private boolean selected = false;
    private int locked = LumiPeer.LOCKED;
    private Drawable icon = null;
    private boolean warning = false;

    public static final int UNLOCKED = 0;
    public static final int LOCKED = 1;
    public static final int BLACKOUT = 2;


    public LumiPeer(NearbyPeersManager.Endpoint endpoint) {
        if (endpoint != null) {
            buddyName = endpoint.getName();
            id = endpoint.getId();
        }
    }

    public boolean hasCurrentWarning() {
        return warning;
    }

    public void setWarningStatus(boolean newWarning) {
        warning = newWarning;
    }

    public int getLockStatus() {
        return locked;
    }

    public void setLocked(int locked) {
        this.locked = locked;
    }

    public boolean isSelected() {
        return selected;
    }

    public void toggleSelected() {
        selected = !selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
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
