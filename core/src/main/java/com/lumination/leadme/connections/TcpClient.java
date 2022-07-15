package com.lumination.leadme.connections;

import android.util.Log;

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
    public String IpAddress;
    public int port = 54320;
    public String ID;

    public TcpClient(String clientAddress, String clientID) {
        TAG += clientID; //ensures logs are identifiable
        IpAddress = clientAddress;
        ID = clientID;
    }

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
//                        setLocalName("No_Name_Yet");
                    } else {
                        setLocalName(inputList.get(1));
                    }
                    break;
                case "PRINT":
                    Log.d(TAG, "inputHandler: " + inputList.get(1));
                    break;
                case "ACTION":
                    NetworkManager.executorService.submit(() -> NetworkManager.updateParent(inputList.get(1),ID,"ACTION"));
                    break;
                case "PING":
                    Log.d(TAG, "inputReceived: ping messages are purposely ignored");
                    break;
                case "IMAGE":
                    NetworkManager.executorService.submit(() -> NetworkManager.updateParent(inputList.get(1),ID,"IMAGE"));
                    break;
                case "DISCONNECT":
                    NetworkManager.executorService.submit(() -> NetworkManager.updateParent(Name, ID, "LOST"));
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
        NetworkManager.executorService.submit(() -> NetworkManager.updateParent(name+":"+IpAddress.toString(),ID,"NAME"));
    }
}
