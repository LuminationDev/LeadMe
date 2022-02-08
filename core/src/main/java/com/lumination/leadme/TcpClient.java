package com.lumination.leadme;

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
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TcpClient extends Thread{
    public InetAddress IpAdress;
    public int port;
    public int ID;
    String TAG = "TcpClient: ";
    Socket client;
    String Name;

    NetworkAdapter parent;

    TcpClient tcp;
    boolean runAgain=true;
    boolean checkIn = true;
    ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(3);
    Runnable tcpRunner = () -> {
        if (client.isConnected() && !client.isClosed()) {
            for (int i = 0; i < parent.currentClients.size(); i++) {
                client Client = parent.currentClients.get(i);
                if (ID == Client.ID) {
                    while (Client.messageQueue.size() > 0) {
                        Log.d(TAG, "sending: " + Client.messageQueue.get(0).message + " " + Client.messageQueue.get(0).type);
                        sendToClient(Client.messageQueue.get(0).message, Client.messageQueue.get(0).type);
                        parent.currentClients.get(i).messageQueue.remove(0);
                        try {
                            Thread.currentThread().sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
            //Log.d(TAG, "run: checking for messages");
        }
    };
    Runnable ConnectionCheck = () -> {
        try {
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            out.println("PING,"+ID); //set to be ignored at the client end, tcp will tell us if they have disconnected
            if (out.checkError()){
                Log.e(TAG, "ERROR writing data to socket student has disconnected");
                parent.executorService.submit(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      parent.updateParent(Name, ID, "LOST");
                                                  }
                                              });

                scheduledExecutor.shutdown();
                runAgain=false;
            }
        } catch (IOException e) {
            Log.d(TAG, "checkConnection: failed");
            e.printStackTrace();
        }
    };
    Runnable inputRun = () -> {
        while(checkIn) {
            try {
                InputStreamReader inreader = new InputStreamReader(client.getInputStream());
                BufferedReader in = new BufferedReader(inreader);
                String Input = in.readLine();

                inputRecieved(Input);
                if (Input == null) {
                    checkIn = false;
                }
            } catch (IOException e) {
                checkIn = false;
                e.printStackTrace();
            }
        }
    };

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
        client = clientSocket;
        tcp = this;

        scheduledExecutor.scheduleAtFixedRate(tcpRunner,0,1500, TimeUnit.MILLISECONDS);

        scheduledExecutor.scheduleAtFixedRate(ConnectionCheck,300,2000, TimeUnit.MILLISECONDS);

        //new Thread(inputRun).start();
        scheduledExecutor.scheduleAtFixedRate(inputRun,0,1000, TimeUnit.MILLISECONDS);
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
                case "IMAGE":
                    parent.executorService.submit(() -> parent.updateParent(inputList.get(1),ID,"IMAGE"));
                    break;
                default:
                    break;
            }
        }
    }

    private void inputActionHandler(String action) {
        //not sure why student would send action but implemented anyway
        Log.d(TAG, "inputActionHandler: Action recieved: "+action);
        parent.executorService.submit(new Runnable() {
            @Override
            public void run() {
                parent.updateParent(action,ID,"ACTION");
            }
        });

    }

    //updates parent with name
    public void setLocalName(String name){
        Name=name;
        parent.executorService.submit(new Runnable() {
            @Override
            public void run() {
                parent.updateParent(name+":"+IpAdress.toString(),ID,"NAME");
            }
        });

    }

    void sendToClient(String message, String type){
        Log.d(TAG, "sendToClient: "+message+" Type: "+type);

       // simply sends message to the client attached to this process
        scheduledExecutor.schedule(new Runnable() {
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
                    //checkConnection();
                    e.printStackTrace();
                }
                if(type=="DISCONNECT"){
                    try {
                        client.close();
                        runAgain=false;
                        checkIn=false;
                        scheduledExecutor.shutdown();
                        Log.d(TAG, "sendToClient: client socket closed and student disconnected");
                        parent.executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                parent.updateParent("disconnect complete",ID,"DISCONNECT");
                            }
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
        },0L,TimeUnit.MILLISECONDS);

        //executed after sending message so the student is notified that it is being forcefully disconnected.

    }
}
