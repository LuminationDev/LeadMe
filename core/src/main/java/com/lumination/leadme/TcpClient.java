package com.lumination.leadme;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TcpClient extends Thread {
    public InetAddress IpAdress;
    public int port;
    public int ID;
    String TAG = "TcpClient: ";
    Socket client;
    String Name;
    int counter = 0;

    NetworkAdapter parent;

    TcpClient tcp;

    public TcpClient(Socket clientSocket, NetworkAdapter netContext, int clientID) {

        TAG += clientID; //ensures logs are identifiable
        IpAdress = clientSocket.getInetAddress();
        port = clientSocket.getPort();
        try {
            clientSocket.setKeepAlive(true);
        } catch (SocketException e) {
            Log.d(TAG, "TcpClient: keep alive failed");
            e.printStackTrace();
        }
        ID = clientID;
        parent = netContext;
        client=clientSocket;
        tcp=this;


        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean runAgain=true;
                    if(client.isConnected()) {
                        inputHandler();//checks if any input
                        checkConnection();
                        Iterator<client> iterator = parent.currentClients.iterator();
                        while(iterator.hasNext()){
                            client Client = iterator.next();
                            if(ID==Client.ID){
                                while(Client.messageQueue.size()>0){
                                    if(Client.messageQueue.get(0).type.equals("DISCONNECT")){
                                        runAgain=false;
                                    }
                                    Log.d(TAG, "sending: "+Client.messageQueue.get(0).message+" "+Client.messageQueue.get(0).type);
                                    sendToClient(Client.messageQueue.get(0).message,Client.messageQueue.get(0).type);
                                    Client.messageQueue.remove(0);
                                }
                            }
                            Log.d(TAG, "run: checking for messages");
                        }
                        if(runAgain) {
                            new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
                        }else{
                            Log.d(TAG, "run: the runnable has been interrupted");
                        }
                    }
            }
        }, 1000);

    }

    private void checkConnection() {
        Thread thread = new Thread() {//no network ops on main thread
            @Override
            public void run() {
        try {
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            out.println("PING,connection check"); //set to be ignored at the client end, tcp will tell us if they have disconnected
            if (out.checkError()){
                System.out.println("ERROR writing data to socket student has disconnected");
                parent.updateParent(Name,ID,"LOST");
            }
        } catch (IOException e) {
            Log.d(TAG, "checkConnection: failed");
            e.printStackTrace();
        }
            }
        };
        thread.start();
    }

    boolean checkIn = true;
    public void inputHandler() {
        //new thread as input hangs waiting for a response
        Thread thread = new Thread() {//no network ops on main thread
            @Override
            public void run() {
                while(checkIn) {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        String Input = in.readLine();
                        inputRecieved(Input);
                        if(Input==null){
                            checkIn=false;
                        }
                    } catch (IOException e) {
                        checkIn=false;
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

    private void inputRecieved(String input) {
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
                    inputActionHandler(inputList.get(1));
                    break;
                case "PING":
                    Log.d(TAG, "inputRecieved: ping messages are purposely ignored");
                    break;
                default:
                    break;
            }
        }
    }

    private void inputActionHandler(String action) {
        //not sure why student would send action but implemented anyway
        Log.d(TAG, "inputActionHandler: Action recieved");
        parent.updateParent(action,ID,"ACTION");
    }

    //updates parent with name
    public void setLocalName(String name){
        Name=name;
        parent.updateParent(name+":"+IpAdress.toString(),ID,"NAME");
    }
    void sendToClient(String message, String type){
        Log.d(TAG, "sendToClient: "+message+" Type: "+type);

       // simply sends message to the client attached to this process
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    PrintWriter out =   new PrintWriter(
                                        new BufferedWriter(
                                        new OutputStreamWriter(client.getOutputStream())), true);
                    out.println(type + "," + message);
                    out.flush();
                } catch (IOException e) {
                    Log.d(TAG, "sendToClient: failed to write message to client, checking if there is a connection");
                    checkConnection();
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        try {
            thread.join(); //output stream shouldn't hang so thread can be joined
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //executed after sending message so the student is notified that it is being forcefully disconnected.
        if(type=="DISCONNECT"){
            try {
                client.close();
                Log.d(TAG, "sendToClient: client socket closed and student disconnected");
                parent.updateParent("disconnect complete",ID,"DISCONNECT");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
    }
}
