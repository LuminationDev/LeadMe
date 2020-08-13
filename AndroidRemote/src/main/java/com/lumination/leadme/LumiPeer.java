package com.lumination.leadme;

public class LumiPeer {
    private String buddyName;
    private String id;
    private String status = "Connected";

    public LumiPeer(NearbyPeersManager.Endpoint endpoint){
        if(endpoint != null) {
            buddyName = endpoint.getName();
            id = endpoint.getId();
        }
    }

    public void setStatus(String status){
        this.status = status;
    }

    public  String getStatus(){
        return status;
    }

    public void setBuddyName(String newName) {
        if (newName != null && newName.length() > 0) {
            this.buddyName = newName;
        }
    }

    public boolean hasDisplayName(){
        return !(buddyName == null || buddyName.length() == 0);
    }

    public String getID(){
        if(id == null){
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
