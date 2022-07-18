package com.lumination.leadme.models;

import android.util.Log;
import com.lumination.leadme.managers.NetworkManager;

import java.util.Arrays;
import java.util.List;

public class Learner {
    private String TAG = "Learner: ";

    private String Name;
    public String IpAddress;
    public String ID;

    public Learner(String clientAddress, String clientID) {
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
