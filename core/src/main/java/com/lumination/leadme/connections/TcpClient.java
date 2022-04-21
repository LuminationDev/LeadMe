package com.lumination.leadme.connections;

import android.util.Log;

import com.lumination.leadme.managers.NSDManager;
import com.lumination.leadme.managers.NetworkManager;
import com.lumination.leadme.services.NetworkService;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class to handle the connections to individual students, runs three separate threads to send
 * messages, check connection and handle input.
 */
public class TcpClient extends Thread {
    private String TAG = "TcpClient: ";

    private String Name;
    public InetAddress IpAddress;
    public int port = 54320;
    public int ID;

    ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(3);

    public TcpClient(InetAddress clientAddress, int clientID) {
        TAG += clientID; //ensures logs are identifiable
        IpAddress = clientAddress;
        ID = clientID;

        scheduledExecutor.scheduleAtFixedRate(ConnectionCheck,300,10000, TimeUnit.MILLISECONDS);
    }

    /**
     * Responsible for checking the connection between the Leader and the Leaner associated with
     * this socket connection. Sends a ping every set time period if no other messages are being
     * sent.
     */
    Runnable ConnectionCheck = () -> {
        if(NetworkService.isRunning) {
            Log.d(TAG, "Sending Ping to: " + ID);
            NetworkService.sendToClient(ID, String.valueOf(ID), "PING");
        } else {
            scheduledExecutor.shutdown();
        }
    };

    /**
     * Responsible for handling incoming messages from the learner socket connection. Passes the
     * message of to the inputReceived function to determine an action.
     */
    public void inputReceived(String input) {
        if(input==null || !input.contains(",")){
            return;
        }
        Log.d(TAG, "inputHandler: "+ input);
        List<String> inputList = Arrays.asList(input.split(","));
        if(inputList.size()>1) {
            switch (inputList.get(0)) {
                case "NAME":
                    if (inputList.get(1).length() == 0) {
                        setLocalName("No_Name_Yet");
                    } else {
                        setLocalName(inputList.get(1));
                    }
                    break;
                case "PRINT":
                    Log.d(TAG, "inputHandler: " + inputList.get(1));
                    break;
                case "ACTION":
                    NSDManager.executorService.submit(() -> NetworkManager.updateParent(inputList.get(1),ID,"ACTION"));
                    break;
                case "PING":
                    Log.d(TAG, "inputReceived: ping messages are purposely ignored");
                    break;
                case "IMAGE":
                    NSDManager.executorService.submit(() -> NetworkManager.updateParent(inputList.get(1),ID,"IMAGE"));
                    break;
                case "DISCONNECT":
                    NSDManager.executorService.submit(() -> NetworkManager.updateParent(Name, ID, "LOST"));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Updates network adapter with name.
     * @param name A string representing the name of the student.
     */
    public void setLocalName(String name){
        Name=name;
        NSDManager.executorService.submit(() -> NetworkManager.updateParent(name+":"+IpAddress.toString(),ID,"NAME"));
    }

    public void shutdownTCP() {
        scheduledExecutor.shutdown();
    }
}
