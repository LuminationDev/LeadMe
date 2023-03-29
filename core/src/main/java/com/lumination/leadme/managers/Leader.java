package com.lumination.leadme.managers;

public class Leader {
    private String name;
    private String id;
    private String ipAddress;
    public Leader(String name, String id, String ipAddress) {
        this.name = name;
        this.id = id;
        this.ipAddress = ipAddress;
    }
    public String getDisplayName() {
        return name;
    }
    public String getID() {
        return id;
    }
    public String getIpAddress() {
        return ipAddress;
    }
}
