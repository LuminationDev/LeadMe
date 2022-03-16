package com.lumination.leadme.connections;

import java.util.ArrayList;

public class Client {
    public String name;
    public int ID;
    public int pingCycle = 1;
    public ArrayList<Msg> messageQueue = new ArrayList<>();//fifo structure
}
